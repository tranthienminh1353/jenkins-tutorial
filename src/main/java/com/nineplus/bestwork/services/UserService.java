package com.nineplus.bestwork.services;

import java.nio.charset.StandardCharsets;
import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collection;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.Set;
import java.util.stream.Collectors;

import org.apache.commons.lang3.ObjectUtils;
import org.apache.commons.lang3.StringUtils;
import org.modelmapper.ModelMapper;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageImpl;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;
import org.springframework.data.domain.Sort;
import org.springframework.security.core.authority.SimpleGrantedAuthority;
import org.springframework.security.core.userdetails.User;
import org.springframework.security.core.userdetails.UserDetails;
import org.springframework.security.core.userdetails.UserDetailsService;
import org.springframework.security.core.userdetails.UsernameNotFoundException;
import org.springframework.security.crypto.bcrypt.BCryptPasswordEncoder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import com.nineplus.bestwork.dto.CompanyResDto;
import com.nineplus.bestwork.dto.CompanyUserReqDto;
import com.nineplus.bestwork.dto.PageResDto;
import com.nineplus.bestwork.dto.PageSearchUserDto;
import com.nineplus.bestwork.dto.PermissionResDto;
import com.nineplus.bestwork.dto.RPageDto;
import com.nineplus.bestwork.dto.UserCompanyReqDto;
import com.nineplus.bestwork.dto.UserDetectResDto;
import com.nineplus.bestwork.dto.UserListIdDto;
import com.nineplus.bestwork.dto.UserReqDto;
import com.nineplus.bestwork.dto.UserResDto;
import com.nineplus.bestwork.dto.UserWithProjectResDto;
import com.nineplus.bestwork.entity.CompanyEntity;
import com.nineplus.bestwork.entity.RoleEntity;
import com.nineplus.bestwork.entity.UserEntity;
import com.nineplus.bestwork.exception.BestWorkBussinessException;
import com.nineplus.bestwork.model.UserAuthDetected;
import com.nineplus.bestwork.model.enumtype.Status;
import com.nineplus.bestwork.repository.AssignTaskRepository;
import com.nineplus.bestwork.repository.CompanyRepository;
import com.nineplus.bestwork.repository.RoleRepository;
import com.nineplus.bestwork.repository.UserProjectRepository;
import com.nineplus.bestwork.repository.UserRepository;
import com.nineplus.bestwork.services.impl.ScheduleServiceImpl;
import com.nineplus.bestwork.utils.CommonConstants;
import com.nineplus.bestwork.utils.CommonConstants.RoleName;
import com.nineplus.bestwork.utils.ConvertResponseUtils;
import com.nineplus.bestwork.utils.Enums.TRole;
import com.nineplus.bestwork.utils.MessageUtils;
import com.nineplus.bestwork.utils.PageUtils;
import com.nineplus.bestwork.utils.UserAuthUtils;

@Service
@Transactional
public class UserService implements UserDetailsService {
	int countUserLoginFailedBlocked = 5;

	@SuppressWarnings("unused")
	private final Logger logger = LoggerFactory.getLogger(UserService.class);

	@Autowired
	UserRepository userRepo;

	@Autowired
	PermissionService permissionService;

	@Autowired
	UserAuthUtils userAuthUtils;

	@Autowired
	BCryptPasswordEncoder encoder;

	@Autowired
	ConvertResponseUtils convertResponseUtils;

	@Autowired
	PageUtils responseUtils;

	@Autowired
	CompanyRepository companyRepository;

	@Autowired
	RoleRepository roleRepository;

	@Autowired
	MessageUtils messageUtils;

	@Autowired
	MailSenderService mailSenderService;

	@Autowired
	MailStorageService mailStorageService;

	@Autowired
	ScheduleService scheduleService;

	@Autowired
	AssignTaskRepository assignTaskRepository;

	@Autowired
	ModelMapper modelMapper;

