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
import org.springframework.data.redis.core.RedisTemplate;
import org.springframework.messaging.simp.SimpMessagingTemplate;

import java.util.Optional;

@Slf4j
@Configuration
@RequiredArgsConstructor
public class ProgressFlowConfig {

    private final AnalysisStatusFacade statusFacade;
    private final FileEntityRepository fileEntityRepository;
    private final SimpMessagingTemplate ws;

    @ServiceActivator(inputChannel = "progressChannel")
    public void handleProgressMessage(Message<ProgressMessageDTO> message) {
        ProgressMessageDTO dto = message.getPayload();

        if (Thread.interrupted()) {
            log.warn("Progress thread was interrupted — clearing interrupt flag before Redis access");
        }


        try {
            Optional<ProcessingStatus> processingStatus = statusFacade.status().get(dto.fileId());
            if (processingStatus.isPresent() && !processingStatus.get().equals(dto.status())) {
                fileEntityRepository.findById(dto.fileId()).ifPresent(f -> {
                    f.setStatus(dto.status());
                    fileEntityRepository.save(f);
                });
            }
            statusFacade.status().set(dto.fileId(), dto.status());
            statusFacade.progress().set(dto.fileId(), dto);
            ws.convertAndSend("/topic/progress/" + dto.fileId(), dto);
            log.info("Progress update for file {}: {}% [{}]",
                    dto.fileId(), dto.progress(), dto.status());
        } catch (Exception e) {
            log.error("Error handling progress message for {}: {}", dto.fileId(), e.getMessage(), e);
        }
    }

    // Для отладки ошибок в интеграции
    @ServiceActivator(inputChannel = "errorChannel")
    public void handleError(Message<?> error) {
        log.error("Error in progress integration flow: {}", error.getPayload());
    }
}
