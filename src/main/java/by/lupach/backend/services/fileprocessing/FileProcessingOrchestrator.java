package by.lupach.backend.services.fileprocessing;

import by.lupach.backend.dtos.FileQueueMessageDTO;
import by.lupach.backend.entities.*;
import by.lupach.backend.repositories.*;
import by.lupach.backend.services.redis.AnalysisStatusFacade;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;

import java.time.LocalDateTime;
import java.util.UUID;
import java.util.concurrent.*;

@Slf4j
@Service
@RequiredArgsConstructor
public class FileProcessingOrchestrator {

    private final FileEntityRepository fileRepo;
    private final AnalysisResultRepository resultRepo;
    private final AnalysisStatisticsRepository statsRepo;
    private final AnalysisStatusFacade statusFacade;
    private final StreamingFileProcessor processor;
    private final ProgressNotifier notifier;

    private final ExecutorService exec = Executors.newCachedThreadPool();

    private final ConcurrentHashMap<UUID, Future<?>> tasks = new ConcurrentHashMap<>();

    public void startProcessing(FileQueueMessageDTO msg) {
        UUID id = msg.fileId();
        if (tasks.containsKey(id)) return;

        Future<?> f = exec.submit(() -> handleProcessing(msg));
        tasks.put(id, f);
    }

    private void handleProcessing(FileQueueMessageDTO msg) {
        UUID id = msg.fileId();
        try {
            updateFileStatus(id, ProcessingStatus.PROCESSING);
            statusFacade.status().set(id, ProcessingStatus.PROCESSING);

            FileEntity file = fileRepo.findById(id).orElseThrow();
            AnalysisResult res = new AnalysisResult();
            res.setFile(file);
            res.setProcessStartTime(LocalDateTime.now());

            AnalysisStatistics stats = initStats(res);
            processor.processFile(msg.filePath(), id, stats);

            res.setProcessEndTime(LocalDateTime.now());
            res.setStatistics(stats);
            resultRepo.save(res);
            statsRepo.save(stats);

            updateFileStatus(id, ProcessingStatus.COMPLETED);
            statusFacade.status().set(id, ProcessingStatus.COMPLETED);

        } catch (InterruptedException e) {
            handleCancel(id);
            Thread.currentThread().interrupt();
        } catch (Exception e) {
            log.error("Error processing {}: {}", id, e.getMessage());
            handleFailure(id, e);
        } finally {
            statusFacade.cleanup(id);
            tasks.remove(id);
        }
    }

    private void updateFileStatus(UUID id, ProcessingStatus status) {
        fileRepo.findById(id).ifPresent(f -> {
            f.setStatus(status);
            fileRepo.save(f);
        });
    }

    private void handleCancel(UUID id) {
        updateFileStatus(id, ProcessingStatus.CANCELLED);
        notifier.notify(id, ProcessingStatus.CANCELLED, 0, "Cancelled by user");
    }

    private void handleFailure(UUID id, Exception e) {
        updateFileStatus(id, ProcessingStatus.FAILED);
        notifier.notify(id, ProcessingStatus.FAILED, 0, e.getMessage());
    }

    private AnalysisStatistics initStats(AnalysisResult res) {
        AnalysisStatistics s = new AnalysisStatistics();
        s.setAnalysisResult(res);
        s.setMinValue(Double.MAX_VALUE);
        s.setMaxValue(Double.MIN_VALUE);
        s.setTotalRecords(0L);
        s.setProcessedRecords(0L);
        s.setSkippedRecords(0L);
        s.setSumValue(0.0);
        s.setSumOfSquares(0.0);
        return s;
    }

    public void cancelProcessing(UUID id) {
        statusFacade.cancellation().requestCancellation(id);
        Future<?> f = tasks.get(id);
        if (f != null) f.cancel(true);
    }
}
