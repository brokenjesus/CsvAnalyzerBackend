package by.lupach.backend.configs;

import by.lupach.backend.dtos.ProgressMessageDTO;
import by.lupach.backend.entities.FileEntity;
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
import java.util.UUID;

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
        UUID fileId = progressMessage.fileId();
        Optional<ProcessingStatus> currentStatus = analysisStatusFacade.status().get(fileId);

        if (currentStatus.isEmpty() || !currentStatus.get().equals(progressMessage.status())) {
            FileEntity fileEntity = fileEntityRepository.findById(fileId).orElse(null);

            if (fileEntity != null) {
                fileEntity.setStatus(progressMessage.status());
                fileEntityRepository.save(fileEntity);
                log.info("Статус файла ID: {} успешно обновлен на: {}", fileId, progressMessage.status());
            } else {
                log.warn("Файл с ID: {} не найден в репозитории", fileId);
            }
        } else {
            log.debug("Статус файла ID: {} не изменился: {}", fileId, progressMessage.status());
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