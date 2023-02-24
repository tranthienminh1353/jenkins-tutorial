package com.nineplus.bestwork.dto;

import com.fasterxml.jackson.annotation.JsonProperty;
import com.nineplus.bestwork.entity.ProjectTypeEntity;

import lombok.Data;
import lombok.EqualsAndHashCode;
import lombok.NoArgsConstructor;

/**
 * 
 * @author DiepTT
 *
 */

@Data
@NoArgsConstructor
@EqualsAndHashCode(callSuper = false)
public class ProjectResDto extends BaseDto {

	/**
	 * 
	 */
	private static final long serialVersionUID = 6657618958290343215L;

	@JsonProperty("id")
	private String id;

	@JsonProperty("projectName")
	private String projectName;

	@JsonProperty("description")
	private String description;

	@JsonProperty("projectType")
	private ProjectTypeEntity projectType;

	@JsonProperty("notificationFlag")
	private Boolean notificationFlag;

	@JsonProperty("isPaid")
	private Boolean isPaid;

	@JsonProperty("status")
	private String status;

	@JsonProperty("startDate")
	private String startDate;
	
	@JsonProperty("createBy")
	private String createBy;

}
