/*
 * Copyright 2018 datagear.tech
 *
 * Licensed under the LGPLv3 license:
 * http://www.gnu.org/licenses/lgpl-3.0.html
 */

package com.example.demo.jdbc.entity;

/**
 * 数据库模式实体。
 * 
 * @author datagear@163.com
 *
 */
public class Schema {

	/** 标题 */
	private String title;

	/** 连接URL */
	private String url;

	/** 连接用户 */
	private String user;

	/** 连接密码 */
	private String password;

	/** 数据库驱动程序路径名 */
	private DriverEntity driverEntity;

	public Schema(String title, String url, String user, String password, DriverEntity driverEntity) {
		this.title = title;
		this.url = url;
		this.user = user;
		this.password = password;
		this.driverEntity = driverEntity;
	}

	public String getTitle() {
		return title;
	}

	public void setTitle(String title) {
		this.title = title;
	}

	public String getUrl() {
		return url;
	}

	public void setUrl(String url) {
		this.url = url;
	}

	public String getUser() {
		return user;
	}

	public void setUser(String user) {
		this.user = user;
	}

	public String getPassword() {
		return password;
	}

	public void setPassword(String password) {
		this.password = password;
	}

	public DriverEntity getDriverEntity() {
		return driverEntity;
	}

	public void setDriverEntity(DriverEntity driverEntity) {
		this.driverEntity = driverEntity;
	}
}
