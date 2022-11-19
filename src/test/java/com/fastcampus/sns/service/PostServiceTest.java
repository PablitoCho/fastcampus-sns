package com.fastcampus.sns.service;

import com.fastcampus.sns.exception.ErrorCode;
import com.fastcampus.sns.exception.SnsApplicationException;
import com.fastcampus.sns.fixture.PostEntityFixture;
import com.fastcampus.sns.fixture.UserEntityFixture;
import com.fastcampus.sns.model.entity.PostEntity;
import com.fastcampus.sns.model.entity.UserEntity;
import com.fastcampus.sns.repository.PostEntityRepository;
import com.fastcampus.sns.repository.UserEntityRepository;
import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.test.mock.mockito.MockBean;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;

import java.util.Optional;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

@SpringBootTest
public class PostServiceTest {

    @Autowired
    private PostService postService;

    @MockBean
    private PostEntityRepository postEntityRepository;

    @MockBean
    private UserEntityRepository userEntityRepository;

    @Test
    void post_success() {
        String title = "title";
        String body = "body";
        String userName = "userName";

        // mocking
        when(userEntityRepository.findByUserName(userName))
                .thenReturn(Optional.of(mock(UserEntity.class)));
        when(postEntityRepository.save(any()))
                .thenReturn(mock(PostEntity.class));

        Assertions.assertDoesNotThrow(() -> postService.create(title, body, userName));
    }

    @Test
    void post_failure_no_such_user() {
        // Post작성 시 요청한 유저가 존재하지 않는 경우
        String title = "title";
        String body = "body";
        String userName = "userName";

        // mocking
        when(userEntityRepository.findByUserName(userName))
                .thenReturn(Optional.empty());
        when(postEntityRepository.save(any()))
                .thenReturn(mock(PostEntity.class));

        SnsApplicationException e = Assertions.assertThrows(SnsApplicationException.class,
                () -> postService.create(title, body, userName));
        Assertions.assertEquals(ErrorCode.USER_NOT_FOUND, e.getErrorCode());
    }

    @Test
    void post_modify_success() {
        String title = "title";
        String body = "body";
        String userName = "userName";
        Integer postId = 1;

        // mocking(1. find user, 2. find post 3. check permission
        PostEntity postEntity = PostEntityFixture.get(userName, postId, 1);
        UserEntity userEntity = postEntity.getUser();

        when(userEntityRepository.findByUserName(userName))
                .thenReturn(Optional.of(userEntity));

        when(postEntityRepository.findById(postId))
                .thenReturn(Optional.of(postEntity));

        when(postEntityRepository.saveAndFlush(any()))
                .thenReturn(postEntity);

        Assertions.assertDoesNotThrow(() -> postService.modify(title, body, userName, 1));
    }

    @Test
    void post_modifiy_failure_no_such_post_index() {
        String title = "title";
        String body = "body";
        String userName = "userName";
        Integer postId = 1;

        PostEntity postEntity = PostEntityFixture.get(userName, postId, 1);
        UserEntity userEntity = postEntity.getUser();

        when(userEntityRepository.findByUserName(userName))
                .thenReturn(Optional.of(userEntity));

        when(postEntityRepository.findById(postId))
                .thenReturn(Optional.empty());

        SnsApplicationException e = Assertions.assertThrows(SnsApplicationException.class,
                () -> postService.modify(title, body, userName, 1));
        Assertions.assertEquals(ErrorCode.POST_NOT_FOUND, e.getErrorCode());

    }

    @Test
    void post_modifiy_failure_not_authorized() {
        String title = "title";
        String body = "body";
        String userName = "userName";
        Integer postId = 1;

        PostEntity postEntity = PostEntityFixture.get(userName, postId, 1);
        UserEntity writer = UserEntityFixture.get("userName1", "password", 2); // 다른 유저(원작성자)

        when(userEntityRepository.findByUserName(userName))
                .thenReturn(Optional.of(writer));
        when(postEntityRepository.findById(postId))
                .thenReturn(Optional.of(postEntity));

        SnsApplicationException e = Assertions.assertThrows(SnsApplicationException.class,
                () -> postService.modify(title, body, userName, 1));
        Assertions.assertEquals(ErrorCode.INVALID_PERMISSION, e.getErrorCode());

    }

    @Test
    void post_delete_success() {
        // 삭제 성공
        String userName = "userName";
        Integer postId = 1;

        // mocking(1. find user, 2. find post 3. check permission
        PostEntity postEntity = PostEntityFixture.get(userName, postId, 1);
        UserEntity userEntity = postEntity.getUser();

        when(userEntityRepository.findByUserName(userName))
                .thenReturn(Optional.of(userEntity));

        when(postEntityRepository.findById(postId))
                .thenReturn(Optional.of(postEntity));

        Assertions.assertDoesNotThrow(() -> postService.delete(userName, 1));
    }

    @Test
    void post_delete_failure_no_such_post_index() {
        // 삭제하려는 Post가 존재하지 않는 경우
        String userName = "userName";
        Integer postId = 1;

        PostEntity postEntity = PostEntityFixture.get(userName, postId, 1);
        UserEntity userEntity = postEntity.getUser();

        when(userEntityRepository.findByUserName(userName))
                .thenReturn(Optional.of(userEntity));

        when(postEntityRepository.findById(postId))
                .thenReturn(Optional.empty());

        SnsApplicationException e = Assertions.assertThrows(SnsApplicationException.class,
                () -> postService.delete(userName, 1));
        Assertions.assertEquals(ErrorCode.POST_NOT_FOUND, e.getErrorCode());

    }

    @Test
    void post_delete_failure_not_authorized() {
        // Post를 삭제할 권한이 없는 경우
        String userName = "userName";
        Integer postId = 1;

        PostEntity postEntity = PostEntityFixture.get(userName, postId, 1);
        UserEntity writer = UserEntityFixture.get("userName1", "password", 2); // 다른 유저(원작성자)

        when(userEntityRepository.findByUserName(userName))
                .thenReturn(Optional.of(writer));
        when(postEntityRepository.findById(postId))
                .thenReturn(Optional.of(postEntity));

        SnsApplicationException e = Assertions.assertThrows(SnsApplicationException.class,
                () -> postService.delete(userName, 1));
        Assertions.assertEquals(ErrorCode.INVALID_PERMISSION, e.getErrorCode());

    }

    @Test
    void feed_list_success() {
        Pageable pageable = mock(Pageable.class);
        when(postEntityRepository.findAll(pageable)).thenReturn(Page.empty());
        Assertions.assertDoesNotThrow(() -> postService.list(pageable));
    }

    @Test
    void my_feed_list_success() {
        Pageable pageable = mock(Pageable.class);
        UserEntity user = mock(UserEntity.class);
        when(userEntityRepository.findByUserName(any())).thenReturn(Optional.of(user));
        when(postEntityRepository.findAllByUser(user, pageable)).thenReturn(Page.empty());
        Assertions.assertDoesNotThrow(() -> postService.my("", pageable));
    }
}
