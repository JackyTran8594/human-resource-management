package com.ansv.humanresource.repository;

import com.ansv.humanresource.model.UserEntity;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.JpaSpecificationExecutor;
import org.springframework.data.jpa.repository.Modifying;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import javax.transaction.Transactional;
import java.util.List;
import java.util.Optional;

@Repository
public interface UserEntityRepository extends JpaRepository<UserEntity, Long>, JpaSpecificationExecutor<UserEntity>, UserEntityRepositoryCustom {

    @Transactional
    @Modifying
    @Query(value = "DELETE FROM user_entity WHERE id IN :listId", nativeQuery = true)
    Integer deleteByListId(@Param("listId") List<Long> listId);

    UserEntity findByUsername(String username);

}
