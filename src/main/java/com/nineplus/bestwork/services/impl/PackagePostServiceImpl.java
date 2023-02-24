package com.nineplus.bestwork.services.impl;

import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;
import java.util.Optional;

import org.apache.commons.lang3.ObjectUtils;
import org.apache.commons.lang3.StringUtils;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.data.util.Pair;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.multipart.MultipartFile;

import com.jcraft.jsch.ChannelSftp;
import com.jcraft.jsch.Session;
import com.nineplus.bestwork.dto.CustomClearancePackageFileResDto;
import com.nineplus.bestwork.dto.FileStorageResDto;
import com.nineplus.bestwork.dto.PackagePostReqDto;
import com.nineplus.bestwork.dto.PackagePostResDto;
import com.nineplus.bestwork.dto.PostCommentReqDto;
import com.nineplus.bestwork.entity.FileStorageEntity;
import com.nineplus.bestwork.entity.PackagePost;
import com.nineplus.bestwork.exception.BestWorkBussinessException;
import com.nineplus.bestwork.model.UserAuthDetected;
import com.nineplus.bestwork.repository.PackageFileProjection;
import com.nineplus.bestwork.repository.PackagePostRepository;
import com.nineplus.bestwork.services.IPackagePostService;
import com.nineplus.bestwork.services.ISftpFileService;
import com.nineplus.bestwork.services.IStorageService;
import com.nineplus.bestwork.utils.CommonConstants;
import com.nineplus.bestwork.utils.Enums.FolderType;
import com.nineplus.bestwork.utils.UserAuthUtils;

@Service
@Transactional
public class PackagePostServiceImpl implements IPackagePostService {

	@Autowired
	UserAuthUtils userAuthUtils;

	@Autowired
	PackagePostRepository packagePostRepository;

	@Autowired
	ISftpFileService sftpFileService;

	@Autowired
	IStorageService iStorageService;

	@Override
	@Transactional
	public PackagePost savePackagePost(PackagePostReqDto packagePostReqDto, long awbId)
			throws BestWorkBussinessException {
		PackagePost packagePost = new PackagePost();
		UserAuthDetected userAuthRoleReq = userAuthUtils.getUserInfoFromReq(false);
		try {
			packagePost.setAirWayBill(awbId);
			packagePost.setDescription(packagePostReqDto.getDescription());
			packagePost.setComment(packagePostReqDto.getComment());
			packagePost.setCreateBy(userAuthRoleReq.getUsername());
			packagePost.setUpdateBy(userAuthRoleReq.getUsername());
			packagePost.setCreateDate(LocalDateTime.now());
			packagePost.setUpdateDate(LocalDateTime.now());
			return packagePostRepository.save(packagePost);
		} catch (Exception ex) {
			throw new BestWorkBussinessException(CommonConstants.MessageCode.eP0001, null);
		}
	}

	@Override
	public void updatePackagePost(List<MultipartFile> mFiles, PackagePostReqDto packagePostReqDto, long awbId)
			throws BestWorkBussinessException {
		PackagePost createPackagePost = null;
		if (!sftpFileService.isValidFile(mFiles)) {
			throw new BestWorkBussinessException(CommonConstants.MessageCode.eF0002, null);
		}
		try {
			// Save information for post invoice
			createPackagePost = this.savePackagePost(packagePostReqDto, awbId);
			long postPackageId = createPackagePost.getId();
			// Upload file of post invoice into sever
			Pair<Session, ChannelSftp> sftpConnection = sftpFileService.getConnection();
			Session session = sftpConnection.getFirst();
			ChannelSftp channel = sftpConnection.getSecond();
			for (MultipartFile mFile : mFiles) {
				String pathServer = sftpFileService.uploadPackage(mFile, awbId, postPackageId, sftpConnection);
				// Save path file of post invoice
				iStorageService.storeFile(postPackageId, FolderType.PACKAGE, pathServer);
			}
			sftpFileService.disconnect(session, channel);
		} catch (Exception ex) {
			throw new BestWorkBussinessException(CommonConstants.MessageCode.eA0002, null);
		}

	}

