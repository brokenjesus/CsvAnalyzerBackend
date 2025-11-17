package by.lupach.backend.configs;

import by.lupach.backend.dtos.FileQueueMessageDTO;
import by.lupach.backend.entities.AnalysisResult;
import by.lupach.backend.services.FileAnalysisService;
import by.lupach.backend.services.HistoryService;
import by.lupach.backend.services.fileprocessing.StreamingFileProcessingService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.integration.annotation.IntegrationComponentScan;
import org.springframework.integration.annotation.ServiceActivator;
import org.springframework.integration.channel.DirectChannel;
import org.springframework.integration.config.EnableIntegration;
import org.springframework.integration.dsl.IntegrationFlow;
import org.springframework.messaging.MessageChannel;

import java.util.UUID;

@Slf4j
@Configuration
@EnableIntegration
@IntegrationComponentScan
@RequiredArgsConstructor
public class FileProcessingFlowConfig {

    private static final String FILE_PROCESSING_CHANNEL = "fileProcessingChannel";
    private static final String CANCEL_FILE_PROCESSING_CHANNEL = "cancelFileProcessingChannel";
    private static final String ANALYSIS_SAVE_CHANNEL = "analysisSaveChannel";
    private static final String ERROR_CHANNEL = "errorChannel";

    private final StreamingFileProcessingService streamingFileProcessingService;
    private final FileAnalysisService fileAnalysisService;
    private final HistoryService historyService;

    @Bean
    public MessageChannel fileProcessingChannel() {
        return new DirectChannel();
    }

    @Bean
    public MessageChannel cancelFileProcessingChannel() {
        return new DirectChannel();
    }

    @Bean
    public MessageChannel analysisSaveChannel() {
        return new DirectChannel();
    }

    @Bean
    public MessageChannel errorChannel() {
        return new DirectChannel();
    }

    @Bean
    public IntegrationFlow handleFileProcessing() {
        return IntegrationFlow.from(FILE_PROCESSING_CHANNEL)
                .handle(FileQueueMessageDTO.class, (payload, headers) -> {
                    log.info("Start processing file: {}", payload.filePath());
                    AnalysisResult result = streamingFileProcessingService.processFile(payload);
                    log.info("Completed processing: {}", payload.filePath());
                    return result;
                })
                .channel(ANALYSIS_SAVE_CHANNEL)
                .get();
    }

    @Bean
    public IntegrationFlow handleCancelFileProcessing() {
        return IntegrationFlow.from(CANCEL_FILE_PROCESSING_CHANNEL)
                .handle(UUID.class, (payload, headers) -> {
                    log.info("Cancel requested for file: {}", payload);
                    streamingFileProcessingService.cancelProcessing(payload);
                    return null;
                })
                .get();
    }

    @Bean
    public IntegrationFlow analysisSaveFlow() {
        return IntegrationFlow.from(ANALYSIS_SAVE_CHANNEL)
                .handle(AnalysisResult.class, (payload, headers) -> {
                    log.info("Saving analysis result for file: {}", payload.getFile().getId());
                    fileAnalysisService.saveResult(payload);
                    historyService.cleanupOldRecords();
                    return null;
                })
                .get();
    }

    @Bean
    public IntegrationFlow errorFlow() {
        return IntegrationFlow.from(ERROR_CHANNEL)
                .handle(message -> {
                    log.error("Error in file processing flow: {}", message.getPayload());
                })
                .get();
    }

    @ServiceActivator(inputChannel = ERROR_CHANNEL)
    public void handleGlobalError(Exception exception) {
        log.error("Global error in flow: {}", exception.getMessage(), exception);
    }
}
