package com.nineplus.bestwork.services.impl;

import java.io.BufferedInputStream;
import java.io.BufferedOutputStream;
import java.io.File;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.OutputStream;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.time.LocalDate;
import java.time.format.DateTimeFormatter;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;

import org.apache.commons.io.FilenameUtils;
import org.apache.commons.io.IOUtils;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Configuration;
import org.springframework.context.annotation.PropertySource;
import org.springframework.data.util.Pair;
import org.springframework.stereotype.Service;
import org.springframework.web.multipart.MultipartFile;

import com.jcraft.jsch.ChannelSftp;
import com.jcraft.jsch.JSch;
import com.jcraft.jsch.JSchException;
import com.jcraft.jsch.Session;
import com.jcraft.jsch.SftpException;
import com.nineplus.bestwork.exception.BestWorkBussinessException;
import com.nineplus.bestwork.exception.FileHandleException;
import com.nineplus.bestwork.services.ISftpFileService;
import com.nineplus.bestwork.utils.CommonConstants;
import com.nineplus.bestwork.utils.DateUtils;
import com.nineplus.bestwork.utils.Enums.FolderType;

import lombok.extern.slf4j.Slf4j;

@Slf4j
@Service
@Configuration
@PropertySource("classpath:application.properties")
public class SftpFileServiceImpl implements ISftpFileService {

	/**
	 * The Constant SEPARATOR.
	 */
	public static final String SEPARATOR = "/";

	public static final String HYPHEN = "-";

	public static final String INVOICE_NAME_PREFIX = "(inv)";

	public static final String PACKAGE_NAME_PREFIX = "(pac)";

	public static final String IMAGE_BEFORE_NAME_PREFIX = "(evi)";

	/**
	 * The Constant ROOT_PATH.
	 */
	@Value("${fileServer.root}")
	public String ROOT_PATH;

	/**
	 * The Constant Temporary folder.
	 */
	@Value("${fileServer.temp}")
	public String TEMP_FOLDER;

	/**
	 * The Constant HOST.
	 */
	@Value("${fileServer.host}")
	private String SFTP_HOST;

	/**
	 * The Constant PORT.
	 */
	@Value("${fileServer.port}")
	private int SFTP_PORT;

	/**
	 * The Constant USER.
	 */
	@Value("${fileServer.user}")
	private String SFTP_USER;

	/**
	 * The Constant PASSWORD.
	 */
	@Value("${fileServer.password}")
	private String SFTP_PASSWORD;

	@Value("${fileServer.maxSize}")
	private float MAX_SIZE_FILE;

	private static final int INVOICE_NUMBER = 1;

	private static final int PACKAGE_NUMBER = 2;

	private static final int EVIDENCE_BEFORE_NUMBER = 3;

	private static final int EVIDENCE_AFTER_NUMBER = 4;

	public static final String INVOICE_PATH = "invoices";

	public static final String PACKAGE_PATH = "packages";

	public static final String EVIDENCE_BEFORE_PATH = "evidenceBefore";

	public static final String EVIDENCE_AFTER_PATH = "evidenceAfter";

	public static final String CONSTRUCTION_PATH = "constructions";

	public static final String PROGRESS_PATH = "progress";

	public static final String PROGRESS_PATH_BEFORE = "fileBefore";

	public static final String PROGRESS_PATH_AFTER = "fileAfter";

	@Override
	public boolean isExistFolder(ChannelSftp channel, String path) {
		try {
			channel.lstat(path);
			return true;
		} catch (SftpException ex) {
			return false;
		}
	}

	@Override
	public String createFolder(ChannelSftp channel, String path) {
		try {
			channel.mkdir(path);
			return path;
		} catch (SftpException ex) {
			throw new FileHandleException(ex.getMessage(), ex);
		}
	}

	@Override
	public void createFolderCommonRoot(List<String> folderStrings) {
		Session session = null;
		ChannelSftp channel = null;

		try {
			Pair<Session, ChannelSftp> sftpConnection = this.getConnection();
			session = sftpConnection.getFirst();
			channel = sftpConnection.getSecond();

			for (String folderStr : folderStrings) {
				if (!isExistFolder(channel, folderStr)) {
					createFolder(channel, folderStr);
				}
			}

			disconnect(session, channel);
		} catch (FileHandleException e) {
			disconnect(session, channel);
			throw new FileHandleException(e.getMessage(), e);
		}

	}

