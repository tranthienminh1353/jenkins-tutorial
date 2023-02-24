package com.nineplus.bestwork.repository;

import java.util.List;
import java.util.Optional;

import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Modifying;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import com.nineplus.bestwork.dto.PageSearchUserDto;
import com.nineplus.bestwork.entity.UserEntity;

@Repository
public interface UserRepository extends JpaRepository<UserEntity, Long> {

	UserEntity findByUserName(String username);

	@Query(value = "select u.*" + "from " + "T_SYS_APP_USER u " + "join T_SYS_APP_ROLE tsar on "
			+ "tsar.id = u.role_id " + "left join T_COMPANY_USER tcu on " + "u.id = tcu.user_id "
			+ "left join T_COMPANY tc on " + "tcu.company_id = tc.id " + "where "
			+ "1 = (case when tsar.name in ('sysadmin','sys-companyadmin') then 1 " + "else case when ( " + "select "
			+ "count(tc2.id) " + "from " + "T_COMPANY tc2 " + "where "
			+ "STR_TO_DATE(tc2.start_date,'%Y-%m-%dT%H:%i:%s') <= STR_TO_DATE(:now,'%Y-%m-%dT%H:%i:%s') "
			+ "and  STR_TO_DATE(tc2.expired_date,'%Y-%m-%dT%H:%i:%s') >= STR_TO_DATE(:now,'%Y-%m-%dT%H:%i:%s') "
			+ " and tc2.id = tc.id) > 0 " + "then 1 " + "else 0 " + "end " + "end ) "
			+ "and u.user_name = :username and u.enable = 1 " + "and u.count_login_failed <= 5 ", nativeQuery = true)
	UserEntity findByUserNameLogIn(@Param("username") String username, @Param("now") String now);

	UserEntity findByEmail(String email);

	@Query(value = "select u.* from T_SYS_APP_USER u JOIN T_COMPANY_USER tcu ON (u.id = tcu.user_id) where tcu.company_id in ?1", nativeQuery = true)
	List<UserEntity> findAllUserByCompanyIdList(List<Long> ids);
	
	@Modifying
	@Query(value = "UPDATE T_SYS_APP_USER c set c.enable = 0 where c.id in ?1", nativeQuery = true)
	void inactiveUser(List<Long> ids);

	@Query(value = "select t.* from T_SYS_APP_USER t JOIN T_COMPANY_USER tcu ON (t.id = tcu.user_id) where tcu.company_id = :companyId ORDER BY tcu.user_id  LIMIT 1 ", nativeQuery = true)
	UserEntity findUserByOrgId(Long companyId);
	
	@Query(value = """
			select uc.company_id from T_COMPANY_USER uc join T_SYS_APP_USER u on u.id = uc.user_id
			where u.user_name = :username""", nativeQuery = true)
	int findCompanyIdByAdminUsername(@Param("username") String companyAdminUsername);

	@Query(value = """
			select * from T_SYS_APP_USER u
			join T_COMPANY_USER uc on uc.user_id = u.id
			where uc.company_id = :companyId """, countQuery = """
			select * from T_SYS_APP_USER u
			join T_COMPANY_USER uc on uc.user_id = u.id
			where uc.company_id = :companyId """, nativeQuery = true)
	Page<UserEntity> findAllUsersByCompanyId(@Param("companyId") int companyId, Pageable pageable);

	@Query(value = " select u.* from T_SYS_APP_USER u " + " where user_name like %?1% " + " or first_name like %?1% "
			+ " or last_name like %?1% " + " or email like %?1% " + " or tel_no like %?1%", nativeQuery = true)
	List<UserEntity> getUsersByKeyword(String keyword);

	@Query(value = """
			select tsau.* from T_SYS_APP_USER tsau
			join T_COMPANY_USER tcu on tsau.id = tcu.user_id
			join T_SYS_APP_ROLE tsar on tsar.id = tsau.role_id
			join T_COMPANY tc on tc.id = tcu.company_id
			where (tsau.email like :#{#pageCondition.keyword} or tsau.user_name like :#{#pageCondition.keyword}
			or tsau.first_name like :#{#pageCondition.keyword} or tsau.last_name like :#{#pageCondition.keyword}
			or tsau.tel_no like :#{#pageCondition.keyword} )
			and tsau.enable like :#{#pageCondition.status}
			and tsar.id like :#{#pageCondition.role}
			and tc.id like :companyId
			""", nativeQuery = true, countQuery = """
			select tsau.* from T_SYS_APP_USER tsau
			join T_COMPANY_USER tcu on tsau.id = tcu.user_id
			join T_SYS_APP_ROLE tsar on tsar.id = tsau.role_id
			join T_COMPANY tc on tc.id = tcu.company_id
			where (tsau.email like :#{#pageCondition.keyword}
			or tsau.user_name like :#{#pageCondition.keyword}
			or tsau.first_name like :#{#pageCondition.keyword}
			or tsau.last_name like :#{#pageCondition.keyword}
			or tsau.tel_no like :#{#pageCondition.keyword} )
			and tsau.enable like :#{#pageCondition.status}
			and tsar.id like :#{#pageCondition.role}
			and tc.id like :companyId
			""")
	Page<UserEntity> getAllUsers(Pageable pageable, @Param("companyId") String companyId,
			@Param("pageCondition") PageSearchUserDto pageCondition);

