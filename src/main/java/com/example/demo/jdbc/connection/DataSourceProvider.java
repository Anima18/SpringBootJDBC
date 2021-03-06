package com.example.demo.jdbc.connection;


import com.example.demo.dto.RowDto;
import com.example.demo.dto.SchemaDto;
import com.example.demo.dto.TableDataListInput;
import com.example.demo.dto.TableDetailInput;
import com.example.demo.dto.base.PagedResultDto;
import com.example.demo.jdbc.entity.*;
import com.example.demo.jdbc.exception.DBMetaResolverException;
import com.example.demo.jdbc.exception.TableNotFoundException;
import com.example.demo.jdbc.util.JDBCCompatiblity;
import com.example.demo.jdbc.util.JdbcUtil;
import com.example.demo.jdbc.util.TableType;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.github.benmanes.caffeine.cache.Cache;
import com.github.benmanes.caffeine.cache.Caffeine;
import com.github.dozermapper.core.Mapper;
import com.zaxxer.hikari.HikariConfig;
import com.zaxxer.hikari.HikariDataSource;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Component;
import org.springframework.web.bind.annotation.RequestBody;

import javax.sql.DataSource;
import javax.validation.Valid;
import java.sql.*;
import java.util.*;
import java.util.concurrent.TimeUnit;
import java.util.function.Function;
import java.util.stream.Collectors;

/**
 * @author jianjianhong
 * @date 2022/4/27
 */
@Component
public class DataSourceProvider {
    private static final String[] DEFAULT_TABLE_TYPES = { TableType.TABLE, TableType.VIEW, TableType.SYSTEM_TABLE,
            TableType.GLOBAL_TEMPORARY, TableType.LOCAL_TEMPORARY, TableType.ALIAS, TableType.SYNONYM };

    private Cache<ConnectionIdentity, InternalDataSourceHolder> internalDataSourceCache;

    @Autowired
    private DriverEntityRepository driverEntityRepository;
    @Autowired
    private Mapper mapper;

    public DataSourceProvider() {
        this.internalDataSourceCache = Caffeine.newBuilder().maximumSize(50)
                .expireAfterAccess(60 * 24, TimeUnit.MINUTES)
                .removalListener(new DriverBasicDataSourceRemovalListener()).build();
    }

    private Schema getSchema(SchemaDto input) throws DBMetaResolverException {
        Schema schema = mapper.map(input, Schema.class);
        DriverEntity driverEntity = driverEntityRepository.getDriverEntityMap().get(input.getDriverPath());

        if(driverEntity == null) {
            throw new DBMetaResolverException("["+input.getDriverPath()+"]?????????????????????????????????");
        }
        schema.setDriverEntity(driverEntity);
        return schema;
    }

    public Connection getConnection(SchemaDto input) throws DBMetaResolverException {
        Schema schema = getSchema(input);
        Driver driver = loadDriver(schema);

        String url = schema.getUrl();
        Properties properties = new Properties();
        properties.put("user", schema.getUser());
        properties.put("password", schema.getPassword());

        ConnectionIdentity connectionIdentity = ConnectionIdentity.valueOf(url, properties);

        Connection connection = null;
        InternalDataSourceHolder dataSourceHolder = null;
        try {
            dataSourceHolder = this.internalDataSourceCache.get(connectionIdentity,
                    new Function<ConnectionIdentity, InternalDataSourceHolder>()
                    {
                        @Override
                        public InternalDataSourceHolder apply(ConnectionIdentity key)
                        {
                            DataSource dataSource = new DriverBasicDataSource(driver, url, properties);
                            InternalDataSourceHolder holder = new InternalDataSourceHolder();
                            holder.setDataSource(dataSource);
                            return holder;
                        }
                    });
            // ?????????????????????????????????????????????????????????getDataSource()???null???InternalDataSourceHolder
            if (!dataSourceHolder.hasDataSource()) {
                connection = driver.connect(url, properties);

            } else {
                connection = dataSourceHolder.getDataSource().getConnection();

            }
        } catch (Exception e) {
            throw new DBMetaResolverException(e);
        }
        return connection;
    }

    private Driver loadDriver(Schema schema) throws DBMetaResolverException {
        DriverEntity driverEntity = schema.getDriverEntity();
        String driverId = driverEntity.getId();
        String driverClassName = driverEntity.getDriverClassName();

        //????????????????????????
        try {
            PathDriverClassLoader pathDriverClassLoader = new PathDriverClassLoader(driverEntityRepository.getDriverFile(driverId));
            Class<?> driverToolClass = pathDriverClassLoader.loadClass(DriverTool.class.getName());
            Object driverTool = driverToolClass.newInstance();
            Class.forName(driverClassName, true, pathDriverClassLoader);
            Driver driver = (Driver) driverTool.getClass().getMethod("getDriver", String.class).invoke(driverTool, driverClassName);

            if(!driver.acceptsURL(schema.getUrl())) {
                throw new Exception(driverEntity + " 's driver can not accept url [" + schema.getUrl() + "]");
            }
            return driver;
        } catch (Exception e) {
            throw new DBMetaResolverException(e);
        }
    }


