package ai.binbun.gateway.observability;

public interface MetricsCollector {
    void incrementCounter(String name, String... tags);
    void recordHistogram(String name, double value, String... tags);
    void setGauge(String name, double value, String... tags);
}
