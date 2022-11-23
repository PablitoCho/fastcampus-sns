package com.fastcampus.sns.service;

import com.fastcampus.sns.exception.ErrorCode;
import com.fastcampus.sns.exception.SnsApplicationException;
import com.fastcampus.sns.model.Alarm;
import com.fastcampus.sns.model.User;
import com.fastcampus.sns.model.entity.UserEntity;
import com.fastcampus.sns.repository.AlarmEntityRepository;
import com.fastcampus.sns.repository.UserCacheRepository;
import com.fastcampus.sns.repository.UserEntityRepository;
import com.fastcampus.sns.util.JwtTokenUtils;
import lombok.RequiredArgsConstructor;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.security.core.Authentication;
import org.springframework.security.crypto.bcrypt.BCryptPasswordEncoder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.Optional;

@Service
@RequiredArgsConstructor
public class UserService {

    private final UserEntityRepository userEntityRepository;
    private final AlarmEntityRepository alarmEntityRepository;
    private final BCryptPasswordEncoder encoder;
    private final UserCacheRepository userCacheRepository;

    @Value("${jwt.secret-key}")
    private String secretKey;

    @Value("${jwt.token.expired-time-ms}")
    private Long expiredTimeMs;

    @Transactional // join 도중 exception 발생시, entity를 save하는 부분이 rollback이 된다.
    public User join(String userName, String password) {
        // 1. userName이 이미 등록되었는지 확인
        userEntityRepository.findByUserName(userName).ifPresent(it -> {
            throw new SnsApplicationException(ErrorCode.DUPLICATED_USER_NAME, String.format("%s is duplicated", userName));
        });

        // 2. 회원 가입이 가능하다면 진행 = user DB 등록 (repository 필요)
        UserEntity userEntity = userEntityRepository.save(UserEntity.of(userName, encoder.encode(password)));
        return User.fromEntity(userEntity);
    }

    // TODO : implement
    public String login(String userName, String password)  {
        // 1-1. 회원가입 여부 확인
        User user = loadUserByUserName(userName);
//                userEntityRepository
//                .findByUserName(userName)
//                .orElseThrow(() -> new SnsApplicationException(ErrorCode.USER_NOT_FOUND, String.format("%s not found", userName)));

        // 1-2. 캐시에 데이터 올리기
        userCacheRepository.setUser(user);

        // 2. 회원이 존재한다면, 비밀번호 확인
        if(!encoder.matches(password, user.getPassword())){
//        if(!userEntity.getPassword().equals(password)) {
            throw new SnsApplicationException(ErrorCode.INVALID_PASSWORD);
        }

        // 3. Token 생성
        String token = JwtTokenUtils.generateToken(userName, secretKey, expiredTimeMs);

        //return token(encrypted string) when succeeds
        return token;
    }

    public User loadUserByUserName(String userName) {
        return userCacheRepository
                    .getUser(userName) // 캐시에 없는 경우 null
                    .orElseGet( // 캐시에 없는 경우 DB에서 조회
                        () -> userEntityRepository.findByUserName(userName).map(User::fromEntity)
                                .orElseThrow(() -> new SnsApplicationException(ErrorCode.USER_NOT_FOUND, String.format("%s user not found.", userName)))
                    );
    }

    public Page<Alarm> alarmList(Integer userId, Pageable pageable) {
        return alarmEntityRepository.findAllByUserId(userId, pageable).map(Alarm::fromEntity);
    }
}
