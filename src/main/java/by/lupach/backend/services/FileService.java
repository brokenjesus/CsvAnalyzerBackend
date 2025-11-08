package by.lupach.backend.services;

import by.lupach.backend.configs.RedisConfig;
import by.lupach.backend.dtos.FileQueueMessageDTO;
import by.lupach.backend.entities.FileEntity;
import by.lupach.backend.entities.ProcessingStatus;
import by.lupach.backend.exceptions.FileSizeAboveLimitException;
import by.lupach.backend.exceptions.InvalidFileExtensionException;
import by.lupach.backend.repositories.FileEntityRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.data.redis.core.RedisTemplate;
import org.springframework.stereotype.Service;
import org.springframework.web.multipart.MultipartFile;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.time.LocalDateTime;
import java.util.Objects;
import java.util.UUID;

@Service
@RequiredArgsConstructor
public class FileService {

    private final FileEntityRepository fileRepository;
    private final AnalysisStatusService statusService;
    private final RedisTemplate<String, FileQueueMessageDTO> fileQueueRedisTemplate;

    @Value("${app.upload.dir:uploads}")
    private String uploadDir;

    @Value("${app.max.file.size:52428800}")
    private long maxFileSize;

    public UUID uploadAndQueueFile(MultipartFile file) throws IOException {
        if (!Objects.equals(file.getContentType(), "text/csv") &&
                !Objects.requireNonNull(file.getOriginalFilename()).toLowerCase().endsWith(".csv")) {
            throw new InvalidFileExtensionException("Only CSV files are allowed");
        }

        if (file.getSize() > maxFileSize || file.isEmpty()) {
            throw new FileSizeAboveLimitException("File size exceeds maximum limit of 50MB");
        }


        FileEntity fileEntity = FileEntity.builder()
                .fileName(file.getOriginalFilename())
                .fileSize(file.getSize())
                .uploadTime(LocalDateTime.now())
                .status(ProcessingStatus.PENDING)
                .build();
        fileEntity = fileRepository.save(fileEntity);

        Path filePath = Paths.get(uploadDir, fileEntity.getFilePath());
        Files.createDirectories(filePath.getParent());
        file.transferTo(filePath);

        // Установка статуса и прогресса в Redis
        statusService.setStatus(fileEntity.getId(), ProcessingStatus.PENDING);
        statusService.setProgress(fileEntity.getId(), 0);

        // Отправка сообщения в очередь Redis
        FileQueueMessageDTO queueMessage = new FileQueueMessageDTO(
                fileEntity.getId(),
                filePath.toString(),
                file.getOriginalFilename(),
                file.getSize()
        );
        fileQueueRedisTemplate.opsForList().rightPush(RedisConfig.FILE_QUEUE, queueMessage);

        return fileEntity.getId();
    }


    public void updateFileStatus(UUID fileId, ProcessingStatus status) {
        fileRepository.findById(fileId).ifPresent(fileEntity -> {
            fileEntity.setStatus(status);
            fileRepository.save(fileEntity);
        });
    }
}