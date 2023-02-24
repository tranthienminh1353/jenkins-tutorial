package com.nineplus.bestwork.repository;

import java.util.List;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Modifying;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import com.nineplus.bestwork.entity.AirWayBill;

@Repository
public interface AirWayBillRepository extends JpaRepository<AirWayBill, Long> {
	AirWayBill findByCode(String code);

	List<AirWayBill> findByProjectCode(String projectCode);

	@Modifying
	@Query(value = "UPDATE AIRWAY_BILL SET status = :destinationStatus WHERE id = :airWayBillId", nativeQuery = true)
	void changeStatus(long airWayBillId, int destinationStatus);
  
	String findCodeById(long id);

	Integer countAllByCodeInAndStatus(List<String> lstCode,Integer status);

	@Query(value = " SELECT COUNT(s.id) from (SELECT ab.id FROM PROJECT p JOIN AIRWAY_BILL ab ON p.id = ab.project_code WHERE p.create_by = :createBy AND ab.status = :status " +
			"UNION SELECT ab2.id FROM ASSIGN_TASK at2 JOIN AIRWAY_BILL ab2 ON at2.project_id = ab2.project_code " +
			"WHERE at2.user_id = :id AND ab2.status = :status AND ( at2.can_view = 1 OR at2.can_edit = 1 )) s", nativeQuery = true)
	Integer countAwbUserStatus(@Param("id") long id, @Param("status") Integer status, @Param("createBy") String createBy);

	@Query(value = " SELECT COUNT(s.id) from (SELECT ab.id FROM PROJECT p JOIN AIRWAY_BILL ab ON p.id = ab.project_code WHERE p.create_by = :createBy " +
			"UNION SELECT ab2.id FROM ASSIGN_TASK at2 JOIN AIRWAY_BILL ab2 ON at2.project_id = ab2.project_code " +
			"WHERE at2.user_id = :id AND ( at2.can_view = 1 OR at2.can_edit = 1 )) s", nativeQuery = true)
	Integer countAwbUser(@Param("id") long id, @Param("createBy") String createBy);
}
