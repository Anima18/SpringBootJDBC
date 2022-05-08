package com.example.demo.jdbc;

import com.example.demo.dto.SchemaDto;
import com.example.demo.jdbc.entity.DriverEntity;
import com.example.demo.jdbc.entity.Schema;
import com.example.demo.jdbc.entity.SimpleTable;
import com.example.demo.jdbc.entity.TableDetail;
import com.example.demo.jdbc.exception.DBMetaResolverException;
import com.example.demo.jdbc.connection.DataSourceProvider;
import com.example.demo.jdbc.connection.DriverEntityRepository;

import java.sql.Connection;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

/**
 * @author jianjianhong
 * @date 2022/4/27
 */
public class Test {
    public static void main(String[] args){
        try {
            SchemaDto schema = new SchemaDto("test",
                    "jdbc:postgresql://192.168.44.99:5432/crux-scheduled-work",
                    "postgres",
                    "unitech",
                    "jdbcpostgresql42d2jre8");

            //testConnection(schema);
            testTableList(schema);
            //testTableDetail(schema, "visual_template");
        }catch (Exception e) {
            e.printStackTrace();
        }
    }

    public static void testConnection(SchemaDto schema) {
        try {
            new DataSourceProvider().testConnection(schema);
            System.out.println("测试连接成功");
        } catch (DBMetaResolverException throwables) {
            throwables.printStackTrace();
            System.out.println("测试连接失败:"+throwables.getMessage());
        }
    }

    public static void testTableList(SchemaDto schema) {
        try {
            List<SimpleTable> tables = new DataSourceProvider().getTableList(schema);
            tables.forEach(simpleTable -> System.out.println(simpleTable.toString()));
        } catch (DBMetaResolverException throwables) {
            throwables.printStackTrace();
        }
    }

    public static void testTableDetail(SchemaDto schema, String tableName) {
        try {
            DataSourceProvider dataSourceProvider = new DataSourceProvider();
            Connection cn = dataSourceProvider.getConnection(schema);
            TableDetail tableDetail = dataSourceProvider.getTableDetail(cn, tableName);

            System.out.println(tableDetail.getName());
        } catch (DBMetaResolverException throwables) {
            throwables.printStackTrace();
        }
    }
}
