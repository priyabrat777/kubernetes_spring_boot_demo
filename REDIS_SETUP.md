# Redis Caching Setup Guide

This document provides comprehensive information about the Redis caching implementation in the Kubernetes Spring Boot Demo application.

## Architecture Overview

The application uses **Redis** as a distributed cache layer to improve performance and reduce database load. The caching implementation follows enterprise best practices with:

- **Spring Cache Abstraction**: Declarative caching using annotations
- **Redis as Cache Store**: Distributed, in-memory cache with persistence
- **Graceful Degradation**: Application continues to work if Redis is unavailable
- **JSON Serialization**: Human-readable cache entries
- **Configurable TTL**: Default 10 minutes, customizable per environment

### Architecture Diagram

```
┌─────────────────┐
│  Spring Boot    │
│  Application    │
│                 │
│  ┌───────────┐  │
│  │  Service  │  │──┐
│  │   Layer   │  │  │ @Cacheable
│  └───────────┘  │  │ @CachePut
│                 │  │ @CacheEvict
└─────────────────┘  │
         │           │
         ▼           ▼
    ┌─────────────────────┐
    │   Redis Cache       │
    │   (In-Memory)       │
    │                     │
    │  ┌──────────────┐   │
    │  │ dataItems    │   │
    │  │ allDataItems │   │
    │  └──────────────┘   │
    └─────────────────────┘
              │
              ▼
         [Persistence]
         (RDB + AOF)
```

## Deployment Instructions

### Prerequisites

- Kubernetes cluster (Minikube for local development)
- kubectl configured
- Docker for building images

### Step-by-Step Deployment

1. **Build the Application**
   ```bash
   cd /Users/priyabrat_mac/workspaces/kubernetes_spring_boot_demo
   mvn clean package -DskipTests
   ```

2. **Build Docker Image**
   ```bash
   eval $(minikube docker-env)
   docker build -t spring-boot-k8s-demo:latest .
   ```

3. **Deploy to Kubernetes**
   ```bash
   ./deploy.sh
   ```

   The script will deploy in this order:
   - Namespace
   - ConfigMaps (including Redis config)
   - Secrets
   - Persistent Volumes (PostgreSQL, Redis, Application)
   - Redis (StatefulSet + Service)
   - PostgreSQL (Deployment + Service)
   - Spring Boot Application (Deployment + Service + HPA)

4. **Verify Deployment**
   ```bash
   kubectl get all -n spring-boot-demo
   ```

   Expected output should show:
   - `redis-0` pod in Running state
   - `postgres-*` pod in Running state
   - `spring-boot-demo-*` pods in Running state

### Accessing the Application

```bash
# Option 1: Port forwarding
kubectl port-forward -n spring-boot-demo svc/spring-boot-service 8080:80

# Option 2: Minikube service
minikube service spring-boot-nodeport -n spring-boot-demo
```

## Cache Operations Examples

### 1. Check Cache Statistics

```bash
curl http://localhost:8080/api/cache/stats
```

**Response:**
```json
{
  "cacheCount": 2,
  "cacheNames": ["dataItems", "allDataItems"],
  "cacheSizes": {
    "dataItems": 5,
    "allDataItems": 1
  },
  "redisConnected": true,
  "timestamp": 1700000000000
}
```

### 2. List All Cache Keys

```bash
curl http://localhost:8080/api/cache/keys
```

### 3. Create Data Item (Caches Automatically)

```bash
curl -X POST http://localhost:8080/api/data \
  -H "Content-Type: application/json" \
  -d '{
    "name": "Test Item",
    "description": "This will be cached"
  }'
```

### 4. Get Data Item (Cache Hit/Miss)

```bash
# First call - Cache MISS (fetches from database)
curl http://localhost:8080/api/data/{id}

# Second call - Cache HIT (returns from Redis)
curl http://localhost:8080/api/data/{id}
```

Check logs to see cache operations:
```bash
kubectl logs -f -l app=spring-boot-demo -n spring-boot-demo | grep -i cache
```

### 5. Clear All Caches

```bash
curl -X DELETE http://localhost:8080/api/cache/clear
```

### 6. Clear Specific Cache

```bash
curl -X DELETE http://localhost:8080/api/cache/clear/dataItems
```

### 7. Evict Specific Cache Entry

```bash
curl -X DELETE http://localhost:8080/api/cache/evict/dataItems/{id}
```

### 8. Get Redis Server Info

```bash
curl http://localhost:8080/api/cache/info
```

### 9. Update Cache TTL

```bash
curl -X PUT http://localhost:8080/api/cache/ttl/dataItems/{id} \
  -H "Content-Type: application/json" \
  -d '{"ttl": 300}'
```

### 10. Check Application Info (includes cache status)

```bash
curl http://localhost:8080/api/info
```

## Monitoring Cache Performance

### View Cache Logs

```bash
# View all cache-related logs
kubectl logs -f -l app=spring-boot-demo -n spring-boot-demo | grep -i cache

# View Redis logs
kubectl logs -f redis-0 -n spring-boot-demo
```

### Connect to Redis CLI

```bash
kubectl exec -it redis-0 -n spring-boot-demo -- redis-cli

# Inside Redis CLI:
> PING
> KEYS k8sdemo:*
> GET "k8sdemo:dataItems::{id}"
> TTL "k8sdemo:dataItems::{id}"
> INFO
> DBSIZE
```