	@Autowired
	RoleService roleService;

	public void saveUser(UserEntity user) {
		userRepo.save(user);
	}

	public UserEntity getUserByUsername(String userName) {
		return userRepo.findByUserName(userName);
	}

	public boolean isBlocked(int countLoginFailed) {
		return countLoginFailed >= countUserLoginFailedBlocked;
	}

	@Override
	public UserDetails loadUserByUsername(String userName) throws UsernameNotFoundException {
		UserEntity user = userRepo.findByUserNameLogIn(userName, LocalDateTime.now().toString());
		if (ObjectUtils.isEmpty(user)) {
			throw new UsernameNotFoundException("User not found");
		}
		Collection<SimpleGrantedAuthority> authorities = new ArrayList<>();
		authorities.add(new SimpleGrantedAuthority(user.getRole().getRoleName()));
		return new User(user.getUserName(), user.getPassword(), authorities);
	}

	public UserResDto convertUserToUserDto(UserEntity user) {
		UserResDto dto = null;
		if (user != null) {
			dto = new UserResDto();
			dto.setId(user.getId());
			dto.setUserName(user.getUserName());
			dto.setEmail(user.getEmail());
			dto.setRole(user.getRole());
			dto.setCountLoginFailed(String.valueOf(countUserLoginFailedBlocked));
			dto.setRole(user.getRole());
			dto.setEnable(user.isEnable());
			dto.setTelNo(user.getTelNo());
			dto.setFirstName(user.getFirstName());
			dto.setLastName(user.getLastName());
		}
		return dto;
	}

	@Transactional(rollbackFor = { Exception.class })
	public void registNewUser(CompanyUserReqDto companyUserReqDto, CompanyEntity tCompany, RoleEntity role)
			throws BestWorkBussinessException {
		UserAuthDetected userAuthRoleReq = userAuthUtils.getUserInfoFromReq(false);
		UserEntity newUser = new UserEntity();
		Set<CompanyEntity> companyUser = new HashSet<CompanyEntity>();
		UserCompanyReqDto newUserCompany = companyUserReqDto.getUser();
		companyUser.add(tCompany);
		newUser.setEmail(newUserCompany.getEmail());
		newUser.setUserName(newUserCompany.getUserName());
		newUser.setEnable(newUserCompany.isEnabled());
		newUser.setFirstName(newUserCompany.getFirstName());
		newUser.setLastName(newUserCompany.getLastName());
		newUser.setLoginFailedNum(0);
		newUser.setPassword(encoder.encode(newUserCompany.getPassword()));
		newUser.setTelNo(newUserCompany.getTelNo());
		newUser.setRole(role);
		newUser.setCompanys(companyUser);
		newUser.setCreateBy(userAuthRoleReq.getUsername());
		newUser.setCreateDate(LocalDateTime.now());

		newUser = userRepo.save(newUser);
		if (newUser.getRole().getRoleName().equals(CommonConstants.RoleName.SYS_COMPANY_ADMIN)) {
			roleService.createDefaultRoleForAdmin(newUser);
			permissionService.createPermissionsForNewSysCompanyAdmin(newUser);
		}
		mailStorageService.saveMailRegisterUserCompToSendLater(newUserCompany.getEmail(), tCompany.getCompanyName(),
				newUserCompany.getUserName(), newUserCompany.getPassword());
		ScheduleServiceImpl.isCompleted = true;
	}

	public UserEntity getUserByCompanyId(long companyId) {
		return userRepo.findUserByOrgId(companyId);
	}

	public long findCompanyIdByAdminUsername(UserAuthDetected userAuthRoleReq) {
		String companyAdminUserName = userAuthRoleReq.getUsername();
		long companyId = userRepo.findCompanyIdByAdminUsername(companyAdminUserName);
		return companyId;
	}

	@SuppressWarnings("unused")
	private List<UserEntity> getUsersByKeyword(String keyword) {
		return this.userRepo.getUsersByKeyword(keyword);
	}

