package by.lupach.backend.configs;

import by.lupach.backend.dtos.FileQueueMessageDTO;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.data.redis.core.RedisTemplate;
import org.springframework.data.redis.serializer.GenericJackson2JsonRedisSerializer;
import org.springframework.integration.channel.DirectChannel;
import org.springframework.integration.config.EnableIntegration;
import org.springframework.integration.dsl.IntegrationFlow;
import org.springframework.integration.redis.inbound.RedisQueueMessageDrivenEndpoint;
import org.springframework.messaging.Message;
import org.springframework.messaging.MessageChannel;

@Slf4j
@Configuration
@EnableIntegration
@RequiredArgsConstructor
public class FileIntegrationConfig {

    private final RedisTemplate<String, FileQueueMessageDTO> fileQueueRedisTemplate;
    private final FileProcessingFlowConfig fileProcessingFlowConfig;

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
        endpoint.setBeanName("fileQueueEndpoint");

        // Используем тот же сериализатор, что и RedisTemplate
        endpoint.setSerializer(fileQueueRedisTemplate.getValueSerializer());

        // Отключаем ожидание Message, т.к. в очереди чистый DTO
        endpoint.setExpectMessage(false);

        return endpoint;
    }


    @Bean
    public IntegrationFlow fileProcessingFlow() {
        return IntegrationFlow
                .from(redisQueueEndpoint())
                .handle(Message.class, (payload, headers) -> {
                    // payload уже Message<FileQueueMessageDTO>
                    @SuppressWarnings("unchecked")
                    Message<FileQueueMessageDTO> message = (Message<FileQueueMessageDTO>) payload;
                    log.info("Received from Redis queue: {}", message.getPayload());
                    fileProcessingFlowConfig.handleFileMessage(message);
                    return null;
                })
                .get();
    }
}
