# PostgreSQL Database Setup and Deployment Guide

## Overview

This guide covers the PostgreSQL database integration with the Spring Boot application in Kubernetes.

## Database Architecture

- **Database**: PostgreSQL 16 (Alpine)
- **Storage**: Persistent storage using PersistentVolume
- **Connection**: Spring Boot connects via Kubernetes Service DNS (`postgres-service`)
- **Credentials**: Stored securely in Kubernetes Secrets
- **Initialization**: Automatic schema creation and sample data insertion

## Quick Start with PostgreSQL

### 1. Build the Updated Application

```bash
cd /Users/priyabrat_mac/workspaces/kubernetes_spring_boot_demo

# Build with PostgreSQL dependencies
mvn clean package

# Build Docker image
docker build -t spring-boot-k8s-demo:latest .
```

### 2. Start Minikube and Load Images

```bash
# Start Minikube
minikube start --cpus=4 --memory=4096

# Enable metrics server
minikube addons enable metrics-server

# Load Spring Boot image
minikube image load spring-boot-k8s-demo:latest
```

### 3. Deploy PostgreSQL and Application

```bash
# Deploy in correct order
kubectl apply -f k8s/namespace.yaml
kubectl apply -f k8s/configmap.yaml
kubectl apply -f k8s/postgres-configmap.yaml
kubectl apply -f k8s/secret.yaml
kubectl apply -f k8s/postgres-pv-pvc.yaml
kubectl apply -f k8s/pv-pvc.yaml
kubectl apply -f k8s/postgres-deployment.yaml
kubectl apply -f k8s/postgres-service.yaml

# Wait for PostgreSQL to be ready
kubectl wait --for=condition=ready pod -l app=postgres -n spring-boot-demo --timeout=120s

# Deploy Spring Boot application
kubectl apply -f k8s/deployment.yaml
kubectl apply -f k8s/service.yaml
kubectl apply -f k8s/hpa.yaml

# Wait for application to be ready
kubectl wait --for=condition=ready pod -l app=spring-boot-demo -n spring-boot-demo --timeout=180s
```

## Database Configuration

### ConfigMap (`postgres-configmap.yaml`)
```yaml
POSTGRES_DB: demodb
POSTGRES_USER: demouser
```

### Secret (`secret.yaml`)
```yaml
postgres-password: ZGVtb3Bhc3M=  # base64("demopass")
postgres-username: ZGVtb3VzZXI=  # base64("demouser")
```

### Connection String
```
jdbc:postgresql://postgres-service:5432/demodb
```

## Testing Database Connectivity

### 1. Check PostgreSQL Pod Status

```bash
# Get PostgreSQL pod
kubectl get pods -n spring-boot-demo -l app=postgres

# Check PostgreSQL logs
kubectl logs -l app=postgres -n spring-boot-demo
```

### 2. Connect to PostgreSQL Directly

```bash
# Get PostgreSQL pod name
POSTGRES_POD=$(kubectl get pod -n spring-boot-demo -l app=postgres -o jsonpath='{.items[0].metadata.name}')

# Connect to PostgreSQL
kubectl exec -it $POSTGRES_POD -n spring-boot-demo -- psql -U demouser -d demodb

# Inside psql:
\dt                          # List tables
SELECT * FROM data_items;    # Query data
\q                           # Quit
```

### 3. Test via Spring Boot API

```bash
# Get service URL
minikube service spring-boot-nodeport -n spring-boot-demo --url

# Test endpoints (replace <URL> with actual URL)
# Get all data (should show items from database)
curl <URL>/api/data

# Create new item (persists to database)
curl -X POST <URL>/api/data \
  -H "Content-Type: application/json" \
  -d '{"name":"Database Test","description":"Stored in PostgreSQL"}'

# Verify persistence by restarting pods
kubectl delete pod -l app=spring-boot-demo -n spring-boot-demo

# Wait for new pods
kubectl wait --for=condition=ready pod -l app=spring-boot-demo -n spring-boot-demo --timeout=120s

# Data should still be there
curl <URL>/api/data
```

## Database Schema

The application uses Hibernate to auto-create the schema. The `data_items` table structure:

```sql
CREATE TABLE data_items (
    id VARCHAR(36) PRIMARY KEY,
    name VARCHAR(255) NOT NULL,
    description VARCHAR(1000),
    timestamp BIGINT NOT NULL
);
```

## Monitoring Database

### View Database Logs

```bash
# Follow PostgreSQL logs
kubectl logs -f -l app=postgres -n spring-boot-demo

# View Spring Boot database logs
kubectl logs -f -l app=spring-boot-demo -n spring-boot-demo | grep -i postgres
```

### Check Database Resources

```bash
# Check PostgreSQL resource usage
kubectl top pod -l app=postgres -n spring-boot-demo

# Check persistent volume
kubectl get pv postgres-pv
kubectl get pvc postgres-pvc -n spring-boot-demo
```

### Database Health Check

```bash
# Check PostgreSQL readiness
kubectl exec -n spring-boot-demo -l app=postgres -- pg_isready -U demouser -d demodb
```

