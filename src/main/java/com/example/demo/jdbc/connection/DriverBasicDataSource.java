package com.example.demo.jdbc.connection;

import org.apache.commons.dbcp2.BasicDataSource;
import org.apache.commons.dbcp2.ConnectionFactory;
import org.apache.commons.dbcp2.DriverConnectionFactory;

import java.sql.Driver;
import java.sql.SQLException;
import java.util.Properties;

/**
 * @author jianjianhong
 * @date 2022/5/6
 */
public class DriverBasicDataSource extends BasicDataSource {
    private Driver driver;

    private Properties connectionProperties;

    public DriverBasicDataSource(Driver driver, String url, Properties properties) {
        super();
        this.driver = driver;
        super.setDriverClassName(driver.getClass().getName());
        super.setUrl(url);
        this.connectionProperties = properties;
    }

    @Override
    public Driver getDriver()
    {
        return driver;
    }

    @Override
    public void setDriver(Driver driver)
    {
        this.driver = driver;
    }

    @Override
    protected ConnectionFactory createConnectionFactory() throws SQLException {
        return new DriverConnectionFactory(driver, getUrl(), this.connectionProperties);
    }
}
