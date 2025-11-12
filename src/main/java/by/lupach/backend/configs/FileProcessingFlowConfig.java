// File: configs/FileProcessingFlowConfig.java
package by.lupach.backend.configs;

import by.lupach.backend.dtos.FileQueueMessageDTO;
import by.lupach.backend.services.fileprocessing.StreamingFileProcessingService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.integration.annotation.ServiceActivator;
import org.springframework.integration.channel.DirectChannel;
import org.springframework.integration.config.EnableIntegration;
import org.springframework.messaging.Message;
import org.springframework.messaging.MessageChannel;

@Slf4j
@Configuration
@EnableIntegration
@RequiredArgsConstructor
public class FileProcessingFlowConfig {

    private final StreamingFileProcessingService fileProcessingService;

    @Bean
    public MessageChannel fileProcessingChannel() {
        return new DirectChannel();
    }

    @ServiceActivator(inputChannel = "fileProcessingChannel")
    public void handleFileMessage(Message<FileQueueMessageDTO> message) {
        log.info("Received file processing message: {}", message.getPayload());
        try {
            fileProcessingService.processFile(message.getPayload());
        } catch (Exception e) {
            log.error("Error processing file message: {}", message.getPayload(), e);
        }
    }

    // Service activator для обработки ошибок
    @ServiceActivator(inputChannel = "errorChannel")
    public void handleError(Message<?> message) {
        log.error("Error in integration flow: {}", message.getPayload());
    }
}