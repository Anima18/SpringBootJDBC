package com.example.demo.dto.base;

import lombok.Data;
@Data
public class Query {

    /** 查询关键字 */
    private String keyword;

    /** 查询条件 */
    private String condition;

    /** 排序方式 */
    private Order[] orders;

    /** 针对keyword，是否使用“NOT LIKE”而非“LIKE” */
    private boolean notLike = false;

}