    /**
     * ????????????????????????
     * @param input
     * @throws Exception
     */
    public List<SimpleTable> getTableList(SchemaDto input) throws DBMetaResolverException {
        Connection cn = null;
        List<SimpleTable> tables = new ArrayList<>();
        try {
            cn = getConnection(input);

            String catalog = cn.getCatalog();
            DatabaseMetaData metaData = cn.getMetaData();
            String schemaStr = getSchema(cn);

            tables = getTableList(catalog, metaData, schemaStr, "%");
        }catch (Exception e) {
            throw new DBMetaResolverException(e);
        }finally {
            if(cn != null) {
                JdbcUtil.closeConnection(cn);
            }
        }
        return tables.stream().filter(table->table.getType().equals("TABLE")).collect(Collectors.toList());
    }

    private List<SimpleTable> getTableList(String catalog, DatabaseMetaData metaData, String schemaStr, String tableNamePattern) throws Exception {
        List<SimpleTable> tables = new ArrayList<>();
        String[] tableTypes = getTableTypes(metaData);
        ResultSet rs = metaData.getTables(catalog, schemaStr, tableNamePattern, tableTypes);
        while (rs.next()) {
            String name = rs.getString("TABLE_NAME");
            String type = TableType.toTableType(rs.getString("TABLE_TYPE"));
            String remarks = rs.getString("REMARKS");
            tables.add(new SimpleTable(name, type, remarks));
        }
        return tables;
    }

    public TableDetail getTableDetail(TableDetailInput input) throws DBMetaResolverException {
        Connection connection = getConnection(input.getSchemaDto());
        return getTableDetail(connection, input.getTableName());
    }

    /**
     * ???????????????
     * @param cn
     * @param tableName
     * @return
     * @throws Exception
     */
    public TableDetail getTableDetail(Connection cn, String tableName) throws DBMetaResolverException {
        @JDBCCompatiblity("??????cn???readonly????????????????????????DatabaseMetaData.isReadOnly()?????????true????????????Postgresql JDBC 42.2.5??????"
                + "??????????????????Table.readonly??????????????????????????????false????????????????????????")
        boolean readonly = JdbcUtil.isReadonlyIfSupports(cn, true);
        if (readonly)
            JdbcUtil.setReadonlyIfSupports(cn, false);

        try {
            String catalog = cn.getCatalog();
            DatabaseMetaData metaData = cn.getMetaData();
            String schema = getSchema(cn);
            return getTableDetail(cn, metaData, catalog, schema, tableName);
        } catch (Exception e) {
            throw new DBMetaResolverException(e);
        }
    }

    private TableDetail getTableDetail(Connection cn, DatabaseMetaData metaData, String catalog, String schema, String tableName) throws Exception  {
        boolean readonly = cn.getMetaData().isReadOnly();
        List<SimpleTable> simpleTables = getTableList(catalog, metaData, schema, tableName);

        if (simpleTables == null || simpleTables.isEmpty())
            throw new TableNotFoundException(tableName);

        SimpleTable simpleTable = simpleTables.get(0);
        TableMetaResolver tableMetaResolver = new TableMetaResolver();
        TableDetail table = new TableDetail();
        table.setName(simpleTable.getName());
        table.setType(simpleTable.getType());
        table.setComment(simpleTable.getComment());
        table.setColumns(tableMetaResolver.getColumns(cn, metaData, catalog, schema, tableName, null));
        table.setPrimaryKey(tableMetaResolver.getPrimaryKey(cn, metaData, catalog, schema, tableName));
        table.setUniqueKeys(tableMetaResolver.getUniqueKeys(cn, metaData, catalog, schema, tableName));
        //table.setImportKeys(getImportKeys(cn, metaData, catalog, schema, tableName));
        table.setReadonly(readonly);

        return table;
    }


    /**
     * ??????????????????
     * <p>
     * ??????????????????{@linkplain #DEFAULT_TABLE_TYPES}?????????
     * </p>
     *
     * @param metaData
     * @return
     */
    private String[] getTableTypes(DatabaseMetaData metaData) {
        String[] types = null;

        ResultSet rs = null;
        try {
            List<String> typeList = new ArrayList<>();
            rs = metaData.getTableTypes();

            while (rs.next())
                typeList.add(rs.getString(1));

            types = typeList.toArray(new String[typeList.size()]);
        } catch (SQLException e) {
            //LOGGER.warn("can not get table types :", e);
        } finally {
            JdbcUtil.closeResultSet(rs);
        }

        if (types == null || types.length == 0) {
            //LOGGER.warn("no table types found for {}, the default will return", ConnectionOption.valueOfNonNull(cn));
            return DEFAULT_TABLE_TYPES;
        }

        return types;
    }

