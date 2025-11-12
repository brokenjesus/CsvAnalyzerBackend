package by.lupach.backend.dtos;

import java.util.UUID;

public record FileUploadResponseDTO (
    UUID fileId,
    String fileName
){}
