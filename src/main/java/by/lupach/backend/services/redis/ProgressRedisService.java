package by.lupach.backend.services.redis;

import by.lupach.backend.dtos.ProgressMessageDTO;
import lombok.RequiredArgsConstructor;
import org.springframework.data.redis.core.RedisTemplate;
import org.springframework.stereotype.Service;

import java.time.Duration;
import java.util.Optional;
import java.util.UUID;

import static by.lupach.backend.configs.RedisConfig.PROGRESS_PREFIX;

@Service
@RequiredArgsConstructor
public class ProgressRedisService {
    private final RedisTemplate<String, ProgressMessageDTO> progressRedis;

    public void set(UUID id, ProgressMessageDTO message) {
        progressRedis.opsForValue().set(PROGRESS_PREFIX + id, message, Duration.ofHours(1));
    }

    public Optional<ProgressMessageDTO> get(UUID fileId) {
        return Optional.ofNullable(progressRedis.opsForValue().get(PROGRESS_PREFIX + fileId));
    }

    public void delete(UUID fileId) {
        progressRedis.delete(PROGRESS_PREFIX + fileId);
    }

    public Optional<Integer> getProgressValue(UUID fileId) {
        return Optional.ofNullable(progressRedis.opsForValue().get(PROGRESS_PREFIX + fileId))
                .map(ProgressMessageDTO::progress);
    }
}