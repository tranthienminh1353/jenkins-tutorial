package com.nineplus.bestwork.services.impl;

import java.io.IOException;
import java.math.BigInteger;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.HashSet;
import java.util.List;
import java.util.Optional;
import java.util.Set;

import javax.persistence.Tuple;

import org.apache.commons.lang3.ObjectUtils;
import org.modelmapper.ModelMapper;
import org.springframework.beans.BeanUtils;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;
import org.springframework.data.domain.Sort;
import org.springframework.data.util.Pair;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.multipart.MultipartFile;

import com.jcraft.jsch.ChannelSftp;
import com.jcraft.jsch.Session;
import com.nineplus.bestwork.dto.AirWayBillReqDto;
import com.nineplus.bestwork.dto.AirWayBillResDto;
import com.nineplus.bestwork.dto.CompanyBriefResDto;
import com.nineplus.bestwork.dto.ConstructionReqDto;
import com.nineplus.bestwork.dto.ConstructionResDto;
import com.nineplus.bestwork.dto.CountLocationDto;
import com.nineplus.bestwork.dto.FileStorageResDto;
import com.nineplus.bestwork.dto.IdsToDelReqDto;
import com.nineplus.bestwork.dto.NationResDto;
import com.nineplus.bestwork.dto.NotificationReqDto;
import com.nineplus.bestwork.dto.PageResDto;
import com.nineplus.bestwork.dto.PageSearchConstrctDto;
import com.nineplus.bestwork.dto.ProjectResDto;
import com.nineplus.bestwork.dto.RPageDto;
import com.nineplus.bestwork.entity.AirWayBill;
import com.nineplus.bestwork.entity.AssignTaskEntity;
import com.nineplus.bestwork.entity.CompanyEntity;
import com.nineplus.bestwork.entity.ConstructionEntity;
import com.nineplus.bestwork.entity.FileStorageEntity;
import com.nineplus.bestwork.entity.NationEntity;
import com.nineplus.bestwork.entity.ProgressEntity;
import com.nineplus.bestwork.entity.ProjectEntity;
import com.nineplus.bestwork.entity.UserEntity;
import com.nineplus.bestwork.exception.BestWorkBussinessException;
import com.nineplus.bestwork.model.UserAuthDetected;
import com.nineplus.bestwork.repository.AssignTaskRepository;
import com.nineplus.bestwork.repository.ConstructionRepository;
import com.nineplus.bestwork.repository.ProgressRepository;
import com.nineplus.bestwork.services.CompanyService;
import com.nineplus.bestwork.services.IAirWayBillService;
import com.nineplus.bestwork.services.IConstructionService;
import com.nineplus.bestwork.services.INationService;
import com.nineplus.bestwork.services.IProgressService;
import com.nineplus.bestwork.services.IProjectService;
import com.nineplus.bestwork.services.ISftpFileService;
import com.nineplus.bestwork.services.IStorageService;
import com.nineplus.bestwork.services.NotificationService;
import com.nineplus.bestwork.services.UserService;
import com.nineplus.bestwork.utils.CommonConstants;
import com.nineplus.bestwork.utils.ConvertResponseUtils;
import com.nineplus.bestwork.utils.Enums.ConstructionStatus;
import com.nineplus.bestwork.utils.Enums.FolderType;
import com.nineplus.bestwork.utils.MessageUtils;
import com.nineplus.bestwork.utils.UserAuthUtils;

/**
 * 
 * @author DiepTT
 *
 */
@Service
public class ConstructionServiceImpl implements IConstructionService {
	@Autowired
	private IProgressService progressService;

	@Autowired
	private ConstructionRepository cstrtRepo;

	@Autowired
	UserAuthUtils userAuthUtils;

	@Autowired
	private ConvertResponseUtils convertResponseUtils;

	@Autowired
	private IAirWayBillService awbService;

	@Autowired
	private IProjectService projectService;

	@Autowired
	private ISftpFileService sftpService;

	@Autowired
	private IStorageService storageService;

	@Autowired
	private AssignTaskRepository assignTaskRepo;

	@Autowired
	private UserService userService;

	@Autowired
	ModelMapper modelMapper;

	@Autowired
	MessageUtils messageUtils;

	@Autowired
	NotificationService notifyService;

	@Autowired
	ProgressRepository progressRepo;

	@Autowired
	INationService nationService;

	@Autowired
	CompanyService companyService;

