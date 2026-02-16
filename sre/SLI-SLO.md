# SLI/SLO Framework for ImageCloud

## Overview

This document defines the Service Level Indicators (SLIs) and Service Level Objectives (SLOs) for the ImageCloud microservices platform. These metrics are critical for measuring system reliability and performance.

## Service Level Indicators (SLIs)

SLIs are carefully selected metrics that measure the quality of service provided to users.

### 1. Conversion Success Rate

**Definition:** Percentage of image conversions that complete successfully without errors.

**Measurement:**
```promql
100 * sum(rate(imagecloud_conversion_requests_total{status="success"}[5m])) 
/ sum(rate(imagecloud_conversion_requests_total{status=~"success|failed"}[5m]))
```

**Why it matters:** Core business metric - users expect their images to convert successfully.

**Tracked in:** Main Service metrics

---

### 2. Conversion Latency (P95)

**Definition:** 95th percentile time for image conversion processing.

**Measurement:**
```promql
histogram_quantile(0.95, 
  sum(rate(imagecloud_image_conversion_duration_seconds_bucket{status="success"}[5m])) 
  by (le, target_format)
)
```

**Why it matters:** User experience depends on fast conversion times.

**Tracked in:** Conversion Service metrics

---

### 3. Service Availability

**Definition:** Percentage of time services are up and responding to health checks.

**Measurement:**
```promql
100 * avg(up{job=~".*-service"})
```

**Why it matters:** Users cannot upload or retrieve images if services are down.

**Tracked in:** Prometheus `up` metric

---

### 4. Queue Processing Time (P95)

**Definition:** 95th percentile time from receiving a message to completing processing.

**Measurement:**
```promql
histogram_quantile(0.95, 
  sum(rate(imagecloud_queue_message_processing_duration_seconds_bucket[5m])) 
  by (le, queue)
)
```

**Why it matters:** Long queue processing indicates system overload or performance issues.

**Tracked in:** Conversion Service message consumer

---

### 5. Database Query Performance (P95)

**Definition:** 95th percentile database query execution time.

**Measurement:**
```promql
histogram_quantile(0.95, 
  sum(rate(imagecloud_database_query_duration_seconds_bucket[5m])) 
  by (le, operation)
)
```

**Why it matters:** Database performance impacts overall system responsiveness.

**Tracked in:** Main Service database operations

---

## Service Level Objectives (SLOs)

SLOs define target values for SLIs that we commit to achieving.

### 1. Conversion Success Rate SLO

**Target:** ≥ 99.0%

**Time Window:** Rolling 30 days

**Rationale:** 
- Allows ~7 hours of failures per month
- Accounts for invalid inputs, unsupported formats, and system errors
- Industry standard for batch processing systems

**Alert Threshold:** < 99% for 5 minutes

**Error Budget:** 1% = ~432 failed conversions per 43,200 total

---

### 2. Conversion Latency SLO

**Target:** P95 < 30 seconds

**Time Window:** Rolling 5 minutes

**Rationale:**
- Users expect near-instant results for small images
- Acceptable wait time for larger files
- Includes queue wait + processing + network time

**Alert Threshold:** P95 > 30s for 5 minutes

**Error Budget:** 5% of conversions can exceed 30s

---

### 3. Service Availability SLO

**Target:** ≥ 99.5%

**Time Window:** Rolling 30 days

**Rationale:**
- Allows ~3.6 hours downtime per month
- Covers planned maintenance + incidents
- Standard for non-critical microservices

**Alert Threshold:** Service down for 2 minutes

**Downtime Budget:** 3.6 hours/month

---

### 4. Queue Processing Time SLO

**Target:** P95 < 5 seconds

**Time Window:** Rolling 5 minutes

**Rationale:**
- Pure processing overhead (excludes image conversion)
- Indicates healthy RabbitMQ and consumer performance
- Fast feedback to main service

**Alert Threshold:** P95 > 5s for 5 minutes

---

### 5. Database Query Performance SLO

**Target:** P95 < 1 second

**Time Window:** Rolling 5 minutes