	@Transactional(rollbackFor = { Exception.class })
	public UserEntity createUser(UserReqDto userReqDto) throws BestWorkBussinessException {

		UserAuthDetected userAuthRoleReq = userAuthUtils.getUserInfoFromReq(false);
		String createUser = userAuthRoleReq.getUsername();

		this.validateUserInfo(userReqDto, userAuthRoleReq);
		this.chkExistUsername(userReqDto);
		this.chkExistEmail(userReqDto);
		Set<CompanyEntity> companies = new HashSet<>();
		Optional<CompanyEntity> companyOpt = companyRepository.findById(userReqDto.getCompany());
		companies.add(companyOpt.get());

		UserEntity user = new UserEntity();
		user.setCompanys(companies);
		user.setUserName(userReqDto.getUserName());
		user.setPassword(encoder.encode(userReqDto.getPassword()));
		user.setFirstName(userReqDto.getFirstName());
		user.setLastName(userReqDto.getLastName());
		user.setEmail(userReqDto.getEmail());
		user.setTelNo(userReqDto.getTelNo());
		user.setEnable(userReqDto.isEnabled());
		user.setLoginFailedNum(0);
		user.setRole(this.roleRepository.findById(userReqDto.getRole()).get());
		user.setCreateBy(createUser);
		user.setCreateDate(LocalDateTime.now());
		user.setDeleteFlag(0);
		if (ObjectUtils.isNotEmpty(userReqDto.getAvatar())) {
			user.setUserAvatar(userReqDto.getAvatar().getBytes());
		}
		UserEntity createdUser = this.userRepo.save(user);
		if (user.getRole().getRoleName().equals(CommonConstants.RoleName.SYS_COMPANY_ADMIN)) {
			roleService.createDefaultRoleForAdmin(createdUser);
			permissionService.createPermissionsForNewSysCompanyAdmin(createdUser);
		}
		mailStorageService.saveMailRegisterUserCompToSendLater(userReqDto.getEmail(), companyOpt.get().getCompanyName(),
				userReqDto.getUserName(), userReqDto.getPassword());
		ScheduleServiceImpl.isCompleted = true;

		return createdUser;
	}

	private void validateUserInfo(UserReqDto userReqDto, UserAuthDetected userAuthRoleReq)
			throws BestWorkBussinessException {
		if (ObjectUtils.isEmpty(userReqDto.getRole())) {
			throw new BestWorkBussinessException(CommonConstants.MessageCode.EMP0001,
					new Object[] { CommonConstants.Character.ROLE_FIELD });
		}
		Optional<RoleEntity> roleOpt = roleRepository.findById(userReqDto.getRole());
		if (roleOpt.isEmpty()) {
			throw new BestWorkBussinessException(CommonConstants.MessageCode.eR0002, null);
		}

		if (ObjectUtils.isEmpty(userReqDto.getCompany())) {
			throw new BestWorkBussinessException(CommonConstants.MessageCode.EMP0001,
					new Object[] { CommonConstants.Character.COMPANY_FIELD });
		}
		Optional<CompanyEntity> companyOpt = companyRepository.findById(userReqDto.getCompany());
		if (!userAuthRoleReq.getIsSysAdmin() && companyOpt.isEmpty()) {
			throw new BestWorkBussinessException(CommonConstants.MessageCode.CPN0003, null);
		}
		if (ObjectUtils.isEmpty(userReqDto.getUserName())) {
			throw new BestWorkBussinessException(CommonConstants.MessageCode.EMP0001,
					new Object[] { CommonConstants.Character.USER_NAME });
		}

		if (ObjectUtils.isEmpty(userReqDto.getFirstName())) {
			throw new BestWorkBussinessException(CommonConstants.MessageCode.EMP0001,
					new Object[] { CommonConstants.Character.FIRSTNAME_FIELD });
		}
		if (ObjectUtils.isEmpty(userReqDto.getLastName())) {
			throw new BestWorkBussinessException(CommonConstants.MessageCode.EMP0001,
					new Object[] { CommonConstants.Character.LASTNAME_FIELD });
		}
		if (ObjectUtils.isEmpty(userReqDto.getEmail())) {
			throw new BestWorkBussinessException(CommonConstants.MessageCode.EMP0001,
					new Object[] { CommonConstants.Character.EMAIL_FIELD });
		}
	}