	/**
	 * Function: get page of constructions with condition
	 */
	@Override
	public PageResDto<ConstructionResDto> getPageConstructions(PageSearchConstrctDto pageSearchDto)
			throws BestWorkBussinessException {
		UserAuthDetected userAuthRoleReq = this.getUserAuthRoleReq();

		try {
			Pageable pageable = this.convertSearch(pageSearchDto);

			List<String> prjIds = this.getCanViewPrjIds(userAuthRoleReq);
//			Page<ConstructionResDto> pageCstrt = null;
//			if (pageSearchDto.getCompanyId() == 0 && "%%".equals(pageSearchDto.getLocation())
//					&& pageSearchDto.getNationId() == 0 && "%%".equals(pageSearchDto.getProjectId())) {
//				pageCstrt = cstrtRepo.findCstrtByPrjIds(prjIds, pageSearchDto, pageable);
//			} else {
//				pageCstrt = cstrtRepo.findCstrtByCondition(prjIds, pageSearchDto, pageable);
//			}

			Page<ConstructionEntity> pageCstrt = null;
			if (pageSearchDto.getCompanyId() == 0 && "%%".equals(pageSearchDto.getLocation())
					&& pageSearchDto.getNationId() == 0 && "%%".equals(pageSearchDto.getProjectId())) {
				pageCstrt = cstrtRepo.findCstrtByPrjIds(prjIds, pageSearchDto, pageable);
			} else {
				pageCstrt = cstrtRepo.findCstrtByCondition(prjIds, pageSearchDto, pageable);
			}
			PageResDto<ConstructionResDto> pageResDto = new PageResDto<>();
			RPageDto metaData = new RPageDto();
			metaData.setNumber(pageCstrt.getNumber());
			metaData.setSize(pageCstrt.getSize());
			metaData.setTotalElements(pageCstrt.getTotalElements());
			metaData.setTotalPages(pageCstrt.getTotalPages());
			pageResDto.setMetaData(metaData);

			List<ConstructionResDto> constructionResDtos = new ArrayList<>();
			for (ConstructionEntity construction : pageCstrt.getContent()) {
				ConstructionResDto dto = this.trsferCstrtToResDto(construction);
				constructionResDtos.add(dto);
			}
			pageResDto.setContent(constructionResDtos);
//			pageResDto.setContent(pageCstrt.getContent());

			return pageResDto;
		} catch (Exception ex) {
			throw new BestWorkBussinessException(ex.getMessage(), null);
		}
	}

	private List<String> getCanViewPrjIds(UserAuthDetected userAuthRoleReq) {
		List<ProjectEntity> canViewPrjList = projectService.getPrjLstByAnyUsername(userAuthRoleReq);
		List<String> prjIds = new ArrayList<>();
		for (ProjectEntity project : canViewPrjList) {
			prjIds.add(project.getId());
		}
		return prjIds;
	}

	/**
	 * Private function: get userAuthDetected
	 * 
	 * @return UserAuthDetected
	 * @throws BestWorkBussinessException
	 */
	private UserAuthDetected getUserAuthRoleReq() throws BestWorkBussinessException {
		return userAuthUtils.getUserInfoFromReq(false);
	}

	/**
	 * Private function: convert from PageSearchDto to Pageable and search condition
	 * 
	 * @param pageSearchDto
	 * @return Pageable
	 * @throws BestWorkBussinessException
	 */
	private Pageable convertSearch(PageSearchConstrctDto pageSearchDto) throws BestWorkBussinessException {
		// keyword
		if ("".equals(pageSearchDto.getKeyword())) {
			pageSearchDto.setKeyword("%%");
		} else {
			pageSearchDto.setKeyword("%" + pageSearchDto.getKeyword() + "%");
		}
		// status
		if (pageSearchDto.getStatus() < 0 || pageSearchDto.getStatus() >= ConstructionStatus.values().length) {
			pageSearchDto.setStatus(-1);
		}

		// companyId
		if (pageSearchDto.getCompanyId() <= 0) {
			pageSearchDto.setCompanyId(0);
		}

		// nation
		if (pageSearchDto.getNationId() <= 0 || pageSearchDto.getNationId() > this.nationService.findAll().size()) {
			pageSearchDto.setNationId(0);
		}

		// location
		if ("".equals(pageSearchDto.getLocation()) || ObjectUtils.isEmpty(pageSearchDto.getLocation())) {
			pageSearchDto.setLocation("%%");
		} else {
			pageSearchDto.setLocation("%" + pageSearchDto.getLocation() + "%");
		}
		// projectId
		if ("".equals(pageSearchDto.getProjectId()) || ObjectUtils.isEmpty(pageSearchDto.getProjectId())) {
			pageSearchDto.setProjectId("%%");
		}

		String mappedColumn = convertResponseUtils.convertResponseConstruction(pageSearchDto.getSortBy());
		return PageRequest.of(Integer.parseInt(pageSearchDto.getPage()), Integer.parseInt(pageSearchDto.getSize()),
				Sort.by(pageSearchDto.getSortDirection(), mappedColumn));
	}

