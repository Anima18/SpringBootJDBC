package com.example.demo.jdbc.connection;


import com.example.demo.dto.SchemaDto;
import com.example.demo.dto.TableDetailInput;
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

import javax.sql.DataSource;
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
            throw new DBMetaResolverException("["+input.getDriverPath()+"]数据库驱动程序不存在！");
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
            // 底层数据源无法支持此驱动时将会创建一个getDataSource()为null的InternalDataSourceHolder
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

        //动态加载驱动程序
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
     * 获取数据库表列表
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
        return tables;
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
     * 获取表结构
     * @param cn
     * @param tableName
     * @return
     * @throws Exception
     */
    public TableDetail getTableDetail(Connection cn, String tableName) throws DBMetaResolverException {
        @JDBCCompatiblity("如果cn为readonly，某些驱动程序的DatabaseMetaData.isReadOnly()也将为true（比如：Postgresql JDBC 42.2.5），"
                + "这会导致解析Table.readonly不正确，因此这里设为false，以保证解析正确")
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
     * 获取表类型。
     * <p>
     * 如果查不到，{@linkplain #DEFAULT_TABLE_TYPES}将返回
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
     * 获取数据库连接的schema
     * @param cn
     * @return
     */
    private String getSchema(Connection cn) {
        String schema;

        try {
            @JDBCCompatiblity("JDBC4.1（JDK1.7）才有Connection.getSchema()接口，为了兼容JDBC4.0（JDK1.6），"
                    + "所以这里捕获Throwable，避免出现底层java.lang.Error")
            String mySchema = cn.getSchema();
            schema = mySchema;
        } catch (Throwable e) {
            @JDBCCompatiblity("在JDBC4.0（JDK1.6）中需要将其设置为null，才符合DatabaseMetaData.getTables(...)等接口的参数要求")
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
}
