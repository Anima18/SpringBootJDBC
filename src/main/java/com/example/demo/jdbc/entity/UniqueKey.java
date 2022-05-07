
package com.example.demo.jdbc.entity;

import java.util.List;

/**
 * 唯一键。
 * 
 * @author jianjianhong
 *
 */
public class UniqueKey extends AbstractKey {
    public UniqueKey(List<String> columnNames) {
        super(columnNames);
    }
}