	private void chkExistUsername(UserReqDto userReqDto) throws BestWorkBussinessException {
		UserEntity user = this.userRepo.findByUserName(userReqDto.getUserName());
		if (ObjectUtils.isNotEmpty(user)) {
			throw new BestWorkBussinessException(CommonConstants.MessageCode.EXS0003,
					new Object[] { CommonConstants.Character.USER_NAME });
		}
	}

	private void chkExistEmail(UserReqDto userReqDto) throws BestWorkBussinessException {
		UserEntity userByEmail = this.userRepo.findByEmail(userReqDto.getEmail());
		if (ObjectUtils.isNotEmpty(userByEmail)) {
			throw new BestWorkBussinessException(CommonConstants.MessageCode.EXS0003,
					new Object[] { CommonConstants.Character.USER_MAIL });
		}
	}

	public UserResDto getUserById(long userId) throws BestWorkBussinessException {
		UserAuthDetected userAuthRoleReq = userAuthUtils.getUserInfoFromReq(false);
		CompanyEntity company = companyRepository.findById(findCompanyIdByUsername(userAuthRoleReq))
				.orElse(new CompanyEntity());
		Optional<UserEntity> userOpt;
		if (userAuthRoleReq.getIsSysAdmin() || userAuthRoleReq.getIsSysCompanyAdmin() || ObjectUtils.isEmpty(company)) {
			userOpt = this.userRepo.findById(userId);
		} else {
			userOpt = this.userRepo.findUserById(userId, String.valueOf(company.getId()));
		}

		if (userOpt.isEmpty()) {
			throw new BestWorkBussinessException(CommonConstants.MessageCode.ECU0002, null);
		}
		UserEntity user = userOpt.get();
		UserResDto userResDto = new UserResDto();
		userResDto.setId(userId);
		userResDto.setUserName(user.getUserName());
		userResDto.setFirstName(user.getFirstName());
		userResDto.setLastName(user.getLastName());
		userResDto.setEmail(user.getEmail());
		userResDto.setTelNo(user.getTelNo());
		userResDto.setEnable(user.isEnable());
		if (user.getLoginFailedNum() > countUserLoginFailedBlocked) {
			userResDto.setEnable(false);
		}
		userResDto.setRole(user.getRole());
		for (CompanyEntity comp : user.getCompanys()) {
			userResDto.setCompany(comp);
		}
		if (null != user.getUserAvatar()) {
			userResDto.setAvatar(new String(user.getUserAvatar(), StandardCharsets.UTF_8));
		}
		userResDto.setCreateDate(String.valueOf(user.getCreateDate()));
		userResDto.setUpdateDate(String.valueOf(user.getUpdateDate()));
		return userResDto;
	}

	@Transactional(rollbackFor = { Exception.class })
	public void deleteUser(UserListIdDto listId) throws BestWorkBussinessException {
		try {
			List<UserEntity> userList = userRepo.findAllById(Arrays.asList(listId.getUserIdList()));
			if (userList.isEmpty() || userList.size() < listId.getUserIdList().length) {
				throw new BestWorkBussinessException(CommonConstants.MessageCode.ECU0005, listId.getUserIdList());
			}
			userRepo.inactiveUser(Arrays.asList(listId.getUserIdList()));
			
		} catch (Exception ex) {
			throw new BestWorkBussinessException(CommonConstants.MessageCode.ECU0005, listId.getUserIdList());
		}
	}

