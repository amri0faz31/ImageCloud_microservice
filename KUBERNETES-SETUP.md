# Kubernetes Setup

Deploy ImageCloud on Minikube with monitoring stack.

## Prerequisites

- Minikube (`brew install minikube`)
- kubectl (`brew install kubectl`)
- Docker Desktop running


## Setup Steps

### 1. Start Cluster

```bash
minikube start --cpus=4 --memory=8192 --driver=docker
kubectl get nodes  # Verify node is Ready
```

### 2. Enable Ingress

```bash
minikube addons enable ingress
kubectl get pods -n ingress-nginx  # Wait for Running status
```

### 3. Build Images

Build all services inside Minikube's Docker daemon:

```bash
eval $(minikube docker-env)

# Auth Service
cd backend/auth-service
mvn clean package -DskipTests
docker build -t imagecloud/auth-service:latest .

# Main Service
cd ../main-service
mvn clean package -DskipTests
docker build -t imagecloud/main-service:latest .

# Conversion Service
cd ../conversion-service
mvn clean package -DskipTests
docker build -t imagecloud/conversion-service:latest .

# Verify all images
docker images | grep imagecloud
```

Run `eval $(minikube docker-env)` in each new terminal session.

### 4. Deploy Resources

```bash
cd /Users/amrifazlul/imageCloud

kubectl apply -f kubernetes/namespace.yaml
kubectl apply -f kubernetes/configmaps.yaml
kubectl apply -f kubernetes/secrets.yaml
kubectl apply -f kubernetes/postgres.yaml
kubectl apply -f kubernetes/postgres-main.yaml
kubectl apply -f kubernetes/rabbitmq.yaml
kubectl apply -f kubernetes/zipkin.yaml
kubectl apply -f kubernetes/prometheus.yaml
kubectl apply -f kubernetes/grafana.yaml
kubectl apply -f kubernetes/alertmanager.yaml
kubectl apply -f kubernetes/auth-service.yaml
kubectl apply -f kubernetes/main-service.yaml
kubectl apply -f kubernetes/conversion-service.yaml
kubectl apply -f kubernetes/ingress.yaml

kubectl get pods -n imagecloud -w  # Watch until all Running
```

### 5. Configure DNS

```bash
minikube ip  # Get IP address
sudo nano /etc/hosts

# Add line (replace with your IP):
192.168.49.2    imagecloud.local
```

### 6. Test Deployment

```bash
curl http://imagecloud.local/api/auth/actuator/health

# Test image upload (after login)
curl -X POST http://imagecloud.local/api/images/upload \
  -H "X-User-Id: 1" \
  -F "file=@test.png" \
  -F "targetFormat=jpg"
```

## Access Services

- Grafana: http://imagecloud.local/grafana (admin/admin)
- Prometheus: http://imagecloud.local/prometheus
- Zipkin: http://imagecloud.local/zipkin
- RabbitMQ: Port-forward to access management UI
  ```bash
  kubectl port-forward -n imagecloud svc/rabbitmq 15672:15672
  # Then access http://localhost:15672 (guest/guest)
  ```

- Grafana: http://imagecloud.local/grafana (admin/admin)
- Prometheus: http://imagecloud.local/prometheus
- Zipkin: http://imagecloud.local/zipkin

Import dashboards from `monitoring/grafana/provisioning/dashboards/`.
deployment/main-service -n imagecloud
kubectl logs -f deployment/conversion-service -n imagecloud
kubectl logs -f deployment/rabbitmq -n imagecloud
kubectl logs -f 
## Common Tasks

