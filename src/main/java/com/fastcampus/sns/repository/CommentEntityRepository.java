package com.fastcampus.sns.repository;

import com.fastcampus.sns.model.entity.CommentEntity;
import com.fastcampus.sns.model.entity.LikeEntity;
import com.fastcampus.sns.model.entity.PostEntity;
import com.fastcampus.sns.model.entity.UserEntity;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Modifying;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;
import java.util.Optional;

@Repository
public interface CommentEntityRepository extends JpaRepository<CommentEntity, Integer> {

    Page<CommentEntity> findAllByPost(PostEntity post, Pageable pageable); // Post로 index걸기

    /**
     * JPA의 맹점 : 비효율적인 delete. DELETE 작업은 JPA에서 제공하는 delete를 사용하지 않는 것이 바람직함
     * JPA는 영속성이라는 것을 관리함 (DB에서 가져온 data의 life cycle을 영속성 컨텍스트에 담아 관리한다.)
     * 즉, 영속성을 위해 일단 데이터를 DB에서 가지고 와야 한다.
     * 위의 경우 단순 삭제만 되면 되는데(DELETE query만 날리면 되는데), 먼저 삭제할 데이터들을 일단 가지고 오고 난 후, 삭제한다.
     */
    @Transactional
    @Modifying
    @Query("UPDATE CommentEntity entity SET deleted_at = NOW() WHERE entity.post = :post")
    void deleteAllByPost(@Param("post") PostEntity postEntity);

}