## Troubleshooting Database Issues

### PostgreSQL Pod Not Starting

```bash
# Check pod status
kubectl get pods -n spring-boot-demo -l app=postgres

# Describe pod for events
kubectl describe pod -l app=postgres -n spring-boot-demo

# Check logs
kubectl logs -l app=postgres -n spring-boot-demo

# Common issues:
# - PVC not bound: Check PV and PVC status
# - Insufficient resources: Check resource limits
```

### Spring Boot Cannot Connect to Database

```bash
# Check if PostgreSQL service exists
kubectl get svc postgres-service -n spring-boot-demo

# Check if PostgreSQL is ready
kubectl get pods -l app=postgres -n spring-boot-demo

# Check Spring Boot logs for connection errors
kubectl logs -l app=spring-boot-demo -n spring-boot-demo | grep -i "connection\|postgres\|database"

# Verify init container completed
kubectl describe pod -l app=spring-boot-demo -n spring-boot-demo | grep -A 10 "Init Containers"

# Test connectivity from Spring Boot pod
kubectl exec -it <spring-boot-pod> -n spring-boot-demo -- sh
# Inside pod:
nc -zv postgres-service 5432
```

### Database Connection Timeout

```bash
# Check if PostgreSQL is accepting connections
kubectl exec -it <postgres-pod> -n spring-boot-demo -- psql -U demouser -d demodb -c "SELECT 1;"

# Verify credentials in secret
kubectl get secret spring-boot-secret -n spring-boot-demo -o jsonpath='{.data.postgres-username}' | base64 -d
kubectl get secret spring-boot-secret -n spring-boot-demo -o jsonpath='{.data.postgres-password}' | base64 -d

# Check ConfigMap for database URL
kubectl get configmap spring-boot-config -n spring-boot-demo -o yaml | grep DATABASE_URL
```

### Data Not Persisting

```bash
# Check if PVC is bound
kubectl get pvc postgres-pvc -n spring-boot-demo

# Check PV status
kubectl get pv postgres-pv

# Verify volume mount in pod
kubectl describe pod -l app=postgres -n spring-boot-demo | grep -A 5 "Mounts"

# Check if data directory exists
kubectl exec -it <postgres-pod> -n spring-boot-demo -- ls -la /var/lib/postgresql/data/
```

## Database Backup and Restore

### Backup Database

```bash
# Create backup
kubectl exec -it <postgres-pod> -n spring-boot-demo -- pg_dump -U demouser demodb > backup.sql

# Or backup to pod and copy out
kubectl exec <postgres-pod> -n spring-boot-demo -- pg_dump -U demouser demodb > /tmp/backup.sql
kubectl cp spring-boot-demo/<postgres-pod>:/tmp/backup.sql ./backup.sql
```

### Restore Database

```bash
# Copy backup to pod
kubectl cp ./backup.sql spring-boot-demo/<postgres-pod>:/tmp/backup.sql

# Restore
kubectl exec -it <postgres-pod> -n spring-boot-demo -- psql -U demouser -d demodb -f /tmp/backup.sql
```

## Scaling Considerations

**Note**: The current PostgreSQL deployment uses a single replica. For production:

1. Consider using **StatefulSet** instead of Deployment
2. Implement **PostgreSQL replication** for high availability
3. Use **PostgreSQL Operator** for advanced management
4. Consider managed database services (AWS RDS, Google Cloud SQL, etc.)

## Database Maintenance

### Reset Database

```bash
# Delete PostgreSQL pod (data persists in PV)
kubectl delete pod -l app=postgres -n spring-boot-demo

# Wait for new pod
kubectl wait --for=condition=ready pod -l app=postgres -n spring-boot-demo --timeout=120s

# Data should still be there
```

### Clean Database Data

```bash
# Delete PVC (this will delete all data)
kubectl delete pvc postgres-pvc -n spring-boot-demo

# Delete PV
kubectl delete pv postgres-pv

# Recreate
kubectl apply -f k8s/postgres-pv-pvc.yaml
kubectl delete pod -l app=postgres -n spring-boot-demo
```

## Performance Tuning

### PostgreSQL Configuration

Edit `postgres-configmap.yaml` to add PostgreSQL configuration:

```yaml
data:
  POSTGRES_DB: demodb
  POSTGRES_USER: demouser
  # Add PostgreSQL configuration
  max_connections: "100"
  shared_buffers: "128MB"
```

### Connection Pooling

Spring Boot uses HikariCP by default. Configure in `application.properties`:

```properties
spring.datasource.hikari.maximum-pool-size=10
spring.datasource.hikari.minimum-idle=5
spring.datasource.hikari.connection-timeout=20000
```

## Summary

✅ PostgreSQL 16 running in Kubernetes
✅ Persistent storage for data
✅ Automatic schema creation via Hibernate
✅ Secure credential management
✅ Health checks and monitoring
✅ Init container ensures database readiness
✅ Service discovery via Kubernetes DNS

The application now uses PostgreSQL for data persistence instead of in-memory storage!
