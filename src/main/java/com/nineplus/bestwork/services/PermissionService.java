package com.nineplus.bestwork.services;

import java.sql.Timestamp;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.function.Function;
import java.util.stream.Collectors;

import com.nineplus.bestwork.entity.UserEntity;
import com.nineplus.bestwork.model.enumtype.Status;
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

import com.fasterxml.jackson.databind.ObjectMapper;
import com.nineplus.bestwork.dto.PageResDto;
import com.nineplus.bestwork.dto.PermissionResDto;
import com.nineplus.bestwork.dto.RegPermissionDto;
import com.nineplus.bestwork.dto.SearchDto;
import com.nineplus.bestwork.entity.RoleEntity;
import com.nineplus.bestwork.entity.SysMonitorEntity;
import com.nineplus.bestwork.entity.SysPermissionEntity;
import com.nineplus.bestwork.exception.BestWorkBussinessException;
import com.nineplus.bestwork.model.UserAuthDetected;
import com.nineplus.bestwork.repository.PermissionRepository;
import com.nineplus.bestwork.repository.RoleRepository;
import com.nineplus.bestwork.repository.SysMonitorRepository;
import com.nineplus.bestwork.utils.CommonConstants;
import com.nineplus.bestwork.utils.MessageUtils;
import com.nineplus.bestwork.utils.PageUtils;
import com.nineplus.bestwork.utils.UserAuthUtils;

@Service
@Transactional
public class PermissionService {

	private final Logger logger = LoggerFactory.getLogger(CompanyService.class);

	@Autowired
	private UserAuthUtils userAuthUtils;

	@Autowired
	private MessageUtils messageUtils;

	@Autowired
	private PermissionRepository permissionRepository;

	@Autowired
	private RoleRepository sysRoleRepository;

	@Autowired
	private SysMonitorRepository monitorRepository;
	@Autowired
	private ModelMapper modelMapper;

	@Autowired
	private PageUtils responseUtils;

	@Autowired
	private ObjectMapper objectMapper;

	@Autowired
	@Lazy
	private UserService userService;

	@Autowired
	private RoleService roleService;

	public void deletePermission(Long id) throws BestWorkBussinessException {
		try {
			Optional<SysPermissionEntity> sysPermission = permissionRepository.findById(id);
			sysPermission.ifPresent(permission -> permissionRepository.delete(permission));
		} catch (Exception ex) {
			logger.error(messageUtils.getMessage(CommonConstants.MessageCode.RLF0002, null), ex);
			throw new BestWorkBussinessException(CommonConstants.MessageCode.RLF0002, null);
		}
	}

	public List<PermissionResDto> updatePermissions(List<RegPermissionDto> dtos) throws BestWorkBussinessException {
		List<PermissionResDto> resDtos = new ArrayList<>();
		dtos.forEach(regPermissionDto -> {
			try {
				resDtos.addAll(this.updatePermissionsByRole(regPermissionDto));
			} catch (BestWorkBussinessException e) {
				throw new RuntimeException(e);
			}
		});
		return resDtos;
	}

	public List<PermissionResDto> updatePermissionsByRole(RegPermissionDto dto) throws BestWorkBussinessException {
		try {
			Optional<RoleEntity> role = sysRoleRepository.findById(dto.getRoleId());
			UserEntity admin = userService.getAdminUser(userAuthUtils.getUserInfoFromReq(false).getUsername());
			if (role.isEmpty()) {
				logger.error(messageUtils.getMessage(CommonConstants.MessageCode.E1X0014, null));
				throw new BestWorkBussinessException(CommonConstants.MessageCode.E1X0014, null);
			}
			List<PermissionResDto> sysPermissions = dto.getMonitorInfo().stream().map(permissionDto -> {
				SysPermissionEntity sysPermission = new SysPermissionEntity();
				SysMonitorEntity sysMonitor = new SysMonitorEntity();
				if (permissionDto.getId() != null) {
					sysPermission = permissionRepository.findById(permissionDto.getId())
							.orElse(new SysPermissionEntity());
					sysMonitor = sysPermission.getSysMonitor();
				} else if (permissionDto.getMonitorId() != null) {
					sysMonitor = monitorRepository.findById(permissionDto.getMonitorId())
							.orElse(new SysMonitorEntity());
				}
				try {
					if (sysPermission.getId() != null) {
						sysPermission.setUpdatedUser(userAuthUtils.getUserInfoFromReq(false).getUsername());
						sysPermission.setUpdatedDate(new Timestamp(System.currentTimeMillis()));
					} else {
						sysPermission.setCreatedUser(userAuthUtils.getUserInfoFromReq(false).getUsername());
						sysPermission.setCreatedDate(new Timestamp(System.currentTimeMillis()));
					}
				} catch (BestWorkBussinessException e) {
					throw new RuntimeException(e);
				}
				sysMonitor.setName(permissionDto.getMonitorName());
				sysPermission.setSysRole(role.get());
				sysPermission.setSysMonitor(sysMonitor);
				sysPermission.setCanDelete(permissionDto.getCanDelete());
				sysPermission.setCanEdit(permissionDto.getCanEdit());
				sysPermission.setCanAdd(permissionDto.getCanAdd());
				sysPermission.setCanAccess(permissionDto.getCanAccess());
				sysPermission.setStatus(permissionDto.getStatus());
				sysPermission.setUser(admin);
				return modelMapper.map(permissionRepository.save(sysPermission), PermissionResDto.class);
			}).collect(Collectors.toList());
			return sysPermissions;
		} catch (Exception ex) {
			logger.error(messageUtils.getMessage(CommonConstants.MessageCode.E1X0001, null), ex);
			throw new BestWorkBussinessException(CommonConstants.MessageCode.E1X0001, null);
		}
	}

