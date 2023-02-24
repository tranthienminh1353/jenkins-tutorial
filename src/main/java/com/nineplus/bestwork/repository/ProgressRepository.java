package com.nineplus.bestwork.repository;

import java.util.List;

import javax.persistence.Tuple;
import javax.transaction.Transactional;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Modifying;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import com.nineplus.bestwork.entity.ProgressEntity;

@Repository
public interface ProgressRepository extends JpaRepository<ProgressEntity, Long> {
	List<ProgressEntity> findByConstructionId(Long constructionId);

	List<ProgressEntity> findByConstructionIdOrderByIdDesc(Long valueOf);

	@Query(value = "SELECT p.id FROM PROGRESS_TRACKING p WHERE p.project_id in ?1", nativeQuery = true)
	List<Long> getAllProgressByProject(List<String> listProjectId);

	@Query(value = " delete from PROGRESS_TRACKING p where p.construction_id in :cstrtIds ", nativeQuery = true)
	@Modifying
	@Transactional
	void deleteByCstrtIdList(Long[] cstrtIds);

	@Query(value = " SELECT s.* FROM (SELECT c.construction_name, pt.title, pt.create_date , pt.status, c.id, c.start_date, c.end_date " +
			"FROM CONSTRUCTION c JOIN PROJECT p ON p.id = c.project_code JOIN ASSIGN_TASK at ON p.id = at.project_id " +
			"JOIN PROGRESS_TRACKING pt ON c.id = pt.construction_id " +
			"WHERE at.user_id = :userId AND (at.can_view = 1 OR at.can_edit = 1) " +
			"UNION " +
			"SELECT c2.construction_name, pt2.title, pt2.create_date , pt2.status, c2.id, c2.start_date, c2.end_date " +
			"FROM CONSTRUCTION c2 JOIN PROJECT p2 ON p2.id = c2.project_code " +
			"JOIN PROGRESS_TRACKING pt2 ON c2.id = pt2.construction_id " +
			"WHERE p2.create_by = :createBy )s " +
			"ORDER BY s.create_date DESC LIMIT 5", nativeQuery = true)
	List<Tuple> getProgressUser(@Param("userId") Long userId, @Param("createBy") String createBy);

}
