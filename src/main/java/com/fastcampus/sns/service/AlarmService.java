package com.fastcampus.sns.service;

import com.fastcampus.sns.exception.ErrorCode;
import com.fastcampus.sns.exception.SnsApplicationException;
import com.fastcampus.sns.repository.EmitterRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.web.servlet.mvc.method.annotation.SseEmitter;

import java.io.IOException;

@Slf4j
@Service
@RequiredArgsConstructor
public class AlarmService {

    private final static Long DEFAULT_TIMEOUT = 60L * 1000 * 60;
    private final static String ALARM_NAME = "alarm";

    private final EmitterRepository emitterRepository;

    public SseEmitter connectAlarm(Integer userId) {
        // SseEmitter instance는 브라우저 connect당 하나의 instance가 생긴다.
        // 즉, 생성된 SseEmitter instance들을 저장하고 관리할 class 필요(EmitterRepository)
        SseEmitter sseEmitter = new SseEmitter(DEFAULT_TIMEOUT);
        emitterRepository.save(userId, sseEmitter); // 접속한 브라우저(User)에 생성된 emitter 저장
        sseEmitter.onCompletion(() -> emitterRepository.delete(userId)); // 끝날 때의 동작
        sseEmitter.onTimeout(() -> emitterRepository.delete(userId)); // Timeout시의 동작.
        try {
            sseEmitter.send(SseEmitter.event()
                    .id("")
                    .name(ALARM_NAME) // event name (브라우저에서 listener가 듣고 있는 event 이름)
                    .data("connect completed")
            );
        } catch (IOException exception) {
            throw new SnsApplicationException(ErrorCode.ALARM_CONNECT_ERROR);
        }
        return sseEmitter;
    }

    public void send(Integer alarmId, Integer userId) {
        emitterRepository.get(userId)
            .ifPresentOrElse(sseEmitter -> {
                try {
                    sseEmitter.send(
                            SseEmitter.event()
                                    .id(alarmId.toString())
                                    .name(ALARM_NAME)
                                    .data("new alarm")
                    );
                } catch (IOException e) {
                    // 에러 발생시 해당 emitter를 캐시로 들고 있을 필요가 없음(삭제)
                    emitterRepository.delete(userId);
                    throw new SnsApplicationException(ErrorCode.ALARM_CONNECT_ERROR);
                }
            }, () -> log.info("No emitter found")); // 알람을 받는 User가 브라우저에 접속하지 않은 경우 emitter가 존재하지 않음.(subscription을 안 했으므로)
    }
}
