package by.lupach.backend.services.files;

import by.lupach.backend.dtos.FileQueueMessageDTO;
import by.lupach.backend.dtos.FileUploadResponseDTO;
import by.lupach.backend.entities.AnalysisResult;
import by.lupach.backend.entities.FileEntity;
import by.lupach.backend.entities.ProcessingStatus;
import by.lupach.backend.exceptions.AnalysisNotFoundException;
import by.lupach.backend.exceptions.FileSizeAboveLimitException;
import by.lupach.backend.exceptions.InvalidFileExtensionException;
import by.lupach.backend.repositories.FileEntityRepository;
import by.lupach.backend.services.fileprocessing.ProgressNotifier;
import by.lupach.backend.services.redis.AnalysisStatusFacade;
import lombok.RequiredArgsConstructor;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.multipart.MultipartFile;

import java.io.IOException;
import java.time.LocalDateTime;
import java.util.Objects;
import java.util.UUID;

@Service
@RequiredArgsConstructor
public class FileService {

    private final FileEntityRepository fileRepo;
    private final FileStorageService storageService;
    private final FileQueuePublisher queuePublisher;
    private final FileStorageService fileStorageService;
    private final AnalysisStatusFacade analysisStatusFacade;
    private final ProgressNotifier notifier;

    @Value("${app.max.file.size:52428800}")
    private long maxSize;

    public FileUploadResponseDTO uploadAndQueue(MultipartFile file) throws IOException {
        validate(file);

        FileEntity entity = FileEntity.builder()
                .fileName(file.getOriginalFilename())
                .fileSize(file.getSize())
                .uploadTime(LocalDateTime.now())
                .status(ProcessingStatus.PENDING)
                .build();

        entity = fileRepo.save(entity);
        storageService.saveFile(file, entity.getFilePath());

        notifier.notify(entity.getId(), ProcessingStatus.PENDING, 0, "Added to queue");
//        statusFacade.progress().setProgress(entity.getId(), 0);

        queuePublisher.enqueue(new FileQueueMessageDTO(
                entity.getId(),
                entity.getFilePath(),
                entity.getFileName(),
                entity.getFileSize()
        ));

        return new FileUploadResponseDTO(entity.getId(), entity.getFileName());
    }

    private void validate(MultipartFile file) {
        if (file.isEmpty() || file.getSize() > maxSize) {
            throw new FileSizeAboveLimitException("File size exceeds limit");
        }
        if (!Objects.requireNonNull(file.getOriginalFilename()).toLowerCase().endsWith(".csv")) {
            throw new InvalidFileExtensionException("Only CSV files are allowed");
        }
    }


    @Transactional
    public void deleteAnalysisByFileId(UUID fileId) {
        FileEntity result = fileRepo.getReferenceById(fileId);

        fileStorageService.deletePhysicalFileViaInternalPath(result.getFilePath());
        fileRepo.delete(result);
        analysisStatusFacade.cleanup(fileId);
    }
}