	@Override
	public byte[] getFile(String pathFileDownload, Pair<Session, ChannelSftp> sftpConnection)
			throws BestWorkBussinessException {
		byte[] resBytes = null;
		ChannelSftp channel = null;
		Session session = null;
		try {
			session = sftpConnection.getFirst();
			channel = sftpConnection.getSecond();
			if (isExistFolder(channel, pathFileDownload)) {
				resBytes = IOUtils.toByteArray(channel.get(pathFileDownload));
			}
		} catch (SftpException | IOException e) {
			// disconnect to sftp server
			disconnect(session, channel);
			throw new BestWorkBussinessException(e.getMessage(), null);
		}

		return resBytes;
	}

	@Override
	public String uploadInvoice(MultipartFile file, long awbId, long Id, Pair<Session, ChannelSftp> sftpConnection)
			throws BestWorkBussinessException {
		return upload(file, FolderType.INVOICE, awbId, Id, sftpConnection);
	}

	@Override
	public String uploadPackage(MultipartFile file, long airWayBill, long Id,
			Pair<Session, ChannelSftp> sftpConnection) {
		return upload(file, FolderType.PACKAGE, airWayBill, Id, sftpConnection);
	}

	@Override
	public String uploadEvidenceBefore(MultipartFile file, long airWayBill, long Id,
			Pair<Session, ChannelSftp> sftpConnection) {
		return upload(file, FolderType.EVIDENCE_BEFORE, airWayBill, Id, sftpConnection);
	}

	@Override
	public String uploadEvidenceAfter(MultipartFile file, long airWayBill, long Id,
			Pair<Session, ChannelSftp> sftpConnection) {
		return upload(file, FolderType.EVIDENCE_AFTER, airWayBill, Id, sftpConnection);
	}

	@Override
	public String uploadConstructionDrawing(Pair<Session, ChannelSftp> sftpConnection, MultipartFile file,
			long constructionId) {
		return uploadImage(sftpConnection, file, FolderType.CONSTRUCTION, constructionId, 3);
	}

	@Override
	public String uploadProgressImage(Pair<Session, ChannelSftp> sftpConnection, MultipartFile file, long progressId,
			int subType) {
		if (subType == 1) {
			return uploadImage(sftpConnection, file, FolderType.PROGRESS, progressId, subType);
		} else {
			return uploadImage(sftpConnection, file, FolderType.PROGRESS, progressId, subType);
		}
	}

	/**
	 * Gets the connection.
	 *
	 * @return the connection
	 */
	public Pair<Session, ChannelSftp> getConnection() {
		try {
			JSch jsch = new JSch();
			Session session = jsch.getSession(SFTP_USER, SFTP_HOST, SFTP_PORT);
			session.setConfig("StrictHostKeyChecking", "no");
			session.setPassword(SFTP_PASSWORD);
			log.info("Connecting to session server SFTP");
			session.connect();
			ChannelSftp channel = (ChannelSftp) session.openChannel("sftp");
			channel.connect();
			log.info("Connecting to channel server SFTP");
			return Pair.of(session, channel);
		} catch (JSchException e) {
			throw new FileHandleException(e.getMessage(), e);
		}
	}

	private String upload(MultipartFile mfile, FolderType folderType, long awbId, Long Id,
			Pair<Session, ChannelSftp> sftpConnection) {
		Session session = null;
		ChannelSftp channel = null;
		String pathTemp = null;
		String finalPath = null;

		// Create folder in sftp server.
		try {
			session = sftpConnection.getFirst();
			channel = sftpConnection.getSecond();

			String absolutePathInSftpServer = getPathSeverUpload(folderType);
			if (!isExistFolder(channel, absolutePathInSftpServer)) {
				pathTemp = this.createFolder(channel, absolutePathInSftpServer);
			} else {
				absolutePathInSftpServer = absolutePathInSftpServer + SEPARATOR + buildSubFolderName(folderType);
				if (!isExistFolder(channel, absolutePathInSftpServer)) {
					pathTemp = this.createFolder(channel, absolutePathInSftpServer);
				} else {
					pathTemp = absolutePathInSftpServer;
				}
			}
			pathTemp = pathTemp + SEPARATOR + awbId;
			if (!isExistFolder(channel, pathTemp)) {
				pathTemp = this.createFolder(channel, pathTemp);
			}

			pathTemp = pathTemp + SEPARATOR + Id;
			if (!isExistFolder(channel, pathTemp)) {
				pathTemp = this.createFolder(channel, pathTemp);
			}

			String fileName = FilenameUtils.getName(mfile.getOriginalFilename());

			// save file.
			channel.cd(pathTemp);
			channel.put(mfile.getInputStream(), fileName);
			finalPath = pathTemp + SEPARATOR + fileName;
		} catch (IOException | SftpException e) {
			disconnect(session, channel);
			throw new FileHandleException(e.getMessage(), e);
		}
		return finalPath;
	}

