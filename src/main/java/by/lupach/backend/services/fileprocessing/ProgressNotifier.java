package by.lupach.backend.services.fileprocessing;

import by.lupach.backend.dtos.ProgressMessageDTO;
import by.lupach.backend.entities.ProcessingStatus;
import lombok.RequiredArgsConstructor;
import org.springframework.messaging.MessageChannel;
import org.springframework.messaging.support.MessageBuilder;
import org.springframework.stereotype.Service;

import java.time.LocalDateTime;
import java.util.UUID;

@Service
@RequiredArgsConstructor
public class ProgressNotifier {

    private final MessageChannel progressChannel;

    public void notify(UUID fileId, ProcessingStatus status, int progress, String message) {
        ProgressMessageDTO dto = new ProgressMessageDTO(fileId, status, progress, message, LocalDateTime.now());
        progressChannel.send(MessageBuilder.withPayload(dto).build());
    }
}
