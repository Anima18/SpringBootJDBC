package com.example.demo.dto;

import lombok.AllArgsConstructor;
import lombok.Data;

@Data
@AllArgsConstructor
public class SchemaDto {
    /** 标题 */
    private String title;

    /** 连接URL */
    private String url;

    /** 连接用户 */
    private String user;

    /** 连接密码 */
    private String password;

    /** 数据库驱动程序路径名 */
    private String driverPath;
}
