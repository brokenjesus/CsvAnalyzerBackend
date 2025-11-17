package by.lupach.backend.services;

import by.lupach.backend.dtos.AnalysisResultDTO;
import by.lupach.backend.dtos.AnalysisStatisticsDTO;
import by.lupach.backend.dtos.PageResponseDTO;
import by.lupach.backend.entities.AnalysisResult;
import by.lupach.backend.entities.FileEntity;
import by.lupach.backend.repositories.AnalysisResultRepository;
import by.lupach.backend.services.files.FileService;
import by.lupach.backend.services.files.FileStorageService;
import jakarta.annotation.Resource;
import lombok.RequiredArgsConstructor;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.core.convert.ConversionService;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.nio.file.Path;
import java.time.Duration;
import java.util.List;
import java.util.UUID;
import java.util.stream.Collectors;

@Service
@RequiredArgsConstructor
public class HistoryService {

    private final AnalysisResultRepository analysisResultRepository;
    private final FileService fileService;
    @Value("${app.max.history.records}")
    private static final int MAX_HISTORY_RECORDS = 10;
    private final ConversionService conversionService;

    public PageResponseDTO<AnalysisResultDTO> getHistory(int page, int size) {
        Pageable pageable = PageRequest.of(page, size);
        Page<AnalysisResult> resultsPage = analysisResultRepository.findLatestAnalysisResults(pageable);

        Page<AnalysisResultDTO> dtoPage = resultsPage.map(result ->
                conversionService.convert(result, AnalysisResultDTO.class)
        );

        return new PageResponseDTO<>(
                dtoPage.getContent(),
                dtoPage.getNumber(),
                dtoPage.getSize(),
                dtoPage.getTotalElements(),
                dtoPage.getTotalPages(),
                dtoPage.isLast()
        );
    }

    public void cleanupOldRecords() {
        long totalRecords = analysisResultRepository.countAnalysisResults();

        if (totalRecords > MAX_HISTORY_RECORDS) {
            List<AnalysisResult> oldest = analysisResultRepository
                    .findOldestResults(PageRequest.of(0, (int)(totalRecords - MAX_HISTORY_RECORDS)));
            oldest.forEach(f-> fileService.deleteAnalysisByFileId(f.getFile().getId()));
        }
        }
}