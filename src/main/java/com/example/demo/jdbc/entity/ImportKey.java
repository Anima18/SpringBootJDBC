
package com.example.demo.jdbc.entity;

import java.util.List;

/**
 * 导入外键。
 * 
 * @author jianjianhong
 *
 */
public class ImportKey extends AbstractKey {

	/** 主表名 */
	private String primaryTableName;

	/** 主表键列名 */
	private List<String> primaryColumnNames;

}
