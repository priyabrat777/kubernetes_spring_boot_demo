# Kubernetes Cheatsheet & YAML Reference

This document serves as a one-stop guide for writing Kubernetes configuration files. It covers common resources, syntax, and best practices.

## Table of Contents
- [Core Resources](#core-resources)
  - [Pod](#pod)
  - [Deployment](#deployment)
  - [Service](#service)
- [Configuration & Storage](#configuration--storage)
  - [ConfigMap](#configmap)
  - [Secret](#secret)
  - [PersistentVolumeClaim (PVC)](#persistentvolumeclaim-pvc)
- [Health Checks (Probes)](#health-checks-probes)
- [Resources (CPU/Memory)](#resources-cpumemory)
- [Common Commands](#common-commands)

---

## Core Resources

### Pod
The smallest deployable unit in Kubernetes.

```yaml
apiVersion: v1
kind: Pod
metadata:
  name: my-pod
  labels:
    app: my-app
spec:
  containers:
    - name: my-container
      image: nginx:latest
      ports:
        - containerPort: 80
```

### Deployment
Manages a set of replicated Pods.

```yaml
apiVersion: apps/v1
kind: Deployment
metadata:
  name: my-deployment
spec:
  replicas: 3
  selector:
    matchLabels:
      app: my-app
  template:
    metadata:
      labels:
        app: my-app
    spec:
      containers:
        - name: my-container
          image: nginx:1.14.2
          ports:
            - containerPort: 80
```

### Service
Exposes an application running on a set of Pods.

#### ClusterIP (Default - Internal only)
```yaml
apiVersion: v1
kind: Service
metadata:
  name: my-service
spec:
  selector:
    app: my-app
  ports:
    - protocol: TCP
      port: 80        # Port exposed by the Service
      targetPort: 80  # Port on the Container
```

#### NodePort (External access via Node IP)
```yaml
apiVersion: v1
kind: Service
metadata:
  name: my-nodeport-service
spec:
  type: NodePort
  selector:
    app: my-app
  ports:
    - port: 80
      targetPort: 80
      nodePort: 30007 # Optional: range 30000-32767
```

#### LoadBalancer (External access via Cloud Provider LB)
```yaml
apiVersion: v1
kind: Service
metadata:
  name: my-lb-service
spec:
  type: LoadBalancer
  selector:
    app: my-app
  ports:
    - port: 80
      targetPort: 80
```

---

## Configuration & Storage

### ConfigMap
Decouple configuration artifacts from image content.

```yaml
apiVersion: v1
kind: ConfigMap
metadata:
  name: my-config
data:
  # Key-value pairs
  database_url: "postgres://db:5432/mydb"
  debug_mode: "true"
  # File-like content
  config.properties: |
    key1=value1
    key2=value2
```

**Usage in Pod:**
```yaml
env:
  - name: DB_URL
    valueFrom:
      configMapKeyRef:
        name: my-config
        key: database_url
```

### Secret
Store sensitive information (passwords, OAuth tokens, ssh keys).

```yaml
apiVersion: v1
kind: Secret
metadata:
  name: my-secret
type: Opaque
data:
  # Values must be base64 encoded
  # echo -n 'secret_value' | base64
  username: YWRtaW4=
  password: cGFzc3dvcmQ=
```

**Usage in Pod:**
```yaml
env:
  - name: SECRET_USERNAME
    valueFrom:
      secretKeyRef:
        name: my-secret
        key: username
```

### PersistentVolumeClaim (PVC)
Request storage resources.

```yaml
apiVersion: v1
kind: PersistentVolumeClaim
metadata:
  name: my-pvc
spec:
  accessModes:
    - ReadWriteOnce
  resources:
    requests:
      storage: 1Gi
```

**Usage in Pod:**
```yaml
volumes:
  - name: my-storage
    persistentVolumeClaim:
      claimName: my-pvc
containers:
  - name: my-container
    volumeMounts:
      - mountPath: "/data"
        name: my-storage
```

---

## Health Checks (Probes)

Ensure your application is running correctly.

```yaml
livenessProbe:
  httpGet:
    path: /healthz
    port: 8080
  initialDelaySeconds: 3
  periodSeconds: 3

readinessProbe:
  httpGet:
    path: /ready
    port: 8080
  initialDelaySeconds: 5
  periodSeconds: 5

startupProbe:
  httpGet:
    path: /startup
    port: 8080
  failureThreshold: 30
  periodSeconds: 10
```

---

## Resources (CPU/Memory)

Manage container resource usage.

```yaml
resources:
  requests:
    memory: "64Mi"
    cpu: "250m" # 0.25 CPU
  limits:
    memory: "128Mi"
    cpu: "500m" # 0.5 CPU
```

---

## Common Commands

| Action | Command |
| :--- | :--- |
| **Apply Config** | `kubectl apply -f <filename.yaml>` |
| **Get Pods** | `kubectl get pods` |
| **Get All Resources** | `kubectl get all` |
| **Describe Resource** | `kubectl describe <resource_type> <name>` |
| **Logs** | `kubectl logs <pod_name>` |
| **Logs (Follow)** | `kubectl logs -f <pod_name>` |
| **Exec into Pod** | `kubectl exec -it <pod_name> -- /bin/bash` |
| **Delete Resource** | `kubectl delete -f <filename.yaml>` |
| **Port Forward** | `kubectl port-forward <pod_name> 8080:80` |
