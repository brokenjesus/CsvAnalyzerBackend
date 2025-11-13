package by.lupach.backend.services;

import by.lupach.backend.dtos.AnalysisResultDTO;
import by.lupach.backend.entities.AnalysisResult;
import by.lupach.backend.entities.AnalysisStatistics;
import by.lupach.backend.entities.ProcessingStatus;
import by.lupach.backend.exceptions.AnalysisNotFoundException;
import by.lupach.backend.repositories.AnalysisResultRepository;
import by.lupach.backend.repositories.AnalysisStatisticsRepository;
import by.lupach.backend.services.redis.AnalysisStatusFacade;
import lombok.RequiredArgsConstructor;
import org.springframework.core.convert.ConversionService;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;
import java.util.UUID;

@Service
@RequiredArgsConstructor
public class FileAnalysisService {

    private final AnalysisStatusFacade analysisStatusFacade;
    private final AnalysisResultRepository analysisResultRepository;
//    private final AnalysisStatisticsRepository statsRepo;
    private final ConversionService conversionService;

    @Transactional
    public void saveResult(AnalysisResult result) {
//        if (result.getStatistics() != null) {
//            statsRepo.save(result.getStatistics());
//        }
        result.setProcessEndTime(LocalDateTime.now());
        analysisStatusFacade.analysis().set(result.getId(), conversionService.convert(result, AnalysisResultDTO.class));
        analysisResultRepository.save(result);
    }

    public AnalysisResultDTO getAnalysisDetailsByFileId(UUID fileId) {
        return analysisStatusFacade.analysis().get(fileId)
                .orElseGet(
                        () -> conversionService.convert(
                            analysisResultRepository.findAnalysisResultByFile_Id(fileId)
                                    .orElseThrow(() -> new AnalysisNotFoundException("Analysis result not found: " + fileId)),
                            AnalysisResultDTO.class
                        )
                );
    }

    @Transactional
    public void deleteAnalysisByFileId(UUID fileId) {
        AnalysisResult result = analysisResultRepository.findAnalysisResultByFile_Id(fileId)
                .orElseThrow(() -> new AnalysisNotFoundException("Analysis result not found: " + fileId));

        analysisResultRepository.delete(result);
        analysisStatusFacade.cleanup(result.getId());
    }
}
