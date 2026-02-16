# Service Level Indicators and Objectives

This document defines the reliability and performance targets for ImageCloud's core services.

## Service Level Indicators

### Conversion Success Rate

Tracks the percentage of image conversions that complete without errors. Measured across all conversion requests with status tracking.

```promql
100 * sum(rate(imagecloud_conversion_requests_total{status="success"}[5m])) 
/ sum(rate(imagecloud_conversion_requests_total{status=~"success|failed"}[5m]))
```

Source: Main service conversion request counter

### Conversion Latency

Measures processing time from conversion request to completion. P95 latency broken down by target format.

```promql
histogram_quantile(0.95, 
  sum(rate(imagecloud_image_conversion_duration_seconds_bucket{status="success"}[5m])) 
  by (le, target_format)
)
```

Source: Conversion service duration histogram

### Service Availability

Standard uptime metric based on Prometheus health check scraping.

```promql
100 * avg(up{job=~".*-service"})
```

Source: Prometheus up metric

### Queue Processing Time

End-to-end message processing latency including deserialization, conversion coordination, and response publishing.

```promql
histogram_quantile(0.95, 
  sum(rate(imagecloud_queue_message_processing_duration_seconds_bucket[5m])) 
  by (le, queue)
)
```

Source: Conversion service message consumer

### Database Query Performance

Query execution time for image metadata operations (save, find, update).

```promql
histogram_quantile(0.95, 
  sum(rate(imagecloud_database_query_duration_seconds_bucket[5m])) 
  by (le, operation)
)
```

Source: Main service database instrumentation

## Service Level Objectives

## Service Level Objectives

| Metric | Target | Window | Error Budget |
|--------|--------|--------|--------------|
| Conversion Success Rate | ≥ 99% | 30 days | 1% (432 failures per 43.2K requests) |
| Conversion Latency (P95) | < 30s | 5 min | 5% of requests may exceed |
| Service Availability | ≥ 99.5% | 30 days | 3.6 hours downtime/month |
| Queue Processing (P95) | < 5s | 5 min | Messaging overhead only |
| Database Queries (P95) | < 1s | 5 min | Includes blob operations |

## Prometheus Alerts

Configured in `kubernetes/prometheus.yaml`:

| Alert | Threshold | Duration | Severity |
|-------|-----------|----------|----------|
| ServiceDown | up == 0 | 2 min | CRITICAL |
| HighConversionFailureRate | failure rate > 1% | 5 min | WARNING |
| HighConversionLatency | P95 > 30s | 5 min | WARNING |
| SlowQueueProcessing | P95 > 5s | 5 min | WARNING |
| SlowDatabaseQueries | P95 > 1s | 5 min | WARNING |

## Accessing Metrics

### Grafana Dashboard

Import `monitoring/grafana/dashboards/imagecloud-sre-dashboard.json` or access pre-configured dashboard at http://imagecloud.local/grafana

Panels include success rate gauge, latency percentiles by format, error rates, queue depth, and database performance.

### Raw Prometheus Queries

Access at http://imagecloud.local/prometheus

```promql
# Success rate over last 5 minutes
100 * sum(rate(imagecloud_conversion_requests_total{status="success"}[5m])) 
/ sum(rate(imagecloud_conversion_requests_total[5m]))

# Request rate
sum(rate(imagecloud_conversion_requests_total{status="success"}[1m])) * 60

# Failed conversions in last hour
sum(increase(imagecloud_conversion_requests_total{status="failed"}[1h]))

# Average conversion time by format
avg(rate(imagecloud_image_conversion_duration_seconds_sum[5m])) by (target_format)
```

### Distributed Traces

Zipkin at http://imagecloud.local/zipkin shows request flow from frontend through main service, RabbitMQ, and conversion service. Useful for identifying bottlenecks and debugging failed requests.

## Error Budget Policy

When SLO is breached (error budget exhausted):
- Halt feature development
- Focus on stability fixes and performance optimization
- Add retry logic, improve error handling
- Use Grafana/Zipkin for root cause analysis

When error budget is healthy (>50% remaining):
- Normal development velocity
- Safe to experiment with new approaches
- Maintenance windows acceptable

## Implementation Notes

Metrics instrumentation uses Micrometer with Spring Boot Actuator. All services expose metrics at `/actuator/prometheus` for Prometheus scraping every 15 seconds.

Custom metrics use appropriate types:
- Counters for event tracking (conversions, failures)
- Histograms for latency measurements (enables percentile calculation)
- Tags for dimensional analysis (format, status, operation)

Histogram buckets configured for expected latency ranges. Queue processing expected in milliseconds, conversion time in seconds, so bucket sizes differ accordingly.