	private String uploadImage(Pair<Session, ChannelSftp> sftpConnection, MultipartFile mfile, FolderType folderType,
			Long objId, int subType) {
		Session session = null;
		ChannelSftp channel = null;
		String pathTemp = null;
		String finalPath = null;

		// Create folder in sftp server.
		try {
			session = sftpConnection.getFirst();
			channel = sftpConnection.getSecond();

			String absolutePathInSftpServer = getPathSeverUpload(folderType);
			if (!isExistFolder(channel, absolutePathInSftpServer)) {
				pathTemp = this.createFolder(channel, absolutePathInSftpServer);
			} else {
				absolutePathInSftpServer = absolutePathInSftpServer + SEPARATOR + buildSubFolderName(folderType);
				if (!isExistFolder(channel, absolutePathInSftpServer)) {
					pathTemp = this.createFolder(channel, absolutePathInSftpServer);
				} else {
					pathTemp = absolutePathInSftpServer;
				}
			}

			pathTemp = pathTemp + SEPARATOR + objId;
			if (!isExistFolder(channel, pathTemp)) {
				pathTemp = this.createFolder(channel, pathTemp);
			}
			if (subType == 1) {
				pathTemp = pathTemp + SEPARATOR + PROGRESS_PATH_BEFORE;
			} else if (subType == 2) {
				pathTemp = pathTemp + SEPARATOR + PROGRESS_PATH_AFTER;
			}
			if (!isExistFolder(channel, pathTemp)) {
				pathTemp = this.createFolder(channel, pathTemp);
			}
			String fileName = FilenameUtils.getName(mfile.getOriginalFilename());

			// save file.
			channel.cd(pathTemp);
			channel.put(mfile.getInputStream(), fileName);
			finalPath = pathTemp + SEPARATOR + fileName;
		} catch (IOException | SftpException e) {
			disconnect(session, channel);
			throw new FileHandleException(e.getMessage(), e);
		}
		return finalPath;
	}

	/**
	 * create path file upload.
	 *
	 * @param folderType the folder type
	 * @return the string
	 */
	public String buildSubFolderName(FolderType folderType) {
		LocalDate date = LocalDate.now();
		DateTimeFormatter formatter = DateTimeFormatter.ofPattern(DateUtils.YYYYMMDD);
		return date.format(formatter);
	}

	public String createFileName(FolderType folderType, String airWayBill) {
		String fileName = new StringBuilder().append(folderType).append(airWayBill)
				.append(buildSubFolderName(folderType)).toString();
		return fileName;
	}

	public FolderType getFolderType(int typeFile) {
		FolderType folderType;

		switch (typeFile) {
		case INVOICE_NUMBER:
			folderType = FolderType.INVOICE;
			break;
		case PACKAGE_NUMBER:
			folderType = FolderType.PACKAGE;
			break;
		case EVIDENCE_BEFORE_NUMBER:
			folderType = FolderType.EVIDENCE_BEFORE;
			break;
		case EVIDENCE_AFTER_NUMBER:
			folderType = FolderType.EVIDENCE_AFTER;
			break;
		default:
			folderType = FolderType.DEFAULT;
			break;
		}

		return folderType;
	}

	private String getParentPath(FolderType folderType) {
		String res = "";

		switch (folderType) {
		case INVOICE:
			res = INVOICE_PATH;
			break;
		case PACKAGE:
			res = PACKAGE_PATH;
			break;
		case EVIDENCE_BEFORE:
			res = EVIDENCE_BEFORE_PATH;
			break;
		case EVIDENCE_AFTER:
			res = EVIDENCE_AFTER_PATH;
			break;
		case CONSTRUCTION:
			res = CONSTRUCTION_PATH;
			break;
		case PROGRESS:
			res = PROGRESS_PATH;
			break;
		default:
			break;
		}

		return res;
	}