	public PermissionResDto getPermission(Long id) throws BestWorkBussinessException {
		Optional<SysPermissionEntity> sysPermission = permissionRepository.findById(id);
		if (sysPermission.isPresent()) {
			return modelMapper.map(sysPermission.get(), PermissionResDto.class);
		}
		throw new BestWorkBussinessException(CommonConstants.MessageCode.E1X0003, null);
	}

	public Map<Long, List<PermissionResDto>> getMapPermissions(List<String> roleList, List<Integer> lstStt, String principal)
			throws BestWorkBussinessException {
		Map<Long, List<PermissionResDto>> mapPermission = new HashMap<>();
		List<SysPermissionEntity> sysPermissionEntities = this.getPermissionsByRole(roleList, lstStt, null, principal);
		return convertPermissionMap(mapPermission, sysPermissionEntities);
	}

	public Map<Long, List<PermissionResDto>> getMapPermissionsVoter(List<String> roleList, List<Integer> lstStt, String principal)
			throws BestWorkBussinessException {
		Map<Long, List<PermissionResDto>> mapPermission = new HashMap<>();
		List<SysPermissionEntity> sysPermissionEntities = this.getPermissionsByRoleVoter(roleList, lstStt, null, principal);
		return convertPermissionMap(mapPermission, sysPermissionEntities);
	}

	private Map<Long, List<PermissionResDto>> convertPermissionMap(Map<Long, List<PermissionResDto>> mapPermission, List<SysPermissionEntity> sysPermissionEntities) {
		sysPermissionEntities.forEach(sysPermissionEntity -> {
			PermissionResDto permissionResDto = objectMapper.convertValue(sysPermissionEntity, PermissionResDto.class);
			permissionResDto.setMonitorName(sysPermissionEntity.getSysMonitor().getName());
			permissionResDto.setMonitorId(sysPermissionEntity.getSysMonitor().getId());
			permissionResDto.setRoleId(sysPermissionEntity.getSysRole().getId());
			permissionResDto.setAdminId(sysPermissionEntity.getUser().getId());
			if (mapPermission.containsKey(sysPermissionEntity.getSysMonitor().getId())) {
				mapPermission.get(sysPermissionEntity.getSysMonitor().getId()).add(permissionResDto);
			} else {
				List<PermissionResDto> resDtos = new ArrayList<>();
				resDtos.add(permissionResDto);
				mapPermission.put(sysPermissionEntity.getSysMonitor().getId(), resDtos);
			}
		});
		return mapPermission;
	}

	public List<SysPermissionEntity> getPermissionsByRole(List<String> roleName, List<Integer> lstStt, Long actionId, String principal) {
		UserEntity adminUser = userService.getAdminUser(principal);
		if (ObjectUtils.isNotEmpty(actionId)){
			return permissionRepository.findAllByRoleNameAndAction(roleName, lstStt, actionId, adminUser.getId());
		}
		return permissionRepository.findAllBySysRole_RoleName(roleName, lstStt, adminUser.getId());
	}



