# ImageCloud

Microservices platform for converting images between formats (JPG, PNG, GIF, BMP, WebP). Built with Spring Boot microservices communicating through RabbitMQ queues, deployed on Kubernetes. Features JWT authentication, async processing, and full observability with Prometheus/Grafana metrics and distributed tracing.

## Architecture

Three Spring Boot services communicating via RabbitMQ:
- **Auth Service**: User authentication and JWT token management
- **Main Service**: Image upload coordination and conversion requests
- **Conversion Service**: Background image processing worker

Two PostgreSQL databases for auth and image storage. React frontend for upload/history UI.

## Tech Stack

**Backend**: Spring Boot 3.2, Java 17, Spring Security, Spring AMQP  
**Data**: PostgreSQL 15, RabbitMQ 3.12  
**Frontend**: React 18  
**Infrastructure**: Kubernetes, NGINX Ingress  
**Observability**: Micrometer, Prometheus, Grafana, Zipkin

## Quick Start

```bash
# Start Minikube
minikube start --cpus=4 --memory=8192
minikube addons enable ingress

# Build services
eval $(minikube docker-env)
cd backend/auth-service && mvn clean package -DskipTests && docker build -t imagecloud/auth-service:latest .
cd ../main-service && mvn clean package -DskipTests && docker build -t imagecloud/main-service:latest .
cd ../conversion-service && mvn clean package -DskipTests && docker build -t imagecloud/conversion-service:latest .

# Deploy
cd /Users/amrifazlul/imageCloud
kubectl apply -f kubernetes/

# Configure DNS
echo "$(minikube ip) imagecloud.local" | sudo tee -a /etc/hosts
```

## Endpoints

- Auth API: http://imagecloud.local/api/auth
- Images API: http://imagecloud.local/api/images
- Grafana: http://imagecloud.local/grafana (admin/admin)
- Prometheus: http://imagecloud.local/prometheus
- Zipkin: http://imagecloud.local/zipkin

## API Usage

```bash
# Register
curl -X POST http://imagecloud.local/api/auth/signup \
  -H "Content-Type: application/json" \
  -d '{"email":"user@test.com","password":"pass123"}'

# Login
curl -X POST http://imagecloud.local/api/auth/login \
  -H "Content-Type: application/json" \
  -d '{"email":"user@test.com","password":"pass123"}'

# Upload image for conversion
curl -X POST http://imagecloud.local/api/images/upload \
  -H "X-User-Id: 1" \
  -F "file=@image.png" \
  -F "targetFormat=jpg"

# Get history
curl http://imagecloud.local/api/images/history -H "X-User-Id: 1"
```

## Project Structure

```
backend/
  auth-service/          JWT authentication
  main-service/          Image coordination
  conversion-service/    Image processing worker
frontend/react-app/      Upload UI
kubernetes/*.yaml        Deployments, services, ingress
monitoring/grafana/      Dashboards
sre/                     Observability docs
```

## Observability

Custom Micrometer metrics exported at `/actuator/prometheus`:
- Conversion success/failure counters by format
- Conversion duration histograms (P95/P99)
- Queue processing latency
- Database query performance

SLO targets: 99% success rate, P95 latency < 30s, 99.5% availability

Grafana dashboard: `monitoring/grafana/dashboards/imagecloud-sre-dashboard.json`

## Common Commands

```bash
# View pods
kubectl get pods -n imagecloud

# Logs
kubectl logs -f deployment/main-service -n imagecloud

# Scale
kubectl scale deployment auth-service --replicas=3 -n imagecloud

# Database access
kubectl exec -it deployment/postgres -n imagecloud -- psql -U postgres -d imagecloud_auth

# Rebuild after changes
eval $(minikube docker-env)
cd backend/main-service && mvn clean package -DskipTests && docker build -t imagecloud/main-service:latest .
kubectl rollout restart deployment/main-service -n imagecloud

# Cleanup
kubectl delete namespace imagecloud
```

## Documentation

- [KUBERNETES-SETUP.md](KUBERNETES-SETUP.md) - Deployment guide and troubleshooting
- [sre/MONITORING.md](sre/MONITORING.md) - Monitoring infrastructure
- [sre/SLI-SLO.md](sre/SLI-SLO.md) - SRE metrics and service levels
