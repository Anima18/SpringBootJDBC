package com.example.demo.jdbc.connection;

import com.github.benmanes.caffeine.cache.RemovalCause;
import com.github.benmanes.caffeine.cache.RemovalListener;
import org.checkerframework.checker.nullness.qual.NonNull;
import org.checkerframework.checker.nullness.qual.Nullable;

import javax.sql.DataSource;
import java.sql.SQLException;

/**
 * {@linkplain DriverBasicDataSource}缓存移除监听器。
 *  <p>
 * 它负责调用{@linkplain DriverBasicDataSource#close()}。
 * 	</p>
 * @author jianjianhong
 * @date 2022/5/6
 */
public class DriverBasicDataSourceRemovalListener
        implements RemovalListener<ConnectionIdentity, InternalDataSourceHolder> {
    @Override
    public void onRemoval(@Nullable ConnectionIdentity key, @Nullable InternalDataSourceHolder value,
                          @NonNull RemovalCause cause) {
        if (!value.hasDataSource())
            return;

        DataSource dataSource = value.getDataSource();

        if (!(dataSource instanceof DriverBasicDataSource))
            throw new UnsupportedOperationException(
                    "This " + DriverBasicDataSourceRemovalListener.class.getSimpleName() + " only support "
                            + DriverBasicDataSource.class.getSimpleName());

        try {
            ((DriverBasicDataSource) dataSource).close();

            /*if (LOGGER.isDebugEnabled())
                LOGGER.debug("Close internal data source for {}", key);*/
        } catch (SQLException e) {
            //LOGGER.error("Close internal data source exception:", e);
        }
    }
}
