package com.example.demo.jdbc.util;

import java.sql.ResultSet;
import java.sql.ResultSetMetaData;
import java.sql.SQLException;
import java.util.HashMap;
import java.util.Map;

/**
 * @author jianjianhong
 * @date 2022/5/6
 */
public class MetaResultSet {
    private ResultSet resultSet;
    private Map<String, Integer> _nameColumnIndexMap = new HashMap<>();

    public MetaResultSet(ResultSet resultSet) {
        this.resultSet = resultSet;
    }

    /**
     * 获取列值。
     *
     * @param name
     * @param defaultValue
     * @return
     * @throws SQLException
     */
    public String getString(String name, String defaultValue) throws SQLException {
        int columnIndex = getColumnIndexAndCache(this.resultSet, name);

        if (columnIndex < 1)
            return defaultValue;

        String value = this.resultSet.getString(columnIndex);

        return (value == null || this.resultSet.wasNull() ? defaultValue : value);
    }

    /**
     * 获取列值。
     *
     * @param name
     * @param defaultValue
     * @return
     * @throws SQLException
     */
    public Integer getInt(String name, Integer defaultValue) throws SQLException {
        int columnIndex = getColumnIndexAndCache(this.resultSet, name);

        if (columnIndex < 1)
            return defaultValue;

        int value = this.resultSet.getInt(columnIndex);

        return (this.resultSet.wasNull() ? defaultValue : value);
    }

    private int getColumnIndexAndCache(ResultSet rs, String name) throws SQLException {
        Integer columnIndex = this._nameColumnIndexMap.get(name);
        if (columnIndex == null)
        {
            columnIndex = getColumnIndex(rs, name);
            this._nameColumnIndexMap.put(name, columnIndex);
        }

        return columnIndex;
    }

    private int getColumnIndex(ResultSet rs, String name) throws SQLException {
        ResultSetMetaData meta = rs.getMetaData();
        int columnCount = meta.getColumnCount();

        for (int i = 1; i <= columnCount; i++)
        {
            @JDBCCompatiblity("这里列名忽略大小写比较，避免不规范的驱动程序")
            String columnName = getColumnName(meta, i);
            if (columnName.equalsIgnoreCase(name))
                return i;
        }

        return -1;
    }

    private String getColumnName(ResultSetMetaData metaData, int column) throws SQLException  {
        String columnName = metaData.getColumnLabel(column);
        if (StringUtil.isEmpty(columnName))
            columnName = metaData.getColumnName(column);

        return columnName;
    }
}
