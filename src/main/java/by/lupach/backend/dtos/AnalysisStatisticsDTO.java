package by.lupach.backend.dtos;

public record AnalysisStatisticsDTO(
        Long totalRecords,
        Long processedRecords,
        Long skippedRecords,
        Double minValue,
        Double maxValue,
        Double meanValue,
        Double stdDeviation,
        Long uniqueValuesCount
) {}