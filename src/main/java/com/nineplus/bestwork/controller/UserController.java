package com.nineplus.bestwork.controller;

import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;
import javax.validation.Valid;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.PropertySource;
import org.springframework.http.ResponseEntity;
import org.springframework.validation.BindingResult;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import com.nineplus.bestwork.dto.PageResDto;
import com.nineplus.bestwork.dto.PageSearchUserDto;
import com.nineplus.bestwork.dto.UserDetectResDto;
import com.nineplus.bestwork.dto.UserListIdDto;
import com.nineplus.bestwork.dto.UserReqDto;
import com.nineplus.bestwork.dto.UserResDto;
import com.nineplus.bestwork.entity.UserEntity;
import com.nineplus.bestwork.exception.BestWorkBussinessException;
import com.nineplus.bestwork.services.UserService;
import com.nineplus.bestwork.utils.CommonConstants;
import com.nineplus.bestwork.utils.TokenUtils;
import com.nineplus.bestwork.utils.UserAuthUtils;

@PropertySource("classpath:application.properties")
@RequestMapping(value = CommonConstants.ApiPath.BASE_PATH + "/users")
@RestController
public class UserController extends BaseController {

	@Autowired
	UserService userService;

	@Autowired
	TokenUtils tokenUtils;

	@Autowired
	UserAuthUtils userAuthUtils;

	@Value("${app.login.jwtPrefix}")
	private String PRE_STRING;

	@GetMapping("/isCheckLogin")
	public ResponseEntity<? extends Object> isCheckLogin(HttpServletRequest request, HttpServletResponse response) {
		return success(CommonConstants.MessageCode.S1X0010, null, null);
	}

	/**
	 * Get list company
	 *
	 * @return list company
	 */
	@PostMapping("/list")
	public ResponseEntity<?> getAllUsers(@Valid @RequestBody(required = false) PageSearchUserDto pageCondition,
			BindingResult bindingResult) {
		if (bindingResult.hasErrors() || null == pageCondition)
			return failed(CommonConstants.MessageCode.ECU0002, null);
		PageResDto<UserResDto> pageUser;
		try {
			pageUser = userService.getAllUsers(pageCondition);
		} catch (BestWorkBussinessException ex) {
			return failed(ex.getMsgCode(), ex.getParam());
		}
		return success(CommonConstants.MessageCode.sU0006, pageUser, null);
	}

	@PostMapping("/create")
	public ResponseEntity<?> registerUser(@RequestBody UserReqDto userReqDto) {
		UserEntity createdUser;
		try {
			createdUser = userService.createUser(userReqDto);
		} catch (BestWorkBussinessException ex) {
			return failed(ex.getMsgCode(), ex.getParam());
		}
		return success(CommonConstants.MessageCode.SCU0001, createdUser, null);
	}

	@GetMapping("/{userId}")
	public ResponseEntity<?> getUserById(@PathVariable long userId) throws BestWorkBussinessException {
		UserResDto userResDto = new UserResDto();
		try {
			userResDto = userService.getUserById(userId);
		} catch (BestWorkBussinessException e) {
			return failed(e.getMsgCode(), e.getParam());
		}
		return success(CommonConstants.MessageCode.SCU0002, userResDto, null);
	}

	@PutMapping("/update/{id}")
	public ResponseEntity<?> updateUser(@PathVariable long id, @RequestBody UserReqDto userReqDto)
			throws BestWorkBussinessException {

		try {
			userService.editUser(userReqDto, id);
		} catch (BestWorkBussinessException ex) {
			return failed(ex.getMsgCode(), ex.getParam());
		}
		return success(CommonConstants.MessageCode.SCU0003, null, null);
	}

	@PostMapping("/delete")
	public ResponseEntity<? extends Object> deleteUser(@RequestBody(required = false) UserListIdDto listId) {
		try {
			userService.deleteUser(listId);
		} catch (NullPointerException ex) {
			return failed(CommonConstants.MessageCode.SU0003, null);
		} catch (BestWorkBussinessException ex) {
			return failed(ex.getMsgCode(), ex.getParam());
		}
		return success(CommonConstants.MessageCode.SCU0004, null, null);
	}

	@GetMapping("/roles")
	public ResponseEntity<?> getRoles() throws BestWorkBussinessException {
		return ResponseEntity.ok(this.userService.getAllRoles());
	}

	@GetMapping("/companies")
	public ResponseEntity<?> getCompanyOfUser() {
		Object company;
		try {
			company = this.userService.getAllCompanyOfUser();
		} catch (BestWorkBussinessException ex) {
			return failed(ex.getMsgCode(), ex.getParam());
		}
		return ResponseEntity.ok(company);
	}

	@GetMapping("/detect-infor")
	public ResponseEntity<? extends Object> detectUserLogin(HttpServletRequest request, HttpServletResponse response) {
		String accessToken = tokenUtils.getTokenFromRequest(request, CommonConstants.Authentication.ACCESS_TOKEN);
		if (accessToken != null) {
			try {
				String username = tokenUtils.getUserNameFromToken(accessToken);
				UserDetectResDto userDetect = userService.detectUser(username);
				return userDetect != null ? success(CommonConstants.MessageCode.sUS0001, userDetect, null)
						: failed(CommonConstants.MessageCode.E1X0003, null);
			} catch (Exception ex) {
				return failed(CommonConstants.MessageCode.E1X0003, null);
			}
		}
		return success(CommonConstants.MessageCode.S1X0003, null, null);
	}
}