	/**
	 * Private function: get all projects that current user is being involved
	 * (creating or/and being assigned)
	 * 
	 * @param curUsername
	 * @return List<ProjectEntity>
	 */
	private List<ProjectEntity> getPrjInvolvedByCompUser(String curUsername) {
		List<ProjectEntity> creatingPrjList = projectService.getPrjCreatedByCurUser(curUsername);
		List<ProjectEntity> assignedPrjList = projectService.getPrjAssignedToCurUser(curUsername);
		Set<ProjectEntity> projectSet = new HashSet<>();
		if (creatingPrjList != null)
			projectSet.addAll(creatingPrjList);
		if (assignedPrjList != null)
			projectSet.addAll(assignedPrjList);
		return new ArrayList<>(projectSet);
	}

	/**
	 * Function: create construction
	 * 
	 * @param constructionReqDto
	 */
	@Override
	@Transactional
	public void createConstruction(ConstructionReqDto constructionReqDto, List<MultipartFile> drawings)
			throws BestWorkBussinessException {
		UserAuthDetected userAuthDetected = this.getUserAuthRoleReq();
		String curUsername = userAuthDetected.getUsername();

		if (!chkCurUserCanCreateCstrt(userAuthDetected, constructionReqDto.getProjectCode())) {
			throw new BestWorkBussinessException(CommonConstants.MessageCode.E1X0014, null);
		}
		this.chkExistCstrtNameWhenCreating(constructionReqDto);
		this.validateCstrtInfo(constructionReqDto);

		CompanyEntity company = this.companyService.findByCrtedPrjId(constructionReqDto.getProjectCode());
		if (ObjectUtils.isEmpty(company)) {
			throw new BestWorkBussinessException(CommonConstants.MessageCode.CPN0007, null);
		}

		ConstructionEntity construction = new ConstructionEntity();
		construction.setCreateBy(curUsername);
		construction.setCompanyId(company.getId());
		this.trsferDtoToCstrt(constructionReqDto, construction);
		try {
			construction = this.cstrtRepo.save(construction);
			projectService.updateStsProject(construction.getProjectCode());

			if (!sftpService.isValidFile(drawings)) {
				throw new BestWorkBussinessException(CommonConstants.MessageCode.eF0002, null);
			}
			Pair<Session, ChannelSftp> sftpConnection = sftpService.getConnection();
			Session session = sftpConnection.getFirst();
			ChannelSftp channel = sftpConnection.getSecond();
			for (MultipartFile file : drawings) {
				if (!file.isEmpty()) {
					String pathServer = this.sftpService.uploadConstructionDrawing(sftpConnection, file, construction.getId());
					storageService.storeFile(construction.getId(), FolderType.CONSTRUCTION, pathServer);
				}
			}
			sftpService.disconnect(session,channel);
			
			this.sendNotify(construction, curUsername, true, false);
		} catch (BestWorkBussinessException ex) {
			throw new BestWorkBussinessException(CommonConstants.MessageCode.FILE0002, null);
		}
	}

	/**
	 * Private function: transfer from ConstructionReqDto to ConstructionEntity and
	 * save ConstructionEntity to database
	 * 
	 * @param cstrtReqDto
	 * @param construction
	 */
	private void trsferDtoToCstrt(ConstructionReqDto cstrtReqDto, ConstructionEntity construction) {
		construction.setConstructionName(cstrtReqDto.getConstructionName());
		construction.setDescription(cstrtReqDto.getDescription());
		construction.setLocation(cstrtReqDto.getLocation());
		construction.setNationId(cstrtReqDto.getNationId());
		construction.setStartDate(cstrtReqDto.getStartDate());
		construction.setEndDate(cstrtReqDto.getEndDate());
		construction.setStatus(cstrtReqDto.getStatus());
		construction.setProjectCode(cstrtReqDto.getProjectCode());
		List<AirWayBill> airWayBills = new ArrayList<>();
		for (AirWayBillReqDto dto : cstrtReqDto.getAwbCodes()) {
			AirWayBill awb = this.awbService.findByCode(dto.getCode());
			airWayBills.add(awb);
		}
		construction.setAirWayBills(airWayBills);
	}

