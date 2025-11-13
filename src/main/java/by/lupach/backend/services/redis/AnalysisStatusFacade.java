package by.lupach.backend.services.redis;

import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;

import java.util.UUID;

@Service
@RequiredArgsConstructor
public class AnalysisStatusFacade {

    private final StatusRedisService statusService;
    private final ProgressRedisService progressService;
    private final FileAnalysisRedisService fileAnalysisRedisService;

    public void cleanup(UUID fileId) {
        statusService.delete(fileId);
        progressService.delete(fileId);
        fileAnalysisRedisService.delete(fileId);
    }

    public StatusRedisService status() {
        return statusService;
    }

    public ProgressRedisService progress() {
        return progressService;
    }

    public FileAnalysisRedisService analysis() {
        return fileAnalysisRedisService;
    }
}
