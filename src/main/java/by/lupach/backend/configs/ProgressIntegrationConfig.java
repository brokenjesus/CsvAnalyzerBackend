package by.lupach.backend.configs;

import by.lupach.backend.dtos.ProgressMessageDTO;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.data.redis.core.RedisTemplate;
import org.springframework.integration.channel.DirectChannel;
import org.springframework.integration.config.EnableIntegration;
import org.springframework.integration.dsl.IntegrationFlow;
import org.springframework.messaging.Message;
import org.springframework.messaging.MessageChannel;

@Slf4j
@Configuration
@EnableIntegration
@RequiredArgsConstructor
public class ProgressIntegrationConfig {

    private final RedisTemplate<String, ProgressMessageDTO> progressRedisTemplate;
    private final ProgressFlowConfig progressFlowConfig;

    @Bean
    public MessageChannel progressChannel() {
        return new DirectChannel();
    }

    @Bean
    public IntegrationFlow progressFlow() {
        return IntegrationFlow
                .from(progressChannel())
                .handle(Message.class, (payload, headers) -> {
                    @SuppressWarnings("unchecked")
                    Message<ProgressMessageDTO> message = (Message<ProgressMessageDTO>) payload;
                    log.debug("Received progress message: {}", message.getPayload());
                    progressFlowConfig.handleProgressMessage(message);
                    return null;
                })
                .get();
    }
}
