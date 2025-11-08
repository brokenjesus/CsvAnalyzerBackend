package by.lupach.backend.services;

import by.lupach.backend.dtos.FileQueueMessageDTO;
import by.lupach.backend.dtos.ProgressMessageDTO;
import by.lupach.backend.entities.*;
import by.lupach.backend.repositories.*;
import jakarta.annotation.PreDestroy;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.data.redis.RedisSystemException;
import org.springframework.messaging.simp.SimpMessagingTemplate;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.io.BufferedReader;
import java.nio.file.Files;
import java.nio.file.Path;
import java.time.LocalDateTime;
import java.util.HashSet;
import java.util.Set;
import java.util.UUID;
import java.util.concurrent.*;

@Slf4j
@Service
@RequiredArgsConstructor
public class StreamingFileProcessingService {

    private final FileEntityRepository fileRepository;
    private final AnalysisResultRepository analysisResultRepository;
    private final AnalysisStatisticsRepository statisticsRepository;
    private final AnalysisStatusService statusService;
    private final SimpMessagingTemplate messagingTemplate;
    private final HistoryService historyService;

    private final Set<UUID> activeProcesses = ConcurrentHashMap.newKeySet();
    private final ConcurrentHashMap<UUID, Future<?>> processingFutures = new ConcurrentHashMap<>();
    private final ScheduledExecutorService scheduler = Executors.newScheduledThreadPool(10);
    private final ExecutorService processingExecutor = Executors.newCachedThreadPool();

    public void processFile(FileQueueMessageDTO message) {
        UUID fileId = message.fileId();
        String filePath = message.filePath();

        if (!activeProcesses.add(fileId)) {
            log.warn("File {} is already being processed", fileId);
            return;
        }

        // Запускаем задачу с возможностью отмены
        Future<?> future = processingExecutor.submit(() -> {
            Thread currentThread = Thread.currentThread();
            String originalName = currentThread.getName();
            currentThread.setName("FileProcessing-" + fileId);

            try {
                processFileInternal(fileId, filePath);
            } finally {
                currentThread.setName(originalName);
            }
        });

        processingFutures.put(fileId, future);
    }

    private void processFileInternal(UUID fileId, String filePath) {
        ScheduledFuture<?> cancellationMonitor = null;

        try {
            updateStatusAndNotify(fileId, ProcessingStatus.PROCESSING, 0, "Starting file processing");

            FileEntity fileEntity = fileRepository.findById(fileId)
                    .orElseThrow(() -> new RuntimeException("File not found: " + fileId));

            fileEntity.setStatus(ProcessingStatus.PROCESSING);
            fileRepository.save(fileEntity);

            AnalysisResult analysisResult = createAnalysisResult(fileEntity);
            AnalysisStatistics statistics = initializeStatistics(analysisResult);

            // Запускаем мониторинг отмены в отдельном шедулере
            cancellationMonitor = scheduler.scheduleAtFixedRate(() -> {
                if (statusService.isCancellationRequested(fileId)) {
                    Thread processingThread = Thread.currentThread();
                    processingThread.interrupt();
                }
            }, 100, 100, TimeUnit.MILLISECONDS);

            // Потоковая обработка файла с поддержкой прерывания
            processFileWithStreaming(filePath, fileId, statistics);

            // Завершение обработки
            completeProcessing(fileEntity, analysisResult, statistics, fileId);
            fileEntity.setStatus(ProcessingStatus.COMPLETED);
            fileRepository.save(fileEntity);

        } catch (InterruptedException e) {
            // Обработка прерывания по отмене
            log.info("Processing interrupted for file: {}", fileId);
            handleCancellation(fileId);
            Thread.currentThread().interrupt(); // Восстанавливаем флаг прерывания
        } catch (Exception e) {
            // Проверяем, является ли исключение следствием прерывания
            if (isCancellationRelatedException(e)) {
                log.info("Processing cancelled for file: {}", fileId);
                handleCancellation(fileId);
            } else {
                handleProcessingFailure(fileId, e);
            }
        } finally {
            if (cancellationMonitor != null) {
                cancellationMonitor.cancel(true);
            }
            cleanupProcessing(fileId);
        }
    }

