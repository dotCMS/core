package com.dotcms.visitor.filter.characteristics;

import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;

public interface ClickHouseLogger {

    void log(HttpServletRequest request, HttpServletResponse response) ;

    public default ClickHouseLogger getVisitorLoggerImpl() {
        return new ClickHouseLoggerImpl();
    }
}
