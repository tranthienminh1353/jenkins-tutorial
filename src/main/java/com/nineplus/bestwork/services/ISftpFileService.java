package com.nineplus.bestwork.services;

import java.util.List;

import org.springframework.data.util.Pair;
import org.springframework.web.multipart.MultipartFile;

import com.jcraft.jsch.ChannelSftp;
import com.jcraft.jsch.Session;
import com.nineplus.bestwork.exception.BestWorkBussinessException;

public interface ISftpFileService {
	boolean isExistFolder(ChannelSftp channel, String path);

	String createFolder(ChannelSftp channel, String path);

	void createFolderCommonRoot(List<String> folderStrings);

	byte[] getFile(String pathFileDownload, Pair<Session, ChannelSftp> sftpConnection) throws BestWorkBussinessException;

	String uploadInvoice(MultipartFile file, long awbId, long invoiceId, Pair<Session, ChannelSftp> sftpConnection) throws BestWorkBussinessException;

	String uploadPackage(MultipartFile file, long awbId, long packageId, Pair<Session, ChannelSftp> sftpConnection) throws BestWorkBussinessException;

	String uploadEvidenceBefore(MultipartFile file, long awbId, long evidenceBeforeId, Pair<Session, ChannelSftp> sftpConnection) throws BestWorkBussinessException;

	String uploadEvidenceAfter(MultipartFile file, long awbId, long evidenceAfterId, Pair<Session, ChannelSftp> sftpConnection) throws BestWorkBussinessException;

	boolean isValidFile(List<MultipartFile> file);

	boolean isImageFile(List<MultipartFile> file);

	String uploadConstructionDrawing(Pair<Session, ChannelSftp> sftpConnection, MultipartFile file, long constructionId);

	List<String> downloadFileTemp(long awbId, List<String> listPathFileDownload);

	String uploadProgressImage(Pair<Session, ChannelSftp> sftpConnection, MultipartFile file, long progressId, int type);
	
	boolean removeFile(String pathFileServer, Pair<Session, ChannelSftp> sftpConnection);
	
	Pair<Session, ChannelSftp> getConnection() throws BestWorkBussinessException;
	
	void disconnect(Session session, ChannelSftp channel);

}
