package io.testable.selenium;

import com.fasterxml.jackson.annotation.JsonIgnore;

public class TestableMetric {

    public enum Type { Timing, Counter, Histogram }

    private Type type;
    private String resource;
    private String url;
    private String namespace;
    private String name;
    private String units;
    private String key;
    private Long val;

    private TestableMetric(Type type,
                           String resource,
                           String url,
                           String namespace,
                           String name,
                           String units,
                           String key,
                           long val) {
        this.type = type;
        this.resource = resource;
        this.url = url;
        this.namespace = namespace;
        this.name = name;
        this.units = units;
        this.key = key;
        this.val = val;
    }

    @JsonIgnore
    public Type getType() {
        return type;
    }

    public String getUrl() {
        return url;
    }

    public String getNamespace() {
        return namespace;
    }

    public String getName() {
        return name;
    }

    public String getUnits() {
        return units;
    }

    public String getKey() {
        return key;
    }

    public long getVal() {
        return val;
    }

    public String getResource() {
        return resource;
    }

    public static Builder newCounterBuilder() {
        return new Builder(Type.Counter);
    }

    public static Builder newTimingBuilder() {
        return new Builder(Type.Timing);
    }

    public static Builder newHistogramBuilder() {
        return new Builder(Type.Histogram);
    }

    public static Builder newBuilder(Type type) {
        return new Builder(type);
    }

    public static class Builder {

        private final TestableMetric.Type type;
        private String resource;
        private String url;
        private String namespace;
        private String name;
        private String units;
        private String key;
        private long val;

        public Builder(TestableMetric.Type type) {
            this.type = type;
        }

        public Builder withResource(String resource) {
            this.resource = resource;
            return this;
        }

        public Builder withUrl(String url) {
            this.url = url;
            return this;
        }

        public Builder withNamespace(String namespace) {
            this.namespace = namespace;
            return this;
        }

        public Builder withName(String name) {
            this.name = name;
            return this;
        }

        public Builder withUnits(String units) {
            this.units = units;
            return this;
        }

        public Builder withKey(String key) {
            this.key = key;
            return this;
        }

        public Builder withVal(long val) {
            this.val = val;
            return this;
        }

        public TestableMetric build() {
            return new TestableMetric(type, resource, url, namespace, name, units, key, val);
        }
    }
}
