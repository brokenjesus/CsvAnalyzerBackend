package by.lupach.backend.configs;

import by.lupach.backend.dtos.ProgressMessageDTO;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.context.annotation.Configuration;
import org.springframework.integration.annotation.ServiceActivator;
import org.springframework.messaging.Message;
import org.springframework.data.redis.core.RedisTemplate;
import org.springframework.messaging.simp.SimpMessagingTemplate;

@Slf4j
@Configuration
@RequiredArgsConstructor
public class ProgressFlowConfig {

    private final RedisTemplate<String, ProgressMessageDTO> progressRedisTemplate;
    private final SimpMessagingTemplate ws;

    @ServiceActivator(inputChannel = "progressChannel")
    public void handleProgressMessage(Message<ProgressMessageDTO> message) {
        ProgressMessageDTO dto = message.getPayload();
        try {
            String redisKey = RedisConfig.PROGRESS_PREFIX + dto.fileId();
            progressRedisTemplate.opsForValue().set(redisKey, dto);
            ws.convertAndSend("/topic/progress/" + dto.fileId(), dto);
            log.info("Progress update for file {}: {}% [{}]",
                    dto.fileId(), dto.progress(), dto.status());
        } catch (Exception e) {
            log.error("Error handling progress message for {}: {}", dto.fileId(), e.getMessage(), e);
        }
    }

    // Для отладки ошибок в интеграции
    @ServiceActivator(inputChannel = "errorChannel")
    public void handleError(Message<?> error) {
        log.error("Error in progress integration flow: {}", error.getPayload());
    }
}
