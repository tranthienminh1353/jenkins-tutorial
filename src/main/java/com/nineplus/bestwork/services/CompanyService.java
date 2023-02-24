package com.nineplus.bestwork.services;

import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Date;
import java.util.List;
import java.util.Optional;

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

import com.nineplus.bestwork.dto.CompanyListIdDto;
import com.nineplus.bestwork.dto.CompanyReqDto;
import com.nineplus.bestwork.dto.CompanyResDto;
import com.nineplus.bestwork.dto.CompanyUserReqDto;
import com.nineplus.bestwork.dto.CompanyUserResDto;
import com.nineplus.bestwork.dto.PageResDto;
import com.nineplus.bestwork.dto.PageSearchDto;
import com.nineplus.bestwork.dto.UserCompanyReqDto;
import com.nineplus.bestwork.dto.UserResDto;
import com.nineplus.bestwork.entity.CompanyEntity;
import com.nineplus.bestwork.entity.RoleEntity;
import com.nineplus.bestwork.entity.UserEntity;
import com.nineplus.bestwork.exception.BestWorkBussinessException;
import com.nineplus.bestwork.model.UserAuthDetected;
import com.nineplus.bestwork.repository.CompanyProjection;
import com.nineplus.bestwork.repository.CompanyRepository;
import com.nineplus.bestwork.repository.RoleRepository;
import com.nineplus.bestwork.repository.UserRepository;
import com.nineplus.bestwork.utils.CommonConstants;
import com.nineplus.bestwork.utils.ConvertResponseUtils;
import com.nineplus.bestwork.utils.DateUtils;
import com.nineplus.bestwork.utils.MessageUtils;
import com.nineplus.bestwork.utils.PageUtils;
import com.nineplus.bestwork.utils.UserAuthUtils;

@Service
@Transactional
public class CompanyService {

	private final Logger logger = LoggerFactory.getLogger(CompanyService.class);

	@Autowired
	UserAuthUtils userAuthUtils;

	@Autowired
	MessageUtils messageUtils;

	@Autowired
	RoleRepository roleRepository;

	@Autowired
	CompanyRepository companyRepository;

	@Autowired
	ModelMapper modelMapper;

	@Autowired
	UserService userService;

	@Autowired
	UserRepository userRepos;

	@Autowired
	DateUtils dateUtils;

	@Autowired
	PageUtils responseUtils;

	@Autowired
	ConvertResponseUtils convertResponseUtils;

	@Autowired
	IProjectService iProjectService;

	@Transactional(rollbackFor = { Exception.class })
	public void registCompany(CompanyUserReqDto companyReqDto) throws BestWorkBussinessException {

		// Check role of user
		UserAuthDetected userAuthRoleReq = userAuthUtils.getUserInfoFromReq(false);
		String createUser = userAuthRoleReq.getUsername();
		if (!userAuthRoleReq.getIsSysAdmin() && !userAuthRoleReq.getIsSysCompanyAdmin()) {
			throw new BestWorkBussinessException(CommonConstants.MessageCode.E1X0014, null);
		}

		// validate company information
		this.validateCpmnyInfor(0, companyReqDto.getCompany(), false);

		// validate user information
		this.validateUserInfor(companyReqDto.getUser());

		try {
			// Register company information in DB
			companyReqDto.getCompany().setCreateBy(createUser);
			CompanyEntity newCompanySaved = regist(companyReqDto);
			RoleEntity role = null;
			if (userAuthRoleReq.getIsSysAdmin()) {
				role = roleRepository.findRole(CommonConstants.RoleName.SYS_COMPANY_ADMIN,  userAuthUtils.getUserInfoFromReq(false).getUsername());
			}
			if (userAuthRoleReq.getIsSysCompanyAdmin()) {
				role = roleRepository.findRole(CommonConstants.RoleName.CMPNY_ADMIN,  userAuthUtils.getUserInfoFromReq(false).getUsername());
			}

			// Register user for this company
			userService.registNewUser(companyReqDto, newCompanySaved, role);

		} catch (BestWorkBussinessException ex) {
			logger.error(messageUtils.getMessage(CommonConstants.MessageCode.E1X0001,
					new Object[] { CommonConstants.Character.COMPANY, companyReqDto.getCompany().getCompanyName() }),
					ex);
			throw new BestWorkBussinessException(CommonConstants.MessageCode.E1X0001,
					new Object[] { CommonConstants.Character.COMPANY, companyReqDto.getCompany().getCompanyName() });
		}
	}

