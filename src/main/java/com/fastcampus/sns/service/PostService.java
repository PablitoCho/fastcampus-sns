package com.fastcampus.sns.service;


import com.fastcampus.sns.exception.ErrorCode;
import com.fastcampus.sns.exception.SnsApplicationException;
import com.fastcampus.sns.model.AlarmArgs;
import com.fastcampus.sns.model.AlarmType;
import com.fastcampus.sns.model.Comment;
import com.fastcampus.sns.model.Post;
import com.fastcampus.sns.model.entity.*;
import com.fastcampus.sns.repository.*;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;

@Service
@RequiredArgsConstructor
public class PostService {

    private final PostEntityRepository postEntityRepository;

    private final UserEntityRepository userEntityRepository;

    private final LikeEntityRepository likeEntityRepository;

    private final CommentEntityRepository commentEntityRepository;

    private final AlarmEntityRepository alarmEntityRepository;


    @Transactional
    public void create(String title, String body, String userName) {
        UserEntity userEntity = getUserEntityOrException(userName);
        postEntityRepository.save(PostEntity.of(title, body, userEntity));
    }


    @Transactional
    public Post modify(String title, String body, String userName, Integer postId) {
        UserEntity userEntity =  getUserEntityOrException(userName);
        // Post 존재 여부 확인
        PostEntity postEntity = getPostEntityOrException(postId);
        // Post 원 작성자 본인지 확인
        if(postEntity.getUser() != userEntity) {
            throw new SnsApplicationException(ErrorCode.INVALID_PERMISSION, String.format("%s has no permission with %s", userName, postId));
        }

        // save post
        postEntity.setTitle(title);
        postEntity.setBody(body);
        return Post.fromEntity(postEntityRepository.saveAndFlush(postEntity)); // Post object 반환
    }

    @Transactional
    public void delete(String userName, Integer postId) {
        UserEntity userEntity = getUserEntityOrException(userName);
        // Post 존재 여부 확인
        PostEntity postEntity = getPostEntityOrException(postId);
        // Post 원 작성자 본인지 확인
        if(postEntity.getUser() != userEntity) {
            throw new SnsApplicationException(ErrorCode.INVALID_PERMISSION, String.format("%s has no permission with %s", userName, postId));
        }

        // delete Post
        postEntityRepository.delete(postEntity);
        // 삭제되는 Post에 달린 comment와 like도 함께 삭제
        likeEntityRepository.deleteAllByPost(postEntity);
        commentEntityRepository.deleteAllByPost(postEntity);
    }

    public Page<Post> list(Pageable pageable) {
        return postEntityRepository.findAll(pageable).map(Post::fromEntity);
    }

    public Page<Post> my(String userName, Pageable pageable) {
        UserEntity userEntity = getUserEntityOrException(userName);
        return postEntityRepository.findAllByUser(userEntity, pageable).map(Post::fromEntity);
    }

    @Transactional
    public void like(Integer postId, String userName) {
        PostEntity postEntity = getPostEntityOrException(postId);
        UserEntity userEntity =  getUserEntityOrException(userName);

        // 유저는 like를 한 번만 누를 수 있다. (이미 like를 눌렀는지 확인) -> throw exception
        likeEntityRepository.findByUserAndPost(userEntity, postEntity)
            .ifPresent(it -> {
                throw new SnsApplicationException(ErrorCode.ALREADY_LIKED, String.format("%s has already like post %s", userName, postId));
            });

        // save like
        likeEntityRepository.save(LikeEntity.of(userEntity, postEntity));

        // like 누를 때 alarm 발생
        alarmEntityRepository.save(AlarmEntity.of(
            postEntity.getUser(),
            AlarmType.NEW_LIKE_ON_POST,
            new AlarmArgs(userEntity.getId(), postEntity.getId())
        ));
    }

    public long likeCount(Integer postId) {
        // null일 필요가 없으므로 Integer가 아니라 null로 사용해도 괜찮음.
        PostEntity postEntity = getPostEntityOrException(postId);

        // count likes
//        List<LikeEntity> likeEntities = likeEntityRepository.findAllByPost(postEntity); // 갯수만 필요한데 entity를 다 들고 오는 것이 비효율적
//        return likeEntities.size();
        return likeEntityRepository.countByPost(postEntity);
    }

    @Transactional
    public void comment(Integer postId, String comment, String userName) {
        PostEntity postEntity = getPostEntityOrException(postId);
        UserEntity userEntity = getUserEntityOrException(userName);

        // comment save
        commentEntityRepository.save(CommentEntity.of(userEntity, postEntity, comment));

        // comment 저장시 alarm 발생
        alarmEntityRepository.save(AlarmEntity.of(
                postEntity.getUser(), // Post를 작성한 user가 alarm을 받는다.
                AlarmType.NEW_COMMENT_ON_POST,
                new AlarmArgs(userEntity.getId(), postEntity.getId()))
        );
    }

    public Page<Comment> getComments(Integer postId, Pageable pageable) {
        PostEntity postEntity = getPostEntityOrException(postId);
        return commentEntityRepository.findAllByPost(postEntity, pageable).map(Comment::fromEntity);
    }

    //post exist
    private PostEntity getPostEntityOrException(Integer postId) {
        return postEntityRepository.findById(postId).orElseThrow(() ->
                new SnsApplicationException(ErrorCode.POST_NOT_FOUND, String.format("%s post not found", postId)));
    }

    //user exist
    private UserEntity getUserEntityOrException(String userName) {
        return userEntityRepository.findByUserName(userName)
                .orElseThrow(() -> new SnsApplicationException(ErrorCode.USER_NOT_FOUND, String.format("%s user not found", userName)));
    }



}
