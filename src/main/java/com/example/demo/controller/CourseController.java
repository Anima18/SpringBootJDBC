package com.example.demo.controller;

import com.example.demo.entity.Courseware;
import com.example.demo.jdbc.connection.DataSourceProvider;
import com.example.demo.jdbc.connection.DriverEntityRepository;
import com.example.demo.jdbc.entity.DriverEntity;
import com.example.demo.jdbc.entity.Schema;
import com.example.demo.jdbc.entity.SimpleTable;
import com.example.demo.service.CourseService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RestController;

import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

@RestController
public class CourseController  {

    @Autowired
    CourseService courseService;
    @Autowired
    DataSourceProvider dataSourceProvider;

    @GetMapping("/course/getAll")
    public List<Courseware> getCourseList() {
        return courseService.getCourseList();
    }

    @GetMapping("/connection/getTable")
    public List<SimpleTable> getTable() {
        try {
            List<DriverEntity> driverEntities = DriverEntityRepository.getInstance().readDriverEntities();
            Map<String, DriverEntity> driverEntityMap = driverEntities.stream().collect(Collectors.toMap(com.example.demo.jdbc.entity.DriverEntity::getId, e->e, (k1, k2)->k1));

            Schema schema = new Schema("test",
                    "jdbc:postgresql://192.168.44.99:5432/crux-scheduled-work",
                    "postgres",
                    "unitech",
                    driverEntityMap.get("jdbcpostgresql42d2jre8"));

            return new DataSourceProvider().getTableList(schema);
        }catch (Exception e) {
            e.printStackTrace();
            return null;
        }
    }
}
