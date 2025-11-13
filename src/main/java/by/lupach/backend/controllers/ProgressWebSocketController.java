package by.lupach.backend.controllers;

import by.lupach.backend.dtos.ProgressMessageDTO;
import by.lupach.backend.entities.ProcessingStatus;
import by.lupach.backend.services.fileprocessing.StreamingFileProcessingService;
import by.lupach.backend.services.redis.AnalysisStatusFacade;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.messaging.handler.annotation.*;
import org.springframework.messaging.simp.SimpMessagingTemplate;
import org.springframework.stereotype.Controller;
import org.springframework.web.bind.annotation.CrossOrigin;

import java.time.LocalDateTime;
import java.util.Optional;
import java.util.UUID;

@Slf4j
@Controller
@RequiredArgsConstructor
public class ProgressWebSocketController {

    private final SimpMessagingTemplate ws;
    private final AnalysisStatusFacade analysisStatusFacade;
    private final StreamingFileProcessingService processingService;


    /**
     * Отправляет клиенту текущий прогресс и статус, как только он подключается
     */
    @MessageMapping("/progress/subscribe/{fileId}")
    public void onSubscribe(@DestinationVariable UUID fileId) {
        try {
            ProgressMessageDTO dto =  internalGetProgressMessageDTO(fileId);
            ws.convertAndSend("/topic/progress/" + fileId, dto);
            log.info("Client subscribed to progress updates for file: {}", fileId);
        } catch (Exception e) {
            log.error("Failed to handle subscription for {}: {}", fileId, e.getMessage());
        }
    }

    /**
     * Обработка запроса на отмену анализа файла
     */
    @MessageMapping("/progress/cancel/{fileId}")
    public void cancelProcessing(@DestinationVariable UUID fileId) {
        log.info("Received cancel request for file {}", fileId);
        try {
            processingService.cancelProcessing(fileId);

            ProgressMessageDTO dto = new ProgressMessageDTO(
                    fileId,
                    ProcessingStatus.CANCELLED,
                    0,
                    "Processing cancelled by user",
                    LocalDateTime.now()
            );

            ws.convertAndSend("/topic/progress/" + fileId, dto);
            log.info("Cancellation notification sent for {}", fileId);
        } catch (Exception e) {
            log.error("Failed to cancel processing for {}: {}", fileId, e.getMessage());
        }
    }

    /**
     * Возвращает текущее состояние анализа по запросу (например, для кнопки «Обновить»)
     */
    @MessageMapping("/progress/status/{fileId}")
    public void getStatus(@DestinationVariable UUID fileId) {
        try {
            internalGetProgressMessageDTO(fileId);
        } catch (Exception e) {
            log.error("Failed to fetch status for {}: {}", fileId, e.getMessage());
        }
    }

    private ProgressMessageDTO internalGetProgressMessageDTO(@DestinationVariable UUID fileId) {
        Optional<ProgressMessageDTO> progress = analysisStatusFacade.progress().get(fileId);

        return progress.orElseGet(() -> {
            Optional<ProcessingStatus> status = analysisStatusFacade.status().get(fileId);

            Integer progressValue = null;
            if (status.isPresent() && status.get().equals(ProcessingStatus.COMPLETED)) {
                progressValue = 100;
            }

            return new ProgressMessageDTO(
                    fileId,
                    status.orElse(null),
                    progressValue,
                    "Subscription successful. Current progress: " +
                            (progressValue != null ? progressValue + "%" : "unknown"),
                    LocalDateTime.now()
            );
        });
    }
}
