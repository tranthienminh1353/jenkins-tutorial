package com.nineplus.bestwork.services;

import java.sql.Timestamp;
import java.util.List;
import java.util.Optional;

import com.nineplus.bestwork.entity.RoleEntity;
import com.nineplus.bestwork.entity.SysPermissionEntity;
import com.nineplus.bestwork.entity.UserEntity;
import com.nineplus.bestwork.model.enumtype.Status;
import com.nineplus.bestwork.repository.PermissionRepository;
import com.nineplus.bestwork.repository.RoleRepository;
import org.apache.commons.lang3.ObjectUtils;
import org.apache.commons.lang3.math.NumberUtils;
import org.modelmapper.ModelMapper;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;
import org.springframework.data.domain.Sort;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import com.nineplus.bestwork.dto.MonitorResDto;
import com.nineplus.bestwork.dto.PageResDto;
import com.nineplus.bestwork.dto.SearchDto;
import com.nineplus.bestwork.entity.SysMonitorEntity;
import com.nineplus.bestwork.exception.BestWorkBussinessException;
import com.nineplus.bestwork.repository.SysMonitorRepository;
import com.nineplus.bestwork.utils.CommonConstants;
import com.nineplus.bestwork.utils.MessageUtils;
import com.nineplus.bestwork.utils.PageUtils;
import com.nineplus.bestwork.utils.UserAuthUtils;

@Service
@Transactional
public class MonitorService {

	private final Logger logger = LoggerFactory.getLogger(CompanyService.class);

	@Autowired
	UserAuthUtils userAuthUtils;

	@Autowired
	MessageUtils messageUtils;

	@Autowired
	SysMonitorRepository monitorRepository;

	@Autowired
	RoleRepository roleRepository;

	@Autowired
	PermissionRepository permissionRepository;

	@Autowired
	ModelMapper modelMapper;

	@Autowired
	UserService userService;

	@Autowired
	private PageUtils responseUtils;

	public List<MonitorResDto> getMonitors() {
		List<SysMonitorEntity> entities = monitorRepository.findAll();
		return  entities.stream().map(sysMonitorEntity -> modelMapper.map(sysMonitorEntity, MonitorResDto.class)).toList();
	}

	@Transactional(rollbackFor = { Exception.class })
	public MonitorResDto addMonitor(MonitorResDto dto) throws BestWorkBussinessException {
		SysMonitorEntity monitor = null;
		try {
			monitor = monitorRepository.findSysMonitorByName(dto.getName());
			if (!ObjectUtils.isEmpty(monitor)) {
				logger.error(messageUtils.getMessage(CommonConstants.MessageCode.EXM001, null));
				throw new BestWorkBussinessException(CommonConstants.MessageCode.EXM001, null);
			}
			monitor = new SysMonitorEntity();
			monitor.setName(dto.getName());
			monitor.setIcon(dto.getIcon());
			monitor.setUrl(dto.getUrl());
			monitor.setCreatedDate(new Timestamp(System.currentTimeMillis()));
			monitor.setCreatedUser(userAuthUtils.getUserInfoFromReq(false).getUsername());
			monitor.setIsMenu(dto.getIsMenu());
			monitorRepository.save(monitor);
			UserEntity admin = userService.getAdminUser(userAuthUtils.getUserInfoFromReq(false).getUsername());
			List<RoleEntity> roleEntities = roleRepository.findAll();
			for (RoleEntity role : roleEntities) {
				SysPermissionEntity sysPermission = new SysPermissionEntity();
				sysPermission.setSysRole(role);
				sysPermission.setSysMonitor(monitor);
				sysPermission.setCanAdd(false);
				sysPermission.setCanEdit(false);
				sysPermission.setCanDelete(false);
				sysPermission.setCanAccess(false);
				sysPermission.setCreatedUser(userAuthUtils.getUserInfoFromReq(false).getUsername());
				sysPermission.setCreatedDate(new Timestamp(System.currentTimeMillis()));
				sysPermission.setStatus(Status.ACTIVE.getValue());
				sysPermission.setUser(admin);
				permissionRepository.save(sysPermission);
			}
			return modelMapper.map(monitor, MonitorResDto.class);
		} catch (Exception e) {
			logger.error(messageUtils.getMessage(CommonConstants.MessageCode.E1X0001, null), e);
			throw new BestWorkBussinessException(CommonConstants.MessageCode.E1X0001, null);
		}
	}

	@Transactional(rollbackFor = { Exception.class })
	public MonitorResDto updateMonitor(MonitorResDto dto) throws BestWorkBussinessException {
		Optional<SysMonitorEntity> checkExist;
		try {
			checkExist = monitorRepository.findById(dto.getId());
			if (checkExist.isEmpty()) {
				logger.error(messageUtils.getMessage(CommonConstants.MessageCode.E1X0014, null));
				throw new BestWorkBussinessException(CommonConstants.MessageCode.E1X0014, null);
			}
			SysMonitorEntity monitor = checkExist.get();
			monitor.setName(dto.getName());
			monitor.setIcon(dto.getIcon());
			monitor.setUrl(dto.getUrl());
			monitor.setUpdatedDate(new Timestamp(System.currentTimeMillis()));
			monitor.setUpdatedUser(userAuthUtils.getUserInfoFromReq(false).getUsername());
			monitor.setIsMenu(dto.getIsMenu());
			monitorRepository.save(monitor);
			return modelMapper.map(monitor, MonitorResDto.class);
		} catch (Exception ex) {
			logger.error(messageUtils.getMessage(CommonConstants.MessageCode.E1X0001, null), ex);
			throw new BestWorkBussinessException(CommonConstants.MessageCode.E1X0001, null);
		}
	}

	public PageResDto<MonitorResDto> getMonitors(SearchDto dto) throws BestWorkBussinessException {
		try {
			int pageNumber = NumberUtils.toInt(dto.getPageConditon().getPage());
			if (pageNumber > 0) {
				pageNumber = pageNumber - 1;
			}
			Pageable pageable = PageRequest.of(pageNumber, Integer.parseInt(dto.getPageConditon().getSize()),
					Sort.by(dto.getPageConditon().getSortDirection(), dto.getPageConditon().getSortBy()));
			Page<SysMonitorEntity> pageSysRole = monitorRepository
					.findAllByNameContains(dto.getConditionSearchDto().getName(), pageable);
			return responseUtils.convertPageEntityToDTO(pageSysRole, MonitorResDto.class);
		} catch (Exception ex) {
			logger.error(messageUtils.getMessage(CommonConstants.MessageCode.E1X0001, null), ex);
			throw new BestWorkBussinessException(CommonConstants.MessageCode.E1X0001, null);
		}
	}

	public void deleteMonitor(Long id) throws BestWorkBussinessException {
		try {
			Optional<SysMonitorEntity> monitor = monitorRepository.findById(id);
			monitor.ifPresent(sysMonitor -> monitorRepository.delete(sysMonitor));
		} catch (Exception ex) {
			logger.error(messageUtils.getMessage(CommonConstants.MessageCode.RLF0002, null), ex);
			throw new BestWorkBussinessException(CommonConstants.MessageCode.RLF0002, null);
		}
	}
}