	/**
	 */
	public void validateCpmnyInfor(long companyId, CompanyReqDto companyReqDto, boolean isEdit)
			throws BestWorkBussinessException {
		// Validation register information
		if (ObjectUtils.isNotEmpty(companyReqDto)) {
			String companyName = companyReqDto.getCompanyName();

			// Company name can not be empty
			if (ObjectUtils.isEmpty(companyName)) {
				throw new BestWorkBussinessException(CommonConstants.MessageCode.EMP0001,
						new Object[] { CommonConstants.Character.CMPNY_NAME });
			}

			// Check exists company name in database
			if (!isEdit) {
				CompanyEntity company = companyRepository.findbyCompanyName(companyName);
				if (!ObjectUtils.isEmpty(company)) {
					throw new BestWorkBussinessException(CommonConstants.MessageCode.CPN0005, new Object[] { company });
				}
			} else {
				CompanyEntity currentComp = companyRepository.findById(companyId).get();
				CompanyEntity company = companyRepository.findbyCompanyName(companyName);
				if (ObjectUtils.isNotEmpty(company) && !currentComp.getCompanyName().equals(company.getCompanyName())) {
					throw new BestWorkBussinessException(CommonConstants.MessageCode.CPN0008, null);
				}
			}
			Date startDate = null;
			Date expiredDate = null;
			try {
				startDate = dateUtils.convertStringToDate(companyReqDto.getStartDate());
				expiredDate = dateUtils.convertStringToDate(companyReqDto.getExpiredDate());
			} catch (Exception e) {
				throw new BestWorkBussinessException(e.getMessage(), null);
			}

			// Expired date can not be before start date
			if (expiredDate.before(startDate)) {
				throw new BestWorkBussinessException(CommonConstants.MessageCode.eD0001, null);
			}

			// Expired date can not be before start date
			if (expiredDate.before(new Date())) {
				throw new BestWorkBussinessException(CommonConstants.MessageCode.eD0002, null);
			}
		}

	}

	public void validateUserInfor(UserCompanyReqDto userReq) throws BestWorkBussinessException {
		String userEmail = userReq.getEmail();
		String userName = userReq.getUserName();
		String password = userReq.getPassword();
		// User email can not be empty
		if (ObjectUtils.isEmpty(userEmail)) {
			throw new BestWorkBussinessException(CommonConstants.MessageCode.EMP0001,
					new Object[] { CommonConstants.Character.USER_MAIL });
		}

		// User name can not be empty
		if (ObjectUtils.isEmpty(userName)) {
			throw new BestWorkBussinessException(CommonConstants.MessageCode.EMP0001,
					new Object[] { CommonConstants.Character.USER_NAME });
		}

		// Password can not be empty empty
		if (ObjectUtils.isEmpty(password) || password.length() < 6) {
			throw new BestWorkBussinessException(CommonConstants.MessageCode.EMP0001,
					new Object[] { CommonConstants.Character.PASSWORD });
		}

		// Check exists user name in DB
		UserEntity curUser = userRepos.findByUserName(userName);
		if (!ObjectUtils.isEmpty(curUser)) {
			throw new BestWorkBussinessException(CommonConstants.MessageCode.EXS0003, new Object[] { userName });
		}

		// Check exists user email in DB
		UserEntity user = userRepos.findByEmail(userEmail);
		if (!ObjectUtils.isEmpty(user)) {
			throw new BestWorkBussinessException(CommonConstants.MessageCode.EXS0003, new Object[] { userEmail });
		}

	}

