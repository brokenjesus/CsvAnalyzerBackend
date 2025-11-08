package by.lupach.backend.services;

import by.lupach.backend.configs.RedisConfig;
import by.lupach.backend.dtos.ProgressMessageDTO;
import by.lupach.backend.entities.ProcessingStatus;
import lombok.RequiredArgsConstructor;
import org.springframework.data.redis.core.RedisTemplate;
import org.springframework.stereotype.Service;

import java.time.Duration;
import java.time.LocalDateTime;
import java.util.UUID;

@RequiredArgsConstructor
@Service
public class AnalysisStatusService {

    private final RedisTemplate<String, String> stringRedisTemplate;
    private final RedisTemplate<String, ProgressMessageDTO> progressRedisTemplate;

    public void setStatus(UUID fileId, ProcessingStatus status) {
        stringRedisTemplate.opsForValue().set(
                RedisConfig.STATUS_PREFIX + fileId,
                status.toString(),
                Duration.ofHours(24) // TTL 24 часа
        );
    }

    public ProcessingStatus getStatus(UUID fileId) {
        String status = stringRedisTemplate.opsForValue().get(RedisConfig.STATUS_PREFIX + fileId);
        return status != null ? ProcessingStatus.valueOf(status) : ProcessingStatus.PENDING;
    }

    public void setProgress(UUID fileId, int progress) {
        stringRedisTemplate.opsForValue().set(
                RedisConfig.PROGRESS_PREFIX + fileId,
                String.valueOf(progress),
                Duration.ofHours(24)
        );
    }

    public Integer getProgress(UUID fileId) {
        String progress = stringRedisTemplate.opsForValue().get(RedisConfig.PROGRESS_PREFIX + fileId);
        return progress != null ? Integer.parseInt(progress) : 0;
    }

    public void requestCancellation(UUID fileId) {
        stringRedisTemplate.opsForValue().set(
                RedisConfig.CANCELLATION_PREFIX + fileId,
                "true",
                Duration.ofMinutes(30)
        );
    }

    public boolean isCancellationRequested(UUID fileId) {
        String cancel = stringRedisTemplate.opsForValue().get(RedisConfig.CANCELLATION_PREFIX + fileId);
        return "true".equals(cancel);
    }

    public void saveProgressMessage(ProgressMessageDTO progressMessage) {
        String key = "progress:message:" + progressMessage.fileId();
        progressRedisTemplate.opsForValue().set(key, progressMessage, Duration.ofHours(24));
    }

    public ProgressMessageDTO getProgressMessage(UUID fileId) {
        String key = "progress:message:" + fileId;
        ProgressMessageDTO message = progressRedisTemplate.opsForValue().get(key);

        if (message == null) {
            return new ProgressMessageDTO(
                    fileId,
                    getStatus(fileId),
                    getProgress(fileId),
                    "No progress data available",
                    LocalDateTime.now()
            );
        }

        return message;
    }

    public void cleanup(UUID fileId) {
        // Удаляем все ключи связанные с файлом
        stringRedisTemplate.delete(RedisConfig.STATUS_PREFIX + fileId);
        stringRedisTemplate.delete(RedisConfig.PROGRESS_PREFIX + fileId);
        stringRedisTemplate.delete(RedisConfig.CANCELLATION_PREFIX + fileId);
        progressRedisTemplate.delete("progress:message:" + fileId);
    }
}