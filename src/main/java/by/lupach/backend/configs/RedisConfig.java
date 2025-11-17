package by.lupach.backend.configs;

import by.lupach.backend.dtos.AnalysisResultDTO;
import by.lupach.backend.dtos.FileQueueMessageDTO;
import by.lupach.backend.dtos.ProgressMessageDTO;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.datatype.jsr310.JavaTimeModule;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.data.redis.connection.RedisConnectionFactory;
import org.springframework.data.redis.connection.lettuce.LettuceConnectionFactory;
import org.springframework.data.redis.core.RedisTemplate;
import org.springframework.data.redis.serializer.Jackson2JsonRedisSerializer;
import org.springframework.data.redis.serializer.StringRedisSerializer;
import org.springframework.beans.factory.annotation.Value;

import java.util.UUID;

@Configuration
public class RedisConfig {

    @Value("${spring.data.redis.host}")
    private String redisHost;

    @Value("${spring.data.redis.port}")
    private int redisPort;

    public static final String FILE_PROCESSING_QUEUE = "file:processing:queue";
    public static final String FILE_CANCEL_QUEUE = "file:cancel:queue";
    public static final String STATUS_PREFIX = "file:status:";
    public static final String PROGRESS_PREFIX = "file:progress:";
    public static final String ANALYSIS_PREFIX = "file:analysis:";

    @Bean
    public RedisConnectionFactory redisConnectionFactory() {
        return new LettuceConnectionFactory(redisHost, redisPort);
    }

    private ObjectMapper createObjectMapper() {
        ObjectMapper objectMapper = new ObjectMapper();
        objectMapper.registerModule(new JavaTimeModule());
        objectMapper.disable(com.fasterxml.jackson.databind.SerializationFeature.WRITE_DATES_AS_TIMESTAMPS);
        return objectMapper;
    }

    @Bean
    public RedisTemplate<String, FileQueueMessageDTO> fileQueueRedisTemplate(RedisConnectionFactory factory) {
        RedisTemplate<String, FileQueueMessageDTO> template = new RedisTemplate<>();
        template.setConnectionFactory(factory);

        ObjectMapper objectMapper = createObjectMapper();
        Jackson2JsonRedisSerializer<FileQueueMessageDTO> serializer =
                new Jackson2JsonRedisSerializer<>(objectMapper, FileQueueMessageDTO.class);

        template.setKeySerializer(new StringRedisSerializer());
        template.setValueSerializer(serializer);
        template.setHashKeySerializer(new StringRedisSerializer());
        template.setHashValueSerializer(serializer);
        template.afterPropertiesSet();

        return template;
    }

    @Bean
    public RedisTemplate<String, AnalysisResultDTO> fileAnalysisRedisTemplate(RedisConnectionFactory factory) {
        RedisTemplate<String, AnalysisResultDTO> template = new RedisTemplate<>();
        template.setConnectionFactory(factory);

        ObjectMapper objectMapper = createObjectMapper();
        Jackson2JsonRedisSerializer<AnalysisResultDTO> serializer =
                new Jackson2JsonRedisSerializer<>(objectMapper, AnalysisResultDTO.class);

        template.setKeySerializer(new StringRedisSerializer());
        template.setValueSerializer(serializer);
        template.setHashKeySerializer(new StringRedisSerializer());
        template.setHashValueSerializer(serializer);
        template.afterPropertiesSet();

        return template;
    }

    @Bean
    public RedisTemplate<String, UUID> uuidRedisTemplate(RedisConnectionFactory factory) {
        RedisTemplate<String, UUID> template = new RedisTemplate<>();
        template.setConnectionFactory(factory);

        ObjectMapper objectMapper = createObjectMapper();
        Jackson2JsonRedisSerializer<UUID> serializer =
                new Jackson2JsonRedisSerializer<>(objectMapper, UUID.class);

        template.setKeySerializer(new StringRedisSerializer());
        template.setValueSerializer(serializer);
        template.setHashKeySerializer(new StringRedisSerializer());
        template.setHashValueSerializer(serializer);
        template.afterPropertiesSet();

        return template;
    }


    @Bean
    public RedisTemplate<String, ProgressMessageDTO> progressRedisTemplate(RedisConnectionFactory factory) {
        RedisTemplate<String, ProgressMessageDTO> template = new RedisTemplate<>();
        template.setConnectionFactory(factory);

        ObjectMapper objectMapper = createObjectMapper();
        Jackson2JsonRedisSerializer<ProgressMessageDTO> serializer =
                new Jackson2JsonRedisSerializer<>(objectMapper, ProgressMessageDTO.class);

        template.setKeySerializer(new StringRedisSerializer());
        template.setValueSerializer(serializer);
        template.setHashKeySerializer(new StringRedisSerializer());
        template.setHashValueSerializer(serializer);
        template.afterPropertiesSet();

        return template;
    }
}