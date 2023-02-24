package com.nineplus.bestwork.utils;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.data.domain.Page;
import org.springframework.stereotype.Component;

import com.nineplus.bestwork.dto.PageResDto;
import com.nineplus.bestwork.dto.RPageDto;

@Component
public class PageUtils {
	@Autowired
	BestWorkBeanUtils bestWorkBeanUtils;

	/**
	 * convert page to page response DTO
	 * 
	 * @param page
	 * @param dtoClass
	 * 
	 * @return PageResponseDTO<DTO>
	 */
	public <D, E> PageResDto<D> convertPageEntityToDTO(Page<E> page, Class<D> dtoClass) {
		PageResDto<D> pageResDTO = new PageResDto<>();
		RPageDto rPageDTO = new RPageDto();
		rPageDTO.setTotalPages(page.getTotalPages());
		rPageDTO.setTotalElements(page.getTotalElements());
		rPageDTO.setSize(page.getSize());
		rPageDTO.setNumber(page.getNumber());

		pageResDTO.setMetaData(rPageDTO);
		pageResDTO.setContent(bestWorkBeanUtils.copyListToList(page.getContent(), dtoClass));

		return pageResDTO;
	}

}
