package com.demo.k8s.service;

import com.demo.k8s.model.DataItem;
import com.demo.k8s.repository.DataItemRepository;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

import java.util.*;

@Service
public class DataService {

    @Autowired
    private DataItemRepository repository;

    public DataItem createItem(DataItem item) {
        if (item.getId() == null || item.getId().isEmpty()) {
            item.setId(UUID.randomUUID().toString());
        }
        return repository.save(item);
    }

    public Optional<DataItem> getItem(String id) {
        return repository.findById(id);
    }

    public List<DataItem> getAllItems() {
        return repository.findAll();
    }

    public boolean deleteItem(String id) {
        if (repository.existsById(id)) {
            repository.deleteById(id);
            return true;
        }
        return false;
    }

    public int getItemCount() {
        return (int) repository.count();
    }
}
