package com.likelion.a1.user.infrastructure.persistence;

import com.likelion.a1.user.domain.model.User;
import java.util.Optional;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

interface SpringDataUserRepository extends JpaRepository<User, Long> {
  Optional<User> findByLoginId(String loginId);

  Optional<User> findByEmailIgnoreCase(String email);

  boolean existsByLoginId(String loginId);

  boolean existsByEmailIgnoreCase(String email);

  Page<User> findByApprovalStatusAndDeletedAtIsNullOrderByCreatedAtDesc(
      String approvalStatus, Pageable pageable);

  @Query(
      """
      SELECT u
      FROM User u
      WHERE u.deletedAt IS NULL
        AND (:approvalStatus IS NULL OR u.approvalStatus = :approvalStatus)
        AND (:accountStatus IS NULL OR u.accountStatus = :accountStatus)
        AND (
          :keyword IS NULL
          OR LOWER(u.loginId) LIKE LOWER(CONCAT('%', :keyword, '%'))
          OR LOWER(u.email) LIKE LOWER(CONCAT('%', :keyword, '%'))
          OR LOWER(u.name) LIKE LOWER(CONCAT('%', :keyword, '%'))
        )
      ORDER BY u.createdAt DESC
      """)
  Page<User> searchUsers(
      @Param("approvalStatus") String approvalStatus,
      @Param("accountStatus") String accountStatus,
      @Param("keyword") String keyword,
      Pageable pageable);
}
