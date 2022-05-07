package com.example.demo.repository;

import com.example.demo.entity.Courseware;
import org.springframework.data.jpa.repository.JpaRepository;

public interface CourseRepository extends JpaRepository<Courseware, Integer> {
}
