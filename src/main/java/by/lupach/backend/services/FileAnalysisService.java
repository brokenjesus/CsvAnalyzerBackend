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

    public ProgressMessageDTO getFileProgress(UUID id){
        Optional<ProgressMessageDTO> progressMessage =  analysisStatusFacade.progress().get(id);
        if(progressMessage.isPresent()){
            return progressMessage.get();
        }
        Optional<ProcessingStatus> statusMessage =  analysisStatusFacade.status().get(id);
        if (statusMessage.isPresent()) {
            ProcessingStatus status = statusMessage.get();
            Integer progress = status.equals(COMPLETED) ? 100 : 0;
            String message = "status: " + status + "\tprogress: " + progress;

            return new ProgressMessageDTO(
                    id,
                    status,
                    progress,
                    message,
                    LocalDateTime.now()
            );
        }



        AnalysisResult analysisResult =  analysisResultRepository.getReferenceById(id);

        return null;
    }
}
