#!/bin/bash
# Cleanup script for Spring Boot Kubernetes Demo

set -e

echo "🧹 Cleaning up Spring Boot Kubernetes Demo..."

# Delete all resources
echo "Deleting all Kubernetes resources..."
kubectl delete -f k8s/hpa.yaml --ignore-not-found=true
kubectl delete -f k8s/service.yaml --ignore-not-found=true
kubectl delete -f k8s/deployment.yaml --ignore-not-found=true
kubectl delete -f k8s/postgres-service.yaml --ignore-not-found=true
kubectl delete -f k8s/postgres-deployment.yaml --ignore-not-found=true
kubectl delete -f k8s/pv-pvc.yaml --ignore-not-found=true
kubectl delete -f k8s/postgres-pv-pvc.yaml --ignore-not-found=true
kubectl delete -f k8s/secret.yaml --ignore-not-found=true
kubectl delete -f k8s/postgres-configmap.yaml --ignore-not-found=true
kubectl delete -f k8s/configmap.yaml --ignore-not-found=true
kubectl delete -f k8s/namespace.yaml --ignore-not-found=true

echo "✅ Cleanup completed!"
echo ""
echo "Note: PersistentVolumes may need manual cleanup:"
echo "  kubectl get pv"
echo "  kubectl delete pv <pv-name>"
