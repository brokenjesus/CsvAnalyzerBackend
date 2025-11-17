package by.lupach.backend.services;

import by.lupach.backend.dtos.AnalysisResultDTO;
import by.lupach.backend.entities.AnalysisResult;
import by.lupach.backend.exceptions.AnalysisNotFoundException;
import by.lupach.backend.repositories.AnalysisResultRepository;
import by.lupach.backend.services.files.FileStorageService;
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
    private final ConversionService conversionService;

    @Transactional
    public void saveResult(AnalysisResult result) {
        result.setProcessEndTime(LocalDateTime.now());
        result = analysisResultRepository.save(result);
        analysisStatusFacade.analysis().set(result.getFile().getId(), conversionService.convert(result, AnalysisResultDTO.class));
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
}