	@Override
	public PackagePostResDto getDetailPackage(Long packagePostId) throws BestWorkBussinessException {
		PackagePostResDto packagePostResDto = null;
		Optional<PackagePost> packagePost = this.getPackagePost(packagePostId);
		if (packagePost.isPresent()) {
			packagePostResDto = new PackagePostResDto();
			packagePostResDto.setId(packagePost.get().getId());
			packagePostResDto.setComment(packagePost.get().getComment());
			packagePostResDto.setDescription(packagePost.get().getDescription());
			packagePostResDto.setCreateBy(packagePost.get().getCreateBy());
			packagePostResDto.setUpdateBy(packagePost.get().getUpdateBy());
			packagePostResDto.setCreateDate(packagePost.get().getCreateDate());
			packagePostResDto.setUpdateDate(packagePost.get().getUpdateDate());
			List<FileStorageResDto> fileStorageResponseDtos = new ArrayList<>();
			for (FileStorageEntity file : packagePost.get().getFileStorages()) {
				FileStorageResDto fileStorageResponseDto = new FileStorageResDto();
				fileStorageResponseDto.setId(file.getId());
				fileStorageResponseDto.setName(file.getName());
				fileStorageResponseDto.setCreateDate(file.getCreateDate().toString());
				fileStorageResponseDto.setType(file.getType());
				fileStorageResponseDtos.add(fileStorageResponseDto);
			}
			packagePostResDto.setFileStorages(fileStorageResponseDtos);
		}
		return packagePostResDto;
	}

	@Override
	public Optional<PackagePost> getPackagePost(Long packagePostId) throws BestWorkBussinessException {
		return packagePostRepository.findById(packagePostId);
	}

	@Override
	public List<PackagePostResDto> getAllPackagePost(long awbId) throws BestWorkBussinessException {
		List<PackagePost> listPackagePost = packagePostRepository.findByAirWayBill(awbId);
		List<PackagePostResDto> listPackagePostResDto = null;
		PackagePostResDto res = null;
		if (ObjectUtils.isNotEmpty(listPackagePost)) {
			Pair<Session, ChannelSftp> sftpConnection = sftpFileService.getConnection();
			Session session = sftpConnection.getFirst();
			ChannelSftp channel = sftpConnection.getSecond();
			listPackagePostResDto = new ArrayList<>();
			for (PackagePost packagePost : listPackagePost) {
				res = new PackagePostResDto();
				res.setId(packagePost.getId());
				res.setComment(packagePost.getComment());
				res.setDescription(packagePost.getDescription());
				res.setCreateBy(packagePost.getCreateBy());
				res.setUpdateBy(packagePost.getUpdateBy());
				res.setCreateDate(packagePost.getCreateDate());
				res.setUpdateDate(packagePost.getUpdateDate());
				res.setPostType(CommonConstants.Character.TYPE_POST_PACKAGE);
				List<FileStorageResDto> fileStorageResponseDtos = new ArrayList<>();
				if (ObjectUtils.isNotEmpty(packagePost.getFileStorages())) {
					for (FileStorageEntity file : packagePost.getFileStorages()) {
						FileStorageResDto fileStorageResponseDto = new FileStorageResDto();
						fileStorageResponseDto.setId(file.getId());
						fileStorageResponseDto.setName(file.getName());
						fileStorageResponseDto.setCreateDate(file.getCreateDate().toString());
						fileStorageResponseDto.setType(file.getType());
						fileStorageResponseDto.setChoosen(file.isChoosen());
						// return content file if file is image
						if (Arrays.asList(CommonConstants.Image.IMAGE_EXTENSION).contains(file.getType())) {
							String pathServer = file.getPathFileServer();
							byte[] imageContent = sftpFileService.getFile(pathServer, sftpConnection);
							fileStorageResponseDto.setContent(imageContent);
						}
						fileStorageResponseDtos.add(fileStorageResponseDto);
					}
				}
				res.setFileStorages(fileStorageResponseDtos);
				listPackagePostResDto.add(res);
			}
			// Sort by newest create date
			if (ObjectUtils.isNotEmpty(listPackagePostResDto)) {
				listPackagePostResDto.sort((o1, o2) -> o2.getCreateDate().compareTo(o1.getCreateDate()));
			}
			sftpFileService.disconnect(session, channel);
		}
		return listPackagePostResDto;
	}

