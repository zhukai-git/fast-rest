package com.zhukai.spring.integration.context;

import com.zhukai.spring.integration.common.Session;

import java.lang.reflect.Method;
import java.sql.Connection;
import java.time.LocalDateTime;
import java.util.Collections;
import java.util.HashMap;
import java.util.Map;

/**
 * Created by zhukai on 17-1-12.
 */
public class WebContext {

    public static final String JSESSIONID = "JSESSIONID";

    private static Map<String, Method> webMethods;

    private static ThreadLocal<Connection> transaction = new ThreadLocal();

    private static Map<String, Session> sessions = Collections.synchronizedMap(new HashMap<>());

    public static Connection getTransaction() {
        return transaction.get();
    }

    public static void setTransaction(Connection connection) {
        WebContext.transaction.set(connection);
    }

    public static Session getSession(String sessionId) {
        if (sessions.get(sessionId) == null) {
            sessions.put(sessionId, new Session(sessionId));
        }
        return sessions.get(sessionId);
    }

    public static Map<String, Session> getSessions() {
        return sessions;
    }

    public static Map<String, Method> getWebMethods() {
        return webMethods;
    }

    public static void setWebMethods(Map<String, Method> webMethods) {
        WebContext.webMethods = webMethods;
    }

    public static void refreshSession(String sessionId) {
        if (sessions.get(sessionId) != null) {
            sessions.get(sessionId).setLastConnectionTime(LocalDateTime.now());
        }
    }

}
