package com.fastcampus.sns.model;

import lombok.AllArgsConstructor;
import lombok.Data;

@Data
@AllArgsConstructor
public class AlarmArgs {

    private Integer fromUserId; // alarm을 발생시킨 use의 id
    private Integer targetId; // 알람을 발생시킨 위치의 id(post id, comment id)

}
