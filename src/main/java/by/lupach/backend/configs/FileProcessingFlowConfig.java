package by.lupach.backend.configs;

import by.lupach.backend.dtos.FileQueueMessageDTO;
import by.lupach.backend.entities.AnalysisResult;
import by.lupach.backend.services.FileAnalysisService;
import by.lupach.backend.services.fileprocessing.FileProcessingOrchestrator;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.integration.annotation.IntegrationComponentScan;
import org.springframework.integration.annotation.Router;
import org.springframework.integration.annotation.ServiceActivator;
import org.springframework.integration.channel.DirectChannel;
import org.springframework.integration.config.EnableIntegration;
import org.springframework.integration.dsl.IntegrationFlow;
import org.springframework.messaging.MessageChannel;

@Slf4j
@Configuration
@EnableIntegration
@IntegrationComponentScan
@RequiredArgsConstructor
public class FileProcessingFlowConfig {

    private static final String FILE_PROCESSING_CHANNEL = "fileProcessingChannel";
    private static final String ANALYSIS_SAVE_CHANNEL = "analysisSaveChannel";
    private static final String ERROR_CHANNEL = "errorChannel";

    private final FileProcessingOrchestrator fileProcessingOrchestrator;
    private final FileAnalysisService fileAnalysisService;

    @Bean
    public MessageChannel fileProcessingChannel() {
        return new DirectChannel();
    }

    @Bean
    public MessageChannel analysisSaveChannel() {
        return new DirectChannel();
    }

    @Bean
    public IntegrationFlow handleFileProcessing() {
        return IntegrationFlow.from(FILE_PROCESSING_CHANNEL)
                .handle(FileQueueMessageDTO.class, (payload, headers) -> {
                    log.info("Received file processing message: {}", payload.filePath());
                    AnalysisResult result = fileProcessingOrchestrator.startProcessing(payload);
                    log.info("File processing completed for: {}", payload.filePath());
                    return result;
                })
                .channel(ANALYSIS_SAVE_CHANNEL)
                .get();
    }

    @Bean
    public IntegrationFlow analysisSaveFlow() {
        return IntegrationFlow.from(ANALYSIS_SAVE_CHANNEL)
                .handle(AnalysisResult.class, (payload, headers) -> {
                    log.info("Saving analysis result for file: {}", payload.getFile().getId());
                    fileAnalysisService.saveResult(payload);
                    return null;
                })
                .get();
    }

    @Bean
    public IntegrationFlow errorFlow() {
        return IntegrationFlow.from(ERROR_CHANNEL)
                .handle(message -> {
                    log.error("Error in integration flow: {}", message.getPayload());
                })
                .get();
    }

    @ServiceActivator(inputChannel = ERROR_CHANNEL)
    public void handleGlobalError(Exception exception) {
        log.error("Global error in file processing flow: {}", exception.getMessage(), exception);
    }
}