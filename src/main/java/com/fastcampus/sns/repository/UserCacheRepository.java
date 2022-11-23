package com.fastcampus.sns.repository;

import com.fastcampus.sns.model.User;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.data.redis.core.RedisTemplate;
import org.springframework.stereotype.Repository;

import java.time.Duration;
import java.util.Optional;

@Slf4j
@Repository
@RequiredArgsConstructor
public class UserCacheRepository {

    // Redis에 User 정보를 caching하고, Cache에서 User 정보를 가져오는 repository
    private final RedisTemplate<String, User> userRedisTemplate;
    /*
     * Redis에 캐싱할 때는 TTL(Time to Live)를 걸어야 한다.
     * 한번 접속하고 다시 돌아오지 않는 사용자라면? TTL이 없으면 캐시에 계속 남게 된다.
     * 유휴 회원은 캐시에서 만료되게 하고, 활성 회원만 캐싱할 수 있다. + Redis의 용량도 효율적으로 활용할 수 있다.
     */
    private final static Duration USER_CACHE_TTL = Duration.ofDays(3); // TTL 3 days

    public void setUser(User user) {
        String key = getKey(user.getUsername());
        log.info("Set User to Redis {}:{}", key, user);
      userRedisTemplate.opsForValue().set(key, user, USER_CACHE_TTL); //setIfAbsent를 걸어도 되고, TTL이 있으니 그냥 Set을 사용해도 큰 문제없다.
    }

    public Optional<User> getUser(String userName) {
        String key = getKey(userName);
        User user = userRedisTemplate.opsForValue().get(key);
        log.info("Get data from Redis {}:{}", key, user);
        return Optional.ofNullable(user);
    }

    // User DB에 가장 많이 접근하는 filter에서 key를 userName으로 사용 > redis에서 key로 사용
    // 또한, 나중에 다른 정보도 캐싱할 수 있으므로 캐싱하는 데이터에 따라 prefix를 붙이는 것이 필요
    private String getKey(String userName) {
        return "USER:" + userName;
    }

}
