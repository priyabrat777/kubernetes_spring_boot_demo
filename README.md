# Kubernetes Spring Boot Demo

A complete Spring Boot application demonstrating deployment on Kubernetes using Minikube. This project showcases various Kubernetes features including Deployments, Services, ConfigMaps, Secrets, HorizontalPodAutoscaler, and PersistentVolumes.

## 📋 Table of Contents

- [Features](#features)
- [Prerequisites](#prerequisites)
- [Project Structure](#project-structure)
- [Quick Start](#quick-start)
- [Detailed Setup](#detailed-setup)
- [Testing the Application](#testing-the-application)
- [Kubernetes Features Demonstrated](#kubernetes-features-demonstrated)
- [Useful Commands](#useful-commands)
- [Troubleshooting](#troubleshooting)

## ✨ Features

### Application Features
- RESTful API with multiple endpoints
- Health check endpoints (liveness and readiness)
- Configuration management via ConfigMap
- Secret management for sensitive data
- In-memory CRUD operations
- Spring Boot Actuator integration

### Kubernetes Features
- **Namespace**: Isolated environment for the application
- **Deployment**: 2 replicas with rolling update strategy
- **Services**: ClusterIP and NodePort for different access patterns
- **ConfigMap**: External configuration management
- **Secret**: Secure storage for sensitive data
- **HorizontalPodAutoscaler**: Auto-scaling based on CPU/memory
- **PersistentVolume**: Storage demonstration
- **Health Probes**: Liveness and readiness checks
- **Resource Management**: CPU and memory limits/requests

## 🔧 Prerequisites

Before you begin, ensure you have the following installed:

- **Java 21** - [Download](https://adoptium.net/)
- **Maven 3.9+** - [Download](https://maven.apache.org/download.cgi)
- **Docker** - [Download](https://www.docker.com/products/docker-desktop)
- **Minikube** - [Installation Guide](https://minikube.sigs.k8s.io/docs/start/)
- **kubectl** - [Installation Guide](https://kubernetes.io/docs/tasks/tools/)

Verify installations:
```bash
java -version
mvn -version
docker --version
minikube version
kubectl version --client
```

## 📁 Project Structure

```
kubernetes_spring_boot_demo/
├── src/
│   └── main/
│       ├── java/com/demo/k8s/
│       │   ├── K8sDemoApplication.java
│       │   ├── controller/
│       │   │   └── DemoController.java
│       │   ├── model/
│       │   │   └── DataItem.java
│       │   └── service/
│       │       └── DataService.java
│       └── resources/
│           └── application.properties
├── k8s/
│   ├── namespace.yaml
│   ├── configmap.yaml
│   ├── secret.yaml
│   ├── deployment.yaml
│   ├── service.yaml
│   ├── hpa.yaml
│   └── pv-pvc.yaml
├── Dockerfile
├── .dockerignore
├── pom.xml
└── README.md
```

## 🚀 Quick Start

### 1. Build the Application
```bash
# Navigate to project directory
cd /Users/priyabrat_mac/workspaces/kubernetes_spring_boot_demo

# Build the JAR file
mvn clean package
```

### 2. Build Docker Image
```bash
# Build the Docker image
docker build -t spring-boot-k8s-demo:latest .
```

### 3. Start Minikube
```bash
# Start Minikube cluster
minikube start

# Load the Docker image into Minikube
minikube image load spring-boot-k8s-demo:latest
```

### 4. Deploy to Kubernetes
```bash
# Apply all Kubernetes manifests
kubectl apply -f k8s/

# Wait for pods to be ready
kubectl wait --for=condition=ready pod -l app=spring-boot-demo -n spring-boot-demo --timeout=120s
```

### 5. Access the Application
```bash
# Get the Minikube service URL
minikube service spring-boot-nodeport -n spring-boot-demo

# Or use port forwarding
kubectl port-forward -n spring-boot-demo svc/spring-boot-service 8080:80
```

## 📖 Detailed Setup

### Step 1: Build the Spring Boot Application

```bash
# Clean and build the project
mvn clean package

# The JAR file will be created at: target/k8s-spring-boot-demo-1.0.0.jar
```

### Step 2: Create Docker Image

```bash
# Build the Docker image with multi-stage build
docker build -t spring-boot-k8s-demo:latest .

# Verify the image
docker images | grep spring-boot-k8s-demo

# Optional: Test locally
docker run -p 8080:8080 spring-boot-k8s-demo:latest
```

### Step 3: Setup Minikube

```bash
# Start Minikube with sufficient resources
minikube start --cpus=4 --memory=4096

# Enable metrics server for HPA
minikube addons enable metrics-server

# Load the Docker image into Minikube
minikube image load spring-boot-k8s-demo:latest

# Verify the image is loaded
minikube image ls | grep spring-boot-k8s-demo
```

### Step 4: Deploy to Kubernetes

```bash
# Apply resources in order
kubectl apply -f k8s/namespace.yaml
kubectl apply -f k8s/configmap.yaml
kubectl apply -f k8s/secret.yaml
kubectl apply -f k8s/pv-pvc.yaml
kubectl apply -f k8s/deployment.yaml
kubectl apply -f k8s/service.yaml
kubectl apply -f k8s/hpa.yaml

# Or apply all at once
kubectl apply -f k8s/

# Check deployment status
kubectl get all -n spring-boot-demo

# Watch pods coming up
kubectl get pods -n spring-boot-demo -w
```

## 🧪 Testing the Application

### Access Methods

#### Method 1: Minikube Service (Recommended)
```bash
# This will open the service in your default browser
minikube service spring-boot-nodeport -n spring-boot-demo

# Or get the URL
minikube service spring-boot-nodeport -n spring-boot-demo --url
```

#### Method 2: Port Forwarding
```bash
# Forward local port to service
kubectl port-forward -n spring-boot-demo svc/spring-boot-service 8080:80

# Access at http://localhost:8080
```

#### Method 3: NodePort Direct Access
```bash
# Get Minikube IP
minikube ip

# Access at http://<minikube-ip>:30080
```

### API Endpoints

Once you have the URL (e.g., `http://192.168.49.2:30080`), test these endpoints:

```bash
# Replace <URL> with your actual URL

# Hello endpoint
curl <URL>/api/hello

# Application info
curl <URL>/api/info

# Configuration values (from ConfigMap)
curl <URL>/api/config

# Health check
curl <URL>/api/health

# Get all data items
curl <URL>/api/data

# Get specific data item
curl <URL>/api/data/1

# Create new data item
curl -X POST <URL>/api/data \
  -H "Content-Type: application/json" \
  -d '{"name":"Test Item","description":"Created via API"}'

# Delete data item
curl -X DELETE <URL>/api/data/1

# Actuator health endpoint
curl <URL>/actuator/health
```

## 🎯 Kubernetes Features Demonstrated

### 1. **Namespaces**
```bash
# View namespace
kubectl get namespace spring-boot-demo

# View all resources in namespace
kubectl get all -n spring-boot-demo
```

### 2. **ConfigMaps**
```bash
# View ConfigMap
kubectl get configmap -n spring-boot-demo
kubectl describe configmap spring-boot-config -n spring-boot-demo

# Test: Access /api/config endpoint to see injected values
```

### 3. **Secrets**
```bash
# View Secret
kubectl get secret -n spring-boot-demo
kubectl describe secret spring-boot-secret -n spring-boot-demo

# Decode secret values
kubectl get secret spring-boot-secret -n spring-boot-demo -o jsonpath='{.data.database-password}' | base64 -d
```

### 4. **Deployments**
```bash
# View deployment
kubectl get deployment -n spring-boot-demo
kubectl describe deployment spring-boot-demo -n spring-boot-demo

# Scale deployment
kubectl scale deployment spring-boot-demo --replicas=3 -n spring-boot-demo

# Update image (rolling update)
kubectl set image deployment/spring-boot-demo spring-boot-app=spring-boot-k8s-demo:v2 -n spring-boot-demo

# Rollback
kubectl rollout undo deployment/spring-boot-demo -n spring-boot-demo

# View rollout history
kubectl rollout history deployment/spring-boot-demo -n spring-boot-demo
```

### 5. **Services**
```bash
# View services
kubectl get svc -n spring-boot-demo

# Describe ClusterIP service
kubectl describe svc spring-boot-service -n spring-boot-demo

# Describe NodePort service
kubectl describe svc spring-boot-nodeport -n spring-boot-demo
```

### 6. **HorizontalPodAutoscaler**
```bash
# View HPA
kubectl get hpa -n spring-boot-demo

# Watch HPA in action
kubectl get hpa -n spring-boot-demo -w

# Generate load to trigger scaling
kubectl run -it --rm load-generator --image=busybox -n spring-boot-demo -- /bin/sh
# Inside the pod:
while true; do wget -q -O- http://spring-boot-service/api/hello; done
```

### 7. **PersistentVolumes**
```bash
# View PV and PVC
kubectl get pv
kubectl get pvc -n spring-boot-demo

# Describe PVC
kubectl describe pvc spring-boot-pvc -n spring-boot-demo
```

### 8. **Health Probes**
```bash
# View pod details to see probe configuration
kubectl describe pod -l app=spring-boot-demo -n spring-boot-demo

# Check probe endpoints
kubectl exec -n spring-boot-demo <pod-name> -- wget -qO- http://localhost:8080/actuator/health/liveness
kubectl exec -n spring-boot-demo <pod-name> -- wget -qO- http://localhost:8080/actuator/health/readiness
```

### 9. **Resource Management**
```bash
# View resource usage
kubectl top pods -n spring-boot-demo
kubectl top nodes

# View resource limits
kubectl describe pod -l app=spring-boot-demo -n spring-boot-demo | grep -A 5 "Limits"
```

### 10. **Logs and Debugging**
```bash
# View logs
kubectl logs -l app=spring-boot-demo -n spring-boot-demo

# Follow logs
kubectl logs -f -l app=spring-boot-demo -n spring-boot-demo

# View logs from specific container
kubectl logs <pod-name> -c spring-boot-app -n spring-boot-demo

# Execute commands in pod
kubectl exec -it <pod-name> -n spring-boot-demo -- /bin/sh
```

## 📝 Useful Commands

### Monitoring
```bash
# Watch all resources
kubectl get all -n spring-boot-demo -w

# Get pod details
kubectl get pods -n spring-boot-demo -o wide

# View events
kubectl get events -n spring-boot-demo --sort-by='.lastTimestamp'

# View resource usage
kubectl top pods -n spring-boot-demo
```

### Debugging
```bash
# Describe pod
kubectl describe pod <pod-name> -n spring-boot-demo

# Get pod logs
kubectl logs <pod-name> -n spring-boot-demo

# Execute shell in pod
kubectl exec -it <pod-name> -n spring-boot-demo -- /bin/sh

# Port forward to pod
kubectl port-forward <pod-name> 8080:8080 -n spring-boot-demo
```

### Cleanup
```bash
# Delete all resources
kubectl delete -f k8s/

# Or delete namespace (removes everything)
kubectl delete namespace spring-boot-demo

# Stop Minikube
minikube stop

# Delete Minikube cluster
minikube delete
```

## 🔍 Troubleshooting

### Pods not starting
```bash
# Check pod status
kubectl get pods -n spring-boot-demo

# Describe pod to see events
kubectl describe pod <pod-name> -n spring-boot-demo

# Check logs
kubectl logs <pod-name> -n spring-boot-demo

# Common issues:
# - Image not found: Ensure image is loaded in Minikube
# - ImagePullBackOff: Check imagePullPolicy is set to "Never"
```

### Image not found in Minikube
```bash
# Rebuild and load image
docker build -t spring-boot-k8s-demo:latest .
minikube image load spring-boot-k8s-demo:latest

# Verify image is loaded
minikube image ls | grep spring-boot-k8s-demo

# Restart deployment
kubectl rollout restart deployment/spring-boot-demo -n spring-boot-demo
```

### Service not accessible
```bash
# Check service
kubectl get svc -n spring-boot-demo

# Check endpoints
kubectl get endpoints -n spring-boot-demo

# Verify pods are running
kubectl get pods -n spring-boot-demo

# Test from within cluster
kubectl run -it --rm debug --image=busybox -n spring-boot-demo -- wget -qO- http://spring-boot-service/api/hello
```

### HPA not working
```bash
# Ensure metrics-server is enabled
minikube addons enable metrics-server

# Wait a few minutes for metrics to be available
kubectl get --raw /apis/metrics.k8s.io/v1beta1/nodes

# Check HPA status
kubectl describe hpa spring-boot-hpa -n spring-boot-demo
```

### PersistentVolume issues
```bash
# Check PV status
kubectl get pv

# Check PVC status
kubectl get pvc -n spring-boot-demo

# Describe PVC for events
kubectl describe pvc spring-boot-pvc -n spring-boot-demo
```

## 🎓 Learning Resources

- [Kubernetes Documentation](https://kubernetes.io/docs/)
- [Spring Boot Documentation](https://spring.io/projects/spring-boot)
- [Minikube Documentation](https://minikube.sigs.k8s.io/docs/)
- [Docker Documentation](https://docs.docker.com/)

## 📄 License

This is a demo project for learning purposes.

## 🤝 Contributing

Feel free to fork this project and experiment with different Kubernetes features!

---

**Happy Learning! 🚀**
