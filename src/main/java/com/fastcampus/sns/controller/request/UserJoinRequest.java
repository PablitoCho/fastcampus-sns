package com.fastcampus.sns.controller.request;

import lombok.AllArgsConstructor;
import lombok.Getter;

/**
 * 회원가입시 request body로 사용할 package
 */
@Getter
@AllArgsConstructor
public class UserJoinRequest {

    private String userName;
    private String password;
}
