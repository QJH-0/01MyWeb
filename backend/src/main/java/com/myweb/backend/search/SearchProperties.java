package com.myweb.backend.search;

import org.springframework.boot.context.properties.ConfigurationProperties;

/** 搜索与 outbox：ES 连接、索引名、调度开关。 */
@ConfigurationProperties(prefix = "app.search")
public class SearchProperties {
    private Elasticsearch elasticsearch = new Elasticsearch();
    private String indexName = "myweb_content";
    private Outbox outbox = new Outbox();

    public Elasticsearch getElasticsearch() {
        return elasticsearch;
    }

    public void setElasticsearch(Elasticsearch elasticsearch) {
        this.elasticsearch = elasticsearch;
    }

    public String getIndexName() {
        return indexName;
    }

    public void setIndexName(String indexName) {
        this.indexName = indexName;
    }

    public Outbox getOutbox() {
        return outbox;
    }

    public void setOutbox(Outbox outbox) {
        this.outbox = outbox;
    }

    public static class Elasticsearch {
        private boolean enabled = true;
        private String host = "localhost";
        private int port = 9200;

        public boolean isEnabled() {
            return enabled;
        }

        public void setEnabled(boolean enabled) {
            this.enabled = enabled;
        }

        public String getHost() {
            return host;
        }

        public void setHost(String host) {
            this.host = host;
        }

        public int getPort() {
            return port;
        }

        public void setPort(int port) {
            this.port = port;
        }
    }

    public static class Outbox {
        private boolean schedulerEnabled = true;
        private long pollMs = 5000L;
        private int batchSize = 50;

        public boolean isSchedulerEnabled() {
            return schedulerEnabled;
        }

        public void setSchedulerEnabled(boolean schedulerEnabled) {
            this.schedulerEnabled = schedulerEnabled;
        }

        public long getPollMs() {
            return pollMs;
        }

        public void setPollMs(long pollMs) {
            this.pollMs = pollMs;
        }

        public int getBatchSize() {
            return batchSize;
        }

        public void setBatchSize(int batchSize) {
            this.batchSize = batchSize;
        }
    }
}
