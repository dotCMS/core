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
        return this.getNegotiatedSubprotocol();
    }

    @Override
    public List<Extension> getNegotiatedExtensions() {
        return this.getNegotiatedExtensions();
    }

    @Override
    public boolean isSecure() {
        return this.isSecure();
    }

    @Override
    public boolean isOpen() {
        return this.isOpen();
    }

    @Override
    public long getMaxIdleTimeout() {
        return this.getMaxIdleTimeout();
    }

    @Override
    public void setMaxIdleTimeout(long milliseconds) {
        this.setMaxIdleTimeout(milliseconds);
    }

    @Override
    public void setMaxBinaryMessageBufferSize(int length) {
        this.setMaxBinaryMessageBufferSize(length);
    }

    @Override
    public int getMaxBinaryMessageBufferSize() {
        return this.getMaxBinaryMessageBufferSize();
    }

    @Override
    public void setMaxTextMessageBufferSize(int length) {
        this.setMaxTextMessageBufferSize(length);
    }

    @Override
    public int getMaxTextMessageBufferSize() {
        return this.getMaxTextMessageBufferSize();
    }

    @Override
    public RemoteEndpoint.Async getAsyncRemote() {
        return this.getAsyncRemote();
    }

    @Override
    public RemoteEndpoint.Basic getBasicRemote() {
        return this.getBasicRemote();
    }

    @Override
    public String getId() {
        return this.getId();
    }

    @Override
    public void close() throws IOException {
        this.close();
    }

    @Override
    public void close(CloseReason closeReason) throws IOException {
        this.close(closeReason);
    }

    @Override
    public URI getRequestURI() {
        return this.getRequestURI();
    }

    @Override
    public Map<String, List<String>> getRequestParameterMap() {
        return this.getRequestParameterMap();
    }

    @Override
    public String getQueryString() {
        return this.getQueryString();
    }

    @Override
    public Map<String, String> getPathParameters() {
        return this.getPathParameters();
    }

    @Override
    public Map<String, Object> getUserProperties() {
        return this.getUserProperties();
    }

    @Override
    public Principal getUserPrincipal() {
        return this.getUserPrincipal();
    }

    @Override
    public Set<Session> getOpenSessions() {
        return this.getOpenSessions();
    }
} // E:O:F:SessionWrapper.
