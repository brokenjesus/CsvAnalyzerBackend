package by.lupach.backend.services;

import by.lupach.backend.dtos.ProgressMessageDTO;
import by.lupach.backend.entities.AnalysisResult;
import by.lupach.backend.entities.ProcessingStatus;
import by.lupach.backend.repositories.AnalysisResultRepository;
import by.lupach.backend.services.redis.AnalysisStatusFacade;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;

import java.time.LocalDateTime;
import java.util.Optional;
import java.util.UUID;

import static by.lupach.backend.entities.ProcessingStatus.COMPLETED;

@Service
@RequiredArgsConstructor
public class FileAnalysisService {
    private final AnalysisStatusFacade analysisStatusFacade;
    private final AnalysisResultRepository analysisResultRepository;

}
