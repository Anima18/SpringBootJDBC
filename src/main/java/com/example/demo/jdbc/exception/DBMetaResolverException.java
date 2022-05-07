/*
 * Copyright 2018 datagear.tech
 *
 * Licensed under the LGPLv3 license:
 * http://www.gnu.org/licenses/lgpl-3.0.html
 */

package com.example.demo.jdbc.exception;

import java.sql.SQLException;

/**
 * 数据库元信息解析异常。
 * 
 * @author jianjianhong
 *
 */
public class DBMetaResolverException extends SQLException {
	private static final long serialVersionUID = 1L;

	public DBMetaResolverException()
	{
		super();
	}

	public DBMetaResolverException(String message)
	{
		super(message);
	}

	public DBMetaResolverException(Throwable cause)
	{
		super(cause);
	}

	public DBMetaResolverException(String message, Throwable cause)
	{
		super(message, cause);
	}
}