	/**
	 * Private function: check the validation of constructionReqDto before creating
	 * new construction
	 * 
	 * @param cstrtReqDto
	 * @throws BestWorkBussinessException
	 */
	private void validateCstrtInfo(ConstructionReqDto cstrtReqDto) throws BestWorkBussinessException {
		String constructionName = cstrtReqDto.getConstructionName();
		List<AirWayBillReqDto> awbCodes = cstrtReqDto.getAwbCodes();

		// Check construction name: not blank
		if (ObjectUtils.isEmpty(constructionName)) {
			throw new BestWorkBussinessException(CommonConstants.MessageCode.EMP0001,
					new Object[] { CommonConstants.Character.CONSTRUCTION_NAME });
		}

		// Check if current project (found by project id) exists or not
		Optional<ProjectEntity> curProjectOpt = this.projectService.getProjectById(cstrtReqDto.getProjectCode());
		if (curProjectOpt.isEmpty()) {
			throw new BestWorkBussinessException(CommonConstants.MessageCode.EXS0004,
					new Object[] { CommonConstants.Character.PROJECT });
		}
		ProjectEntity curProject = curProjectOpt.get();

		// Check if current user is involved in current project or not
		UserAuthDetected userAuthRoleReq = this.getUserAuthRoleReq();
		String curUsername = userAuthRoleReq.getUsername();
		List<ProjectEntity> involvedProjectList = this.getPrjInvolvedByCompUser(curUsername);
		if (!involvedProjectList.contains(curProject)) {
			throw new BestWorkBussinessException(CommonConstants.MessageCode.EXS0009, null);
		}

		// Check status
		if (Integer.parseInt(cstrtReqDto.getStatus()) < 0
				|| Integer.parseInt(cstrtReqDto.getStatus()) >= ConstructionStatus.values().length) {
			throw new BestWorkBussinessException(CommonConstants.MessageCode.ECS0008, null);
		}

		// Check AWB codes: not blank
		if (ObjectUtils.isEmpty(awbCodes)) {
			throw new BestWorkBussinessException(CommonConstants.MessageCode.EMP0001,
					new Object[] { CommonConstants.Character.AIR_WAY_BILL });
		}

		// Check existence of AWB codes
		Set<ProjectEntity> prjContainAWBs = new HashSet<>();
		for (AirWayBillReqDto awbReqDto : awbCodes) {
			String code = awbReqDto.getCode();
			AirWayBill airWayBill = this.awbService.findByCode(code);
			if (ObjectUtils.isEmpty(airWayBill)) {
				throw new BestWorkBussinessException(CommonConstants.MessageCode.EXS0004,
						new Object[] { "AWB code " + code });
			}
			Optional<ProjectEntity> projectOpt = this.projectService.getProjectById(airWayBill.getProjectCode());
			if (projectOpt.isEmpty()) {
				throw new BestWorkBussinessException(CommonConstants.MessageCode.EXS0007,
						new Object[] { "code " + code });
			}
			prjContainAWBs.add(projectOpt.get());
		}

		// Check if all the allocated AWB codes exist in current project or not
		if (prjContainAWBs.size() > 1) {
			throw new BestWorkBussinessException(CommonConstants.MessageCode.EXS0005, null);
		} else if (prjContainAWBs.size() == 1 && !prjContainAWBs.contains(curProject)) {
			throw new BestWorkBussinessException(CommonConstants.MessageCode.EXS0008, null);
		} else if (prjContainAWBs.size() == 0) {
			throw new BestWorkBussinessException(CommonConstants.MessageCode.EXS0006, null);
		}

		// Check if the AWB list for the construction contains at least 1 bill that is
		// already customs cleared
		if (!checkAWBStatus(cstrtReqDto.getAwbCodes())) {
			throw new BestWorkBussinessException(CommonConstants.MessageCode.ECS0003, null);
		}
	}

	/**
	 * Private function: check if the air way bill list for the construction
	 * contains at least one bill that is already customs cleared or not
	 * 
	 * @param awbCodes
	 * @return true/false
	 */
	public Boolean checkAWBStatus(List<AirWayBillReqDto> awbCodes) {
		List<String> codes = awbCodes.stream().map(AirWayBillReqDto::getCode).toList();
		return this.awbService.checkExistAwbDone(codes);
	}

	/**
	 * Private function: Check new construction name cannot be the same as existed
	 * construction's when creating new construction
	 * 
	 * @param constructionReqDto
	 * @throws BestWorkBussinessException
	 */
	private void chkExistCstrtNameWhenCreating(ConstructionReqDto constructionReqDto)
			throws BestWorkBussinessException {
		ConstructionEntity existedCstrt = cstrtRepo.findByConstructionName(constructionReqDto.getConstructionName());
		if (!ObjectUtils.isEmpty(existedCstrt)) {
			throw new BestWorkBussinessException(CommonConstants.MessageCode.EXS0003,
					new Object[] { CommonConstants.Character.CONSTRUCTION_NAME });
		}
	}

	/**
	 * Private function: Check new construction name cannot be the same as existed
	 * construction's when updating construction
	 * 
	 * @param constructionReqDto
	 * @param curConstruction
	 * @throws BestWorkBussinessException
	 */
	private void chkExistCstrtNameWhenEditing(ConstructionReqDto constructionReqDto, ConstructionEntity curConstruction)
			throws BestWorkBussinessException {
		ConstructionEntity existedCstrt = cstrtRepo.findByConstructionName(constructionReqDto.getConstructionName());
		if (!ObjectUtils.isEmpty(existedCstrt)
				&& !curConstruction.getConstructionName().equals(existedCstrt.getConstructionName())) {
			throw new BestWorkBussinessException(CommonConstants.MessageCode.EXS0003,
					new Object[] { CommonConstants.Character.CONSTRUCTION_NAME });
		}
	}

