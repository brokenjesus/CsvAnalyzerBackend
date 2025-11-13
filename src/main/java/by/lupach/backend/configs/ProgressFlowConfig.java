package by.lupach.backend.configs;

import by.lupach.backend.dtos.ProgressMessageDTO;
import by.lupach.backend.entities.ProcessingStatus;
import by.lupach.backend.repositories.FileEntityRepository;
import by.lupach.backend.services.redis.AnalysisStatusFacade;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.context.annotation.Configuration;
import org.springframework.integration.annotation.ServiceActivator;
import org.springframework.messaging.Message;
import org.springframework.messaging.simp.SimpMessagingTemplate;

import java.util.Optional;

@Slf4j
@Configuration
@RequiredArgsConstructor
public class ProgressFlowConfig {

    private static final String PROGRESS_TOPIC_PREFIX = "/topic/progress/";

    private final AnalysisStatusFacade analysisStatusFacade;
    private final FileEntityRepository fileEntityRepository;
    private final SimpMessagingTemplate messagingTemplate;

    @ServiceActivator(inputChannel = "progressChannel")
    public void handleProgressMessage(Message<ProgressMessageDTO> message) {
        ProgressMessageDTO progressMessage = message.getPayload();

        if (Thread.interrupted()) {
            log.warn("Progress thread was interrupted - clearing interrupt flag before Redis access");
        }

        try {
            updateDBFileStatusIfChanged(progressMessage);
            updateProgressCache(progressMessage);
            notifyWebSocketClients(progressMessage);

            log.debug("Progress update for file {}: {}% [{}]",
                    progressMessage.fileId(), progressMessage.progress(), progressMessage.status());

        } catch (Exception exception) {
            log.error("Error handling progress message for file {}: {}",
                    progressMessage.fileId(), exception.getMessage(), exception);
        }
    }

    private void updateDBFileStatusIfChanged(ProgressMessageDTO progressMessage) {
        Optional<ProcessingStatus> currentStatus = analysisStatusFacade.status().get(progressMessage.fileId());

        if (currentStatus.isEmpty() || !currentStatus.get().equals(progressMessage.status())) {
            fileEntityRepository.findById(progressMessage.fileId()).ifPresent(fileEntity -> {
                fileEntity.setStatus(progressMessage.status());
                fileEntityRepository.save(fileEntity);
            });
        }
    }

    private void updateProgressCache(ProgressMessageDTO progressMessage) {
        analysisStatusFacade.status().set(progressMessage.fileId(), progressMessage.status());
        analysisStatusFacade.progress().set(progressMessage.fileId(), progressMessage);
    }

    private void notifyWebSocketClients(ProgressMessageDTO progressMessage) {
        String destination = PROGRESS_TOPIC_PREFIX + progressMessage.fileId();
        messagingTemplate.convertAndSend(destination, progressMessage);
    }

    @ServiceActivator(inputChannel = "errorChannel")
    public void handleError(Message<?> errorMessage) {
        log.error("Error in progress integration flow: {}", errorMessage.getPayload());
    }
}