package com.voting.websocket;

import javax.websocket.OnClose;
import javax.websocket.OnOpen;
import javax.websocket.Session;
import javax.websocket.server.ServerEndpoint;

@ServerEndpoint("/voteUpdates")
public class VoteUpdatesEndpoint {

    @OnOpen
    public void onOpen(Session session) {
        VoteUpdateBroadcaster.add(session);
    }

    @OnClose
    public void onClose(Session session) {
        VoteUpdateBroadcaster.remove(session);
    }
}