	/**
	 * Function: get detailed construction by construction id and user involved in
	 * the project
	 * 
	 * @param constructionId
	 * @return ConstructionResDto
	 * @throws BestWorkBussinessException
	 */
	@Override
	public ConstructionResDto findCstrtResById(long constructionId) throws BestWorkBussinessException {
		UserAuthDetected userAuthRoleReq = this.getUserAuthRoleReq();
		Optional<ConstructionEntity> constructionOpt = cstrtRepo.findById(constructionId);
		if (constructionOpt.isEmpty()) {
			return null;
		}
		if (!chkCurUserCanViewCstrt(constructionId, userAuthRoleReq)) {
			throw new BestWorkBussinessException(CommonConstants.MessageCode.E1X0014, null);
		}
		ConstructionEntity cstrt = constructionOpt.get();
		ConstructionResDto cstrtResDto = this.trsferCstrtToResDto(cstrt);
		List<FileStorageResDto> fsResDtos = new ArrayList<>();
		if (!ObjectUtils.isEmpty(cstrt.getFileStorages())) {
			Pair<Session, ChannelSftp> sftpConnection = sftpService.getConnection();
			Session session = sftpConnection.getFirst();
			ChannelSftp channel = sftpConnection.getSecond();
			for (FileStorageEntity file : cstrt.getFileStorages()) {
				FileStorageResDto fsResDto = new FileStorageResDto();
				fsResDto.setId(file.getId());
				fsResDto.setName(file.getName());
				fsResDto.setCreateDate(String.valueOf(file.getCreateDate()));
				fsResDto.setType(file.getType());
				fsResDto.setChoosen(file.isChoosen());
				if (Arrays.asList(CommonConstants.Image.IMAGE_EXTENSION).contains(file.getType())) {
					String pathServer = file.getPathFileServer();
					byte[] imageContent = sftpService.getFile(pathServer, sftpConnection);
					fsResDto.setContent(imageContent);
				}
				fsResDtos.add(fsResDto);
			}
		}
		cstrtResDto.setFileStorages(fsResDtos);

		return cstrtResDto;
	}

	private ConstructionResDto trsferCstrtToResDto(ConstructionEntity cstrt) throws BestWorkBussinessException {
		Optional<ProjectEntity> projectOpt = this.projectService.getProjectById(cstrt.getProjectCode());
		if (projectOpt.isEmpty()) {
			throw new BestWorkBussinessException(CommonConstants.MessageCode.S1X0002, null);
		}
//		CompanyEntity company = this.companyService.findByCrtedPrjId(projectOpt.get().getId());
		Optional<CompanyEntity> companyOpt = this.companyService.findById(cstrt.getCompanyId());
		if (ObjectUtils.isEmpty(companyOpt)) {
			throw new BestWorkBussinessException(CommonConstants.MessageCode.CPN0007, null);
		}
		NationEntity nation = this.nationService.findById(cstrt.getNationId());
		ConstructionResDto cstrtResDto = new ConstructionResDto();
		cstrtResDto.setId(cstrt.getId());
		cstrtResDto.setConstructionName(cstrt.getConstructionName());
		cstrtResDto.setDescription(cstrt.getDescription());
		cstrtResDto.setNationId(cstrt.getNationId());
		cstrtResDto.setNationName(nation.getName());
		cstrtResDto.setLocation(cstrt.getLocation());
		cstrtResDto.setStartDate(cstrt.getStartDate());
		cstrtResDto.setEndDate(cstrt.getEndDate());
		cstrtResDto.setCreateBy(cstrt.getCreateBy());
		cstrtResDto.setStatus(cstrt.getStatus());
		cstrtResDto.setProjectCode(cstrt.getProjectCode());
		cstrtResDto.setProjectName(projectOpt.get().getProjectName());
		cstrtResDto.setCompanyId(cstrt.getCompanyId());
		cstrtResDto.setCompanyName(companyOpt.get().getCompanyName());
		List<AirWayBillResDto> awbCodes = new ArrayList<>();
		if (!ObjectUtils.isEmpty(cstrt.getAirWayBills())) {
			for (AirWayBill airWayBill : cstrt.getAirWayBills()) {
				AirWayBillResDto dto = new AirWayBillResDto();
				dto = modelMapper.map(airWayBill, AirWayBillResDto.class);
				dto.setStatus(airWayBill.getStatus());
				awbCodes.add(dto);
			}
		}
		cstrtResDto.setAwbCodes(awbCodes);
		return cstrtResDto;
	}

	/**
	 * Private function: get project that contains the current construction
	 * 
	 * @param constructionId
	 * @return ProjectEntity
	 */
	private ProjectEntity getPrjByCurCstrt(long constructionId) {
		ProjectEntity project = this.projectService.getPrjByCstrtId(constructionId);
		return project;
	}

