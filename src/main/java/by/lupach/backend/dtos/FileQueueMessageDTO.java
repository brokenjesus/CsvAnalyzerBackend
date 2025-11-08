package by.lupach.backend.dtos;

import java.util.UUID;

public record FileQueueMessageDTO(
        UUID fileId,
        String filePath,
        String fileName,
        Long fileSize
) {}
