
package com.example.demo.jdbc.entity;

import java.util.List;

/**
 * 表键。
 * 
 * @author jianjianhong
 *
 */
public abstract class AbstractKey {
	/** 列名 */
	private List<String> columnNames;

	/** 键名 */
	private String keyName;

	public AbstractKey() {
	}

	public AbstractKey(List<String> columnNames) {
		this.columnNames = columnNames;
	}

	public List<String> getColumnNames() {
		return columnNames;
	}

	public void setColumnNames(List<String> columnNames) {
		this.columnNames = columnNames;
	}

	public String getKeyName() {
		return keyName;
	}

	public void setKeyName(String keyName) {
		this.keyName = keyName;
	}
}
