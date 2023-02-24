package com.nineplus.bestwork.dto;

import java.time.LocalDateTime;

import com.fasterxml.jackson.annotation.JsonProperty;

import lombok.Data;
import lombok.EqualsAndHashCode;
import lombok.NoArgsConstructor;

@Data
@NoArgsConstructor
@EqualsAndHashCode(callSuper = false)
public class UserResponseDTO extends BaseDto {
	/**
	 * 
	 */
	private static final long serialVersionUID = -866422097458065228L;

	@JsonProperty("id")
	private long id;

	@JsonProperty("enabled")
	private Boolean enabled;

	@JsonProperty("role")
	private String role;

	@JsonProperty("user_nm")
	private String userNm;

	@JsonProperty("email")
	private String email;

	@JsonProperty("first_nm")
	private String firstNm;

	@JsonProperty("last_nm")
	private String lastNm;

	@JsonProperty("is_deleted")
	private boolean isDeleted;

	@JsonProperty("is_blocked")
	private boolean isBlocked;

	@JsonProperty("count_login_failed")
	private int countLoginFailed;

	@JsonProperty("created_dt")
	private LocalDateTime createDt;

	@JsonProperty("updated_dt")
	private LocalDateTime updatedDt;

}
