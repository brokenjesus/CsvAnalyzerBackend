package by.lupach.backend.services.files;

import by.lupach.backend.configs.RedisConfig;
import lombok.RequiredArgsConstructor;
import org.springframework.data.redis.core.RedisTemplate;
import org.springframework.stereotype.Service;

import java.util.UUID;

@Service
@RequiredArgsConstructor
public class FileCancelProcessingPublisher {

    private final RedisTemplate<String, UUID> redis;

    public void enqueue(UUID fileId) {
        redis.opsForList().rightPush(RedisConfig.FILE_CANCEL_QUEUE, fileId);
    }
}