	public long findCompanyIdByUsername(UserAuthDetected userAuthRoleReq) {
		String companyAdminUserName = userAuthRoleReq.getUsername();
		long companyId;
		try {
			companyId = userRepo.findCompanyIdByAdminUsername(companyAdminUserName);
		} catch (Exception e) {
			return -1;
		}
		return companyId;
	}

	public PageResDto<UserResDto> getAllUsers(PageSearchUserDto pageCondition) throws BestWorkBussinessException {
		PageResDto<UserResDto> pageResponseDto = new PageResDto<>();
		Pageable pageable = convertSearch(pageCondition);
		UserAuthDetected userAuthRoleReq = userAuthUtils.getUserInfoFromReq(false);
		CompanyEntity company = companyRepository.findById(findCompanyIdByUsername(userAuthRoleReq))
				.orElse(new CompanyEntity());
		Page<UserEntity> userPage;
		if (ObjectUtils.isEmpty(company) || userAuthRoleReq.getIsSysAdmin()) {
			userPage = userRepo.getUsersBySysAdmin(pageable, userAuthRoleReq.getUsername(), pageCondition);
		} else {
			if (userAuthRoleReq.getIsSysCompanyAdmin()) {
				userPage = userRepo.getAllUsersBySysComAdmin(pageable, userAuthRoleReq.getUsername(), pageCondition);
			} else {
				userPage = userRepo.getAllUsers(pageable, String.valueOf(company.getId()), pageCondition);
			}
		}
		List<UserEntity> result = userPage.getContent().stream()
				.filter(u -> !userAuthRoleReq.getUsername().equals(u.getUserName())).collect(Collectors.toList());
		userPage = new PageImpl<UserEntity>(result, pageable, userPage.getTotalElements());
		RPageDto rPageDto = createRPageDto(userPage);
		List<UserResDto> userResDtoList = convertTUser(userPage);
		pageResponseDto.setContent(userResDtoList);
		pageResponseDto.setMetaData(rPageDto);
		return pageResponseDto;
	}

	private List<UserResDto> convertTUser(Page<UserEntity> userPage) throws BestWorkBussinessException {
		List<UserResDto> userResDtoList = new ArrayList<>();
		try {
			for (UserEntity user : userPage.getContent()) {
				UserResDto userResDto = new UserResDto();
				userResDto.setUserName(user.getUserName());
				userResDto.setEmail(user.getEmail());
				userResDto.setFirstName(user.getFirstName());
				userResDto.setLastName(user.getLastName());
				userResDto.setTelNo(user.getTelNo());
				userResDto.setRole(user.getRole());
				userResDto.setId(user.getId());
				userResDto.setEnable(user.isEnable());
//				userResDto.setAvatar(Arrays.toString(tUser.getUserAvatar()));
				userResDto.setAvatar(null);
				userResDtoList.add(userResDto);
			}
		} catch (Exception e) {
			throw new BestWorkBussinessException(CommonConstants.MessageCode.ECU0002, null);
		}

		return userResDtoList;
	}

	private RPageDto createRPageDto(Page<UserEntity> userPage) throws BestWorkBussinessException {
		RPageDto rPageDto = new RPageDto();
		try {
			if (!userPage.isEmpty()) {
				rPageDto.setNumber(userPage.getNumber());
				rPageDto.setSize(userPage.getSize());
				rPageDto.setTotalPages(userPage.getTotalPages());
				rPageDto.setTotalElements(userPage.getTotalElements());
			}
		} catch (Exception e) {
			throw new BestWorkBussinessException(CommonConstants.MessageCode.ECU0002, null);
		}
		return rPageDto;
	}

