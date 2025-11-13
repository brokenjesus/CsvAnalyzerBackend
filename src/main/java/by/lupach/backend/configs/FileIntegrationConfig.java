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

@Slf4j
@Configuration
@EnableIntegration
@RequiredArgsConstructor
public class FileIntegrationConfig {

    private static final String FILE_PROCESSING_CHANNEL = "fileProcessingChannel";
    private static final String FILE_QUEUE_ENDPOINT_BEAN_NAME = "fileQueueEndpoint";

    private final RedisTemplate<String, FileQueueMessageDTO> fileQueueRedisTemplate;

    @Bean
    public MessageChannel fileInputChannel() {
        return new DirectChannel();
    }

    @Bean
    public RedisQueueMessageDrivenEndpoint redisQueueEndpoint() {
        RedisQueueMessageDrivenEndpoint endpoint = new RedisQueueMessageDrivenEndpoint(
                RedisConfig.FILE_QUEUE,
                fileQueueRedisTemplate.getConnectionFactory()
        );

        endpoint.setOutputChannel(fileInputChannel());
        endpoint.setBeanName(FILE_QUEUE_ENDPOINT_BEAN_NAME);
        endpoint.setSerializer(fileQueueRedisTemplate.getValueSerializer());
        endpoint.setExpectMessage(false);

        return endpoint;
    }

    @Bean
    public IntegrationFlow fileProcessingFlow() {
        return IntegrationFlow
                .from(redisQueueEndpoint())
                .channel(FILE_PROCESSING_CHANNEL)
                .get();
    }
}