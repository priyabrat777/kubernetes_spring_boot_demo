#!/bin/bash
# Deployment script for Spring Boot Kubernetes Demo with PostgreSQL
# This script deploys the application in the correct order

set -e  # Exit on error

echo "🚀 Starting deployment of Spring Boot Kubernetes Demo with PostgreSQL..."

# Colors for output
GREEN='\033[0;32m'
BLUE='\033[0;34m'
NC='\033[0m' # No Color

# Step 1: Create namespace
echo -e "${BLUE}📦 Step 1: Creating namespace...${NC}"
kubectl apply -f k8s/namespace.yaml

# Step 2: Create ConfigMaps
echo -e "${BLUE}⚙️  Step 2: Creating ConfigMaps...${NC}"
kubectl apply -f k8s/configmap.yaml
kubectl apply -f k8s/postgres-configmap.yaml

# Step 3: Create Secrets
echo -e "${BLUE}🔐 Step 3: Creating Secrets...${NC}"
kubectl apply -f k8s/secret.yaml

# Step 4: Create Persistent Volumes
echo -e "${BLUE}💾 Step 4: Creating Persistent Volumes...${NC}"
kubectl apply -f k8s/postgres-pv-pvc.yaml
kubectl apply -f k8s/pv-pvc.yaml

# Step 5: Deploy PostgreSQL
echo -e "${BLUE}🐘 Step 5: Deploying PostgreSQL...${NC}"
kubectl apply -f k8s/postgres-deployment.yaml
kubectl apply -f k8s/postgres-service.yaml

# Wait for PostgreSQL to be ready
echo -e "${BLUE}⏳ Waiting for PostgreSQL to be ready...${NC}"
kubectl wait --for=condition=ready pod -l app=postgres -n spring-boot-demo --timeout=120s

echo -e "${GREEN}✅ PostgreSQL is ready!${NC}"

# Step 6: Deploy Spring Boot Application
echo -e "${BLUE}☕ Step 6: Deploying Spring Boot Application...${NC}"
kubectl apply -f k8s/deployment.yaml
kubectl apply -f k8s/service.yaml
kubectl apply -f k8s/hpa.yaml

# Wait for Spring Boot to be ready
echo -e "${BLUE}⏳ Waiting for Spring Boot application to be ready...${NC}"
kubectl wait --for=condition=ready pod -l app=spring-boot-demo -n spring-boot-demo --timeout=180s

echo -e "${GREEN}✅ Spring Boot application is ready!${NC}"

# Display status
echo -e "\n${GREEN}🎉 Deployment completed successfully!${NC}\n"

echo "📊 Deployment Status:"
kubectl get all -n spring-boot-demo

echo -e "\n🔗 Access the application:"
echo "   Option 1: minikube service spring-boot-nodeport -n spring-boot-demo"
echo "   Option 2: kubectl port-forward -n spring-boot-demo svc/spring-boot-service 8080:80"

echo -e "\n📝 Useful commands:"
echo "   View logs: kubectl logs -f -l app=spring-boot-demo -n spring-boot-demo"
echo "   View PostgreSQL logs: kubectl logs -f -l app=postgres -n spring-boot-demo"
echo "   Connect to DB: kubectl exec -it \$(kubectl get pod -n spring-boot-demo -l app=postgres -o jsonpath='{.items[0].metadata.name}') -n spring-boot-demo -- psql -U demouser -d demodb"
