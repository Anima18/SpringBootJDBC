package com.example.demo.jdbc.entity;

/**
 * @author jianjianhong
 * @date 2022/4/27
 */
public class SimpleTable {
    /** 名称 */
    private String name;

    /** 类型 */
    private String type;

    /** 描述 */
    private String comment;

    public SimpleTable(String name, String type, String comment) {
        this.name = name;
        this.type = type;
        this.comment = comment;
    }

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

    @Override
    public String toString() {
        return "SimpleTable{" +
                "name='" + name + '\'' +
                ", type='" + type + '\'' +
                ", comment='" + comment + '\'' +
                '}';
    }
}