	private String getPathSeverUpload(FolderType folderType) {
		return ROOT_PATH + SEPARATOR + getParentPath(folderType);
	}

	@Override
	public boolean isValidFile(List<MultipartFile> mFiles) {
		for (MultipartFile file : mFiles) {
			float fileSizeInMegabytes = file.getSize() / 1_000_000.0f;
			// file must be < 5MB
			if (fileSizeInMegabytes >= MAX_SIZE_FILE) {
				return false;
			}
		}
		return true;
	}

	@Override
	public List<String> downloadFileTemp(long awbId, List<String> listPathFileDownload) {
		ChannelSftp channel = null;
		Session session = null;
		List<String> listPathFile = new ArrayList<>();
		try {
			String temporaryFolder = ROOT_PATH + TEMP_FOLDER;
			Pair<Session, ChannelSftp> sftpConnection = this.getConnection();
			session = sftpConnection.getFirst();
			channel = sftpConnection.getSecond();
			for (String pathFile : listPathFileDownload) {
				String fileName = FilenameUtils.getName(pathFile);
				if (pathFile.contains("/invoices")) {
					fileName = INVOICE_NAME_PREFIX + fileName;
				} else if (pathFile.contains("/packages")) {
					fileName = PACKAGE_NAME_PREFIX + fileName;
				} else if (pathFile.contains("/evidenceBefore")) {
					fileName = IMAGE_BEFORE_NAME_PREFIX + fileName;
				}
				byte[] buffer = new byte[1024];
				if (isExistFolder(channel, pathFile)) {
					BufferedInputStream bis = new BufferedInputStream(channel.get(pathFile));
					Path path = Files.createDirectories(Paths.get(temporaryFolder + SEPARATOR + awbId));
					String pathFileSever = path + SEPARATOR + fileName;
					File newFile = new File(pathFileSever);
					// Check if already exist this file
					int i = 1;
					while (newFile.exists() && !newFile.isDirectory()) {
						String fileRename = FilenameUtils.getBaseName(pathFileSever) + "(" + i + ")."
								+ FilenameUtils.getExtension(pathFileSever);
						pathFileSever = path + SEPARATOR + fileRename;
						newFile = new File(pathFileSever);
						i++;
					}
					listPathFile.add(pathFileSever);
					OutputStream os = new FileOutputStream(newFile);
					BufferedOutputStream bos = new BufferedOutputStream(os);
					int readCount;
					while ((readCount = bis.read(buffer)) > 0) {
						bos.write(buffer, 0, readCount);
					}
					bis.close();
					bos.close();

				}
			}
		} catch (Exception ex) {
			disconnect(session, channel);
			log.error(ex.getMessage(), ex);
			throw new FileHandleException(ex.getMessage(), ex);
		} finally {
			// disconnect to sftp server.
			disconnect(session, channel);
		}
		return listPathFile;
	}

	@Override
	public boolean isImageFile(List<MultipartFile> mFiles) {
		for (MultipartFile file : mFiles) {
			// file must be < 5MB
			String fileExtensions = FilenameUtils.getExtension(file.getOriginalFilename());
			if (Arrays.asList(CommonConstants.Image.IMAGE_EXTENSION).contains(fileExtensions.trim())) {
				return true;
			}
		}
		return false;
	}

	@Override
	public boolean removeFile(String pathFileServer, Pair<Session, ChannelSftp> sftpConnection) {
		ChannelSftp channel = null;
		try {
			channel = sftpConnection.getSecond();
			// Check exist path on sever
			if (!isExistFolder(channel, pathFileServer)) {
				return false;
			}
			channel.rm(pathFileServer);
		} catch (SftpException ex) {
			throw new FileHandleException(ex.getMessage(), ex);
		}
		return true;
	}

	/**
	 * Disconnect.
	 *
	 * @param session the session
	 * @param channel the channel
	 */
	@Override
	public void disconnect(Session session, ChannelSftp channel) {
		if (channel != null) {
			log.info("Disconect to channel server SFTP");
			channel.exit();
		}

		if (session != null) {
			log.info("Disconect to session server SFTP");
			session.disconnect();
		}

	}

}