	@Query(value = """
			select tsau.* from T_SYS_APP_USER tsau
			join T_SYS_APP_ROLE tsar on tsar.id = tsau.role_id
			where (tsau.create_by = :curUsername
			or tsau.create_by in (select u.user_name from T_SYS_APP_USER u where u.create_by = :curUsername))
			and (tsau.email like :#{#pageCondition.keyword} or tsau.user_name like :#{#pageCondition.keyword}
			or tsau.first_name like :#{#pageCondition.keyword} or tsau.last_name like :#{#pageCondition.keyword}
			or tsau.tel_no like :#{#pageCondition.keyword} )
			and tsau.enable like :#{#pageCondition.status}
			and tsar.id like :#{#pageCondition.role} """, nativeQuery = true, countQuery = """
			select tsau.* from T_SYS_APP_USER tsau
			join T_SYS_APP_ROLE tsar on tsar.id = tsau.role_id
			where (tsau.create_by = :curUsername
			or tsau.create_by in (select u.user_name from T_SYS_APP_USER u where u.create_by = :curUsername))
			and (tsau.email like :#{#pageCondition.keyword} or tsau.user_name like :#{#pageCondition.keyword}
			or tsau.first_name like :#{#pageCondition.keyword} or tsau.last_name like :#{#pageCondition.keyword}
			or tsau.tel_no like :#{#pageCondition.keyword} )
			and tsau.enable like :#{#pageCondition.status}
			and tsar.id like :#{#pageCondition.role} """)
	Page<UserEntity> getAllUsersBySysComAdmin(Pageable pageable, String curUsername, PageSearchUserDto pageCondition);

	@Query(value = """
			select tsau.* from T_SYS_APP_USER tsau
			join T_SYS_APP_ROLE tsar on tsar.id = tsau.role_id
			where tsau.create_by = :curUsername
			and (tsau.email like :#{#pageCondition.keyword} or tsau.user_name like :#{#pageCondition.keyword}
			or tsau.first_name like :#{#pageCondition.keyword} or tsau.last_name like :#{#pageCondition.keyword}
			or tsau.tel_no like :#{#pageCondition.keyword} )
			and tsau.enable like :#{#pageCondition.status}
			and tsar.id like :#{#pageCondition.role} """, nativeQuery = true, countQuery = """
			select tsau.* from T_SYS_APP_USER tsau
			join T_SYS_APP_ROLE tsar on tsar.id = tsau.role_id
			where tsau.create_by = :curUsername
			and (tsau.email like :#{#pageCondition.keyword} or tsau.user_name like :#{#pageCondition.keyword}
			or tsau.first_name like :#{#pageCondition.keyword} or tsau.last_name like :#{#pageCondition.keyword}
			or tsau.tel_no like :#{#pageCondition.keyword} )
			and tsau.enable like :#{#pageCondition.status}
			and tsar.id like :#{#pageCondition.role} """)
	Page<UserEntity> getUsersBySysAdmin(Pageable pageable, String curUsername, PageSearchUserDto pageCondition);

	@Query(value = """
			select u.* from T_SYS_APP_USER u
			join T_COMPANY_USER tcu on u.id = tcu.user_id
			join T_COMPANY tc on tc.id = tcu.company_id
			where u.id = :userId and tc.id like :companyId """, nativeQuery = true)
	Optional<UserEntity> findUserById(@Param("userId") long userId, @Param("companyId") String companyId);

	@Query(value = " select * from T_SYS_APP_USER where user_name = ?1 ", nativeQuery = true)
	UserEntity findUserByUserName(String username);

	@Query(value = """
			select * from T_SYS_APP_USER u join ASSIGN_TASK t on t.user_id = u.id
			where t.project_id = :prjId and t.can_edit = 1 """, nativeQuery = true)
	List<UserEntity> findUserAllwUpdPrj(String prjId);

	List<UserEntity> findAllByRole_RoleName (String roleName);

}
