package com.example.demo.jdbc.connection;

import com.example.demo.jdbc.entity.*;
import com.example.demo.jdbc.exception.DBMetaResolverException;
import com.example.demo.jdbc.util.JDBCCompatiblity;
import com.example.demo.jdbc.util.JdbcUtil;
import com.example.demo.jdbc.util.MetaResultSet;
import com.example.demo.jdbc.util.StringUtil;

import java.sql.*;
import java.util.*;
import java.util.stream.Collectors;

/**
 * 表元数据解析
 * @author jianjianhong
 * @date 2022/5/6
 */
public class TableMetaResolver {

    /** 获取唯一键
     * @param cn
     * @param metaData
     * @param schema
     * @param tableName
     * @return 返回{@code null}表示无唯一键
     * @throws DBMetaResolverException
     */
    protected List<UniqueKey> getUniqueKeys(Connection cn, DatabaseMetaData metaData, String catalog, String schema,
                                        String tableName) throws DBMetaResolverException
    {
        List<UniqueKey> uniqueKeys = new ArrayList<>();

        ResultSet rs = null;

        List<String> keyNames = new ArrayList<>();
        Map<String, List<String>> keyColumnNamess = new HashMap<>();

        try {
            rs = metaData.getIndexInfo(catalog, schema, tableName, true, false);
            MetaResultSet mrs = new MetaResultSet(rs);

            while (rs.next()) {
                @JDBCCompatiblity("某些驱动程序INDEX_NAME列可能为nul，但COLUMN_NAME不为null，此行应是有效的，"
                        + "而某些驱动程序会返回INDEX_NAME和COLUMN_NAME都为null的无效行，所以，这里统一先把它们整理出来，下面再筛选过滤")
                String keyName = mrs.getString("INDEX_NAME", "");
                String columnName = mrs.getString("COLUMN_NAME", null);

                if(!keyNames.contains(keyName)) {
                    keyNames.add(keyName);
                    keyColumnNamess.put(keyName, new ArrayList<>());
                }

                if(!StringUtil.isEmpty(columnName)) {
                    keyColumnNamess.get(keyName).add(columnName);
                }
            }
        }catch (SQLException e){
            return uniqueKeys;
        }finally {
            JdbcUtil.closeResultSet(rs);
        }

        if (!keyNames.isEmpty()) {
            uniqueKeys = keyNames.stream().map(keyName -> {
                UniqueKey key = new UniqueKey(keyColumnNamess.get(keyName));
                key.setKeyName(keyName);
                return key;
            }).collect(Collectors.toList());
        }

        return uniqueKeys;
    }

    /**
     *  获取表主键
     * @param cn
     * @param metaData
     * @param schema
     * @param tableName
     * @return 返回{@code null}表示无主键。
     * @throws DBMetaResolverException
     */
    protected PrimaryKey getPrimaryKey(Connection cn, DatabaseMetaData metaData, String catalog, String schema,
                                       String tableName) throws DBMetaResolverException
    {
        PrimaryKey primaryKey = null;

        ResultSet rs = null;
        try {
            rs = metaData.getPrimaryKeys(catalog, schema, tableName);
            MetaResultSet mrs = new MetaResultSet(rs);

            List<String> columnNames = new ArrayList<>();
            String keyName = null;

            while (rs.next()) {
                String columnName = mrs.getString("COLUMN_NAME", null);

                if (StringUtil.isEmpty(keyName))
                    keyName = mrs.getString("PK_NAME", "");

                if(!StringUtil.isEmpty(columnName) && !columnNames.contains(columnName)) {
                    columnNames.add(columnName);
                }
            }

            if (!columnNames.isEmpty()) {
                primaryKey = new PrimaryKey(columnNames);
                primaryKey.setKeyName(keyName);
            }

            return primaryKey;
        } catch (SQLException e) {
            @JDBCCompatiblity("当tableName是视图时，某些驱动（比如Oracle）可能会抛出SQLSyntaxErrorException异常")
            PrimaryKey nullPrimaryKey = null;
            return nullPrimaryKey;
        } finally {
            JdbcUtil.closeResultSet(rs);
        }
    }

