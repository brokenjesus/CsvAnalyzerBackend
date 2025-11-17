package by.lupach.backend.services.files;

import by.lupach.backend.configs.RedisConfig;
import by.lupach.backend.dtos.FileQueueMessageDTO;
import lombok.RequiredArgsConstructor;
import org.springframework.data.redis.core.RedisTemplate;
import org.springframework.stereotype.Service;

@Service
@RequiredArgsConstructor
public class FileQueuePublisher {

    private final RedisTemplate<String, FileQueueMessageDTO> redis;

    public void enqueue(FileQueueMessageDTO message) {
        redis.opsForList().rightPush(RedisConfig.FILE_PROCESSING_QUEUE, message);
    }
}