### Monitor Cache Hit/Miss Ratio

Watch the application logs for cache operations:
- `Cache MISS` - Data fetched from database
- `Cache HIT` - Data returned from Redis (faster)
- `Cache PUT` - Data added to cache
- `Cache EVICT` - Data removed from cache

## Troubleshooting Guide

### Redis Pod Not Starting

```bash
# Check pod status
kubectl get pods -n spring-boot-demo -l app=redis

# Check pod logs
kubectl logs redis-0 -n spring-boot-demo

# Describe pod for events
kubectl describe pod redis-0 -n spring-boot-demo
```

**Common Issues:**
- PVC not bound: Check PV/PVC status
- Image pull error: Ensure network connectivity
- Configuration error: Check ConfigMap

### Application Can't Connect to Redis

```bash
# Test Redis connectivity from application pod
kubectl exec -it <spring-boot-pod> -n spring-boot-demo -- sh
nc -zv redis-service 6379

# Check Redis service
kubectl get svc redis-service -n spring-boot-demo

# Check endpoints
kubectl get endpoints redis-service -n spring-boot-demo
```

### Cache Not Working

1. **Check Redis Connection**
   ```bash
   curl http://localhost:8080/api/cache/info
   ```

2. **Check Application Logs**
   ```bash
   kubectl logs -f -l app=spring-boot-demo -n spring-boot-demo | grep -E "Redis|Cache"
   ```

3. **Verify Configuration**
   ```bash
   kubectl get configmap spring-boot-config -n spring-boot-demo -o yaml
   ```

### Redis Data Persistence Issues

```bash
# Check PVC status
kubectl get pvc redis-pvc -n spring-boot-demo

# Check if data directory is mounted
kubectl exec -it redis-0 -n spring-boot-demo -- ls -la /data

# Check Redis persistence settings
kubectl exec -it redis-0 -n spring-boot-demo -- redis-cli CONFIG GET save
kubectl exec -it redis-0 -n spring-boot-demo -- redis-cli CONFIG GET appendonly
```

## Performance Tuning

### Adjust Cache TTL

Update ConfigMap:
```yaml
CACHE_TTL: "300000"  # 5 minutes in milliseconds
```

Then restart pods:
```bash
kubectl rollout restart deployment spring-boot-demo -n spring-boot-demo
```

### Adjust Redis Memory

Update `redis-configmap.yaml`:
```conf
maxmemory 500mb  # Increase from 200mb
```

Apply changes:
```bash
kubectl apply -f k8s/redis-configmap.yaml
kubectl delete pod redis-0 -n spring-boot-demo  # StatefulSet will recreate
```

### Connection Pool Tuning

Update `application.properties`:
```properties
spring.data.redis.jedis.pool.max-active=50
spring.data.redis.jedis.pool.max-idle=20
spring.data.redis.jedis.pool.min-idle=10
```

## Configuration Reference

### Environment Variables

| Variable | Default | Description |
|----------|---------|-------------|
| `REDIS_HOST` | `localhost` | Redis server hostname |
| `REDIS_PORT` | `6379` | Redis server port |
| `CACHE_TTL` | `600000` | Cache TTL in milliseconds (10 min) |

### Cache Names

| Cache Name | Purpose | TTL | Eviction Policy |
|------------|---------|-----|-----------------|
| `dataItems` | Individual data items by ID | 10 min | On update/delete |
| `allDataItems` | List of all items | 10 min | On any create/update/delete |

### Redis Configuration

- **Persistence**: RDB snapshots + AOF
- **Memory Policy**: `allkeys-lru` (evict least recently used)
- **Max Memory**: 200MB (configurable)
- **Port**: 6379
- **Storage**: 1Gi persistent volume

## Best Practices

1. **Monitor Cache Hit Ratio**: Aim for >80% hit ratio for frequently accessed data
2. **Set Appropriate TTL**: Balance between freshness and performance
3. **Use Selective Caching**: Don't cache everything, only frequently accessed data
4. **Handle Cache Failures**: Application should work without cache
5. **Clear Cache on Updates**: Ensure data consistency
6. **Monitor Memory Usage**: Prevent Redis OOM errors
7. **Use Persistence**: Enable RDB and AOF for data durability
8. **Regular Backups**: Backup Redis data for disaster recovery

## Production Considerations

For production deployments, consider:

1. **Redis Sentinel**: High availability with automatic failover
2. **Redis Cluster**: Horizontal scaling and sharding
3. **Separate Redis Instances**: Different instances for different purposes
4. **SSL/TLS**: Encrypt Redis connections
5. **Authentication**: Enable Redis password protection
6. **Resource Limits**: Set appropriate CPU/memory limits
7. **Monitoring**: Use Prometheus + Grafana for metrics
8. **Backup Strategy**: Regular automated backups
9. **Disaster Recovery**: Test recovery procedures

## Additional Resources

- [Spring Cache Documentation](https://docs.spring.io/spring-framework/docs/current/reference/html/integration.html#cache)
- [Spring Data Redis](https://spring.io/projects/spring-data-redis)
- [Redis Documentation](https://redis.io/documentation)
- [Redis Best Practices](https://redis.io/topics/best-practices)
