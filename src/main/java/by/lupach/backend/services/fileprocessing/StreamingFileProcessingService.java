package by.lupach.backend.services.fileprocessing;

import by.lupach.backend.dtos.FileQueueMessageDTO;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;

import java.util.UUID;

@Service
@RequiredArgsConstructor
public class StreamingFileProcessingService {

    private final FileProcessingOrchestrator orchestrator;

    public void processFile(FileQueueMessageDTO msg) {
        orchestrator.startProcessing(msg);
    }

    public void cancelProcessing(UUID fileId) {
        orchestrator.cancelProcessing(fileId);
    }
}
