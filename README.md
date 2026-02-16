# ImageCloud

ImageCloud is a distributed image conversion platform built using a microservices architecture. The system handles asynchronous image format conversion through message queues, with JWT-based authentication and comprehensive observability features.

## Overview

This project demonstrates a production-ready microservices implementation deployed on Kubernetes. The platform accepts image uploads from users, converts them to different formats asynchronously, and stores both the original and converted images with full metadata tracking.

The system consists of three main services: an authentication service for user management, a main service that coordinates image uploads and conversions, and a dedicated conversion service that processes images in the background. Communication between services happens through RabbitMQ message queues, ensuring loose coupling and resilience.

## Current Implementation Status

The backend infrastructure is fully implemented and operational, including:
- Three Spring Boot microservices with health checks and metrics
- Two PostgreSQL databases for authentication and image storage
- RabbitMQ message broker with request/response queues
- Complete Kubernetes deployment manifests
- Monitoring stack with Prometheus, Grafana, and Zipkin
- Custom SLI/SLO definitions with Micrometer metrics instrumentation
- React frontend for image upload and history viewing

The frontend currently runs locally and has not yet been containerized for Kubernetes deployment.

## Technology Stack

The backend services are built with Spring Boot 3.2 and Java 17, using Spring Security for JWT authentication and Spring AMQP for RabbitMQ integration. PostgreSQL 15 serves as the persistent storage layer, with separate database instances for authentication and image data. RabbitMQ 3.12 handles asynchronous messaging between services.

The frontend is a React 18 application that communicates with the backend services through REST APIs. For observability, the system uses Micrometer for metrics instrumentation, Prometheus for metrics collection, Grafana for visualization, and Zipkin for distributed tracing.

All components run on Kubernetes, with NGINX serving as the ingress controller for external access.

To run the platform locally, you'll need Minikube with at least 4 CPUs and 8GB of memory. Start by enabling the ingress addon, then build the Docker images within Minikube's Docker environment to avoid the need for a registry push.

### Building and Deploying


### Building and Deploying

```bash
# Start Minikube with sufficient resources
minikube start --cpus=4 --memory=8192
minikube addons enable ingress

# Configure shell to use Minikube's Docker daemon
eval $(minikube docker-env)

# Build auth service
cd backend/auth-service && mvn clean package -DskipTests
docker build -t imagecloud/auth-service:latest .

# Build main service
cd ../main-service && mvn clean package -DskipTests
docker build -t imagecloud/main-service:latest .

# Build conversion service
cd ../conversion-service && mvn clean package -DskipTests
docker build -t imagecloud/conversion-service:latest .

# Deploy all Kubernetes resources
cd /Users/amrifazlul/imageCloud
kubectl apply -f kubernetes/

# Configure local DNS for ingress
echo "$(minikube ip) imagecloud.local" | sudo tee -a /etc/hosts

# Verify deployment
curl http://imagecloud.local/api/auth/actuator/health
```

### Service Endpoints

Once deployed, the following endpoints are available:

- **Auth Service API**: http://imagecloud.local/api/auth
- **Main Service API**: http://imagecloud.local/api/images
- **RabbitMQ Management Console**: http://imagecloud.local/rabbitmq (credentials: guest/guest)
- **Grafana Dashboards**: http://imagecloud.local/grafana (credentials: admin/admin)
- **Prometheus UI**: http://imagecloud.local/prometheus
- **Zipkin Tracing**: http://imagecloud.local/zipkin

## Working with the Cluster

### Viewing Resources

```bash
# List all pods in the imagecloud namespace
kubectl get pods -n imagecloud

# List all services
kubectl get services -n imagecloud

# Check pod logs (follow mode)
kubectl logs -f deployment/auth-service -n imagecloud
```

### Scaling Services

You can scale any deployment horizontally by adjusting the replica count:

```bash
kubectl scale deployment auth-service --replicas=3 -n imagecloud
```

### Updating Services After Code Changes

When you modify service code, rebuild the Docker image and restart the deployment:

```bash
# Ensure you're using Minikube's Docker daemon
eval $(minikube docker-env)

# Rebuild the service
cd backend/auth-service && mvn clean package -DskipTests
docker build -t imagecloud/auth-service:latest .

# Trigger a rollout restart
kubectl rollout restart deployment/auth-service -n imagecloud
```

### Database Access

To connect directly to the PostgreSQL database:

```bash
kubectl exec -it deployment/postgres -n imagecloud -- psql -U postgres -d imagecloud_auth
```

### Cleanup

To remove all resources and reset the environment:

```bash
kubectl delete namespace imagecloud
minikube delete
```

## API Reference

### Authentication

**User Registration**

```http
POST /api/auth/signup
Content-Type: application/json

{
  "email": "user@example.com",
  "password": "password123"
}
```

