package by.lupach.backend.dtos;

import by.lupach.backend.entities.ProcessingStatus;

import java.time.LocalDateTime;
import java.util.UUID;

public record ProgressMessageDTO(
        UUID fileId,
        ProcessingStatus status,
        Integer progress,
        String message,
        LocalDateTime timestamp
) {}