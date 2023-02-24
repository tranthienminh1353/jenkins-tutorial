package com.nineplus.bestwork.services;

import java.time.LocalDateTime;
import java.util.*;

import org.apache.commons.lang3.ObjectUtils;
import org.apache.commons.lang3.math.NumberUtils;
import org.modelmapper.ModelMapper;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.context.annotation.Lazy;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;
import org.springframework.data.domain.Sort;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import com.nineplus.bestwork.dto.PageResDto;
import com.nineplus.bestwork.dto.ResRoleDto;
import com.nineplus.bestwork.dto.SearchDto;
import com.nineplus.bestwork.entity.RoleEntity;
import com.nineplus.bestwork.entity.UserEntity;
import com.nineplus.bestwork.exception.BestWorkBussinessException;
import com.nineplus.bestwork.model.UserAuthDetected;
import com.nineplus.bestwork.repository.RoleRepository;
import com.nineplus.bestwork.utils.CommonConstants;
import com.nineplus.bestwork.utils.CommonConstants.RoleName;
import com.nineplus.bestwork.utils.MessageUtils;
import com.nineplus.bestwork.utils.PageUtils;
import com.nineplus.bestwork.utils.UserAuthUtils;

@Service
@Transactional
public class RoleService {

	private final Logger logger = LoggerFactory.getLogger(CompanyService.class);

	@Autowired
	UserAuthUtils userAuthUtils;

	@Autowired
	MessageUtils messageUtils;

	@Autowired
	RoleRepository roleRepository;

	@Autowired
	ModelMapper modelMapper;

	@Autowired
	@Lazy
	UserService userService;

	@Autowired
	private PageUtils responseUtils;

	@Autowired
	@Lazy
	private PermissionService permissionService;

	public ResRoleDto getRole(Long id) throws BestWorkBussinessException {
		Optional<RoleEntity> role = roleRepository.findById(id);
		if (role.isPresent()) {
			return modelMapper.map(role.get(), ResRoleDto.class);
		}
		throw new BestWorkBussinessException(CommonConstants.MessageCode.E1X0003, null);
	}

	@Transactional(rollbackFor = { Exception.class })
	public ResRoleDto addRole(ResRoleDto dto) throws BestWorkBussinessException {
		RoleEntity role = null;
		try {
			role = roleRepository.findRole(dto.getName(), userAuthUtils.getUserInfoFromReq(false).getUsername());
			if (!ObjectUtils.isEmpty(role)) {
				logger.error(messageUtils.getMessage(CommonConstants.MessageCode.E1X0014, null));
				throw new BestWorkBussinessException(CommonConstants.MessageCode.E1X0014, null);
			}
			role = new RoleEntity();
			role.setRoleName(dto.getName());
			role.setDescription(dto.getDescription());
			role.setCreateDate(LocalDateTime.now());
			role.setCreateBy(userAuthUtils.getUserInfoFromReq(false).getUsername());
			role.setDeleteFlag(0);
			role = roleRepository.save(role);
			permissionService.createPermissionsForNewRole(role);
			return modelMapper.map(role, ResRoleDto.class);
		} catch (Exception e) {
			logger.error(messageUtils.getMessage(CommonConstants.MessageCode.E1X0001, null), e);
			throw new BestWorkBussinessException(CommonConstants.MessageCode.E1X0001, null);
		}
	}

	@Transactional(rollbackFor = { Exception.class })
	public ResRoleDto updateRole(ResRoleDto dto) throws BestWorkBussinessException {
		RoleEntity role = null;
		try {
			role = roleRepository.findById(dto.getId()).orElse(null);
			if (ObjectUtils.isEmpty(role)) {
				logger.error(messageUtils.getMessage(CommonConstants.MessageCode.E1X0013, null));
				throw new BestWorkBussinessException(CommonConstants.MessageCode.E1X0013, null);
			}
			role.setRoleName(dto.getName());
			role.setDescription(dto.getDescription());
			role.setUpdateDate(LocalDateTime.now());
			role.setUpdateBy(userAuthUtils.getUserInfoFromReq(false).getUsername());
			roleRepository.save(role);
			return modelMapper.map(role, ResRoleDto.class);
		} catch (Exception e) {
			logger.error(messageUtils.getMessage(CommonConstants.MessageCode.E1X0001, null), e);
			throw new BestWorkBussinessException(CommonConstants.MessageCode.E1X0001, null);
		}
	}

