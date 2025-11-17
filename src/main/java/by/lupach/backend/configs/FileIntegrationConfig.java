package by.lupach.backend.configs;

import by.lupach.backend.dtos.FileQueueMessageDTO;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.data.redis.core.RedisTemplate;
import org.springframework.integration.channel.DirectChannel;
import org.springframework.integration.config.EnableIntegration;
import org.springframework.integration.dsl.IntegrationFlow;
import org.springframework.integration.redis.inbound.RedisQueueMessageDrivenEndpoint;
import org.springframework.messaging.MessageChannel;

import java.util.UUID;

@Slf4j
@Configuration
@EnableIntegration
@RequiredArgsConstructor
public class FileIntegrationConfig {

    private final RedisTemplate<String, FileQueueMessageDTO> fileQueueRedisTemplate;
    private final RedisTemplate<String, UUID> cancelQueueRedisTemplate;

    @Bean
    public MessageChannel fileInputChannel() {
        return new DirectChannel();
    }

    @Bean
    public MessageChannel cancelInputChannel() {
        return new DirectChannel();
    }

    @Bean
    public RedisQueueMessageDrivenEndpoint fileProcessingQueueEndpoint() {
        RedisQueueMessageDrivenEndpoint endpoint = new RedisQueueMessageDrivenEndpoint(
                RedisConfig.FILE_PROCESSING_QUEUE,
                fileQueueRedisTemplate.getConnectionFactory()
        );

        endpoint.setOutputChannel(fileInputChannel());
        endpoint.setSerializer(fileQueueRedisTemplate.getValueSerializer());
        endpoint.setExpectMessage(false);

        return endpoint;
    }

    @Bean
    public RedisQueueMessageDrivenEndpoint cancelQueueEndpoint() {
        RedisQueueMessageDrivenEndpoint endpoint = new RedisQueueMessageDrivenEndpoint(
                RedisConfig.FILE_CANCEL_QUEUE,
                cancelQueueRedisTemplate.getConnectionFactory()
        );

        endpoint.setOutputChannel(cancelInputChannel());
        endpoint.setSerializer(cancelQueueRedisTemplate.getValueSerializer());
        endpoint.setExpectMessage(false);

        return endpoint;
    }

    @Bean
    public IntegrationFlow fileProcessingInboundFlow() {
        return IntegrationFlow
                .from(fileProcessingQueueEndpoint())
                .channel("fileProcessingChannel")
                .get();
    }

    @Bean
    public IntegrationFlow fileCancelInboundFlow() {
        return IntegrationFlow
                .from(cancelQueueEndpoint())
                .channel("cancelFileProcessingChannel")
                .get();
    }
}
