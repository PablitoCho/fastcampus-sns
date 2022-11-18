package com.fastcampus.sns.exception;

import com.fastcampus.sns.controller.response.Response;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.ExceptionHandler;
import org.springframework.web.bind.annotation.RestControllerAdvice;

@Slf4j
@RestControllerAdvice
public class GlobalControllerAdvice {

    /**
     * ExceptionHandler 는 @Controller , @RestController 가 적용된 Bean 에서 발생하는 예외를 잡아서 하나의 메서드에서 처리해주는 기능이다.
     * ExceptionHandler 에 설정한 예외가 발생하면 handler가 실행된다. @Controller, @RestController가 아닌 @Service 나 @Repository 가 적용된 Bean에서는 사용할 수 없다.
     * ExceptionHandler 인터페이스로 들어가 보면 아래와 같다.
     *
     * ControllerAdvice는 @Controller 어노테이션이 있는 모든 곳에서의 예외를 잡을 수 있도록 해준다.
     * ControllerAdvice 안에 있는 @ExceptionHandler는 모든 컨트롤러에서 발생하는 예외상황을 잡을 수 있다.
     * ControllerAdvice 의 속성 설정을 통하여 원하는 컨트롤러나 패키지만 선택할 수 있다. 따로 지정을 하지 않으면 모든 패키지에 있는 컨트롤러를 담당하게 된다.
     *
     * RestControllerAdvice 도 @ControllerAdvice와 동일한 역할을 한다. 단지 객체를 반환할 수 있다라는 의미를 가지고 있다.
     * ControllerAdvice 와 달리 응답의 body에 객체를 넣어 반환이 가능하다. @RestController에서의 예외만 잡는다는 뜻이 아니다.
     * RestController에서 발생하든 @Controller 에서 발생하든 @RestControllerAdvice는 다 잡을 수 있다.
     */
    @ExceptionHandler(SnsApplicationException.class)
    public ResponseEntity<?> applicationHandler(SnsApplicationException e) {
        log.error("Error occurs {}", e.toString());
        return ResponseEntity.status(e.getErrorCode().getStatus())
                .body(Response.error(e.getErrorCode().name()));
    }

    @ExceptionHandler(RuntimeException.class)
    public ResponseEntity<?> applicationHandler(RuntimeException e) {
        log.error("Error occurs {}", e.toString());
        return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR)
                .body(Response.error(ErrorCode.INTERNAL_SERVER_ERROR.name()));
    }
}
