package com.nineplus.bestwork.repository;


public interface UserProjectRepository {
	String getProjectId();

	String getProjectName();

	int getCanView();

	int getCanEdit();

}