	private Pageable convertSearch(PageSearchUserDto pageCondition) {
		if (StringUtils.EMPTY.equals(pageCondition.getKeyword())) {
			pageCondition.setKeyword("%%");
		} else {
			pageCondition.setKeyword("%" + pageCondition.getKeyword() + "%");
		}
		if (StringUtils.EMPTY.equals(pageCondition.getRole())) {
			pageCondition.setRole("%%");
		}
		if (StringUtils.EMPTY.equals(pageCondition.getStatus())) {
			pageCondition.setStatus("%%");
		}

		return PageRequest.of(Integer.parseInt(pageCondition.getPage()), Integer.parseInt(pageCondition.getSize()),
				Sort.by(pageCondition.getSortDirection(),
						convertResponseUtils.convertResponseUser(pageCondition.getSortBy())));
	}

	@Transactional(rollbackFor = { Exception.class })
	public UserEntity editUser(UserReqDto userReqDto, long userId) throws BestWorkBussinessException {
		UserAuthDetected userAuthRoleReq = userAuthUtils.getUserInfoFromReq(false);
		UserEntity curUser = this.findUserByUsername(userAuthRoleReq.getUsername());
		UserEntity userToEdit = userRepo.findById(userId).orElse(null);
		// Check if user-to-edit exists or not
		if (ObjectUtils.isEmpty(userToEdit)) {
			throw new BestWorkBussinessException(CommonConstants.MessageCode.ECU0003, null);
		}
		// Check if user whose role is CompanyAdmin or other (Investor, Supplier,
		// Contractor) can edit or not
		if (!userAuthRoleReq.getIsSysAdmin() && !userAuthRoleReq.getIsSysCompanyAdmin()) {
			CompanyEntity company = companyRepository.findById(findCompanyIdByUsername(userAuthRoleReq))
					.orElse(new CompanyEntity());
			if (ObjectUtils.isEmpty(company.getId())) {
				throw new BestWorkBussinessException(CommonConstants.MessageCode.ECU0003, null);
			}
			// Check CompanyAdmin of a company cannot edit users from other company
			if (userAuthRoleReq.getIsOrgAdmin()) {
				if (!curUser.getCompanys().contains(company)) {
					throw new BestWorkBussinessException(CommonConstants.MessageCode.E1X0014, null);
				}
			} else {
				// Check role Investor/Supplier/Contractor can only edit himself
				if (!curUser.equals(userToEdit)) {
					throw new BestWorkBussinessException(CommonConstants.MessageCode.E1X0014, null);
				}
			}
		}

		// Check SysAdmin can edit himself and SysCompanyAdmin(s)
		if (userAuthRoleReq.getIsSysAdmin() && !(TRole.SYS_COMPANY_ADMIN.ordinal() + 1 == userToEdit.getRole().getId()
				|| curUser.equals(userToEdit))) {
			throw new BestWorkBussinessException(CommonConstants.MessageCode.E1X0014, null);
		}
		// Then, role SysCompanyAdmin can edit all users whose role is beneath his

		this.validateUserInfo(userReqDto, userAuthRoleReq);
		// Check email of each user is unique
		if (!userToEdit.getEmail().equals(userReqDto.getEmail())) {
			this.chkExistEmail(userReqDto);
		}
		// Check username cannot be change
		if (!userReqDto.getUserName().equals(userToEdit.getUserName())) {
			throw new BestWorkBussinessException(CommonConstants.MessageCode.ECU0007, null);
		}
		Set<CompanyEntity> companySet = new HashSet<>();
		if (!userAuthRoleReq.getIsSysAdmin() && ObjectUtils.isNotEmpty(userReqDto.getCompany())) {
			CompanyEntity companyCurrent = companyRepository.findByCompanyId(userReqDto.getCompany());
			if (companyCurrent != null) {
				companySet.add(companyCurrent);
				userToEdit.setCompanys(companySet);
			} else {
				throw new BestWorkBussinessException(CommonConstants.MessageCode.CPN0003, null);
			}
		} else {
			userToEdit.setCompanys(userToEdit.getCompanys());
		}
		userToEdit.setId(userId);

		if (ObjectUtils.isNotEmpty(userReqDto.getPassword())) {
			userToEdit.setPassword(encoder.encode(userReqDto.getPassword()));
		}
		userToEdit.setFirstName(userReqDto.getFirstName());
		userToEdit.setLastName(userReqDto.getLastName());
		userToEdit.setEmail(userReqDto.getEmail());
		userToEdit.setTelNo(userReqDto.getTelNo());
		userToEdit.setEnable(userReqDto.isEnabled());
		userToEdit.setLoginFailedNum(0);
		if (ObjectUtils.isNotEmpty(userReqDto.getRole())) {
			RoleEntity roleCurrent = roleRepository.findRole(userReqDto.getRole());
			if (roleCurrent != null) {
				userToEdit.setRole(roleCurrent);
			} else {
				throw new BestWorkBussinessException(CommonConstants.MessageCode.eR0002, null);
			}
		}

		if (ObjectUtils.isNotEmpty(userReqDto.getAvatar())) {
			userToEdit.setUserAvatar(userReqDto.getAvatar().getBytes());
		} else {
			userToEdit.setUserAvatar(null);
		}
		userToEdit.setUpdateDate(LocalDateTime.now());
		userToEdit.setUpdateBy(userAuthRoleReq.getUsername());

		return userRepo.save(userToEdit);
	}