    private void processFileWithStreaming(String filePath, UUID fileId, AnalysisStatistics statistics)
            throws Exception {
        Path path = Path.of(filePath);
        long fileSize = Files.size(path);
        long bytesProcessed = 0;

        try (BufferedReader reader = Files.newBufferedReader(path)) {
            String line;
            Set<Double> uniqueValues = new HashSet<>();

            while ((line = reader.readLine()) != null) {
                bytesProcessed += line.getBytes().length + 1; // +1 for newline
                int progress = (int) ((bytesProcessed * 100) / fileSize);

                processLine(line, statistics, uniqueValues);

                // Обновление прогресса (реже для производительности)
                if (bytesProcessed % 50000 == 0 || progress % 5 == 0) {
                    try {
                        updateStatusAndNotify(fileId, ProcessingStatus.PROCESSING, progress,
                                String.format("Processed %d bytes of %d", bytesProcessed, fileSize));
                    } catch (RedisSystemException e) {
                        // Игнорируем Redis исключения при прерывании
                        if (!Thread.currentThread().isInterrupted()) {
                            throw e;
                        }
                    }

                    // Проверяем флаг прерывания вместо явной проверки отмены
                    if (Thread.currentThread().isInterrupted()) {
                        throw new InterruptedException("Processing interrupted by cancellation request");
                    }
                }
            }

            calculateFinalStatistics(statistics, uniqueValues);
        }
    }

    private void processLine(String line, AnalysisStatistics statistics, Set<Double> uniqueValues) {
        statistics.setTotalRecords(statistics.getTotalRecords() + 1);

        try {
            String[] parts = line.split(",", 2);
            if (parts.length != 2) {
                statistics.setSkippedRecords(statistics.getSkippedRecords() + 1);
                return;
            }

            String valueStr = parts[1].trim();
            if (valueStr.isEmpty()) {
                statistics.setSkippedRecords(statistics.getSkippedRecords() + 1);
                return;
            }

            double value = Double.parseDouble(valueStr);
            updateStatistics(statistics, value, uniqueValues);
            statistics.setProcessedRecords(statistics.getProcessedRecords() + 1);

        } catch (NumberFormatException e) {
            statistics.setSkippedRecords(statistics.getSkippedRecords() + 1);
        } catch (Exception e) {
            log.debug("Error processing line: {}", line, e);
            statistics.setSkippedRecords(statistics.getSkippedRecords() + 1);
        }
    }

    private void updateStatistics(AnalysisStatistics statistics, double value, Set<Double> uniqueValues) {
        statistics.setMinValue(Math.min(statistics.getMinValue(), value));
        statistics.setMaxValue(Math.max(statistics.getMaxValue(), value));
        statistics.setSumValue(statistics.getSumValue() + value);
        statistics.setSumOfSquares(statistics.getSumOfSquares() + (value * value));
        uniqueValues.add(value);
    }

    private void calculateFinalStatistics(AnalysisStatistics statistics, Set<Double> uniqueValues) {
        long processed = statistics.getProcessedRecords();

        if (processed > 0) {
            double mean = statistics.getSumValue() / processed;
            statistics.setMeanValue(mean);

            double variance = (statistics.getSumOfSquares() / processed) - (mean * mean);
            statistics.setStdDeviation(Math.sqrt(Math.max(variance, 0)));
        }

        if (statistics.getMinValue() == Double.MAX_VALUE) statistics.setMinValue(0.0);
        if (statistics.getMaxValue() == Double.MIN_VALUE) statistics.setMaxValue(0.0);

        statistics.setUniqueValuesCount((long) uniqueValues.size());
    }

    private void updateStatusAndNotify(UUID fileId, ProcessingStatus status, int progress, String message) {
        try {
            statusService.setStatus(fileId, status);
            statusService.setProgress(fileId, progress);

            ProgressMessageDTO progressMessage = new ProgressMessageDTO(
                    fileId, status, progress, message, LocalDateTime.now()
            );

            statusService.saveProgressMessage(progressMessage);
            messagingTemplate.convertAndSend("/topic/progress/" + fileId, progressMessage);
        } catch (RedisSystemException e) {
            // Логируем, но не прерываем обработку при ошибках Redis
            if (!Thread.currentThread().isInterrupted()) {
                log.warn("Redis error during status update for file {}: {}", fileId, e.getMessage());
            }
        }
    }

    private boolean isCancellationRelatedException(Exception e) {
        // Проверяем, связано ли исключение с прерыванием/отменой
        if (e instanceof RedisSystemException) {
            Throwable cause = e.getCause();
            if (cause != null && cause.getMessage() != null) {
                return cause.getMessage().contains("interrupted") ||
                        cause.getMessage().contains("Command interrupted");
            }
        }
        return e instanceof InterruptedException ||
                (e.getCause() instanceof InterruptedException);
    }