	public CompanyEntity regist(CompanyUserReqDto companyReqDto) throws BestWorkBussinessException {
		CompanyEntity company = null;
		try {
			company = new CompanyEntity();
			company.setCompanyName(companyReqDto.getCompany().getCompanyName());
			company.setEmail(companyReqDto.getCompany().getEmail());
			company.setTelNo(companyReqDto.getCompany().getTelNo());
			company.setTaxNo(companyReqDto.getCompany().getTaxNo());
			company.setCity(companyReqDto.getCompany().getCity());
			company.setNation(companyReqDto.getCompany().getNation());
			company.setDistrict(companyReqDto.getCompany().getDistrict());
			company.setWard(companyReqDto.getCompany().getWard());
			company.setStreet(companyReqDto.getCompany().getStreet());
			company.setStartDate(companyReqDto.getCompany().getStartDate());
			company.setExpiredDate(companyReqDto.getCompany().getExpiredDate());
			company.setCreateDt(LocalDateTime.now());
			company.setCreateBy(companyReqDto.getCompany().getCreateBy());

			companyRepository.save(company);

		} catch (Exception ex) {
			logger.error(messageUtils.getMessage(CommonConstants.MessageCode.E1X0001, null), ex);
			throw new BestWorkBussinessException(CommonConstants.MessageCode.E1X0001, null);
		}

		return company;
	}

	@Transactional(rollbackFor = { Exception.class })
	public CompanyResDto updateCompany(long companyId, CompanyReqDto companyReqDto) throws BestWorkBussinessException {

		UserAuthDetected userAuthRoleReq = userAuthUtils.getUserInfoFromReq(false);
		// Only system admin can do this
		if (!userAuthRoleReq.getIsSysAdmin() && !userAuthRoleReq.getIsSysCompanyAdmin()) {
			logger.error(messageUtils.getMessage(CommonConstants.MessageCode.E1X0014, null));
			throw new BestWorkBussinessException(CommonConstants.MessageCode.E1X0014, null);
		}

		// validate company information
		this.validateCpmnyInfor(companyId, companyReqDto, true);

		CompanyEntity currentCompany = null;

		try {
			currentCompany = companyRepository.findByCompanyId(companyId);

			if (ObjectUtils.isEmpty(currentCompany)) {
				logger.error(messageUtils.getMessage(CommonConstants.MessageCode.CPN0003, null));
				throw new BestWorkBussinessException(CommonConstants.MessageCode.CPN0003, null);
			}
		} catch (Exception ex) {
			logger.error(messageUtils.getMessage(CommonConstants.MessageCode.E1X0003,
					new Object[] { companyReqDto.getCompanyName() }), ex);
			throw new BestWorkBussinessException(CommonConstants.MessageCode.E1X0003,
					new Object[] { companyReqDto.getCompanyName() });
		}

		try {
			currentCompany.setCompanyName(companyReqDto.getCompanyName());
			currentCompany.setEmail(companyReqDto.getEmail());
			currentCompany.setTelNo(companyReqDto.getTelNo());
			currentCompany.setTaxNo(companyReqDto.getTaxNo());
			currentCompany.setCity(companyReqDto.getCity());
			currentCompany.setNation(companyReqDto.getNation());
			currentCompany.setDistrict(companyReqDto.getDistrict());
			currentCompany.setWard(companyReqDto.getWard());
			currentCompany.setStreet(companyReqDto.getStreet());
			currentCompany.setStartDate(companyReqDto.getStartDate());
			currentCompany.setExpiredDate(companyReqDto.getExpiredDate());
			companyRepository.save(currentCompany);

			CompanyResDto resDTO = modelMapper.map(currentCompany, CompanyResDto.class);
			return resDTO;

		} catch (Exception ex) {
			logger.error(messageUtils.getMessage(CommonConstants.MessageCode.E1X0004,
					new Object[] { CommonConstants.Character.COMPANY, companyReqDto.getCompanyName() }), ex);
			throw new BestWorkBussinessException(CommonConstants.MessageCode.E1X0004,
					new Object[] { CommonConstants.Character.COMPANY, companyReqDto.getCompanyName() });
		}
	}