	public Set<RoleEntity> getAllRoles() throws BestWorkBussinessException {
		UserAuthDetected userAuthRoleReq = userAuthUtils.getUserInfoFromReq(false);
		UserEntity user = this.getAdminUser(userAuthRoleReq.getUsername());
		Set<RoleEntity> roleList = this.roleRepository.getRoleEntityByCreateBy(user.getUserName());
		if (userAuthRoleReq.getIsSysAdmin()) {
			roleList.removeIf(x -> RoleName.SYS_ADMIN.equals(x.getRoleName()));
		}
		if (userAuthRoleReq.getIsSysCompanyAdmin()) {
			roleList.removeIf(x -> RoleName.SYS_ADMIN.equals(x.getRoleName()));
			roleList.removeIf(x -> RoleName.SYS_COMPANY_ADMIN.equals(x.getRoleName()));
		}
		if (userAuthRoleReq.getIsOrgAdmin()) {
			roleList.removeIf(x -> RoleName.SYS_ADMIN.equals(x.getRoleName()));
			roleList.removeIf(x -> RoleName.SYS_COMPANY_ADMIN.equals(x.getRoleName()));
			roleList.removeIf(x -> RoleName.CMPNY_ADMIN.equals(x.getRoleName()));
		}
		return roleList;
	}

	public List<UserEntity> findAll() {
		return this.userRepo.findAll();
	}

	public Object getAllCompanyOfUser() throws BestWorkBussinessException {
		UserAuthDetected userAuthRoleReq = userAuthUtils.getUserInfoFromReq(false);
		List<CompanyEntity> lstResult = companyRepository.findCompanyRelate(userAuthRoleReq.getUsername());
		if (ObjectUtils.isNotEmpty(lstResult)) {
			return lstResult;
		} else {
			return this.companyRepository.findAll();
		}
	}

