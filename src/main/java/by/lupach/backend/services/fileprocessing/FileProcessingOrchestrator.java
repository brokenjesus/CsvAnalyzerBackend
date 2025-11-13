package by.lupach.backend.services.fileprocessing;

import by.lupach.backend.dtos.FileQueueMessageDTO;
import by.lupach.backend.entities.AnalysisResult;
import by.lupach.backend.entities.AnalysisStatistics;
import by.lupach.backend.entities.FileEntity;
import by.lupach.backend.entities.ProcessingStatus;
import by.lupach.backend.repositories.FileEntityRepository;
import by.lupach.backend.services.HistoryService;
import by.lupach.backend.services.files.FileStorageService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;

import java.nio.file.Path;
import java.nio.file.Paths;
import java.time.LocalDateTime;
import java.util.UUID;
import java.util.concurrent.*;

@Slf4j
@Service
@RequiredArgsConstructor
public class FileProcessingOrchestrator {

    private final FileEntityRepository fileRepo;
    private final StreamingFileProcessor processor;
    private final ProgressNotifier notifier;
    private final HistoryService historyService;
    private final FileStorageService fileStorageService;

    @Value("${app.upload.dir:uploads}")
    private String uploadDir;

    private final ExecutorService exec = Executors.newCachedThreadPool();
    private final ConcurrentHashMap<UUID, Future<AnalysisResult>> tasks = new ConcurrentHashMap<>();

    /** Асинхронная обработка файла, возвращает результат анализа */
    public AnalysisResult startProcessing(FileQueueMessageDTO msg) {
        UUID id = msg.fileId();
        if (tasks.containsKey(id)) {
            log.warn("Задача для файла {} уже запущена", id);
            return null;
        }

        Future<AnalysisResult> f = exec.submit(() -> handleProcessing(msg));
        tasks.put(id, f);

        try {
            return f.get(); // дождаться результата
        } catch (Exception e) {
            log.error("Ошибка ожидания результата обработки: {}", e.getMessage());
            return null;
        }
    }

    private AnalysisResult handleProcessing(FileQueueMessageDTO msg) {
        UUID id = msg.fileId();
        Path path = Paths.get(uploadDir, msg.filePath());

        try {
            FileEntity file = fileRepo.findById(id).orElseThrow();
            AnalysisResult res = new AnalysisResult();
            res.setFile(file);
            res.setProcessStartTime(LocalDateTime.now());

            AnalysisStatistics stats = processor.processFile(path, id);

            res.setStatistics(stats);
            res.setProcessEndTime(LocalDateTime.now());

            return res;
        } catch (InterruptedException e) {
            handleCancel(id);
            Thread.currentThread().interrupt();
        } catch (Exception e) {
            log.error("Ошибка обработки файла {}: {}", id, e.getMessage());
            handleFailure(id, e);
        } finally {
            tasks.remove(id);
            historyService.cleanupOldRecords();
            fileStorageService.deletePhysicalFile(path);
        }
        return null;
    }

    private void handleCancel(UUID id) {
        notifier.notify(id, ProcessingStatus.CANCELLED, 0, "Cancelled by user");
    }

    private void handleFailure(UUID id, Exception e) {
        notifier.notify(id, ProcessingStatus.FAILED, 0, e.getMessage());
    }

    public void cancelProcessing(UUID id) {
        Future<?> f = tasks.get(id);
        if (f != null) f.cancel(true);
    }
}
