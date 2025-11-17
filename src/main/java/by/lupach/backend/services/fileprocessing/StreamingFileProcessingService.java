package by.lupach.backend.services.fileprocessing;

import by.lupach.backend.dtos.FileQueueMessageDTO;
import by.lupach.backend.entities.AnalysisResult;
import by.lupach.backend.services.files.FileService;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;

import java.util.UUID;

@Service
@RequiredArgsConstructor
public class StreamingFileProcessingService {

    private final StreamingFileProcessingOrchestrator orchestrator;
    private final FileService fileService;

    public AnalysisResult processFile(FileQueueMessageDTO msg) {
        return orchestrator.startProcessing(msg);
    }

    public void cancelProcessing(UUID fileId) {
        orchestrator.cancelProcessing(fileId);
        fileService.deleteAnalysisByFileId(fileId);
    }
}