	/**
	 * 
	 * @param tCompanyId
	 * @return
	 * @throws BestWorkBussinessException
	 */
	@Transactional(rollbackFor = { Exception.class })
	public Long[] deleteCompany(CompanyListIdDto listId) throws BestWorkBussinessException {
		UserAuthDetected userAuthRoleReq = userAuthUtils.getUserInfoFromReq(false);
		// Only system admin can do this
		if (!userAuthRoleReq.getIsSysAdmin() && !userAuthRoleReq.getIsSysCompanyAdmin()) {
			logger.error(messageUtils.getMessage(CommonConstants.MessageCode.E1X0014, null));
			throw new BestWorkBussinessException(CommonConstants.MessageCode.E1X0014, null);
		}
		try {
			// delete user relate company
			List<UserEntity> allTusers = userRepos.findAllUserByCompanyIdList(Arrays.asList(listId.getLstCompanyId()));
			userRepos.deleteAllInBatch(allTusers);

			// Delete company
			companyRepository.deleteCompaniesWithIds(Arrays.asList(listId.getLstCompanyId()));

			// delete all project of company
			List<String> allProject = iProjectService.getAllProjectIdByCompany(Arrays.asList(listId.getLstCompanyId()));
			iProjectService.deleteProjectByIds(allProject);

		} catch (Exception ex) {
			logger.error(messageUtils.getMessage(CommonConstants.MessageCode.E1X0002, null), ex);
			throw new BestWorkBussinessException(CommonConstants.MessageCode.E1X0002, null);
		}
		return listId.getLstCompanyId();
	}

	/**
	 * 
	 * @param tCompanyId
	 * @return
	 * @throws BestWorkBussinessException
	 */
	@Transactional(rollbackFor = { Exception.class })
	public Long[] inactiveCompany(CompanyListIdDto listId) throws BestWorkBussinessException {
		UserAuthDetected userAuthRoleReq = userAuthUtils.getUserInfoFromReq(false);
		// Only system admin can do this
		if (!userAuthRoleReq.getIsSysAdmin() && !userAuthRoleReq.getIsSysCompanyAdmin()) {
			logger.error(messageUtils.getMessage(CommonConstants.MessageCode.E1X0014, null));
		}
		try {
			// inactive all user relate to company
			List<UserEntity> allTusers = userRepos.findAllUserByCompanyIdList(Arrays.asList(listId.getLstCompanyId()));
			List<Long> userIds = new ArrayList<>();
			for (UserEntity user : allTusers) {
				userIds.add(user.getId());
			}
			userRepos.inactiveUser(userIds);

			// inactive company
			companyRepository.inactiveCompany(Arrays.asList(listId.getLstCompanyId()));
		} catch (Exception ex) {
			logger.error(messageUtils.getMessage(CommonConstants.MessageCode.E1X0002, null), ex);
			throw new BestWorkBussinessException(CommonConstants.MessageCode.E1X0002, null);
		}
		return listId.getLstCompanyId();
	}

	/**
	 * 
	 * @param companyId Company ID
	 * @return Company information
	 */
	public Optional<CompanyEntity> getDetailCompany(long companyId) {
		Optional<CompanyEntity> company = companyRepository.findById(companyId);
		return company;
	}

	/**
	 * 
	 * @param companyId company ID
	 * @return Company and User information
	 * @throws BestWorkBussinessException
	 */
	public CompanyUserResDto getCompanyAndUser(long companyId) throws BestWorkBussinessException {
		CompanyUserResDto userCompanyRes = new CompanyUserResDto();
		CompanyEntity company = companyRepository.findByCompanyId(companyId);
		UserEntity user = userService.getUserByCompanyId(companyId);
		if (company != null && user != null) {
			CompanyResDto resCompany = modelMapper.map(company, CompanyResDto.class);
			UserResDto resUser = modelMapper.map(user, UserResDto.class);
			userCompanyRes.setCompany(resCompany);
			userCompanyRes.setUser(resUser);
		}
		return userCompanyRes;
	}

