package com.example.demo.dto;

import lombok.AllArgsConstructor;
import lombok.Data;

@Data
@AllArgsConstructor
public class TableDetailInput {
    /** 数据库链接 */
    private SchemaDto schemaDto;

    /** 表名 */
    private String tableName;
}