    /**
     * 获取表字段列表
     * @param cn
     * @param metaData
     * @param schema
     * @param tableName
     * @param count
     *            为{@code null}获取全部，否则获取指定数目
     * @return
     * @throws DBMetaResolverException
     */
    public List<Column> getColumns(Connection cn, DatabaseMetaData metaData, String catalog, String schema,
                                    String tableName, Integer count) throws DBMetaResolverException  {
        ResultSet rs = null;
        try {
            rs = getColumnResulSet(cn, metaData, catalog, schema, tableName);
            MetaResultSet mrs = new MetaResultSet(rs);

            List<Column> columns = new ArrayList<>();
            while (rs.next()) {
                columns.add(readColumn(mrs));
            }

            if(columns.isEmpty()) {
                columns = getColumnsByQuery(cn, metaData, catalog, schema, tableName);
            }
            columns = columns.stream().filter(column -> column !=null).collect(Collectors.toList());
            if (count != null) {
                columns = columns.subList(0, count);
            }
            Collections.sort(columns, Comparator.comparingInt(Column::getPosition));

            return columns;
        } catch (SQLException e) {
            throw new DBMetaResolverException(e);
        } finally {
            JdbcUtil.closeResultSet(rs);
        }
    }

    /**
     * 某些类型的表（比如Oracle的同义词），不能通过DatabaseMetaData的getColumns获取列信息，所以，这里采用查询结果集的方式再次读取列信息
     * @param cn
     * @param metaData
     * @param catalog
     * @param schema
     * @param tableName
     * @return
     * @throws SQLException
     */
    private List<Column> getColumnsByQuery(Connection cn, DatabaseMetaData metaData, String catalog, String schema,
                                             String tableName) throws SQLException  {
        String iq = getIdentifierQuote(cn);
        String sql = "SELECT * FROM " + iq + tableName + iq;

        Statement st = null;
        ResultSet rs = null;

        try {
            st = cn.createStatement();
            st.setFetchSize(1);
            rs = st.executeQuery(sql);

            return getColumnsByResultSetMetaData(cn, rs.getMetaData());
        } finally {
            JdbcUtil.closeResultSet(rs);
            JdbcUtil.closeStatement(st);
        }
    }

    /**
     * 获取标识符引用符。
     *
     * @param cn
     * @return
     */
    private String getIdentifierQuote(Connection cn) {
        String iq = null;

        try {
            iq = cn.getMetaData().getIdentifierQuoteString();
        } catch (SQLException e) {

        }

        if(iq == null || iq.isEmpty()) {
            @JDBCCompatiblity("出现异常、，或者不规范的JDBC驱动返回空字符串时，使用JDBC规范规定的空格字符串")
            String iqt = " ";

            iq = iqt;
        }

        return iq;
    }

    private List<Column> getColumnsByResultSetMetaData(Connection cn, ResultSetMetaData resultSetMetaData) throws SQLException {
        int columnCount = resultSetMetaData.getColumnCount();

        List<Column> columns = new ArrayList<Column>(columnCount);

        for (int i = 1; i <= columnCount; i++) {
            Column column = new Column();

            String columnName = getColumnName(resultSetMetaData, i);

            column.setName(columnName);
            column.setType(resultSetMetaData.getColumnType(i));
            column.setTypeName(resultSetMetaData.getColumnTypeName(i));
            column.setSize(resultSetMetaData.getPrecision(i));
            column.setDecimalDigits(resultSetMetaData.getScale(i));
            column.setNullable(DatabaseMetaData.columnNoNulls != resultSetMetaData.isNullable(i));
            column.setAutoincrement(resultSetMetaData.isAutoIncrement(i));

            resolveSortable(column);
            resolveSearchableType(column);

            resolveDefaultValue(column);

            columns.add(column);
        }

        return columns;
    }