    private void handleCancellation(UUID fileId) {
        log.info("Handling cancellation for file: {}", fileId);
        try {
            updateFileStatus(fileId, ProcessingStatus.CANCELLED);
            updateStatusAndNotify(fileId, ProcessingStatus.CANCELLED, 0, "Processing cancelled by user");
        } catch (Exception e) {
            log.warn("Error during cancellation handling for file {}: {}", fileId, e.getMessage());
        }
    }

    private void handleProcessingFailure(UUID fileId, Exception e) {
        log.error("Processing failed for file: {}", fileId, e);
        try {
            updateFileStatus(fileId, ProcessingStatus.FAILED);
            updateStatusAndNotify(fileId, ProcessingStatus.FAILED, 0, "Error: " + e.getMessage());
        } catch (Exception ex) {
            log.error("Error during failure handling for file {}: {}", fileId, ex.getMessage());
        }
    }

    private void cleanupProcessing(UUID fileId) {
        activeProcesses.remove(fileId);
        processingFutures.remove(fileId);
        try {
            statusService.cleanup(fileId);
        } catch (Exception e) {
            log.warn("Error during cleanup for file {}: {}", fileId, e.getMessage());
        }
    }

    private void updateFileStatus(UUID fileId, ProcessingStatus status) {
        fileRepository.findById(fileId).ifPresent(file -> {
            file.setStatus(status);
            fileRepository.save(file);
        });
    }

    private AnalysisResult createAnalysisResult(FileEntity fileEntity) {
        AnalysisResult result = new AnalysisResult();
        result.setFile(fileEntity);
        result.setProcessStartTime(LocalDateTime.now());
        return result;
    }

    private AnalysisStatistics initializeStatistics(AnalysisResult analysisResult) {
        AnalysisStatistics statistics = new AnalysisStatistics();
        statistics.setAnalysisResult(analysisResult);
        statistics.setTotalRecords(0L);
        statistics.setProcessedRecords(0L);
        statistics.setSkippedRecords(0L);
        statistics.setMinValue(Double.MAX_VALUE);
        statistics.setMaxValue(Double.MIN_VALUE);
        statistics.setMeanValue(0.0);
        statistics.setStdDeviation(0.0);
        statistics.setUniqueValuesCount(0L);
        statistics.setSumValue(0.0);
        statistics.setSumOfSquares(0.0);
        return statistics;
    }

    private void completeProcessing(FileEntity fileEntity, AnalysisResult analysisResult,
                                    AnalysisStatistics statistics, UUID fileId) {
        analysisResult.setProcessEndTime(LocalDateTime.now());
        analysisResult.setStatistics(statistics);

        analysisResultRepository.save(analysisResult);
        statisticsRepository.save(statistics);

        fileEntity.setStatus(ProcessingStatus.COMPLETED);
        fileRepository.save(fileEntity);

        updateStatusAndNotify(fileId, ProcessingStatus.COMPLETED, 100, "File processing completed");
        historyService.cleanupOldRecords();
    }

    public void cancelProcessing(UUID fileId) {
        statusService.requestCancellation(fileId);

        // Принудительно прерываем выполнение
        Future<?> future = processingFutures.get(fileId);
        if (future != null && !future.isDone()) {
            boolean cancelled = future.cancel(true); // true - прерывать если запущено
            if (cancelled) {
                log.info("Processing cancelled for file: {}", fileId);
            } else {
                log.warn("Failed to cancel processing for file: {}", fileId);
            }
        } else {
            log.info("No active processing found for file: {}", fileId);
        }
    }

    public boolean isProcessing(UUID fileId) {
        return activeProcesses.contains(fileId);
    }

    @PreDestroy
    public void shutdown() {
        processingExecutor.shutdownNow();
        scheduler.shutdownNow();

        try {
            if (!processingExecutor.awaitTermination(5, TimeUnit.SECONDS)) {
                log.warn("Processing executor did not terminate in time");
            }
            if (!scheduler.awaitTermination(5, TimeUnit.SECONDS)) {
                log.warn("Scheduler did not terminate in time");
            }
        } catch (InterruptedException e) {
            Thread.currentThread().interrupt();
            log.warn("Shutdown interrupted", e);
        }
    }
}