	public UserDetectResDto detectUser(String username) throws BestWorkBussinessException {
		UserEntity user = this.getUserByUsername(username);
		long userId = 0;
		if (user != null) {
			userId = user.getId();
		}
		List<UserProjectRepository> userProject = assignTaskRepository.findListProjectByUser(userId, username);
		List<UserWithProjectResDto> listAssignDto = new ArrayList<>();
		if (userProject != null) {
			for (UserProjectRepository assign : userProject) {
				UserWithProjectResDto assignDto = new UserWithProjectResDto();
				assignDto.setProjectId(assign.getProjectId());
				assignDto.setProjectName(assign.getProjectName());
				assignDto.setCanView(assign.getCanView() == 1 ? true : false );
				assignDto.setCanEdit(assign.getCanEdit() == 1 ? true : false );
				listAssignDto.add(assignDto);
			}
		}
		CompanyEntity company = companyRepository.getCompanyOfUser(userId);
		CompanyResDto companyRes = null;
		if (company != null) {
			companyRes = modelMapper.map(company, CompanyResDto.class);
		}
		UserDetectResDto userResDto = modelMapper.map(user, UserDetectResDto.class);
		if (user.getUserAvatar() != null) {
			userResDto.setAvatar(new String(user.getUserAvatar(), StandardCharsets.UTF_8));

		}
		if (companyRes != null) {
			userResDto.setCompany(companyRes);
		}
		userResDto.setRoleProject(listAssignDto);
		List<String> roleList = new ArrayList<>();
		roleList.add(user.getRole().getRoleName());
		List<Integer> lstStt = new ArrayList<>();
		lstStt.add(Status.ACTIVE.getValue());
		Map<Long, List<PermissionResDto>> permissions = permissionService.getMapPermissionsVoter(roleList, lstStt,
				userAuthUtils.getUserInfoFromReq(false).getUsername());
		userResDto.setPermissions(permissions);
		return userResDto;
	}

	public UserEntity findUserByUsername(String username) {
		UserEntity user = new UserEntity();
		user = userRepo.findUserByUserName(username);
		return user;
	}

	public UserEntity findUserByUserId(long userId) {
		Optional<UserEntity> userOpt = userRepo.findById(userId);
		if (userOpt.isPresent()) {
			return userOpt.get();
		}
		return null;
	}

	public List<UserEntity> findUserAllowUpdPrj(String prjId) {
		List<UserEntity> userList = this.userRepo.findUserAllwUpdPrj(prjId);
		return userList;
	}

	public UserEntity getAdminUser(String childUsername) {
		UserEntity childUser = userRepo.findUserByUserName(childUsername);
		UserEntity adminUser = null;
		if (ObjectUtils.isNotEmpty(childUser)) {
			if (childUser.getRole().getRoleName().equals(CommonConstants.RoleName.SYS_COMPANY_ADMIN)
					|| childUser.getRole().getRoleName().equals(CommonConstants.RoleName.SYS_ADMIN)) {
				return childUser;
			}
			adminUser = userRepo.findByUserName(childUser.getCreateBy());
			return this.getAdminUser(adminUser.getUserName());
		}
		return adminUser;
	}

	public UserEntity getAdminUserVoter(String childUsername) {
		UserEntity childUser = userRepo.findUserByUserName(childUsername);
		UserEntity adminUser = null;
		if (ObjectUtils.isNotEmpty(childUser)) {
			if (childUser.getRole().getRoleName().equals(RoleName.SYS_ADMIN)) {
				return childUser;
			}
			adminUser = userRepo.findUserByUserName(childUser.getCreateBy());
			if (childUser.getRole().getRoleName().equals(RoleName.SYS_COMPANY_ADMIN) ||
					adminUser.getRole().getRoleName().equals(RoleName.SYS_COMPANY_ADMIN)) {
				return adminUser;
			}
			if (!adminUser.getRole().getRoleName().equals(RoleName.SYS_COMPANY_ADMIN) &&
					!adminUser.getRole().getRoleName().equals(RoleName.SYS_ADMIN)) {
				return this.getAdminUserVoter(adminUser.getUserName());
			}
		}
		return adminUser;
	}

	public Integer countAllUserForAdmin(String adminUsername) throws BestWorkBussinessException {
		UserEntity userEntity = userRepo.findByUserName(adminUsername);
		if (userEntity.getRole().getRoleName().equals(RoleName.SYS_ADMIN)){
			return Math.toIntExact(userRepo.count());
		}
		throw new BestWorkBussinessException(CommonConstants.MessageCode.ERU001,null);
	}

	public List<UserEntity> getUsersByRole(String roleName) {
		return userRepo.findAllByRole_RoleName(roleName);
	}

}
