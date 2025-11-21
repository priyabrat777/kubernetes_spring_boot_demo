package com.demo.k8s.controller;

import com.demo.k8s.model.DataItem;
import com.demo.k8s.service.DataService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.net.InetAddress;
import java.net.UnknownHostException;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

@RestController
@RequestMapping("/api")
public class DemoController {

    @Autowired
    private DataService dataService;

    @Value("${app.environment:default}")
    private String environment;

    @Value("${app.version:1.0.0}")
    private String version;

    @Value("${app.feature.enabled:false}")
    private String featureEnabled;

    @GetMapping("/hello")
    public ResponseEntity<Map<String, String>> hello() {
        Map<String, String> response = new HashMap<>();
        response.put("message", "Hello from Spring Boot on Kubernetes!");
        response.put("timestamp", String.valueOf(System.currentTimeMillis()));
        try {
            response.put("hostname", InetAddress.getLocalHost().getHostName());
        } catch (UnknownHostException e) {
            response.put("hostname", "unknown");
        }
        return ResponseEntity.ok(response);
    }

    @GetMapping("/info")
    public ResponseEntity<Map<String, Object>> info() {
        Map<String, Object> response = new HashMap<>();
        response.put("application", "Kubernetes Spring Boot Demo");
        response.put("version", version);
        response.put("environment", environment);
        response.put("javaVersion", System.getProperty("java.version"));
        response.put("itemCount", dataService.getItemCount());

        try {
            response.put("hostname", InetAddress.getLocalHost().getHostName());
            response.put("hostAddress", InetAddress.getLocalHost().getHostAddress());
        } catch (UnknownHostException e) {
            response.put("hostname", "unknown");
            response.put("hostAddress", "unknown");
        }

        return ResponseEntity.ok(response);
    }

    @GetMapping("/config")
    public ResponseEntity<Map<String, String>> config() {
        Map<String, String> response = new HashMap<>();
        response.put("environment", environment);
        response.put("version", version);
        response.put("featureEnabled", featureEnabled);
        response.put("source", "ConfigMap and Environment Variables");
        return ResponseEntity.ok(response);
    }

    @GetMapping("/health")
    public ResponseEntity<Map<String, String>> health() {
        Map<String, String> response = new HashMap<>();
        response.put("status", "UP");
        response.put("timestamp", String.valueOf(System.currentTimeMillis()));
        return ResponseEntity.ok(response);
    }

    @PostMapping("/data")
    public ResponseEntity<DataItem> createData(@RequestBody DataItem item) {
        DataItem created = dataService.createItem(item);
        return ResponseEntity.status(HttpStatus.CREATED).body(created);
    }

    @GetMapping("/data/{id}")
    public ResponseEntity<DataItem> getData(@PathVariable String id) {
        return dataService.getItem(id)
                .map(ResponseEntity::ok)
                .orElse(ResponseEntity.notFound().build());
    }

    @GetMapping("/data")
    public ResponseEntity<List<DataItem>> getAllData() {
        return ResponseEntity.ok(dataService.getAllItems());
    }

    @DeleteMapping("/data/{id}")
    public ResponseEntity<Map<String, String>> deleteData(@PathVariable String id) {
        boolean deleted = dataService.deleteItem(id);
        Map<String, String> response = new HashMap<>();
        if (deleted) {
            response.put("message", "Item deleted successfully");
            return ResponseEntity.ok(response);
        } else {
            response.put("message", "Item not found");
            return ResponseEntity.status(HttpStatus.NOT_FOUND).body(response);
        }
    }
}
