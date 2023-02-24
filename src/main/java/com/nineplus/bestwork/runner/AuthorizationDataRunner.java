package com.nineplus.bestwork.runner;

import java.io.File;
import java.util.List;

import org.apache.commons.lang3.ObjectUtils;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.boot.CommandLineRunner;
import org.springframework.context.annotation.PropertySource;
import org.springframework.data.util.Pair;
import org.springframework.stereotype.Component;

import com.jcraft.jsch.ChannelSftp;
import com.jcraft.jsch.Session;
import com.nineplus.bestwork.entity.UserEntity;
import com.nineplus.bestwork.repository.PermissionRepository;
import com.nineplus.bestwork.repository.RoleRepository;
import com.nineplus.bestwork.repository.SysActionRepository;
import com.nineplus.bestwork.repository.SysMonitorRepository;
import com.nineplus.bestwork.services.ISftpFileService;
import com.nineplus.bestwork.services.PermissionService;
import com.nineplus.bestwork.services.UserService;
import com.nineplus.bestwork.utils.CommonConstants;

import lombok.extern.slf4j.Slf4j;

@Component
@PropertySource("classpath:application.properties")
@Slf4j
public class AuthorizationDataRunner implements CommandLineRunner {

	@Autowired
	private RoleRepository roleRepository;

	@Autowired
	private SysMonitorRepository monitorRepository;

	@Autowired
	private SysActionRepository sysActionRepository;

	@Autowired
	private PermissionRepository permissionRepository;

	@Autowired
	private UserService userService;

	@Autowired
	private PermissionService permissionService;

	@Autowired
	ISftpFileService sftpFileService;

	@Value("${bestwork.app.insertDataFlag}")
	private Boolean insertDataFlag;

	/**
	 * The Constant ROOT_PATH.
	 */
	@Value("${fileServer.root}")
	public String ROOT_PATH;

	/**
	 * The Constant Temporary folder.
	 */
	@Value("${fileServer.temp}")
	public String TEMP_FOLDER;

	@Override
	public void run(String... args) throws Exception {
		if (insertDataFlag) {
			deleteData();
			insertData();
			if (roleRepository.count() > 6) {
				List<UserEntity> userEntities = userService.getUsersByRole(CommonConstants.RoleName.SYS_COMPANY_ADMIN);
				userEntities.forEach(user -> permissionService.createPermissionsForNewSysCompanyAdmin(user));
			}
		}
		if (ObjectUtils.isNotEmpty(ROOT_PATH) && ObjectUtils.isNotEmpty(TEMP_FOLDER)) {
			Session session = null;
			ChannelSftp channel = null;
			try {
				Pair<Session, ChannelSftp> sftpConnection = sftpFileService.getConnection();
				session = sftpConnection.getFirst();
				channel = sftpConnection.getSecond();
				String path = ROOT_PATH + TEMP_FOLDER;
				createFolderTemp(path);
			} catch (Exception e) {
				log.error(e.getMessage(), e);
				sftpFileService.disconnect(session, channel);
			} finally {
				sftpFileService.disconnect(session, channel);
			}

		}
	}

	public void insertData() {
		monitorRepository.insertSystemDataMonitor();
		sysActionRepository.insertSystemDataAction();
		permissionRepository.insertSystemPermissionData();
	}

	public void deleteData() {
		sysActionRepository.deleteAllInBatch();
		monitorRepository.deleteAllInBatch();
		permissionRepository.deleteAllInBatch();
	}

	public void createFolderTemp(String pathDownloadTemp) {
		if (ObjectUtils.isNotEmpty(pathDownloadTemp)) {
			File theDir = new File(pathDownloadTemp);
			if (!theDir.exists()) {
				theDir.mkdirs();
			}
		}
	}
}
