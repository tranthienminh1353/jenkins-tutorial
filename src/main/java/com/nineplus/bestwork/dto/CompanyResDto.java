package com.nineplus.bestwork.dto;

import com.fasterxml.jackson.annotation.JsonProperty;

import lombok.Data;
import lombok.EqualsAndHashCode;

@Data
@EqualsAndHashCode(callSuper = false)
public class CompanyResDto extends BaseDto {
	/**
	 * serialVersionUID
	 */
	private static final long serialVersionUID = 7969015153524943561L;

	@JsonProperty("id")
	private Long id;

	@JsonProperty("companyName")
	private String companyName;

	@JsonProperty("cpEmail")
	private String email;

	@JsonProperty("cpTelNo")
	private String telNo;

	@JsonProperty("taxNo")
	private String taxNo;

	@JsonProperty("national")
	private String nation;

	@JsonProperty("city")
	private String city;

	@JsonProperty("district")
	private String district;

	@JsonProperty("ward")
	private String ward;

	@JsonProperty("street")
	private String street;

	@JsonProperty("startDate")
	private String startDate;

	@JsonProperty("expiredDate")
	private String expiredDate;

	@JsonProperty("status")
	private int isExpired;

}
