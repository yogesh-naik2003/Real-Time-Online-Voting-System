package com.voting.websocket;

import com.voting.util.JsonUtil;

import javax.websocket.Session;
import java.io.IOException;
import java.util.Set;
import java.util.concurrent.ConcurrentHashMap;

public final class VoteUpdateBroadcaster {
    private static final Set<Session> SESSIONS = ConcurrentHashMap.newKeySet();

    private VoteUpdateBroadcaster() {
    }

    public static void add(Session session) {
        SESSIONS.add(session);
    }

    public static void remove(Session session) {
        SESSIONS.remove(session);
    }

    public static void broadcast(String event) {
        String payload = JsonUtil.toJson(new EventMessage(event));
        for (Session session : SESSIONS) {
            if (session.isOpen()) {
                try {
                    session.getBasicRemote().sendText(payload);
                } catch (IOException ex) {
                    remove(session);
                }
            }
        }
    }

    private static class EventMessage {
        private final String type;

        private EventMessage(String type) {
            this.type = type;
        }
    }
}
