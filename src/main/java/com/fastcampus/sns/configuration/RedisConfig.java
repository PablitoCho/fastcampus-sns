package com.fastcampus.sns.configuration;

import com.fastcampus.sns.model.User;
import io.lettuce.core.RedisURI;
import lombok.RequiredArgsConstructor;
import org.springframework.boot.autoconfigure.data.redis.RedisProperties;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.data.redis.connection.RedisConfiguration;
import org.springframework.data.redis.connection.RedisConnectionFactory;
import org.springframework.data.redis.connection.lettuce.LettuceConnectionFactory;
import org.springframework.data.redis.core.RedisTemplate;
import org.springframework.data.redis.repository.configuration.EnableRedisRepositories;
import org.springframework.data.redis.serializer.Jackson2JsonRedisSerializer;
import org.springframework.data.redis.serializer.StringRedisSerializer;

@Configuration
@EnableRedisRepositories
@RequiredArgsConstructor
public class RedisConfig {

    private final RedisProperties redisProperties; // yml파일에 관련 정보(prefix = "spring.redis")를 기입하면, Spring이 자동으로 만들어주는 instance

    @Bean
    public RedisConnectionFactory redisConnectionFactory() {
        RedisURI redisURI = RedisURI.create(redisProperties.getUrl());
        RedisConfiguration configuration = LettuceConnectionFactory.createRedisConfiguration(redisURI);
        LettuceConnectionFactory factory = new LettuceConnectionFactory(configuration);
        factory.afterPropertiesSet();
        return factory;
    }

    /*
     * 변경이 잦은 데이터는 캐싱의 효과가 X
     * 접근이 많은 데이터일수록 캐싱의 효과가 올라감
     * -> User 정보를 캐싱 : 매 API마다 필터에서 유저 존재 여부를 DB에서 체크 -> 캐시로 바꾸면 부하가 훨씬 줄어듦. 또한 변경이 (거의) 없음.
     * 반면에 Comment, Like, Post 등은 수정의 여지도 높으며 삭제 등의 변경이 잦다. User에 비해 접근 빈도도 낮다.
     */
    @Bean // Redis Template(command 수행) Bean으로 등록
    public RedisTemplate<String, User> userRedisTemplate() {
        RedisTemplate<String, User> redisTemplate = new RedisTemplate<>();
        redisTemplate.setConnectionFactory(redisConnectionFactory());
        redisTemplate.setKeySerializer(new StringRedisSerializer());
        redisTemplate.setValueSerializer(new Jackson2JsonRedisSerializer<User>(User.class));
        return redisTemplate;
    }

}