    /**
     * ????????????????????????schema
     * @param cn
     * @return
     */
    private String getSchema(Connection cn) {
        String schema;

        try {
            @JDBCCompatiblity("JDBC4.1???JDK1.7?????????Connection.getSchema()?????????????????????JDBC4.0???JDK1.6??????"
                    + "??????????????????Throwable?????????????????????java.lang.Error")
            String mySchema = cn.getSchema();
            schema = mySchema;
        } catch (Throwable e) {
            @JDBCCompatiblity("???JDBC4.0???JDK1.6???????????????????????????null????????????DatabaseMetaData.getTables(...)????????????????????????")
            String mySchema = null;
            schema = mySchema;
        }

        return schema;
    }

    public Boolean testConnection(SchemaDto input) throws DBMetaResolverException {
        Connection cn = null;
        try {
            cn = getConnection(input);
            return true;
        }finally {
            if(cn != null) {
                JdbcUtil.closeConnection(cn);
            }
        }
    }


    public PagedResultDto<RowDto> getTableData(TableDataListInput input) throws DBMetaResolverException {
        TableDetail table = input.getTableDetail();
        if (table.getColumns() == null || table.getColumns().isEmpty())
            throw new DBMetaResolverException("table["+table.getName()+"]???????????????");

        Connection connection = getConnection(input.getSchemaDto());

        Long totalCount = getTableCount(connection, input.getTableDetail());

        int startRow = input.getPageSize() * (input.getPage()) + 1;
        int count = input.getPageSize();
        String sql = buildTableSql(input, startRow, count);
        try (PreparedStatement pstmt =connection.prepareStatement(sql); ResultSet rs = pstmt.executeQuery()){
            List<RowDto> rowDtos = mapToRows(connection, table, rs, startRow, count);

            return new PagedResultDto<>(totalCount, rowDtos);
        } catch (SQLException e) {
            throw new DBMetaResolverException(e);
        }
    }

    protected List<RowDto> mapToRows(Connection cn, TableDetail table, ResultSet rs, int startRow, int count)
            throws SQLException {
        if (startRow < 1)
            startRow = 1;

        List<RowDto> resultList = new ArrayList<>();

/*
        if (count >= 0 && startRow > 1)
            forwardBefore(rs, startRow);
*/

        int endRow = (count >= 0 ? startRow + count : -1);

        int rowIndex = startRow;
        while (rs.next()) {
            if (endRow >= 0 && rowIndex >= endRow)
                break;

            RowDto row = mapToRow(cn, table, rs);

            resultList.add(row);

            rowIndex++;
        }

        return resultList;
    }


    /**
     * ????????????????????????Row?????????
     *
     * @param cn
     * @param table
     * @param rs
     * @return
     */
    public RowDto mapToRow(Connection cn, TableDetail table, ResultSet rs)
            throws DBMetaResolverException {
        RowDto row = new RowDto();

        try  {
            for(Column column : table.getColumns()) {
                Object value = getColumnValue(rs, column);
                row.put(column.getName(), value);
            }
        }  catch (SQLException e) {
            throw new DBMetaResolverException(e);
        }

        return row;
    }

