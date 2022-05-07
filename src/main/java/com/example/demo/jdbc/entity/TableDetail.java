package com.example.demo.jdbc.entity;

import java.util.List;

/**
 * @author jianjianhong
 * @date 2022/5/6
 */
public class TableDetail {
    /** 名称 */
    private String name;

    /** 类型 */
    private String type;

    /** 描述 */
    private String comment;

    /** 列集 */
    private List<Column> columns;

    /** 主键 */
    private PrimaryKey primaryKey;

    /** 唯一键 */
    private List<UniqueKey> uniqueKeys;

    /** 导入外键 */
    private List<ImportKey> importKeys;

    /** 表是否只读 */
    private boolean readonly = false;

    public String getName() {
        return name;
    }

    public void setName(String name) {
        this.name = name;
    }

    public String getType() {
        return type;
    }

    public void setType(String type) {
        this.type = type;
    }

    public String getComment() {
        return comment;
    }

    public void setComment(String comment) {
        this.comment = comment;
    }

    public List<Column> getColumns() {
        return columns;
    }

    public void setColumns(List<Column> columns) {
        this.columns = columns;
    }

    public PrimaryKey getPrimaryKey() {
        return primaryKey;
    }

    public void setPrimaryKey(PrimaryKey primaryKey) {
        this.primaryKey = primaryKey;
    }

    public List<UniqueKey> getUniqueKeys() {
        return uniqueKeys;
    }

    public void setUniqueKeys(List<UniqueKey> uniqueKeys) {
        this.uniqueKeys = uniqueKeys;
    }

    public List<ImportKey> getImportKeys() {
        return importKeys;
    }

    public void setImportKeys(List<ImportKey> importKeys) {
        this.importKeys = importKeys;
    }

    public boolean isReadonly() {
        return readonly;
    }

    public void setReadonly(boolean readonly) {
        this.readonly = readonly;
    }
}
