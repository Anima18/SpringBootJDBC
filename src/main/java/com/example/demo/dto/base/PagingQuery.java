package com.example.demo.dto.base;

import lombok.Data;

import java.util.Arrays;
@Data
public class PagingQuery extends Query {

    /** 页码 */
    private int page = 1;

    /** 每页记录数 */
    private int pageSize = 10;

}