package by.lupach.backend.services.redis;

import by.lupach.backend.configs.RedisConfig;
import by.lupach.backend.dtos.AnalysisResultDTO;
import by.lupach.backend.entities.AnalysisResult;
import lombok.RequiredArgsConstructor;
import org.springframework.data.redis.core.RedisTemplate;
import org.springframework.stereotype.Service;

import java.time.Duration;
import java.util.Optional;
import java.util.UUID;

@Service
@RequiredArgsConstructor
public class FileAnalysisRedisService {
    private final RedisTemplate<String, AnalysisResultDTO> redis;

    public void set(UUID fileId, AnalysisResultDTO analysisResultDTO) {
        redis.opsForValue().set(RedisConfig.ANALYSIS_PREFIX + fileId, analysisResultDTO, Duration.ofMinutes(30));
    }

    public Optional<AnalysisResultDTO> get(UUID fileId) {
        return Optional.ofNullable(redis.opsForValue().get(RedisConfig.ANALYSIS_PREFIX + fileId));
    }

    public void delete(UUID fileId) {
        redis.delete(RedisConfig.ANALYSIS_PREFIX + fileId);
    }
}