	public Boolean chkCurUserCanCreateCstrt(UserAuthDetected userAuthDetected, String prjCode)
			throws BestWorkBussinessException {
		UserEntity curUser = this.userService.findUserByUsername(userAuthDetected.getUsername());
		if (curUser == null) {
			return false;
		}
		AssignTaskEntity curAssign = this.assignTaskRepo.findByProjectIdAndUserId(prjCode, curUser.getId());
		if (curAssign == null) {
			return false;
		}
		if (userAuthDetected.getIsContractor() && curAssign.isCanEdit()) {
			return true;
		}
		return false;
	}

	/**
	 * Private function: check if a specific user can view a specific construction
	 * or not
	 * 
	 * @param constructionId
	 * @param userAuthRoleReq
	 * @return true/false
	 */
	private Boolean chkCurUserCanViewCstrt(long constructionId, UserAuthDetected userAuthRoleReq) {
		ProjectEntity curPrj = this.getPrjByCurCstrt(constructionId);
		List<ProjectEntity> prjLstCurUserCanView = this.projectService.getPrjLstByAnyUsername(userAuthRoleReq);
		if (prjLstCurUserCanView.contains(curPrj)) {
			return true;
		}
		return false;
	}

	/**
	 * Private function: check if a specific user can edit a specific construction
	 * or not
	 * 
	 * @param construction
	 * @param username
	 * @return true/false
	 */
	private Boolean chkCurUserCanEditDelCstrt(ConstructionEntity construction, String username) {
		if (construction.getCreateBy().equals(username)) {
			return true;
		}
		return false;
	}

	/**
	 * Function: update construction by constructionId
	 * 
	 * @param constructionId
	 * @param constructionReqDto
	 * @throws IOException
	 */
	@Override
	@Transactional
	public void updateConstruction(long constructionId, ConstructionReqDto constructionReqDto,
			List<MultipartFile> drawings) throws BestWorkBussinessException, IOException {
		UserAuthDetected userAuthRoleReq = this.getUserAuthRoleReq();
		String curUsername = userAuthRoleReq.getUsername();
		Optional<ConstructionEntity> constructionOpt = cstrtRepo.findById(constructionId);
		if (constructionOpt.isEmpty()) {
			throw new BestWorkBussinessException(CommonConstants.MessageCode.E1X0003, null);
		}
		ConstructionEntity curConstruction = constructionOpt.get();
		ConstructionEntity originConstruction = new ConstructionEntity();
		BeanUtils.copyProperties(curConstruction, originConstruction);
		if (!chkCurUserCanEditDelCstrt(curConstruction, curUsername)) {
			throw new BestWorkBussinessException(CommonConstants.MessageCode.E1X0014, null);
		}
		this.chkExistCstrtNameWhenEditing(constructionReqDto, curConstruction);
		this.validateCstrtInfo(constructionReqDto);

		CompanyEntity company = this.companyService.findByCrtedPrjId(constructionReqDto.getProjectCode());
		if (ObjectUtils.isEmpty(company)) {
			throw new BestWorkBussinessException(CommonConstants.MessageCode.CPN0007, null);
		}
		curConstruction.setCompanyId(company.getId());

		this.trsferDtoToCstrt(constructionReqDto, curConstruction);

		try {
			curConstruction = this.cstrtRepo.save(curConstruction);
			// send notify to Investor and relative Supplier when construction is done
			if (Integer.parseInt(curConstruction.getStatus()) == ConstructionStatus.DONE.ordinal() && Integer
					.parseInt(curConstruction.getStatus()) != Integer.parseInt(originConstruction.getStatus())) {
				this.sendNotify(curConstruction, curUsername, false, true);
			}

			if (!sftpService.isValidFile(drawings)) {
				throw new BestWorkBussinessException(CommonConstants.MessageCode.eF0002, null);
			}
			// Get file-paths of the construction and remove them from server
			List<String> listPath = this.storageService.getPathFileByCstrtId(constructionId);
			
			Pair<Session, ChannelSftp> sftpConnection = sftpService.getConnection();
			Session session = sftpConnection.getFirst();
			ChannelSftp channel = sftpConnection.getSecond();
			for (String path : listPath) {
				this.sftpService.removeFile(path,sftpConnection);
			}
			// Delete files of the construction from database
			this.storageService.deleteByCstrtId(constructionId);
			// Save new files to database and server
			for (MultipartFile file : drawings) {
				if (ObjectUtils.isNotEmpty(file.getBytes())) {
					String pathServer = this.sftpService.uploadConstructionDrawing(sftpConnection, file, curConstruction.getId());
					storageService.storeFile(curConstruction.getId(), FolderType.CONSTRUCTION, pathServer);
				}
			}
			sftpService.disconnect(session,channel);
		} catch (BestWorkBussinessException ex) {
			throw new BestWorkBussinessException(CommonConstants.MessageCode.FILE0002, null);
		}
	}

