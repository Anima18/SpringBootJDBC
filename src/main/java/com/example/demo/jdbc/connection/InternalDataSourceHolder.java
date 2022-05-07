package com.example.demo.jdbc.connection;

import javax.sql.DataSource;

/**
 * 内置数据源持有类。
 * @author jianjianhong
 * @date 2022/5/6
 */
public class InternalDataSourceHolder {
    /** 内置数据源 */
    private DataSource dataSource = null;

    public InternalDataSourceHolder()
    {
        super();
    }

    /**
     * 是否持有数据源。
     *
     * @return
     */
    public boolean hasDataSource()
    {
        return (this.dataSource != null);
    }

    /**
     * 获取数据源实例。
     *
     * @return 可能为{@code null}
     */
    public DataSource getDataSource() {
        return dataSource;
    }

    public void setDataSource(DataSource dataSource)
    {
        this.dataSource = dataSource;
    }
}