    /**
     * ???????????????
     * <p>
     * ????????????????????????JDBC4.0?????????Data Type Conversion Tables??????????????????Type Conversions
     * Supported by ResultSet getter Methods?????????????????????????????????????????????
     * </p>
     *
     * @param rs
     * @param column
     * @return
     * @throws SQLException
     */
    @JDBCCompatiblity("?????????????????????????????????ResultSet.getObject?????????????????????????????????")
    public Object getColumnValue( ResultSet rs, Column column) throws SQLException {
        Object value = null;
        String columnName = column.getName();
        int sqlType = column.getType();

        switch (sqlType) {
            case Types.ARRAY: {
                value = rs.getArray(columnName);
                break;
            }

            case Types.BIGINT: {
                value = rs.getLong(columnName);
                break;
            }

            case Types.BINARY:  {
                value = rs.getBytes(columnName);
                break;
            }

            case Types.BIT: {
                value = rs.getBoolean(columnName);
                break;
            }

            case Types.BLOB:  {
                value = rs.getBlob(columnName);
                break;
            }

            case Types.BOOLEAN: {
                value = rs.getBoolean(columnName);
                break;
            }

            case Types.CHAR: {
                value = rs.getString(columnName);
                break;
            }

            case Types.CLOB: {
                value = rs.getClob(columnName);
                break;
            }

            case Types.DATALINK: {
                value = rs.getObject(columnName);
                break;
            }

            case Types.DATE: {
                value = rs.getDate(columnName);
                break;
            }

            case Types.DECIMAL: {
                value = rs.getBigDecimal(columnName);
                break;
            }

            case Types.DISTINCT: {
                value = rs.getObject(columnName);
                break;
            }

            case Types.DOUBLE: {
                value = rs.getDouble(columnName);
                break;
            }

            case Types.FLOAT: {
                value = rs.getFloat(columnName);
                break;
            }

            case Types.INTEGER: {
                value = rs.getInt(columnName);
                break;
            }

            case Types.JAVA_OBJECT: {
                value = rs.getObject(columnName);
                break;
            }

            case Types.LONGNVARCHAR: {
                value = rs.getNCharacterStream(columnName);
                break;
            }

            case Types.LONGVARBINARY: {
                value = rs.getBinaryStream(columnName);
                break;
            }

            case Types.LONGVARCHAR: {
                value = rs.getCharacterStream(columnName);
                break;
            }

            case Types.NCHAR: {
                value = rs.getNString(columnName);
                break;
            }

            case Types.NCLOB: {
                value = rs.getNClob(columnName);
                break;
            }

            case Types.NUMERIC: {
                value = rs.getBigDecimal(columnName);
                break;
            }

            case Types.NVARCHAR: {
                value = rs.getNString(columnName);
                break;
            }

            case Types.OTHER: {
                value = rs.getObject(columnName);
                break;
            }

            case Types.REAL: {
                value = rs.getFloat(columnName);
                break;
            }

            case Types.REF: {
                value = rs.getRef(columnName);
                break;
            }

            case Types.REF_CURSOR: {
                value = rs.getObject(columnName);
                break;
            }

            case Types.ROWID:  {
                value = rs.getRowId(columnName);
                break;
            }

            case Types.SMALLINT: {
                value = rs.getShort(columnName);
                break;
            }

            case Types.SQLXML: {
                value = rs.getSQLXML(columnName);
                break;
            }

            case Types.STRUCT: {
                value = rs.getObject(columnName);
                break;
            }

            case Types.TIME:
            case Types.TIME_WITH_TIMEZONE: {
                value = rs.getTime(columnName);
                break;
            }

            case Types.TIMESTAMP:
            case Types.TIMESTAMP_WITH_TIMEZONE: {
                value = rs.getTimestamp(columnName);
                break;
            }

            case Types.TINYINT: {
                value = rs.getByte(columnName);
                break;
            }

            case Types.VARBINARY:  {
                value = rs.getBytes(columnName);
                break;
            }

            case Types.VARCHAR: {
                value = rs.getString(columnName);
                break;
            }

            default: {
                throw new UnsupportedOperationException("Get JDBC [" + sqlType + "] type value is not supported");
            }
        }

        if (rs.wasNull())
            value = null;

        return value;
    }

    /**
     * ??????????????????????????????{@linkplain ResultSet}?????????????????????????????????
     *
     * @param rs
     * @param rowIndex
     *            ????????????{@code 1}??????
     * @throws SQLException
     */
    public void forwardBefore(ResultSet rs, int rowIndex) throws SQLException {
        // ??????????????????????????????????????????????????????????????????????????????????????????
        if (rowIndex == 1)
            return;

        try  {
            rs.absolute(rowIndex - 1);
        } catch (SQLException e) {
            @JDBCCompatiblity("????????????????????????ResultSet?????????absolute???????????????")
            int i = 1;
            for (; i < rowIndex; i++) {
                if (!rs.next())
                    break;
            }
        }
    }

    private String buildTableSql(TableDataListInput input, int startRow, int count) {


        TableDetail table = input.getTableDetail();
        List<Column> columns =  table.getColumns();

        StringBuilder builder = new StringBuilder();
        builder.append("SELECT ");
        for(int i=0; i<columns.size(); i++) {
            builder.append(columns.get(i).getName());
            builder.append(" ");
            if(i < columns.size()-1) {
                builder.append(",");
            }
        }
        builder.append(" FROM "+table.getName()+" ");
        builder.append(" LIMIT " + count + " OFFSET " + (startRow - 1));
        return builder.toString();
    }

    private long getTableCount(Connection cn, TableDetail table) throws DBMetaResolverException {
        String sql = String.format("SELECT COUNT(*) FROM %s", table.getName());

        try ( PreparedStatement pstmt =cn.prepareStatement(sql); ResultSet rs = pstmt.executeQuery()){
            boolean nexted = rs.next();
            long totalCount = rs.getLong(1);
            return totalCount;
        } catch (SQLException e) {
            throw new DBMetaResolverException(e);
        }
    }
}
