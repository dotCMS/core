package com.dotcms.visitor.filter.logger;

import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;

import com.fasterxml.jackson.core.JsonProcessingException;

public interface VisitorLogger {

    String VISITOR_LOG_BASE_NAME = "visitor-v3.log";

    void log(HttpServletRequest request, HttpServletResponse response) throws JsonProcessingException;

    default VisitorLogger getVisitorLoggerImpl() {
        return new VisitorLoggerImpl();
    }
}
