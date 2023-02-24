package com.nineplus.bestwork.dto;

import java.util.List;

import com.fasterxml.jackson.annotation.JsonProperty;

import lombok.Data;
import lombok.EqualsAndHashCode;

@Data
@EqualsAndHashCode(callSuper = false)
public class RegPermissionDto extends BaseDto {

	/**
	 * 
	 */
	private static final long serialVersionUID = -2842665162431283116L;

	@JsonProperty("id")
	private Long roleId;

	@JsonProperty("permissions")
	List<PermissionDto> monitorInfo;
}
