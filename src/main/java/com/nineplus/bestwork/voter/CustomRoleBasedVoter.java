package com.nineplus.bestwork.voter;

import com.nineplus.bestwork.entity.SysActionEntity;
import com.nineplus.bestwork.entity.SysPermissionEntity;
import com.nineplus.bestwork.entity.UserEntity;
import com.nineplus.bestwork.exception.BestWorkBussinessException;
import com.nineplus.bestwork.model.enumtype.Status;
import com.nineplus.bestwork.services.PermissionService;
import com.nineplus.bestwork.services.SysActionService;
import com.nineplus.bestwork.services.UserService;
import com.nineplus.bestwork.utils.CommonConstants;
import org.apache.commons.lang3.ObjectUtils;
import org.springframework.security.access.AccessDecisionVoter;
import org.springframework.security.access.ConfigAttribute;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.GrantedAuthority;
import org.springframework.security.web.FilterInvocation;
import org.springframework.web.context.WebApplicationContext;
import org.springframework.web.context.support.WebApplicationContextUtils;
import org.springframework.web.util.UriTemplate;

import javax.servlet.ServletContext;
import java.net.MalformedURLException;
import java.net.URL;
import java.util.ArrayList;
import java.util.Collection;
import java.util.List;

public class CustomRoleBasedVoter implements AccessDecisionVoter<FilterInvocation> {
	private SysActionService sysActionService;

	private PermissionService permissionService;

	private UserService userService;
	public static String[] PUBLIC_URL;

	public CustomRoleBasedVoter(String[] PUBLIC_URL_LIST) {
		PUBLIC_URL = PUBLIC_URL_LIST;
	}

	@Override
	public boolean supports(ConfigAttribute attribute) {
		// TODO Auto-generated method stub
		return true;
	}

	@Override
	public boolean supports(Class clazz) {
		// TODO Auto-generated method stub
		return true;
	}

	@Override
	public int vote(Authentication authentication, FilterInvocation fi, Collection<ConfigAttribute> attributes) {
		String url = fi.getRequestUrl().split("\\?")[0];
		if (url.contains(CommonConstants.ApiPath.BASE_PATH)) {
			url = url.split(CommonConstants.ApiPath.BASE_PATH)[1];
		}
		if (isWhiteList(url)) {
			return ACCESS_GRANTED;
		}
		String methodType = fi.getRequest().getMethod();

		if (sysActionService == null || permissionService == null || userService == null) {
			ServletContext servletContext = fi.getRequest().getServletContext();
			WebApplicationContext webApplicationContext = WebApplicationContextUtils
					.getWebApplicationContext(servletContext);
			assert webApplicationContext != null;
			sysActionService = webApplicationContext.getBean(SysActionService.class);
			permissionService = webApplicationContext.getBean(PermissionService.class);
			userService = webApplicationContext.getBean(UserService.class);
		}
		fi.getHttpRequest().getMethod();
		List<String> roleNames = authentication.getAuthorities().stream().map(GrantedAuthority::getAuthority).toList();
		UserEntity adminUser = userService.getAdminUserVoter((String) authentication.getPrincipal());
		List<SysActionEntity> actionList = sysActionService.getSysActionBySysRole(roleNames, methodType, adminUser.getId());
		URL url1;
		SysActionEntity actionCheck = null;
		try {
			url1 = new URL(fi.getFullRequestUrl());
		} catch (MalformedURLException e) {
			throw new RuntimeException(e);
		}
		String basePath = url1.getProtocol() +"://"+url1.getHost()+":"+url1.getPort()
				+ url1.getPath().split(fi.getRequestUrl())[0] + CommonConstants.ApiPath.BASE_PATH;
		UriTemplate uriTemplate;
		List<Integer> lstStt = new ArrayList<>();
		lstStt.add(Status.ACTIVE.getValue());
		if (!actionList.isEmpty()) {
			for (SysActionEntity action : actionList) {
				URL url2;
				try {
					url2 = new URL(basePath + action.getUrl());
				} catch (MalformedURLException e) {
					throw new RuntimeException(e);
				}
				if (url1.sameFile(url2))
					{
						actionCheck = action;
						break;
					};
			}
			if (ObjectUtils.isEmpty(actionCheck)) {
				for (SysActionEntity action : actionList) {
					uriTemplate = new UriTemplate(action.getUrl());
					if (!uriTemplate.match(url).isEmpty() || url.equals(action.getUrl())) {
						actionCheck = action;
						break;
					}
				}
			}
		}
		if (actionCheck != null) {
			try {
				List<SysPermissionEntity> permissionEntities = permissionService.getPermissionsByRoleVoter(roleNames, lstStt,
						actionCheck.getId(), (String) authentication.getPrincipal());
				if (!permissionEntities.isEmpty()) {
					SysPermissionEntity sysPermission = permissionEntities.get(0);
					switch (actionCheck.getActionType()) {
					case ADD -> {
						if (sysPermission.isCanAdd()) {
							return ACCESS_GRANTED;
						}
						return ACCESS_DENIED;
					}
					case VIEW -> {
						if (sysPermission.isCanAccess()) {
							return ACCESS_GRANTED;
						}
						return ACCESS_DENIED;
					}
					case EDIT -> {
						if (sysPermission.isCanEdit()) {
							return ACCESS_GRANTED;
						}
						return ACCESS_DENIED;
					}
					case DELETE -> {
						if (sysPermission.isCanDelete()) {
							return ACCESS_GRANTED;
						}
						return ACCESS_DENIED;
					}
					default -> {
						return ACCESS_DENIED;
					}
					}
				}
			} catch (BestWorkBussinessException e) {
				throw new RuntimeException(e);
			}
		}
		return ACCESS_DENIED;
	}

	boolean isWhiteList(String url) {
		UriTemplate uriTemplate;
		for (String publicUrl : PUBLIC_URL) {
			uriTemplate = new UriTemplate(publicUrl);
			if (!uriTemplate.match(url).isEmpty() || publicUrl.equals(url)) {
				return true;
			}
		}
		return false;
	}
}
