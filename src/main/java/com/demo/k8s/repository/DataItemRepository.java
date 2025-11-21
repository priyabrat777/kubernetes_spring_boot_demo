package com.demo.k8s.repository;

import com.demo.k8s.model.DataItem;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

@Repository
public interface DataItemRepository extends JpaRepository<DataItem, String> {
    // JpaRepository provides all basic CRUD operations
    // Additional custom queries can be added here if needed
}
