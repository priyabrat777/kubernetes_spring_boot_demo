package com.demo.k8s.config;

import com.demo.k8s.model.DataItem;
import com.demo.k8s.repository.DataItemRepository;
import org.springframework.boot.CommandLineRunner;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

@Configuration
public class DataInitializer {

    @Bean
    CommandLineRunner initDatabase(DataItemRepository repository) {
        return args -> {
            // Check if data already exists
            if (repository.count() == 0) {
                // Add sample data
                DataItem item1 = new DataItem("1", "Sample Item 1", "This is a sample item stored in PostgreSQL");
                DataItem item2 = new DataItem("2", "Sample Item 2", "Another sample item from database");
                DataItem item3 = new DataItem("3", "Kubernetes Demo", "Demo item for Kubernetes deployment");

                repository.save(item1);
                repository.save(item2);
                repository.save(item3);

                System.out.println("✅ Sample data initialized in database");
            } else {
                System.out.println("ℹ️  Database already contains data, skipping initialization");
            }
        };
    }
}
