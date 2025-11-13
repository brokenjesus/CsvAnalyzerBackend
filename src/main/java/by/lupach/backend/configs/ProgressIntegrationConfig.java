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

    private static final String PROGRESS_CHANNEL = "progressChannel";

    private final ProgressFlowConfig progressFlowConfig;

    @Bean
    public MessageChannel progressChannel() {
        return new DirectChannel();
    }

    @Bean
    public IntegrationFlow progressFlow() {
        return IntegrationFlow
                .from(PROGRESS_CHANNEL)
                .handle(Message.class, (message, headers) -> {
                    progressFlowConfig.handleProgressMessage((Message<ProgressMessageDTO>) message);
                    return null;
                })
                .get();
    }
}