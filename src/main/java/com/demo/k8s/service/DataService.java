package com.demo.k8s.service;

import com.demo.k8s.model.DataItem;
import com.demo.k8s.repository.DataItemRepository;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.cache.annotation.CacheEvict;
import org.springframework.cache.annotation.CachePut;
import org.springframework.cache.annotation.Cacheable;
import org.springframework.stereotype.Service;

import java.util.*;

/**
 * Service layer for DataItem operations with enterprise-grade caching.
 * 
 * Caching Strategy:
 * - Individual items cached by ID in "dataItems" cache
 * - All items list cached in "allDataItems" cache
 * - Cache TTL: 10 minutes (configurable)
 * - Graceful degradation if Redis unavailable
 * 
 * @author Kubernetes Spring Boot Demo
 * @version 1.0.0
 */
@Service
public class DataService {

    private static final Logger logger = LoggerFactory.getLogger(DataService.class);

    @Autowired
    private DataItemRepository repository;

    /**
     * Create a new data item and update cache.
     * 
     * @param item DataItem to create
     * @return Created DataItem with generated ID
     */
    @CachePut(value = "dataItems", key = "#result.id")
    @CacheEvict(value = "allDataItems", allEntries = true)
    public DataItem createItem(DataItem item) {
        long startTime = System.currentTimeMillis();

        if (item.getId() == null || item.getId().isEmpty()) {
            item.setId(UUID.randomUUID().toString());
        }

        DataItem savedItem = repository.save(item);
        long duration = System.currentTimeMillis() - startTime;

        logger.info("Created item with ID '{}' and updated cache (duration: {}ms)", savedItem.getId(), duration);
        logger.debug("Cache PUT: dataItems[{}] = {}", savedItem.getId(), savedItem.getName());
        logger.debug("Cache EVICT: allDataItems (all entries)");

        return savedItem;
    }

    /**
     * Get a data item by ID with caching.
     * 
     * Cache behavior:
     * - First call: Cache MISS - fetches from database and caches result
     * - Subsequent calls: Cache HIT - returns from cache (faster)
     * 
     * @param id Item ID
     * @return Optional containing the item if found
     */
    @Cacheable(value = "dataItems", key = "#id")
    public Optional<DataItem> getItem(String id) {
        long startTime = System.currentTimeMillis();

        Optional<DataItem> item = repository.findById(id);
        long duration = System.currentTimeMillis() - startTime;

        if (item.isPresent()) {
            logger.debug("Cache MISS: dataItems[{}] - fetched from database (duration: {}ms)", id, duration);
            logger.info("Retrieved item '{}' from database", id);
        } else {
            logger.debug("Item with ID '{}' not found in database", id);
        }

        return item;
    }

    /**
     * Get all data items with caching.
     * 
     * Cache behavior:
     * - Empty results are NOT cached (unless = "#result.isEmpty()")
     * - Cache is evicted when items are created, updated, or deleted
     * 
     * @return List of all DataItems
     */
    @Cacheable(value = "allDataItems", unless = "#result.isEmpty()")
    public List<DataItem> getAllItems() {
        long startTime = System.currentTimeMillis();

        List<DataItem> items = repository.findAll();
        long duration = System.currentTimeMillis() - startTime;

        logger.debug("Cache MISS: allDataItems - fetched {} items from database (duration: {}ms)",
                items.size(), duration);
        logger.info("Retrieved {} items from database", items.size());

        return items;
    }

    /**
     * Update an existing data item and refresh cache.
     * 
     * @param id   Item ID
     * @param item Updated item data
     * @return Updated DataItem
     * @throws NoSuchElementException if item not found
     */
    @CachePut(value = "dataItems", key = "#id")
    @CacheEvict(value = "allDataItems", allEntries = true)
    public DataItem updateItem(String id, DataItem item) {
        long startTime = System.currentTimeMillis();

        Optional<DataItem> existingItem = repository.findById(id);
        if (existingItem.isEmpty()) {
            logger.warn("Attempted to update non-existent item with ID '{}'", id);
            throw new NoSuchElementException("Item not found with ID: " + id);
        }

        DataItem itemToUpdate = existingItem.get();
        itemToUpdate.setName(item.getName());
        itemToUpdate.setDescription(item.getDescription());
        itemToUpdate.setTimestamp(System.currentTimeMillis());

        DataItem updatedItem = repository.save(itemToUpdate);
        long duration = System.currentTimeMillis() - startTime;

        logger.info("Updated item with ID '{}' and refreshed cache (duration: {}ms)", id, duration);
        logger.debug("Cache PUT: dataItems[{}] = {}", id, updatedItem.getName());
        logger.debug("Cache EVICT: allDataItems (all entries)");

        return updatedItem;
    }

    /**
     * Delete a data item and evict from cache.
     * 
     * @param id Item ID to delete
     * @return true if deleted, false if not found
     */
    @CacheEvict(value = { "dataItems", "allDataItems" }, key = "#id")
    public boolean deleteItem(String id) {
        long startTime = System.currentTimeMillis();

        if (repository.existsById(id)) {
            repository.deleteById(id);
            long duration = System.currentTimeMillis() - startTime;

            logger.info("Deleted item with ID '{}' and evicted from cache (duration: {}ms)", id, duration);
            logger.debug("Cache EVICT: dataItems[{}]", id);
            logger.debug("Cache EVICT: allDataItems[{}]", id);

            return true;
        }

        logger.debug("Attempted to delete non-existent item with ID '{}'", id);
        return false;
    }

    /**
     * Get the total count of items.
     * 
     * Note: This method is not cached as it's a simple count operation.
     * 
     * @return Total number of items
     */
    public int getItemCount() {
        long count = repository.count();
        logger.debug("Retrieved item count: {}", count);
        return (int) count;
    }
}
