package by.lupach.backend.converters;

import by.lupach.backend.dtos.AnalysisResultDTO;
import by.lupach.backend.dtos.AnalysisStatisticsDTO;
import by.lupach.backend.entities.AnalysisResult;
import by.lupach.backend.entities.FileEntity;
import lombok.RequiredArgsConstructor;
import org.springframework.core.convert.converter.Converter;
import org.springframework.stereotype.Component;

import java.time.Duration;

@Component
@RequiredArgsConstructor
public class AnalysisResultToDtoConverter implements Converter<AnalysisResult, AnalysisResultDTO> {

    @Override
    public AnalysisResultDTO convert(AnalysisResult result) {
        FileEntity file = result.getFile();
        AnalysisStatisticsDTO statsDTO = convertStatistics(result);

        Long processingTimeMs = calculateProcessingTime(result);
        RateCalculationResult rates = calculateRates(result);

        return new AnalysisResultDTO(
                result.getId(),
                file.getId(),
                file.getFileName(),
                file.getFileSize(),
                file.getUploadTime(),
                result.getProcessStartTime(),
                result.getProcessEndTime(),
                file.getStatus(),
                statsDTO,
                processingTimeMs,
                rates.successRate(),
                rates.errorRate()
        );
    }

    private AnalysisStatisticsDTO convertStatistics(AnalysisResult result) {
        if (result.getStatistics() == null) {
            return null;
        }

        var stats = result.getStatistics();
        return new AnalysisStatisticsDTO(
                stats.getTotalRecords(),
                stats.getProcessedRecords(),
                stats.getSkippedRecords(),
                stats.getMinValue(),
                stats.getMaxValue(),
                stats.getMeanValue(),
                stats.getStdDeviation(),
                stats.getUniqueValuesCount()
        );
    }

    private Long calculateProcessingTime(AnalysisResult result) {
        if (result.getProcessStartTime() != null && result.getProcessEndTime() != null) {
            return Duration.between(result.getProcessStartTime(), result.getProcessEndTime()).toMillis();
        }
        return null;
    }

    private RateCalculationResult calculateRates(AnalysisResult result) {
        Double successRate = null;
        Double errorRate = null;

        if (result.getStatistics() != null && result.getStatistics().getTotalRecords() > 0) {
            long total = result.getStatistics().getTotalRecords();
            long processed = result.getStatistics().getProcessedRecords();
            long skipped = result.getStatistics().getSkippedRecords();

            successRate = (double) processed / total * 100;
            errorRate = (double) skipped / total * 100;
        }

        return new RateCalculationResult(successRate, errorRate);
    }

    private record RateCalculationResult(Double successRate, Double errorRate) {}
}