**Rationale:**
- PostgreSQL should handle CRUD operations quickly
- Includes image blob storage/retrieval
- Critical for user-facing operations

**Alert Threshold:** P95 > 1s for 5 minutes

---

## Viewing Metrics

### Grafana Dashboard

Import the dashboard: `monitoring/grafana/dashboards/imagecloud-sre-dashboard.json`

**Access:** http://imagecloud.local/grafana (admin/admin)

**Panels:**
1. Conversion Success Rate Gauge (green = meeting SLO)
2. P95/P99 Conversion Duration graph
3. Conversions per minute
4. Error rate percentage
5. Queue processing time
6. Database query performance

### Prometheus Queries

**Access:** http://imagecloud.local/prometheus

**Example queries:**

```promql
# Current success rate
100 * sum(rate(imagecloud_conversion_requests_total{status="success"}[5m])) 
/ sum(rate(imagecloud_conversion_requests_total[5m]))

# Conversions per minute
sum(rate(imagecloud_conversion_requests_total{status="success"}[1m])) * 60

# Failed conversions count
sum(increase(imagecloud_conversion_requests_total{status="failed"}[1h]))

# Average conversion time by format
avg(rate(imagecloud_image_conversion_duration_seconds_sum[5m])) 
by (target_format)
```

### Zipkin Distributed Tracing

**Access:** http://imagecloud.local/zipkin

**Use cases:**
- Trace request flow: Frontend → Main Service → RabbitMQ → Conversion Service
- Identify bottlenecks in the conversion pipeline
- Debug failed conversions

---

## Alert Rules

Configured in `kubernetes/prometheus.yaml` AlertManager rules:

| Alert | Condition | Duration | Severity |
|-------|-----------|----------|----------|
| ServiceDown | Service not responding | 2 min | CRITICAL |
| HighConversionFailureRate | Failure rate > 1% | 5 min | WARNING |
| HighConversionLatency | P95 > 30s | 5 min | WARNING |
| SlowQueueProcessing | P95 > 5s | 5 min | WARNING |
| SlowDatabaseQueries | P95 > 1s | 5 min | WARNING |

---

## Error Budget Policy

### When Error Budget is Exhausted (SLO breached)

**Actions:**
1. **Stop new feature development**
2. **Focus on reliability improvements:**
   - Fix bugs causing failures
   - Optimize slow code paths
   - Add retry logic
   - Improve error handling
3. **Investigate root causes** using Grafana/Zipkin
4. **Implement preventive measures**

### When Error Budget is Healthy (> 50% remaining)

**Actions:**
- Continue feature development
- Experiment with new technologies
- Perform controlled chaos testing
- Schedule maintenance windows

---

## Interview Talking Points

✅ **"I defined custom SLIs based on the Four Golden Signals"**
- Latency (conversion duration)
- Traffic (conversions per minute)
- Errors (failure rate)
- Saturation (queue depth, database performance)

✅ **"I instrumented microservices with Micrometer for business metrics"**
- Custom counters for success/failure tracking
- Histograms for latency measurements
- Tags for dimensional analysis (format, operation type)

✅ **"I set SLOs aligned with user expectations"**
- 99% success rate (realistic for image processing)
- 30s P95 latency (acceptable user experience)
- Data-driven thresholds based on system capacity

✅ **"I configured AlertManager rules for SLO breaches"**
- Proactive alerting before users notice
- Severity-based escalation
- Error budget awareness

✅ **"I created Grafana dashboards for real-time monitoring"**
- Visual representation of SLI performance
- Historical trends for capacity planning
- Color-coded thresholds (green/yellow/red)

---

## References

- [Google SRE Book - SLIs, SLOs, SLAs](https://sre.google/sre-book/service-level-objectives/)
- [Micrometer Documentation](https://micrometer.io/docs)
- [Prometheus Best Practices](https://prometheus.io/docs/practices/naming/)
- [Four Golden Signals](https://sre.google/sre-book/monitoring-distributed-systems/#xref_monitoring_golden-signals)