Returns a JWT token upon successful registration.

**User Login**

```http
POST /api/auth/login
Content-Type: application/json

{
  "email": "user@example.com",
  "password": "password123"
}
```

Returns a JWT token for authenticated users.

### Image Conversion

**Upload and Convert Image**

```http
POST /api/images/upload
Content-Type: multipart/form-data
X-User-Id: <userId>

Form fields:
- file: <image file>
- targetFormat: jpg|png|gif|bmp|webp
```

Returns:
```json
{
  "imageId": "uuid",
  "status": "processing",
  "message": "Image uploaded and queued for conversion"
}
```

**Get Conversion History**

```http
GET /api/images/history
X-User-Id: <userId>
```

Returns an array of conversion records with original and converted image metadata.

## System Architecture 
- `imagecloud_auth` - User credentials (postgres:5432)
### Data Storage

The system maintains two separate PostgreSQL databases:

- **imagecloud_auth**: Stores user credentials and authentication information (accessible at postgres:5432)
- **imagecloud_main**: Stores image metadata and binary data using PostgreSQL's bytea type (accessible at postgres-main:5432)

This separation follows the principle that each service should own its data, preventing tight coupling between authentication and business logic.

### Configuration Management

Sensitive configuration like database passwords and JWT signing keys are stored in Kubernetes secrets (`kubernetes/secrets.yaml`), while non-sensitive configuration resides in ConfigMaps (`kubernetes/configmaps.yaml`).

## Project Structure

```
backend/
  auth-service/          Authentication and user management
  main-service/          Image upload coordination and RabbitMQ orchestration
  conversion-service/    Background worker for image format conversion
frontend/
  react-app/            User interface for upload and history viewing
kubernetes/
  *.yaml                Deployment manifests, services, ingress, secrets
monitoring/
  grafana/              Dashboard configurations and provisioning
sre/
  MONITORING.md         Observability infrastructure documentation
  SLI-SLO.md           Service level objectives and metrics definitions
```

## Observability and SRE Practices

The platform includes comprehensive observability instrumentation to support production operations and incident response.

### Custom Metrics

All services expose Prometheus-compatible metrics through Spring Boot Actuator at the `/actuator/prometheus` endpoint. The system collects the following custom business metrics using Micrometer:

- **imagecloud_conversion_requests_total**: Counter tracking total conversion requests, labeled by status (initiated, success, failed) and target format
- **imagecloud_image_conversion_duration_seconds**: Histogram measuring actual image processing time, with labels for source format, target format, and outcome
- **imagecloud_queue_message_processing_duration_seconds**: Histogram tracking end-to-end message processing latency through RabbitMQ
- **imagecloud_database_query_duration_seconds**: Histogram measuring database query performance, labeled by operation type

These metrics follow the Four Golden Signals pattern (latency, traffic, errors, saturation) and enable dimensional analysis through their label structure.

### Service Level Objectives

The platform defines clear SLOs that represent user-facing reliability targets:

- **Conversion Success Rate**: 99% of conversion requests should complete successfully (allowing for 1% error budget)
- **Conversion Latency**: 95th percentile conversion time should remain under 30 seconds
- **Service Availability**: Services should be reachable 99.5% of the time (permitting 3.6 hours of downtime per month)
- **Queue Processing Time**: 95th percentile message processing should complete within 5 seconds
- **Database Query Performance**: 95th percentile database queries should execute in under 1 second

These SLOs are backed by Prometheus alert rules that fire when thresholds are breached, enabling proactive incident response.

### Dashboards and Alerting

The Grafana dashboard at `monitoring/grafana/dashboards/imagecloud-sre-dashboard.json` visualizes all key metrics with appropriate thresholds. The dashboard includes panels for success rates, latency percentiles, error rates, and queue depths.

Prometheus monitors five categories of alerts tied directly to SLO violations: service availability, conversion failure rates, high latency, slow queue processing, and database performance degradation. All alerts include severity levels and descriptive annotations.

For distributed request tracing, Zipkin collects trace data from all services, allowing you to visualize the complete path of a request as it moves through authentication, queueing, conversion, and storage.

Detailed documentation of the observability implementation, including PromQL queries and error budget policies, is available in [sre/SLI-SLO.md](sre/SLI-SLO.md).

## Additional Documentation

For more detailed information on specific aspects of the system:

- [KUBERNETES-SETUP.md](KUBERNETES-SETUP.md) - Comprehensive Kubernetes deployment guide and troubleshooting
- [sre/MONITORING.md](sre/MONITORING.md) - Monitoring infrastructure setup and configuration
- [sre/SLI-SLO.md](sre/SLI-SLO.md) - Complete SRE metrics framework and service level definitions
