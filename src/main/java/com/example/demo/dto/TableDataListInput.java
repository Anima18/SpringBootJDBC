package com.example.demo.dto;

import com.example.demo.dto.base.PagingQuery;
import com.example.demo.jdbc.entity.TableDetail;
import lombok.Data;

@Data
public class TableDataListInput extends PagingQuery {
    /** 数据库链接 */
    private SchemaDto schemaDto;

    private TableDetail tableDetail;
}
