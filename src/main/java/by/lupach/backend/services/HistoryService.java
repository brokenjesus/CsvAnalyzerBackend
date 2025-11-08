package by.lupach.backend.services;

import by.lupach.backend.dtos.AnalysisResultDTO;
import by.lupach.backend.dtos.AnalysisStatisticsDTO;
import by.lupach.backend.dtos.PageResponseDTO;
import by.lupach.backend.entities.AnalysisResult;
import by.lupach.backend.entities.FileEntity;
import by.lupach.backend.repositories.AnalysisResultRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.Duration;
import java.util.List;
import java.util.UUID;
import java.util.stream.Collectors;

@Service
@RequiredArgsConstructor
public class HistoryService {

    private final AnalysisResultRepository analysisResultRepository;
    private static final int MAX_HISTORY_RECORDS = 10;
    private static final int PAGE_SIZE = 5;

    public PageResponseDTO<AnalysisResultDTO> getHistory(int page) {
        Pageable pageable = PageRequest.of(page, PAGE_SIZE);
        Page<AnalysisResult> resultsPage = analysisResultRepository.findLatestAnalysisResults(pageable);

        List<AnalysisResultDTO> content = resultsPage.getContent().stream()
                .map(this::convertToDTO)
                .collect(Collectors.toList());

        return new PageResponseDTO<>(
                content,
                resultsPage.getNumber(),
                resultsPage.getSize(),
                resultsPage.getTotalElements(),
                resultsPage.getTotalPages(),
                resultsPage.isLast()
        );
    }

    public AnalysisResultDTO getAnalysisDetails(UUID id) {
        AnalysisResult result = analysisResultRepository.findById(id)
                .orElseThrow(() -> new RuntimeException("Analysis result not found: " + id));
        return convertToDTO(result);
    }

    @Transactional
    public void deleteAnalysis(UUID id) {
        AnalysisResult result = analysisResultRepository.findById(id)
                .orElseThrow(() -> new RuntimeException("Analysis result not found: " + id));

        analysisResultRepository.delete(result);
    }

    @Transactional
    public void cleanupOldRecords() {
        long totalRecords = analysisResultRepository.countAnalysisResults();

        if (totalRecords > MAX_HISTORY_RECORDS) {
            List<AnalysisResult> oldest = analysisResultRepository
                    .findOldestResults(PageRequest.of(0, (int)(totalRecords - MAX_HISTORY_RECORDS)));
            analysisResultRepository.deleteAll(oldest);
        }
    }

    private AnalysisResultDTO convertToDTO(AnalysisResult result) {
        FileEntity file = result.getFile();
        AnalysisStatisticsDTO statsDTO = null;

        if (result.getStatistics() != null) {
            var stats = result.getStatistics();
            statsDTO = new AnalysisStatisticsDTO(
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

        Long processingTimeMs = null;
        if (result.getProcessStartTime() != null && result.getProcessEndTime() != null) {
            processingTimeMs = Duration.between(result.getProcessStartTime(), result.getProcessEndTime()).toMillis();
        }

        Double successRate = null;
        Double errorRate = null;
        if (result.getStatistics() != null && result.getStatistics().getTotalRecords() > 0) {
            long total = result.getStatistics().getTotalRecords();
            long processed = result.getStatistics().getProcessedRecords();
            long skipped = result.getStatistics().getSkippedRecords();

            successRate = (double) processed / total * 100;
            errorRate = (double) skipped / total * 100;
        }

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
                successRate,
                errorRate
        );
    }
}