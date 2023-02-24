package com.nineplus.bestwork.repository;

import java.util.List;
import java.util.Optional;
import java.util.Set;
import java.util.TreeSet;

import org.springframework.beans.factory.annotation.Value;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Modifying;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;
import org.springframework.transaction.annotation.Transactional;

import com.nineplus.bestwork.entity.RoleEntity;

@Repository
public interface RoleRepository extends JpaRepository<RoleEntity, Long> {
	@Value("${Role.insert}")
	static final String insertSQL = null;

	Optional<RoleEntity> findTRoleByRoleNameContains(String roleName);

	Page<RoleEntity> findTRolesByRoleNameContaining(String roleName, Pageable pageable);

	@Query(value = "SELECT * FROM T_SYS_APP_ROLE WHERE UPPER(name) = :role AND create_by = :createBy", nativeQuery = true)
	RoleEntity findRole(String role, String createBy);

	@Query(value = "SELECT * FROM T_SYS_APP_ROLE WHERE id = :roleId", nativeQuery = true)
	RoleEntity findRole(Long roleId);

	@Query(value = "SELECT * FROM T_SYS_APP_ROLE WHERE create_by = :username AND name NOT IN (:exceptList) ORDER BY id ", nativeQuery = true)
	List<RoleEntity> getRoleCreateByExcept(@Param("username") String username, @Param("exceptList") List<String> exceptList);

	@Query(value = "SELECT * FROM T_SYS_APP_ROLE WHERE create_by = :username AND id > (:id) ", nativeQuery = true)
	Set<RoleEntity> getRoleEntityByCreateByOrderByIdAsc(@Param("username") String createBy, @Param("id") Long id);

	@Query(value = "SELECT * FROM T_SYS_APP_ROLE WHERE create_by = :username", nativeQuery = true)
	Set<RoleEntity> getRoleEntityByCreateBy(@Param("username") String createBy);
}
