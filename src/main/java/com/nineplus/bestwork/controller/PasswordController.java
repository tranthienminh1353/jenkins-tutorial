package com.nineplus.bestwork.controller;

import java.io.IOException;
import java.time.LocalDateTime;

import javax.validation.Valid;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.http.ResponseEntity;
import org.springframework.security.crypto.bcrypt.BCryptPasswordEncoder;
import org.springframework.validation.BindingResult;
import org.springframework.web.bind.annotation.CrossOrigin;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import com.nineplus.bestwork.dto.ChangePasswordReqDto;
import com.nineplus.bestwork.dto.ForgotPasswordReqDto;
import com.nineplus.bestwork.dto.ForgotPasswordResDto;
import com.nineplus.bestwork.dto.ResetPasswordReqDto;
import com.nineplus.bestwork.entity.UserEntity;
import com.nineplus.bestwork.exception.BestWorkBussinessException;
import com.nineplus.bestwork.exception.SysUserNotFoundException;
import com.nineplus.bestwork.model.UserAuthDetected;
import com.nineplus.bestwork.services.MailSenderService;
import com.nineplus.bestwork.services.SysUserService;
import com.nineplus.bestwork.services.UserService;
import com.nineplus.bestwork.utils.CommonConstants;
import com.nineplus.bestwork.utils.UserAuthUtils;

import net.bytebuddy.utility.RandomString;

/**
 * 
 * @author DiepTT
 *
 */
@RestController
@RequestMapping("/api/v1")
@CrossOrigin
public class PasswordController extends BaseController {
	@Value("${url.origin}")
	private String url;

	@Autowired
	private SysUserService sysUserService;

	@Autowired
	private MailSenderService mailService;

	@Autowired
	UserAuthUtils userAuthUtils;

	@Autowired
	UserService userService;

	@Autowired
	BCryptPasswordEncoder bCryptPasswordEncoder;

	@PostMapping("/auth/forgot-password")
	public ResponseEntity<? extends Object> processForgotPassword(
			@Valid @RequestBody ForgotPasswordReqDto forgotPasswordReqDto, BindingResult bindingResult)
			throws Exception {

		if (bindingResult.hasErrors()) {
			return failedWithError(CommonConstants.MessageCode.SU0003, bindingResult.getFieldErrors().toArray(), null);
		}

		String emailReq = forgotPasswordReqDto.getEmail();
		UserEntity sysUserReq = this.sysUserService.getUserByEmail(emailReq);
		if (sysUserReq == null) {
			return failedWithError(CommonConstants.MessageCode.SU0002, forgotPasswordReqDto, null);
		}

		String token = RandomString.make(45);

		try {
			sysUserService.updateResetPasswordToken(token, emailReq);
			String resetPasswordLink = url + "/auth/reset-password/" + token;
			String username = sysUserReq.getUserName();
			mailService.sendMailResetPassword(emailReq, username, resetPasswordLink);

		} catch (SysUserNotFoundException e) {
			e.printStackTrace();
		}

		ForgotPasswordResDto forgotPasswordResDto = new ForgotPasswordResDto();
		forgotPasswordResDto.setUsername(sysUserReq.getUserName());
		forgotPasswordResDto.setEmail(sysUserReq.getEmail());
		forgotPasswordResDto.setFirstname(sysUserReq.getFirstName());
		forgotPasswordResDto.setLastname(sysUserReq.getLastName());
		return success(CommonConstants.MessageCode.SU0001, forgotPasswordResDto, null);
	}

	@PostMapping("/auth/reset-password/{token}")
	public ResponseEntity<?> changePassword(@PathVariable String token,
			@Valid @RequestBody ResetPasswordReqDto resetPasswordReqDto, BindingResult bindingResult)
			throws IOException {

		if (bindingResult.hasErrors()) {
			return failedWithError(CommonConstants.MessageCode.SU0003, bindingResult.getFieldErrors().toArray(), null);
		}

		UserEntity sysUser = this.sysUserService.get(token);
		if (sysUser == null) {
			return failedWithError(CommonConstants.MessageCode.SU0002, sysUser, null);
		}

		if (resetPasswordReqDto.getPassword().equals(resetPasswordReqDto.getConfirmPassword())) {

			String newPassword = resetPasswordReqDto.getPassword();
			sysUser.setUpdateDate(LocalDateTime.now());
			sysUser.setUpdateBy(sysUser.getUserName());

			this.sysUserService.updatePassword(sysUser, newPassword);

			return success(CommonConstants.MessageCode.SU0004, null, null);
		} else {
			return failedWithError(CommonConstants.MessageCode.SU0005, resetPasswordReqDto, null);
		}
	}

	@PostMapping("/change-password")
	public ResponseEntity<?> changePassword(@Valid @RequestBody ChangePasswordReqDto changePasswordReqDto,
			BindingResult bindingResult) throws BestWorkBussinessException {

		UserAuthDetected userAuthRoleReq = userAuthUtils.getUserInfoFromReq(false);
		String username = userAuthRoleReq.getUsername();

		UserEntity currentUser = userService.getUserByUsername(username);

		if (bindingResult.hasErrors()) {
			return failedWithError(CommonConstants.MessageCode.SU0003, bindingResult.getFieldErrors().toArray(), null);
		}
		if (currentUser == null) {
			return failed(CommonConstants.MessageCode.ECU0005, null);
		}
		if (!bCryptPasswordEncoder.matches(changePasswordReqDto.getCurrentPassword(), currentUser.getPassword())) {
			return failedWithError(CommonConstants.MessageCode.ECU0006, changePasswordReqDto.getCurrentPassword(),
					null);
		}
		String newPassword = changePasswordReqDto.getNewPassword();
		String confirmPassword = changePasswordReqDto.getConfirmPassword();

		if (newPassword.equals(confirmPassword)) {
			currentUser.setUpdateDate(LocalDateTime.now());
			currentUser.setUpdateBy(username);
			this.sysUserService.updatePassword(currentUser, newPassword);
			return success(CommonConstants.MessageCode.SU0004, null, null);
		} else {
			return failedWithError(CommonConstants.MessageCode.SU0005, null, null);
		}
	}

}
