//package by.lupach.backend.services.redis;
//
//import by.lupach.backend.configs.RedisConfig;
//import lombok.RequiredArgsConstructor;
//import org.springframework.data.redis.core.RedisTemplate;
//import org.springframework.stereotype.Service;
//
//import java.time.Duration;
//import java.util.UUID;
//
//@Service
//@RequiredArgsConstructor
//public class CancellationRedisService {
//    private final RedisTemplate<String, String> redis;
//
//    public void requestCancellation(UUID fileId) {
//        redis.opsForValue().set(RedisConfig.CANCELLATION_PREFIX + fileId, "true", Duration.ofMinutes(30));
//    }
//
//    public void deleteCancellationFlag(UUID fileId) {
//        redis.delete(RedisConfig.CANCELLATION_PREFIX + fileId);
//    }
//}
