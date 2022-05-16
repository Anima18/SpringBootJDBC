/*
 * Copyright 2018 datagear.tech
 *
 * Licensed under the LGPLv3 license:
 * http://www.gnu.org/licenses/lgpl-3.0.html
 */

package com.example.demo.dto;

import java.util.HashMap;
import java.util.Map;

/**
 * 行对象。
 * 
 * @author jianjianhong
 *
 */
public class RowDto extends HashMap<String, Object>
{
	private static final long serialVersionUID = 1L;

	public RowDto()
	{
		super();
	}

	public RowDto(Map<? extends String, ? extends Object> m)
	{
		super(m);
	}
}
