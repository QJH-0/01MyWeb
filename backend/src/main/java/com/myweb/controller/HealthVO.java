package com.myweb.controller;

public class HealthVO {
    private boolean mysql;
    private boolean redis;
    private boolean elasticsearch;
    private boolean minio;

    private String elasticsearchStatus;

    public boolean isMysql() {
        return mysql;
    }

    public void setMysql(boolean mysql) {
        this.mysql = mysql;
    }

    public boolean isRedis() {
        return redis;
    }

    public void setRedis(boolean redis) {
        this.redis = redis;
    }

    public boolean isElasticsearch() {
        return elasticsearch;
    }

    public void setElasticsearch(boolean elasticsearch) {
        this.elasticsearch = elasticsearch;
    }

    public boolean isMinio() {
        return minio;
    }

    public void setMinio(boolean minio) {
        this.minio = minio;
    }

    public String getElasticsearchStatus() {
        return elasticsearchStatus;
    }

    public void setElasticsearchStatus(String elasticsearchStatus) {
        this.elasticsearchStatus = elasticsearchStatus;
    }
}