	@Override
	public byte[] getFile(Long packagePostId, Long fileId) throws BestWorkBussinessException {
		String pathFile = this.getPathFileToDownload(packagePostId, fileId);
		byte[] fileContent = null;
		if (StringUtils.isNotBlank(pathFile)) {
			Pair<Session, ChannelSftp> sftpConnection = sftpFileService.getConnection();
			Session session = sftpConnection.getFirst();
			ChannelSftp channel = sftpConnection.getSecond();
			fileContent = sftpFileService.getFile(pathFile, sftpConnection);
			sftpFileService.disconnect(session, channel);
		}
		return fileContent;
	}

	@Override
	public String getPathFileToDownload(long packagePostId, long fileId) {
		return packagePostRepository.getPathFileServer(packagePostId, fileId);
	}

	@Override
	public List<CustomClearancePackageFileResDto> getPackageClearance(long awbId) throws BestWorkBussinessException {
		List<CustomClearancePackageFileResDto> lst = new ArrayList<>();
		CustomClearancePackageFileResDto customClearancePackageFileResDto = null;
		List<PackageFileProjection> res = packagePostRepository.getClearancePackageInfo(awbId);
		if (ObjectUtils.isNotEmpty(res)) {
			Pair<Session, ChannelSftp> sftpConnection = sftpFileService.getConnection();
			Session session = sftpConnection.getFirst();
			ChannelSftp channel = sftpConnection.getSecond();
			for (PackageFileProjection projection : res) {
				customClearancePackageFileResDto = new CustomClearancePackageFileResDto();
				customClearancePackageFileResDto.setFileId(projection.getFileId());
				customClearancePackageFileResDto.setPostPackageId(projection.getPostPackageId());
				customClearancePackageFileResDto.setName(projection.getName());
				customClearancePackageFileResDto.setType(projection.getType());
				customClearancePackageFileResDto.setPostType(CommonConstants.Character.TYPE_POST_PACKAGE);
				// return content file if file is image
				if (Arrays.asList(CommonConstants.Image.IMAGE_EXTENSION).contains(projection.getType())) {
					String pathServer = projection.getPathFileServer();
					byte[] imageContent = sftpFileService.getFile(pathServer, sftpConnection);
					customClearancePackageFileResDto.setContent(imageContent);
				}
				lst.add(customClearancePackageFileResDto);
			}
			sftpFileService.disconnect(session, channel);
		}
		return lst;
	}

	@Override
	@Transactional
	public PackagePost pushComment(Long postPackageId, PostCommentReqDto postCommentRequestDto)
			throws BestWorkBussinessException {
		PackagePost currentPost = null;
		try {
			if (ObjectUtils.isNotEmpty(postPackageId) && ObjectUtils.isNotEmpty(postCommentRequestDto)) {
				// Check exist post invoice with air way bill in DB
				currentPost = this.packagePostRepository.findByIdAndAirWayBill(postPackageId,
						postCommentRequestDto.getAwbId());
				if (ObjectUtils.isEmpty(currentPost)) {
					throw new BestWorkBussinessException(CommonConstants.MessageCode.eP0003, null);
				}
				// Set comment
				currentPost.setComment(postCommentRequestDto.getComment());
				this.packagePostRepository.save(currentPost);
			}
		} catch (Exception e) {
			throw new BestWorkBussinessException(CommonConstants.MessageCode.eP0004, null);
		}
		return currentPost;
	}
}