```bash
# View resources
kubectl get pods -n imagecloud
kubectl get services -n imagecloud
kubectl describe pod <pod-name> -n imagecloud

# Check logs
kubectl logs -f deployment/auth-service -n imagecloud
kubectl logs -f -l app=auth-service -n imagecloud

# Shell into pod
kubectl exec -it deployment/auth-service -n imagecloud -- sh

# Scale deployment
kubectl scale deployment auth-service --replicas=3 -n imagecloud

# Update after code changes
eval $(minikube docker-env)
cd backend/auth-service && mvn clean package -DskipTests
docker build -t imagecloud/auth-service:latest .
kuOr for main-service/conversion-service
cd backend/main-service && mvn clean package -DskipTests
docker build -t imagecloud/main-service:latest .
kubectl rollout restart deployment/main-service -n imagecloud

# Database access (auth)
kubectl exec -it deployment/postgres -n imagecloud -- psql -U postgres -d imagecloud_auth

# Database access (main)
kubectl exec -it deployment/postgres-main -n imagecloud -- psql -U postgres -d imagecloud_main

# Database access
kubectl exec -it deployment/postgres -n imagecloud -- psql -U postgres -d imagecloud_auth

# SQL commands
\dt                    # List tables
SELECT * FROM users;   # Query data
\q                     # Exit
```

## Troubleshooting

### Pod Pending

```bash
kubectl describe pod <pod-name> -n imagecloud
```

Check Events for resource or scheduling issues.

### Pod CrashLoopBackOff

```bash
kubectl logs <pod-name> -n imagecloud
```

Common causes:
- Database not ready (wait longer)
- Invalid environment variables
- Port conflicts

### ImagePullBackOff

Image not in Minikube's Docker daemon. Rebuild:

```bash
eval $(minikube docker-env)

# For auth-service
cd backend/auth-service && mvn clean package -DskipTests
docker build -t imagecloud/auth-service:latest .

# For main-service
cd backend/main-service && mvn clean package -DskipTests
docker build -t imagecloud/main-service:latest .

# For conversion-service
cd backend/conversion-service && mvn clean package -DskipTests
docker build -t imagecloud/conversion-service:latest .
```

### RabbitMQ Connection Issues

```bash
kubectl logs -f deployment/main-service -n imagecloud | grep rabbit
kubectl logs -f deployment/conversion-service -n imagecloud | grep rabbit
kubectl exec -it deployment/rabbitmq -n imagecloud -- rabbitmqctl list_queues
```

### Ingress Not Working

```bash
kubectl get pods -n ingress-nginx
kubectl get ingress -n imagecloud
kubectl describe ingress imagecloud-ingress -n imagecloud
```

Verify `/etc/hosts` has correct Minikube IP.

## Cleanup

```bash
kubectl delete namespace imagecloud  # Remove all resources
minikube delete                       # Delete cluster
```

## Manifest Files

- **namespace.yaml** - Namespace isolation
- **configmaps.yaml** - Non-secret configuration (DB names, hosts)
- **secrets.yaml** - Passwords, JWT secret (base64)
- **postgres.yaml** - Auth database + PVC
- **postgres-main.yaml** - Main service database + PVC
- **rabbitmq.yaml** - Message broker + PVC
- **auth-service.yaml** - JWT auth (2 replicas, health probes)
- **main-service.yaml** - Image upload/coordination (2 replicas)
- **conversion-service.yaml** - Image conversion worker (2 replicas)
- **prometheus.yaml** - Metrics collection + alerts
- **grafana.yaml** - Dashboard UI
- **alertmanager.yaml** - Alert routing
- **zipkin.yaml** - Distributed tracing
- **ingress.yaml** - HTTP routing (auth, images, monitoring)

## K8s Features Used

**Service Discovery** - K8s DNS resolves service names (rabbitmq.imagecloud.svc.cluster.local)  
**Load Balancing** - Services distribute traffic to pod replicas  
**Health Checks** - Liveness/readiness probes restart failed pods  
**Configuration** - ConfigMaps + Secrets  
**Storage** - PersistentVolumeClaims for databases and RabbitMQ  
**Routing** - Ingress Controller (NGINX)  
**Async Messaging** - RabbitMQ for decoupled services

## Next Steps

1. Deploy frontend as K8s deployment
2. Add HorizontalPodAutoscaler for auto-scaling
3. Implement additional image operations (resize, filters)
4. Add S3/MinIO for image storage instead of DB blobs
5. Deploy to cloud K8s (EKS/GKE/AKS)