    private String getColumnName(ResultSetMetaData metaData, int column) throws SQLException  {
        String columnName = metaData.getColumnLabel(column);
        if (StringUtil.isEmpty(columnName))
            columnName = metaData.getColumnName(column);

        return columnName;
    }

    private ResultSet getColumnResulSet(Connection cn, DatabaseMetaData databaseMetaData, String catalog,
                                        String schema, String tableName) throws SQLException {
        return databaseMetaData.getColumns(catalog, schema, tableName, "%");
    }


    /**
     * 获取字段属性
     * @return 返回{@code null}表示未读取到
     */
    protected Column readColumn(MetaResultSet rs) {
        try {
            String name = rs.getString("COLUMN_NAME", null);
            Integer type = rs.getInt("DATA_TYPE", null);

            if (StringUtil.isEmpty(name) || type == null) {
                return null;
            }

            Column column = new Column(name, type);

            column.setTypeName(rs.getString("TYPE_NAME", ""));
            column.setSize(rs.getInt("COLUMN_SIZE", 0));
            column.setDecimalDigits(rs.getInt("DECIMAL_DIGITS", 0));
            column.setNullable(
                    DatabaseMetaData.columnNoNulls != rs.getInt("NULLABLE", DatabaseMetaData.columnNullable));
            column.setComment(rs.getString("REMARKS", ""));
            column.setDefaultValue(rs.getString("COLUMN_DEF", null));
            column.setAutoincrement("yes".equalsIgnoreCase(rs.getString("IS_AUTOINCREMENT", "no")));
            column.setPosition(rs.getInt("ORDINAL_POSITION", 1));

            resolveSortable(column);
            resolveSearchableType(column);

            resolveDefaultValue(column);

            return column;
        } catch (SQLException e) {
            return null;
        }
    }

    /**
     * 某些驱动程序对有些类型不支持排序（比如Oracle对于BLOB类型）
     * @param column
     */
    private void resolveSortable(Column column) {
        int sqlType = column.getType();

        boolean sortable = (Types.BIGINT == sqlType || Types.BIT == sqlType || Types.BOOLEAN == sqlType
                || Types.CHAR == sqlType || Types.DATE == sqlType || Types.DECIMAL == sqlType || Types.DOUBLE == sqlType
                || Types.FLOAT == sqlType || Types.INTEGER == sqlType || Types.NCHAR == sqlType
                || Types.NUMERIC == sqlType || Types.NVARCHAR == sqlType || Types.REAL == sqlType
                || Types.SMALLINT == sqlType || Types.TIME == sqlType || Types.TIMESTAMP == sqlType
                || Types.TINYINT == sqlType || Types.VARCHAR == sqlType);

        column.setSortable(sortable);
    }

    /**
     * 设置字段默认值
     * @param column
     */
    private void resolveDefaultValue(Column column) {
        if (!column.hasDefaultValue())
            return;

        String value = column.getDefaultValue();
        int len = value.length();

        // 移除开头和结尾的引号
        if (len >= 2 && ((value.charAt(0) == '\'' && value.charAt(len - 1) == '\'')
                || (value.charAt(0) == '"' && value.charAt(len - 1) == '"')))
        {
            value = (len == 2 ? "" : value.substring(1, value.length() - 1));
            column.setDefaultValue(value);
        }

        if (StringUtil.isEmpty(value))
            return;

    }

    /**
     * 很多驱动程序的值为SearchableType.ALL但实际并不支持LIKE语法（比如：PostgreSQL JDBC 42.2.5），
     *  这里为了兼容，不采用数据库级的SearchableType逻辑
     * @param column
     */
    private void resolveSearchableType(Column column) {
        SearchableType searchableType = null;

        int sqlType = column.getType();

        if (Types.CHAR == sqlType || Types.VARCHAR == sqlType || Types.NCHAR == sqlType || Types.NVARCHAR == sqlType)
            searchableType = SearchableType.ALL;

        column.setSearchableType(searchableType);
    }
}
