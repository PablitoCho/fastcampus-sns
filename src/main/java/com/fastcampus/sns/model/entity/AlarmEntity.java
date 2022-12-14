package com.fastcampus.sns.model.entity;

import com.fastcampus.sns.model.AlarmArgs;
import com.fastcampus.sns.model.AlarmType;
import com.vladmihalcea.hibernate.type.json.JsonBinaryType;
import lombok.Getter;
import lombok.Setter;
import org.hibernate.annotations.SQLDelete;
import org.hibernate.annotations.Type;
import org.hibernate.annotations.TypeDef;
import org.hibernate.annotations.Where;

import javax.persistence.*;
import java.sql.Timestamp;
import java.time.Instant;

@Entity
@Table(name = "\"alarm\"", indexes = {
    @Index(name = "user_id_idx", columnList = "user_id") // user로 조회하는 경우가 대부분이기에 index 설정
})
@Getter @Setter
@TypeDef(name = "jsonb", typeClass = JsonBinaryType.class) // jsonb 타입을 사용하기 위해..
@SQLDelete(sql="UPDATE \"alarm\" SET deleted_at = NOW() WHERE id=?") // soft delete (Like에는 삭제 기능이 없어서 불필요함)
@Where(clause = "deleted_at is NULL")
public class AlarmEntity {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Integer id;

    /*
     * ManyToOne default fetch type : EAGER (비효율적). 즉시 join된 데이터를 모두 가져온다(사용여부와 상관없이)
     * fetch type을 Lazy로 바꾼다면? 일단 alarm 정보만 가져온 다음 join된 user 정보는 실제 사용될때 가져온다.
     * (실제 user정보가 필요할 때, 접근을 할때 가져온다.)
     *
     * But, Lazy로 바꾸는 것이 N+1 문제의 근본적인 해결책은 아님. 단지 불필요한 query가 안 나갈뿐...
     * Repository에서 @Query annotation을 통해 불필요한 데이터를 가져오지 않도록(N+1문제가 발생하지 않도록) 수정하는 것이 근본적인 해결책에 더 가까움.
     */
    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "user_id")
    private UserEntity user; // Alarm을 받는 사람 (발생시킨 사람 x)

    @Enumerated(EnumType.STRING)
    private AlarmType alarmType;

    /*
     * alarm text에 들어갈 argument (comment 작성자, like 누른 사람, alarm이 발생한 post 등)
     * 당장 사용하지 않더라도, 향후 확장될 가능성을 고려해서 미리 entity 설계에 포함시켜둠
     * 기본 자료형이 아닌 경우, json으로 저장.
     * 향후 추가적인 필드가 추가되는 등의 번경의 여지가 높기에 유연한 json 타입으로 저장하는 것이 좋은 선택
     * 알람의 성격에 따라 null인 값들이 많을 것으로 예상. 이를 미리 column으로 다 준비해놓으면 null로 다 저장되면서 DB 공간을 비효율적으로 낭비할 수도 있음. > 따라서 json arg 형식으로 유연하게 대처
     */
    // jsonb <- json을 압축해서 저장. jsonb 타입에만 index를 걸 수 있음. (매우 중요!). JPA에서는 jsonb 지원 X(postgreSQL의 기능) > 추가 의존성 hibernate-types 필요
    @Type(type = "jsonb")
    @Column(columnDefinition = "json")
    private AlarmArgs args;

    @Column(name = "registered_at")
    private Timestamp registeredAt;

    @Column(name = "updated_at")
    private Timestamp updatedAt;

    @Column(name = "deleted_at")
    private Timestamp deletedAt;

    @PrePersist
    void registeredAt() {
        this.registeredAt = Timestamp.from(Instant.now());
    }

    @PreUpdate
    void updatedAt() {
        this.updatedAt = Timestamp.from(Instant.now());
    }

    public static AlarmEntity of(UserEntity userEntity, AlarmType alarmType, AlarmArgs args) {
        AlarmEntity entity = new AlarmEntity();
        entity.setUser(userEntity);
        entity.setAlarmType(alarmType);
        entity.setArgs(args);
        return entity;
    }
}
