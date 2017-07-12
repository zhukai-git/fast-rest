package com.zhukai.framework.spring.integration.http;

import java.util.HashMap;
import java.util.Map;

public class Session {

    private String sessionId;

    private long lastConnectionTime;

    private Map<String, Object> attributes;

    public Session(String sessionId) {
        this.sessionId = sessionId;
    }

    public String getSessionId() {
        return sessionId;
    }

    public void setSessionId(String sessionId) {
        this.sessionId = sessionId;
    }

    public Object getAttribute(String key) {
        if (this.attributes == null) {
            this.attributes = new HashMap<>();
        }
        return attributes.get(key);
    }

    public Object getAttributes() {
        return attributes;
    }

    public void setAttribute(String key, Object value) {
        if (this.attributes == null) {
            this.attributes = new HashMap<>();
        }
        this.attributes.put(key, value);
    }

    public long getLastConnectionTime() {
        return lastConnectionTime;
    }

    public void setLastConnectionTime(long lastConnectionTime) {
        this.lastConnectionTime = lastConnectionTime;
    }
}