	/**
	 * 
	 * @param construction
	 * @param curUsername
	 * @param isCrtCstrt   (boolean: true when construction is created)
	 * @param isCstrtDone  (boolean: true when construction is done/finished)
	 * @throws BestWorkBussinessException
	 */
	private void sendNotify(ConstructionEntity construction, String curUsername, boolean isCrtCstrt,
			boolean isCstrtDone) throws BestWorkBussinessException {
		ProjectEntity curPrj = projectService.getPrjByCstrtId(construction.getId());
		List<AirWayBill> curAwbList = construction.getAirWayBills();

		Set<String> involvedUsername = new HashSet<>();
		involvedUsername.add(curPrj.getCreateBy());
		for (AirWayBill awb : curAwbList) {
			involvedUsername.add(awb.getCreateBy());
		}
		String title = "";
		String content = "";
		// Set the title and content for the notify when construction is created
		if (isCrtCstrt) {
			title = messageUtils.getMessage(CommonConstants.MessageCode.TNU0007,
					new Object[] { curPrj.getProjectName() });
			content = messageUtils.getMessage(CommonConstants.MessageCode.CNU0007,
					new Object[] { curUsername, construction.getLocation() });
		}
		// Set the title and content for the notify when construction is done/finished
		else if (isCstrtDone) {
			title = messageUtils.getMessage(CommonConstants.MessageCode.TNU0008,
					new Object[] { curPrj.getProjectName() });
			content = messageUtils.getMessage(CommonConstants.MessageCode.CNU0008,
					new Object[] { construction.getConstructionName(), construction.getLocation() });
		}

		NotificationReqDto notifyReqDto = new NotificationReqDto();
		notifyReqDto.setTitle(title);
		notifyReqDto.setContent(content);

		for (String username : involvedUsername) {
			UserEntity user = userService.findUserByUsername(username);
			notifyReqDto.setUserId(user.getId());
			notifyService.createNotification(notifyReqDto);
		}
	}

	/**
	 * Function: delete constructions by list of construction ids
	 * 
	 * @param ConstructionListIdDto
	 */
	@Override
	@Transactional
	public void deleteConstruction(IdsToDelReqDto idsToDelReqDto) throws BestWorkBussinessException {
		UserAuthDetected userAuthRoleReq = this.getUserAuthRoleReq();
		String curUsername = userAuthRoleReq.getUsername();
		Long[] ids = idsToDelReqDto.getListId();
		List<String> listPath = new ArrayList<>();
		List<ConstructionEntity> cstrtList = new ArrayList<>();
		List<Long> prgIdListToDel = new ArrayList<>();
		for (long id : ids) {
			Optional<ConstructionEntity> cstrtOpt = cstrtRepo.findById(id);
			if (cstrtOpt.isEmpty()) {
				throw new BestWorkBussinessException(CommonConstants.MessageCode.ECS0007, null);
			}
			cstrtList.add(cstrtOpt.get());
			// Get all progresses of the construction
			List<ProgressEntity> prgList = this.progressRepo.findByConstructionId(id);
			for (ProgressEntity prg : prgList) {
				if (prg != null) {
					prgIdListToDel.add(prg.getId());
				}
			}

			// Get file-paths of the construction
			List<String> paths = this.storageService.getPathFileByCstrtId(id);
			for (String path : paths) {
				if (path != null) {
					listPath.add(path);
				}
			}
		}
		for (ConstructionEntity construction : cstrtList) {
			if (!chkCurUserCanEditDelCstrt(construction, curUsername)) {
				throw new BestWorkBussinessException(CommonConstants.MessageCode.E1X0014, null);
			}
		}
		if (cstrtList.contains(null)) {
			throw new BestWorkBussinessException(CommonConstants.MessageCode.ECS0006, null);
		}
		// Delete all progresses of the constructions that will be deleted
		this.progressService.deleteProgressList(prgIdListToDel);

		Pair<Session, ChannelSftp> sftpConnection = sftpService.getConnection();
		Session session = sftpConnection.getFirst();
		ChannelSftp channel = sftpConnection.getSecond();
		// Remove file-paths from server
		for (String path : listPath) {
			this.sftpService.removeFile(path,sftpConnection);
		}
		sftpService.disconnect(session,channel);
		// Delete files of the construction from database
		this.storageService.deleteByCstrtIds(Arrays.asList(ids));
		this.cstrtRepo.deleteAll(cstrtList);
	}

	@Override
	public ConstructionEntity findCstrtById(long constructionId) {
		Optional<ConstructionEntity> cstrtOpt = this.cstrtRepo.findById(constructionId);
		return cstrtOpt.orElse(null);
	}

	@Override
	public ConstructionEntity findCstrtByPrgId(Long progressId) {
		return this.cstrtRepo.findByProgressId(progressId);
	}

