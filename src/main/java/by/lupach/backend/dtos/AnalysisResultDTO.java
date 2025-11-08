package by.lupach.backend.dtos;

import by.lupach.backend.entities.ProcessingStatus;

import java.time.LocalDateTime;
import java.util.UUID;

public record AnalysisResultDTO(
        UUID id,
        UUID fileId,
        String fileName,
        Long fileSize,
        LocalDateTime uploadTime,
        LocalDateTime processStartTime,
        LocalDateTime processEndTime,
        ProcessingStatus status,
        AnalysisStatisticsDTO statistics,
        Long processingTimeMs,
        Double successRate,
        Double errorRate
) {}