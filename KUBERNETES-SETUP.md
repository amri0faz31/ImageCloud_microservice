# Kubernetes Deployment Guide

Complete deployment instructions for running ImageCloud on Minikube.

## Prerequisites

```bash
brew install minikube kubectl
```

Docker Desktop must be running.

## Deployment

### Start Cluster

```bash
minikube start --cpus=4 --memory=8192 --driver=docker
minikube addons enable ingress
```

Wait for ingress controller pods to reach Running state:

```bash
kubectl get pods -n ingress-nginx
```

### Build Service Images

All images must be built in Minikube's Docker environment to avoid registry setup. Run this in each new terminal:

```bash
eval $(minikube docker-env)
```

Build all three services:

```bash
# Auth service
cd backend/auth-service
mvn clean package -DskipTests
docker build -t imagecloud/auth-service:latest .

# Main service
cd ../main-service
mvn clean package -DskipTests
docker build -t imagecloud/main-service:latest .

# Conversion service
cd ../conversion-service
mvn clean package -DskipTests
docker build -t imagecloud/conversion-service:latest .

# Verify
docker images | grep imagecloud
```

### Apply Kubernetes Manifests

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

# Watch deployment progress
kubectl get pods -n imagecloud -w
```

### Configure DNS

Add Minikube IP to /etc/hosts:

```bash
echo "$(minikube ip) imagecloud.local" | sudo tee -a /etc/hosts
```

### Verify

```bash
curl http://imagecloud.local/api/auth/actuator/health
```

## Service Access

| Service | URL | Credentials |
|---------|-----|-------------|
| Auth API | http://imagecloud.local/api/auth | - |
| Main API | http://imagecloud.local/api/images | - |
| Grafana | http://imagecloud.local/grafana | admin/admin |
| Prometheus | http://imagecloud.local/prometheus | - |
| Zipkin | http://imagecloud.local/zipkin | - |
| RabbitMQ | Port forward required | guest/guest |

RabbitMQ management UI:

```bash
kubectl port-forward -n imagecloud svc/rabbitmq 15672:15672
# Access at http://localhost:15672
```

## Common Operations

### View Resources

```bash
kubectl get pods -n imagecloud
kubectl get services -n imagecloud
kubectl describe pod <pod-name> -n imagecloud
```

### Check Logs

```bash
kubectl logs -f deployment/auth-service -n imagecloud
kubectl logs -f -l app=main-service -n imagecloud
```

### Database Access

Auth database:

```bash
kubectl exec -it deployment/postgres -n imagecloud -- psql -U postgres -d imagecloud_auth
```

Main database:

```bash
kubectl exec -it deployment/postgres-main -n imagecloud -- psql -U postgres -d imagecloud_main
```

Common psql commands: `\dt` (list tables), `\q` (exit)

### Scale Deployments

```bash
kubectl scale deployment auth-service --replicas=3 -n imagecloud
```

### Update After Code Changes

```bash
eval $(minikube docker-env)
cd backend/auth-service && mvn clean package -DskipTests
docker build -t imagecloud/auth-service:latest .
kubectl rollout restart deployment/auth-service -n imagecloud
```

Same process applies to main-service and conversion-service.

### Delete Everything

```bash
kubectl delete namespace imagecloud
minikube delete
```

## Troubleshooting

## Troubleshooting

### Pod Stuck in Pending

```bash
kubectl describe pod <pod-name> -n imagecloud
```

Check Events section for resource constraints or scheduling failures.

### CrashLoopBackOff

```bash
kubectl logs <pod-name> -n imagecloud
```

Common causes:
- Database connection refused (PostgreSQL not ready yet - wait longer)
- Missing environment variables
- Port already in use

### ImagePullBackOff

Image wasn't built in Minikube's Docker daemon. Rebuild:

```bash
eval $(minikube docker-env)
cd backend/<service-name>
mvn clean package -DskipTests
docker build -t imagecloud/<service-name>:latest .
```

### RabbitMQ Connection Failures

Check RabbitMQ is running and services can reach it:

```bash
kubectl logs -f deployment/main-service -n imagecloud | grep rabbit
kubectl exec -it deployment/rabbitmq -n imagecloud -- rabbitmqctl list_queues
```

### Ingress Not Responding

Verify ingress controller and routing:

```bash
kubectl get pods -n ingress-nginx
kubectl get ingress -n imagecloud
kubectl describe ingress imagecloud-ingress -n imagecloud
```

Confirm `/etc/hosts` has correct Minikube IP.

## Manifest Reference

- **namespace.yaml** - Creates imagecloud namespace for resource isolation
- **configmaps.yaml** - Non-sensitive config (database names, hostnames, ports)
- **secrets.yaml** - Base64-encoded passwords and JWT signing key
- **postgres.yaml** - Auth database with PersistentVolumeClaim
- **postgres-main.yaml** - Image storage database with separate PVC
- **rabbitmq.yaml** - Message broker with persistence
- **auth-service.yaml** - 2 replicas with liveness/readiness probes
- **main-service.yaml** - 2 replicas with actuator health checks
- **conversion-service.yaml** - 2 replicas for parallel processing
- **prometheus.yaml** - Metrics collection with scrape configs and alert rules
- **grafana.yaml** - Dashboard and visualization
- **alertmanager.yaml** - Alert routing (not fully configured)
- **zipkin.yaml** - Distributed tracing collector
- **ingress.yaml** - NGINX routing rules for all services

## Architecture Notes

Services use Kubernetes DNS for discovery (e.g., `rabbitmq.imagecloud.svc.cluster.local`). 

Each service has a ClusterIP for internal communication. Ingress provides external access via path-based routing.

PersistentVolumeClaims use Minikube's default storage class (hostPath). In production, use proper storage backends.

Health probes: liveness triggers pod restart on failure, readiness removes pod from service endpoints until healthy.

Secrets should use external secret management (Vault, AWS Secrets Manager) in production rather than base64-encoded manifests.