	public List<CompanyProjection> getAllCompany() throws BestWorkBussinessException {
		UserAuthDetected userAuthRoleReq = userAuthUtils.getUserInfoFromReq(false);
		String childUser = userAuthRoleReq.getUsername();
		//Get root administrator of user
		String adminUser =  userService.getAdminUser(childUser).getUserName();
		return companyRepository.getAllCompany(adminUser);
	}

	/**
	 * 
	 * @param pageCondition condition page
	 * @return page of company follow condition
	 * @throws BestWorkBussinessException
	 */
	public PageResDto<CompanyResDto> getCompanyPage(PageSearchDto pageCondition) throws BestWorkBussinessException {
		Page<CompanyEntity> pageCompany;
		try {
			UserAuthDetected userAuthRoleReq = userAuthUtils.getUserInfoFromReq(false);
			String createUser = userAuthRoleReq.getUsername();
			int pageNumber = NumberUtils.toInt(pageCondition.getPage());

			String mappedColumn = convertResponseUtils.convertResponseCompany(pageCondition.getSortBy());
			Pageable pageable = PageRequest.of(pageNumber, Integer.parseInt(pageCondition.getSize()),
					Sort.by(pageCondition.getSortDirection(), mappedColumn));
			pageCompany = companyRepository.getPageCompany(createUser, pageable);
			return responseUtils.convertPageEntityToDTO(pageCompany, CompanyResDto.class);
		} catch (Exception ex) {
			throw new BestWorkBussinessException(CommonConstants.MessageCode.E1X0003, null);
		}
	}

	/**
	 * Search company with keyword
	 * 
	 * @param keyword
	 * @param pageCondition
	 * @return
	 * @throws BestWorkBussinessException
	 */
	public PageResDto<CompanyResDto> searchCompanyPage(String keyword, int status, PageSearchDto pageCondition)
			throws BestWorkBussinessException {
		Page<CompanyEntity> pageTCompany = null;
		try {
			UserAuthDetected userAuthRoleReq = userAuthUtils.getUserInfoFromReq(false);
			String createUser = userAuthRoleReq.getUsername();
			int pageNumber = NumberUtils.toInt(pageCondition.getPage());

			String mappedColumn = convertResponseUtils.convertResponseCompany(pageCondition.getSortBy());
			Pageable pageable = PageRequest.of(pageNumber, Integer.parseInt(pageCondition.getSize()),
					Sort.by(pageCondition.getSortDirection(), mappedColumn));
			if (!keyword.isBlank() && status != 2) {
				pageTCompany = companyRepository.searchCompanyPage(createUser,convertWildCard(keyword), status, pageable);
			} else if (keyword.isBlank()) {
				pageTCompany = companyRepository.searchCompanyPageWithOutKeyWord(createUser,status, pageable);
			} else if (status == 2 && !keyword.isBlank()) {
				pageTCompany = companyRepository.searchCompanyPageWithOutStatus(createUser,convertWildCard(keyword), pageable);
			}
			return responseUtils.convertPageEntityToDTO(pageTCompany, CompanyResDto.class);
		} catch (Exception ex) {
			throw new BestWorkBussinessException(CommonConstants.MessageCode.E1X0003, null);
		}
	}

	private String convertWildCard(String text) {
		return "*" + text + "*";
	}

	public List<CompanyEntity> findByCrtedPrjIds(List<String> prjIds) {
		return this.companyRepository.findByCrtedPrjIds(prjIds);
	}

	public CompanyEntity findByCrtedPrjId(String prjId) {
		return this.companyRepository.findByCrtedPrjId(prjId);
	}

	public Optional<CompanyEntity> findById(long companyId) {
		return this.companyRepository.findById(companyId);
	}

	public Integer countCompanyUser(String username) {
		UserEntity user = userService.findUserByUsername(username);
		if (ObjectUtils.isNotEmpty(user)) {
			if (user.getRole().getRoleName().equals(CommonConstants.RoleName.SYS_ADMIN)){
				return Math.toIntExact(companyRepository.count());
			}
			return user.getCompanys().size();
		}
		return 0;
	}
}