	@Override
	public List<CompanyBriefResDto> getCompanyCrtPrj() throws BestWorkBussinessException {
		List<ProjectEntity> prjList = this.projectService.getPrjLstByAnyUsername(this.getUserAuthRoleReq());
		List<String> prjIds = new ArrayList<>();
		for (ProjectEntity prj : prjList) {
			prjIds.add(prj.getId());
		}
		if (ObjectUtils.isEmpty(prjIds)) {
			throw new BestWorkBussinessException(CommonConstants.MessageCode.E1X0003, null);
		}
		List<CompanyEntity> compList = this.companyService.findByCrtedPrjIds(prjIds);
		if (ObjectUtils.isEmpty(compList)) {
			throw new BestWorkBussinessException(CommonConstants.MessageCode.E1X0003, null);
		}
		return trferCompLstToDtos(compList);
	}

	private List<CompanyBriefResDto> trferCompLstToDtos(List<CompanyEntity> compList) {
		List<CompanyBriefResDto> dtos = new ArrayList<>();
		for (CompanyEntity company : compList) {
			CompanyBriefResDto dto = new CompanyBriefResDto();
			dto.setId(company.getId());
			dto.setCompanyName(company.getCompanyName());
			dtos.add(dto);
		}
		return dtos;
	}

	@Override
	public List<ProjectResDto> getPrjForCurUser() throws BestWorkBussinessException {
		List<ProjectEntity> canViewPrjList = projectService.getPrjLstByAnyUsername(this.getUserAuthRoleReq());
		if (ObjectUtils.isEmpty(canViewPrjList)) {
			throw new BestWorkBussinessException(CommonConstants.MessageCode.E1X0003, null);
		}
		canViewPrjList.sort((p1, p2) -> p1.getId().compareTo(p2.getId()));
		List<ProjectResDto> dtos = new ArrayList<>();
		for (ProjectEntity prj : canViewPrjList) {
			ProjectResDto dto = new ProjectResDto();
			dto.setId(prj.getId());
			dto.setProjectName(prj.getProjectName());
			dto.setDescription(prj.getDescription());
			dto.setProjectType(prj.getProjectType());
			dto.setIsPaid(prj.getIsPaid());
			dto.setNotificationFlag(prj.getNotificationFlag());
			dto.setStartDate(prj.getStartDate());
			dto.setStatus(prj.getStatus());
			dtos.add(dto);
		}
		return dtos;
	}

	@Override
	public List<NationResDto> getNationsByCurCstrt() throws BestWorkBussinessException {
		List<String> canViewPrjIds = this.getCanViewPrjIds(getUserAuthRoleReq());

		List<NationEntity> nationList = new ArrayList<>();
		List<Long> nationIds = this.cstrtRepo.findNationIdsByPrjIds(canViewPrjIds);
		nationList = this.nationService.findByIds(nationIds);
		if (ObjectUtils.isEmpty(nationList)) {
			throw new BestWorkBussinessException(CommonConstants.MessageCode.ENA0002, null);
		}
		List<NationResDto> dtos = new ArrayList<>();
		for (NationEntity nation : nationList) {
			NationResDto dto = new NationResDto();
			dto.setId(nation.getId());
			dto.setName(nation.getName());
			dtos.add(dto);
		}
		return dtos;
	}

	@Transactional
	public void updateStsConstruction(long progressId, String status) throws BestWorkBussinessException {
		ConstructionEntity constructionCur = this.cstrtRepo.findByProgressId(progressId);
		constructionCur.setStatus(status);
		this.cstrtRepo.save(constructionCur);
	}

	@Override
	public ConstructionEntity closeCstrt(long constructionId) throws BestWorkBussinessException {
		Optional<ConstructionEntity> cstrtOpt = this.cstrtRepo.findById(constructionId);
		if (cstrtOpt.isEmpty()) {
			throw new BestWorkBussinessException(CommonConstants.MessageCode.ECS0006, null);
		}
		ConstructionEntity curCstrt = cstrtOpt.get();
		if (!chkCurUserCanEditDelCstrt(curCstrt, this.getUserAuthRoleReq().getUsername())) {
			throw new BestWorkBussinessException(CommonConstants.MessageCode.E1X0014, null);
		}
		;
		curCstrt.setStatus(String.valueOf(ConstructionStatus.DONE.ordinal()));
		return this.cstrtRepo.save(curCstrt);

	}

	public Integer countConstructionUser(String username) {
		UserEntity user = userService.getUserByUsername(username);
		if (ObjectUtils.isNotEmpty(user)) {
			return cstrtRepo.countConstructionUser(null, null, user.getId(), user.getUserName());
		}
		return 0;
	}

	@Override
	public List<CountLocationDto> getLocationsUser(String username) {
		UserEntity user = userService.getUserByUsername(username);
		List<CountLocationDto> countLocationDtos = new ArrayList<>();
		if (ObjectUtils.isNotEmpty(user)) {
			List<Tuple> tuples = cstrtRepo.getTopLocation(user.getId(), user.getUserName());
			if (!tuples.isEmpty()) {
				countLocationDtos = tuples.stream()
						.map(t -> new CountLocationDto(
										t.get(0, String.class),
										t.get(1, BigInteger.class),
										t.get(2, String.class))).toList();
			}
		}
		return countLocationDtos;
	}
}
