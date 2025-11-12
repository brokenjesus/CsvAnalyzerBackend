package by.lupach.backend.services.fileprocessing;

import by.lupach.backend.entities.AnalysisStatistics;
import by.lupach.backend.entities.ProcessingStatus;
import jakarta.annotation.Resource;
import lombok.RequiredArgsConstructor;
import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.scheduling.TaskScheduler;
import org.springframework.stereotype.Component;

import java.io.BufferedReader;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.time.Duration;
import java.time.Instant;
import java.util.Set;
import java.util.UUID;
import java.util.concurrent.ScheduledFuture;
import java.util.concurrent.atomic.AtomicLong;
import java.util.concurrent.atomic.AtomicReference;

@Component
@RequiredArgsConstructor
public class StreamingFileProcessor {

    private final StatisticsCalculator calculator;
    private final ProgressNotifier notifier;

    @Resource(name = "taskScheduler")
    private TaskScheduler scheduler;

    @Value("${app.upload.dir:uploads}")
    private String uploadDir;

    public void processFile(String internalFilePath, UUID fileId, AnalysisStatistics stats) throws Exception {
        Path path = Paths.get(uploadDir, internalFilePath);
        long totalSize = Files.size(path);
        long bytesRead = 0;

        Set<Double> unique = calculator.newUniqueSet();
        notifier.notify(fileId, ProcessingStatus.PROCESSING, 0, "Started processing");

        // Используем AtomicReference для ошибки и AtomicLong для байтов
        AtomicLong currentBytesRead = new AtomicLong(0);
        AtomicReference<Exception> processingError = new AtomicReference<>();

        ScheduledFuture<?> progressTask = scheduler.scheduleAtFixedRate(() -> {
            try {
                // Проверяем ошибки
                Exception error = processingError.get();
                if (error != null) {
                    return; // Прекращаем выполнение если есть ошибка
                }

                long bytes = currentBytesRead.get();
                int progress = Math.min(100, (int) ((bytes * 100) / totalSize));
                notifier.notify(fileId, ProcessingStatus.PROCESSING, progress, "Processed " + progress + "%");

            } catch (Exception e) {
                processingError.set(e);
            }
        }, Instant.now(), Duration.ofMillis(100));

        try (BufferedReader reader = Files.newBufferedReader(path)) {
            String line;
            while ((line = reader.readLine()) != null) {
                // Проверяем прерывание основного потока
                if (Thread.currentThread().isInterrupted()) {
                    throw new InterruptedException();
                }

                // Проверяем ошибки из задачи уведомлений
                Exception error = processingError.get();
                if (error != null) {
                    throw error;
                }

                bytesRead += line.getBytes().length + 1;
                calculator.processLine(line, stats, unique);
                currentBytesRead.set(bytesRead);
            }

            // Завершаем задачу уведомлений
            progressTask.cancel(false);
            notifier.notify(fileId, ProcessingStatus.COMPLETED, 100, "Completed successfully");

        } catch (Exception e) {
            // Сохраняем ошибку для потока уведомлений
            processingError.set(e);
            progressTask.cancel(false);
            throw e;
        } finally {
            // Гарантируем отмену задачи
            if (!progressTask.isDone()) {
                progressTask.cancel(false);
            }
        }

        calculator.finalizeStats(stats, unique);
    }
}