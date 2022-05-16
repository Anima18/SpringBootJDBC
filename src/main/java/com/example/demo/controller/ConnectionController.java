package com.example.demo.controller;

import com.example.demo.dto.TableDataListInput;
import com.example.demo.dto.base.PagedResultDto;
import com.example.demo.dto.RowDto;
import com.example.demo.dto.SchemaDto;
import com.example.demo.dto.TableDetailInput;
import com.example.demo.dto.base.PagingQuery;
import com.example.demo.jdbc.connection.DataSourceProvider;
import com.example.demo.jdbc.entity.SimpleTable;
import com.example.demo.jdbc.entity.TableDetail;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RestController;

import javax.validation.Valid;
import java.util.List;

@RestController
public class ConnectionController {
    @Autowired
    DataSourceProvider dataSourceProvider;

    @PostMapping("/connection/getTable")
    public List<SimpleTable> getTable(@Valid @RequestBody SchemaDto input) {
        try {
            return dataSourceProvider.getTableList(input);
        }catch (Exception e) {
            e.printStackTrace();
            return null;
        }
    }

    @PostMapping("/connection/testConnection")
    public Boolean testConnection(@Valid @RequestBody SchemaDto input) {
        try {
            return dataSourceProvider.testConnection(input);
        }catch (Exception e) {
            e.printStackTrace();
            return false;
        }
    }

    @PostMapping("/connection/tableDetail")
    public TableDetail getTableDetail(@Valid @RequestBody TableDetailInput input) {
        try {
            return dataSourceProvider.getTableDetail(input);
        }catch (Exception e) {
            e.printStackTrace();
            return null;
        }
    }

    @PostMapping("/connection/tableData")
    public PagedResultDto<RowDto> getTableData(@Valid @RequestBody TableDataListInput input) {
        try {
            return dataSourceProvider.getTableData(input);
        }catch (Exception e) {
            e.printStackTrace();
            return null;
        }
    }

}
