/*
 * Copyright 2018 datagear.tech
 *
 * Licensed under the LGPLv3 license:
 * http://www.gnu.org/licenses/lgpl-3.0.html
 */

package com.example.demo.jdbc.entity;

import lombok.AllArgsConstructor;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

/**
 * 数据库模式实体。
 * 
 * @author datagear@163.com
 *
 */
@Getter
@Setter
@AllArgsConstructor
@NoArgsConstructor
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
}
