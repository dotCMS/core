package com.dotcms.rest;

import javax.servlet.ServletOutputStream;
import javax.servlet.http.Cookie;
import javax.servlet.http.HttpServletResponse;
import java.io.IOException;
import java.io.PrintWriter;
import java.util.Collection;
import java.util.Collections;
import java.util.Locale;

public class EmptyHttpResponse implements HttpServletResponse {

    public EmptyHttpResponse() {
    }

    @Override
    public void addCookie(Cookie cookie) {
        // nothing
    }

    @Override
    public boolean containsHeader(String name) {
        return false;
    }

    @Override
    public String encodeURL(String url) {
        return "";
    }

    @Override
    public String encodeRedirectURL(String url) {
        return "";
    }

    @Override
    public String encodeUrl(String url) {
        return "";
    }

    @Override
    public String encodeRedirectUrl(String url) {
        return "";
    }

    @Override
    public void sendError(int sc, String msg) throws IOException {
        // nothing
    }

    @Override
    public void sendError(int sc) throws IOException {
        // nothing
    }

    @Override
    public void sendRedirect(String location) throws IOException {
        // nothing
    }

    @Override
    public void setDateHeader(String name, long date) {
        // nothing
    }

    @Override
    public void addDateHeader(String name, long date) {
        // nothing
    }

    @Override
    public void setHeader(String name, String value) {
        // nothing
    }

    @Override
    public void addHeader(String name, String value) {
        // nothing
    }

    @Override
    public void setIntHeader(String name, int value) {
        // nothing
    }

    @Override
    public void addIntHeader(String name, int value) {
        // nothing
    }

    @Override
    public void setStatus(int sc) {
        // nothing
    }

    @Override
    public void setStatus(int sc, String sm) {
        // nothing
    }

    @Override
    public int getStatus() {
        return 0;
    }

    @Override
    public String getHeader(String name) {
        return "";
    }

    @Override
    public Collection<String> getHeaders(String name) {
        return Collections.emptyList();
    }

    @Override
    public Collection<String> getHeaderNames() {
        return Collections.emptyList();
    }


    @Override
    public void setCharacterEncoding(String charset) {
        // nothing
    }

    @Override
    public String getCharacterEncoding() {
        return "";
    }

    @Override
    public ServletOutputStream getOutputStream() throws IOException {
        return null;
    }

    @Override
    public PrintWriter getWriter() throws IOException {
        return null;
    }

    @Override
    public void setContentLength(int len) {
        // nothing
    }

    @Override
    public void setContentLengthLong(long len) {
        // nothing
    }

    @Override
    public void setContentType(String type) {
        // nothing
    }

    @Override
    public String getContentType() {
        return "";
    }

    @Override
    public void setBufferSize(int size) {
        // nothing
    }

    @Override
    public int getBufferSize() {
        return 0;
    }

    @Override
    public void flushBuffer() throws IOException {
        // nothing
    }

    @Override
    public boolean isCommitted() {
        return false;
    }

    @Override
    public void reset() {

    }

    @Override
    public void resetBuffer() {
        // nothing
    }

    @Override
    public void setLocale(Locale loc) {
        // nothing
    }

    @Override
    public Locale getLocale() {
        return null;
    }


}
