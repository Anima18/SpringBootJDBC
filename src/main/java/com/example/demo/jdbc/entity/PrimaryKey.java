package com.example.demo.jdbc.entity;

import java.util.List;

/**
 * 主键。
 * 
 * @author jianjianhong
 *
 */
public class PrimaryKey extends AbstractKey {
    public PrimaryKey(List<String> columnNames) {
        super(columnNames);
    }
}
