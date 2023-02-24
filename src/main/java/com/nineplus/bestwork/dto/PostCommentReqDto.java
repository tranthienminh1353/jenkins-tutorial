package com.nineplus.bestwork.dto;

import com.fasterxml.jackson.annotation.JsonProperty;

import lombok.EqualsAndHashCode;
import lombok.Getter;
import lombok.Setter;

@Getter
@Setter
@EqualsAndHashCode(callSuper = false)
public class PostCommentReqDto extends BaseDto {
	/**
	 * 
	 */
	private static final long serialVersionUID = 5245229871548232443L;

	@JsonProperty("awbId")
	private long awbId;

	@JsonProperty("comment")
	private String comment;
}
