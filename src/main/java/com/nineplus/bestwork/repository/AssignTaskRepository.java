package com.nineplus.bestwork.repository;

import java.util.List;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;

import com.nineplus.bestwork.entity.AssignTaskEntity;
import org.springframework.data.repository.query.Param;

public interface AssignTaskRepository extends JpaRepository<AssignTaskEntity, Long> {

	@Query(value = "SELECT * FROM ASSIGN_TASK WHERE user_id = ?1 and company_id = ?2 and project_id = ?3", nativeQuery = true)
	AssignTaskEntity findbyCondition(Long userId, Long companyId, String projectId);

	@Query(value = " select * from ASSIGN_TASK where project_id = :projectId and user_id = :userId  ", nativeQuery = true)
	AssignTaskEntity findByProjectIdAndUserId(String projectId, long userId);

	@Query(value = "select distinct p.id as projectId, p.project_name as projectName, CASE WHEN p.create_by = :userName THEN 1 ELSE can_view END as canView, CASE WHEN p.create_by = :userName THEN 1 ELSE can_edit END as canEdit "
			+ "from PROJECT p LEFT JOIN ASSIGN_TASK ast  on p.id = ast.project_id LEFT JOIN T_SYS_APP_USER u on ast.user_id = u.id WHERE u.id = :userId AND u.user_name = :userName OR p.create_by = :userName", nativeQuery = true)
	List<UserProjectRepository> findListProjectByUser(long userId, String userName);

	List<AssignTaskEntity> findByProjectId(String id);

	@Query(value = " SELECT COUNT(p.id) FROM PROJECT p JOIN ASSIGN_TASK at ON at.project_id = p.id WHERE " +
			" ( at.can_view = 1 OR at.can_edit = 1 ) AND at.user_id = :userId ", nativeQuery = true)
	Integer countAllByUserId(@Param("userId") Long userId);

	@Query(value = " SELECT COUNT(s.id) from( SELECT p.id FROM PROJECT p JOIN ASSIGN_TASK at ON at.project_id = p.id WHERE " +
			"MONTH(p.start_date) = :month AND  YEAR(p.start_date) = :year AND " +
			"( at.can_view = 1 OR at.can_edit = 1 ) AND at.user_id = :userId " +
			"UNION SELECT p2.id FROM PROJECT p2 WHERE " +
			"MONTH(p2.start_date) = :month AND  YEAR(p2.start_date) = :year AND p2.create_by = :createBy ) s", nativeQuery = true)
	Integer countAllByUserIdTime(@Param("month") Integer month, @Param("year") Integer year, @Param("userId") Long userId, @Param("createBy") String createBy);
}
