package by.lupach.backend.services.redis;

import by.lupach.backend.configs.RedisConfig;
import by.lupach.backend.entities.ProcessingStatus;
import lombok.RequiredArgsConstructor;
import org.springframework.data.redis.core.RedisTemplate;
import org.springframework.stereotype.Service;

import java.time.Duration;
import java.util.Optional;
import java.util.UUID;

@Service
@RequiredArgsConstructor
public class StatusRedisService{
    private final RedisTemplate<String, String> redis;

    public void set(UUID fileId, ProcessingStatus status) {
        redis.opsForValue().set(RedisConfig.STATUS_PREFIX + fileId, status.name(), Duration.ofMinutes(30));
    }

    public Optional<ProcessingStatus> get(UUID fileId) {
        return Optional.ofNullable(redis.opsForValue().get(RedisConfig.STATUS_PREFIX + fileId))
                .map(ProcessingStatus::valueOf);
    }

    public void delete(UUID fileId) {
        redis.delete(RedisConfig.STATUS_PREFIX + fileId);
    }
}