	public List<SysPermissionEntity> getPermissionsByRoleVoter(List<String> roleName, List<Integer> lstStt, Long actionId, String principal)
			throws BestWorkBussinessException {
		UserEntity adminUser = userService.getAdminUserVoter(principal);
		if (ObjectUtils.isNotEmpty(actionId)){
			return permissionRepository.findAllByRoleNameAndAction(roleName, lstStt, actionId, adminUser.getId());
		}
		return permissionRepository.findAllBySysRole_RoleName(roleName, lstStt, adminUser.getId());
	}

	public PageResDto<PermissionResDto> getPermissions(SearchDto dto) throws BestWorkBussinessException {
		try {
			int pageNumber = NumberUtils.toInt(dto.getPageConditon().getPage());
			if (pageNumber > 0) {
				pageNumber = pageNumber - 1;
			}
			Pageable pageable = PageRequest.of(pageNumber, Integer.parseInt(dto.getPageConditon().getSize()),
					Sort.by(dto.getPageConditon().getSortDirection(), dto.getPageConditon().getSortBy()));
			Page<SysPermissionEntity> pageSysRole = permissionRepository.findAllBySysRole_IdAndSysMonitor_Id(
					dto.getConditionSearchDto().getRoleId(), dto.getConditionSearchDto().getMonitorId(), pageable);
			return responseUtils.convertPageEntityToDTO(pageSysRole, PermissionResDto.class);
		} catch (Exception ex) {
			logger.error(messageUtils.getMessage(CommonConstants.MessageCode.E1X0001, null), ex);
			throw new BestWorkBussinessException(CommonConstants.MessageCode.E1X0001, null);
		}
	}

	public void createPermissionsForNewRole(RoleEntity newRole) throws BestWorkBussinessException {
		SysPermissionEntity sysPermission = new SysPermissionEntity();
		sysPermission.setSysRole(newRole);
		sysPermission.setStatus(Status.ACTIVE.getValue());
		sysPermission.setCreatedDate(new Timestamp(System.currentTimeMillis()));
		sysPermission.setCreatedUser(userAuthUtils.getUserInfoFromReq(false).getUsername());
		sysPermission.setCanEdit(false);
		sysPermission.setCanAdd(false);
		sysPermission.setCanDelete(false);
		sysPermission.setCanAccess(false);
		List<SysMonitorEntity> sysMonitorEntities = monitorRepository.findAll();
		UserEntity adminUser = userService.getAdminUser(userAuthUtils.getUserInfoFromReq(false).getUsername());
		for (SysMonitorEntity monitor : sysMonitorEntities){
			SysPermissionEntity newPermission = sysPermission.clone();
			if (!monitor.getName().equals("Dashboard")){
				sysPermission.setCanEdit(false);
				sysPermission.setCanAdd(false);
				sysPermission.setCanDelete(false);
				sysPermission.setCanAccess(false);
			}
			newPermission.setSysMonitor(monitor);
			newPermission.setUser(adminUser);
			permissionRepository.save(newPermission);
		}
	}

	public void createPermissionsForNewSysCompanyAdmin(UserEntity user) {
		UserEntity adminUser = userService.getAdminUser(user.getUserName());
		List<RoleEntity> newRole = roleService.getAllRoleAdmin(user.getUserName());
		Map<String, RoleEntity> mapRoles = newRole.stream().collect(Collectors.toMap(RoleEntity::getRoleName, Function.identity()));
		List<SysPermissionEntity> sysPermission = permissionRepository.findAllByUser_Role_RoleName(CommonConstants.RoleName.SYS_ADMIN);
		sysPermission.forEach(sysPermissionEntity -> {
			SysPermissionEntity newPermission = sysPermissionEntity.clone();
			if (sysPermissionEntity.getSysRole().getRoleName().equals(CommonConstants.RoleName.SYS_ADMIN) ||
					sysPermissionEntity.getSysRole().getRoleName().equals(CommonConstants.RoleName.SYS_COMPANY_ADMIN)) {
				return;
			}
			newPermission.setUser(adminUser);
			newPermission.setSysRole(mapRoles.get(sysPermissionEntity.getSysRole().getRoleName()));
			newPermission.setCreatedDate(new Timestamp(System.currentTimeMillis()));
			newPermission.setCreatedUser(adminUser.getUserName());
			newPermission.setUpdatedDate(null);
			newPermission.setUpdatedUser(null);
			newPermission.setId(null);
			permissionRepository.save(newPermission);
		});
	}
}
