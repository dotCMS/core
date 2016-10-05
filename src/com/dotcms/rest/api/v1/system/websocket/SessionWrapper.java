package com.dotcms.rest.api.v1.system.websocket;

import com.liferay.portal.model.User;

import javax.websocket.*;
import java.io.IOException;
import java.net.URI;
import java.security.Principal;
import java.util.List;
import java.util.Map;
import java.util.Set;

/**
 * Our implementation of websocket session to keep the userid and roles.
 * @author jsanca
 */
public class SessionWrapper implements Session {

    private final Session session;
    private final User user;

    public SessionWrapper(final Session session, final User user) {

        this.session = session;
        this.user = user;
    }

    public User getUser () {

        return user;
    }

    @Override
    public WebSocketContainer getContainer() {
        return this.session.getContainer();
    }

    @Override
    public void addMessageHandler(MessageHandler handler) throws IllegalStateException {
        this.session.addMessageHandler(handler);
    }

    @Override
    public <T> void addMessageHandler(Class<T> clazz, MessageHandler.Whole<T> handler) {
        this.session.addMessageHandler(clazz, handler);
    }

    @Override
    public <T> void addMessageHandler(Class<T> clazz, MessageHandler.Partial<T> handler) {
        this.session.addMessageHandler(clazz, handler);
    }

    @Override
    public Set<MessageHandler> getMessageHandlers() {
        return this.session.getMessageHandlers();
    }

    @Override
    public void removeMessageHandler(MessageHandler handler) {
        this.session.removeMessageHandler(handler);
    }

    @Override
    public String getProtocolVersion() {
        return this.session.getProtocolVersion();
    }

    @Override
    public String getNegotiatedSubprotocol() {
        return this.session.getNegotiatedSubprotocol();
    }

    @Override
    public List<Extension> getNegotiatedExtensions() {
        return this.session.getNegotiatedExtensions();
    }

    @Override
    public boolean isSecure() {
        return this.session.isSecure();
    }

    @Override
    public boolean isOpen() {
        return this.session.isOpen();
    }

    @Override
    public long getMaxIdleTimeout() {
        return this.session.getMaxIdleTimeout();
    }

    @Override
    public void setMaxIdleTimeout(long milliseconds) {
        this.session.setMaxIdleTimeout(milliseconds);
    }

    @Override
    public void setMaxBinaryMessageBufferSize(int length) {
        this.session.setMaxBinaryMessageBufferSize(length);
    }

    @Override
    public int getMaxBinaryMessageBufferSize() {
        return this.session.getMaxBinaryMessageBufferSize();
    }

    @Override
    public void setMaxTextMessageBufferSize(int length) {
        this.session.setMaxTextMessageBufferSize(length);
    }

    @Override
    public int getMaxTextMessageBufferSize() {
        return this.session.getMaxTextMessageBufferSize();
    }

    @Override
    public RemoteEndpoint.Async getAsyncRemote() {
        return this.session.getAsyncRemote();
    }

    @Override
    public RemoteEndpoint.Basic getBasicRemote() {
        return this.session.getBasicRemote();
    }

    @Override
    public String getId() {
        return this.session.getId();
    }

    @Override
    public void close() throws IOException {
        this.session.close();
    }

    @Override
    public void close(CloseReason closeReason) throws IOException {
        this.session.close(closeReason);
    }

    @Override
    public URI getRequestURI() {
        return this.session.getRequestURI();
    }

    @Override
    public Map<String, List<String>> getRequestParameterMap() {
        return this.session.getRequestParameterMap();
    }

    @Override
    public String getQueryString() {
        return this.session.getQueryString();
    }

    @Override
    public Map<String, String> getPathParameters() {
        return this.session.getPathParameters();
    }

    @Override
    public Map<String, Object> getUserProperties() {
        return this.session.getUserProperties();
    }

    @Override
    public Principal getUserPrincipal() {
        return this.session.getUserPrincipal();
    }

    @Override
    public Set<Session> getOpenSessions() {
        return this.session.getOpenSessions();
    }
} // E:O:F:SessionWrapper.
