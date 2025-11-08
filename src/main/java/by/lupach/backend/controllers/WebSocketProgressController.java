// File: controllers/WebSocketProgressController.java
package by.lupach.backend.controllers;

import by.lupach.backend.dtos.ProgressMessageDTO;
import by.lupach.backend.services.AnalysisStatusService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.messaging.handler.annotation.DestinationVariable;
import org.springframework.messaging.handler.annotation.MessageMapping;
import org.springframework.messaging.simp.SimpMessagingTemplate;
import org.springframework.messaging.simp.annotation.SubscribeMapping;
import org.springframework.stereotype.Controller;

import java.util.UUID;

@Slf4j
@Controller
@RequiredArgsConstructor
public class WebSocketProgressController {

    private final AnalysisStatusService statusService;
    private final SimpMessagingTemplate messagingTemplate;

    @MessageMapping("/file.progress.subscribe")
    @SubscribeMapping("/topic/progress")
    public void subscribeToProgress() {
        log.debug("Client subscribed to progress updates");
    }

    @SubscribeMapping("/topic/progress/{fileId}")
    public ProgressMessageDTO getInitialProgress(@DestinationVariable UUID fileId) {
        log.debug("Client requested initial progress for file: {}", fileId);
        return statusService.getProgressMessage(fileId);
    }

    @MessageMapping("/file.cancel")
    public void cancelProcessing(UUID fileId) {
        log.info("Cancellation requested for file: {}", fileId);
        statusService.requestCancellation(fileId);

        // Немедленно уведомляем клиента об отмене
        ProgressMessageDTO progressMessage = new ProgressMessageDTO(
                fileId,
                statusService.getStatus(fileId),
                statusService.getProgress(fileId),
                "Processing cancellation requested",
                java.time.LocalDateTime.now()
        );

        messagingTemplate.convertAndSend("/topic/progress/" + fileId, progressMessage);
    }
}