	public PageResDto<ResRoleDto> getRoles(SearchDto pageSearchDto) throws BestWorkBussinessException {
		try {
			int pageNumber = NumberUtils.toInt(pageSearchDto.getPageConditon().getPage());
			if (pageNumber > 0) {
				pageNumber = pageNumber - 1;
			}
			Pageable pageable = PageRequest.of(pageNumber, Integer.parseInt(pageSearchDto.getPageConditon().getSize()),
					Sort.by(pageSearchDto.getPageConditon().getSortDirection(),
							pageSearchDto.getPageConditon().getSortBy()));
			Page<RoleEntity> pageSysRole = roleRepository
					.findTRolesByRoleNameContaining(pageSearchDto.getConditionSearchDto().getName(), pageable);
			return responseUtils.convertPageEntityToDTO(pageSysRole, ResRoleDto.class);
		} catch (Exception ex) {
			logger.error(messageUtils.getMessage(CommonConstants.MessageCode.E1X0001, null), ex);
			throw new BestWorkBussinessException(CommonConstants.MessageCode.E1X0001, null);
		}
	}

	public void deleteRole(Long id) throws BestWorkBussinessException {
		try {
			Optional<RoleEntity> role = roleRepository.findById(id);
			role.ifPresent(sysRole -> roleRepository.delete(sysRole));
		} catch (Exception ex) {
			logger.error(messageUtils.getMessage(CommonConstants.MessageCode.RLF0002, null), ex);
			throw new BestWorkBussinessException(CommonConstants.MessageCode.RLF0002, null);
		}
	}

	public Set<RoleEntity> getAllRole() throws BestWorkBussinessException {
		UserAuthDetected userAuthRoleReq = userAuthUtils.getUserInfoFromReq(false);
		UserEntity userEntity = userService.getUserByUsername(userAuthRoleReq.getUsername());
		UserEntity adminUser = userService.getAdminUser(userAuthRoleReq.getUsername());
		Set <RoleEntity> result;
		List<String> exceptList = new ArrayList<>();
		exceptList.add(userEntity.getRole().getRoleName());
		result = roleRepository.getRoleEntityByCreateByOrderByIdAsc(adminUser.getUserName(), userEntity.getRole().getId());
		return result;
	}

	public List<RoleEntity> getAllRoleAdmin(String adminUsername) {
		List<String> exceptList = new ArrayList<>();
		exceptList.add(RoleName.SYS_ADMIN);
		exceptList.add(RoleName.SYS_COMPANY_ADMIN);
		return roleRepository.getRoleCreateByExcept(adminUsername, exceptList);
	}

	public void createDefaultRoleForAdmin(UserEntity admin) {
		List<String> exceptList = new ArrayList<>();
		exceptList.add(RoleName.SYS_ADMIN);
		exceptList.add(RoleName.SYS_COMPANY_ADMIN);
		List<RoleEntity> roleEntities = roleRepository.getRoleCreateByExcept(admin.getCreateBy(), exceptList);
		roleEntities.stream().map(roleEntity -> {
			RoleEntity newRole = roleEntity.clone();
			newRole.setCreateBy(admin.getUserName());
			newRole.setCreateDate(LocalDateTime.now());
			newRole.setUpdateBy(null);
			newRole.setUpdateDate(null);
			newRole.setId(null);
			newRole.setSysPermissions(null);
			newRole.setUsers(null);
			return roleRepository.save(newRole);
		}).toList();
	}
}
