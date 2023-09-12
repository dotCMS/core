/**
 * Copyright (c) 2000-2010 Liferay, Inc. All rights reserved.
 *
 * This library is free software; you can redistribute it and/or modify it under
 * the terms of the GNU Lesser General Public License as published by the Free
 * Software Foundation; either version 2.1 of the License, or (at your option)
 * any later version.
 *
 * This library is distributed in the hope that it will be useful, but WITHOUT
 * ANY WARRANTY; without even the implied warranty of MERCHANTABILITY or FITNESS
 * FOR A PARTICULAR PURPOSE. See the GNU Lesser General Public License for more
 * details.
 */

package com.dotcms.enterprise.license;

import com.dotmarketing.util.Logger;

/**
 * @author Brian Wing Shun Chan
 */
class ServerDetector {

    public static final String GERONIMO_ID = "geronimo";

    public static final String GLASSFISH_ID = "glassfish";

    public static final String JBOSS_ID = "jboss";

    public static final String JETTY_ID = "jetty";

    public static final String JONAS_ID = "jonas";

    public static final String OC4J_ID = "oc4j";

    public static final String RESIN_ID = "resin";

    public static final String TOMCAT_ID = "tomcat";

    public static final String WEBLOGIC_ID = "weblogic";

    public static final String WEBSPHERE_ID = "websphere";

    public static String getServerId() {
        return _instance._serverId;
    }

    public static boolean isGeronimo() {
        return _instance._geronimo;
    }

    public static boolean isGlassfish() {
        return _instance._glassfish;
    }

    public static boolean isJBoss() {
        return _instance._jBoss;
    }

    public static boolean isJetty() {
        return _instance._jetty;
    }

    public static boolean isJOnAS() {
        return _instance._jonas;
    }

    public static boolean isOC4J() {
        return _instance._oc4j;
    }

    public static boolean isResin() {
        return _instance._resin;
    }

    public static boolean isSupportsComet() {
        return _instance._supportsComet;
    }

    public static boolean isTomcat() {
        return _instance._tomcat;
    }

    public static boolean isWebLogic() {
        boolean ret = _instance._webLogic;
        if(isJBoss()){
            return false;
        }
        return ret;
    }

    public static boolean isWebSphere() {
        boolean ret = _instance._webSphere;
        if(isJBoss()){
            return false;
        }
        return ret;
    }

    private ServerDetector() {
        if (_isGeronimo()) {
            _serverId = GERONIMO_ID;
            _geronimo = true;
        }
        else if (_isGlassfish()) {
            _serverId = GLASSFISH_ID;
            _glassfish = true;
        }
        else if (_isJBoss()) {
            _serverId = JBOSS_ID;
            _jBoss = true;
        }
        else if (_isJOnAS()) {
            _serverId = JONAS_ID;
            _jonas = true;
        }
        else if (_isOC4J()) {
            _serverId = OC4J_ID;
            _oc4j = true;
        }
        else if (_isResin()) {
            _serverId = RESIN_ID;
            _resin = true;
        }
        else if (_isWebLogic()) {
            _serverId = WEBLOGIC_ID;
            _webLogic = true;
        }
        else if (_isWebSphere()) {
            _serverId = WEBSPHERE_ID;
            _webSphere = true;
        }

        if (_isJetty()) {
            if (_serverId == null) {
                _serverId = JETTY_ID;
                _jetty = true;
            }
        }
        else if (_isTomcat()) {
            if (_serverId == null) {
                _serverId = TOMCAT_ID;
                _tomcat = true;
            }
        }

        
        /*if (_serverId == null) {
            throw new RuntimeException("Server is not supported");
        }*/
    }

    private boolean _detect(String className) {
        try {
            ClassLoader systemClassLoader =
                ClassLoader.getSystemClassLoader();
            Logger.info(this, "");
            systemClassLoader.loadClass(className);

            return true;
        }
        catch (ClassNotFoundException cnfe) {
            Class<?> classObj = getClass();

            if (classObj.getResource(className) != null) {
                return true;
            }
            else {
                return false;
            }
        }
    }

    private boolean _hasSystemProperty(String key) {
        String value = System.getProperty(key);

        if (value != null) {
            return true;
        }
        else {
            return false;
        }
    }
    
    private boolean _isGeronimo() {
        return _detect(
            "/org/apache/geronimo/system/main/Daemon.class");
    }

    private boolean _isGlassfish() {
        String value = System.getProperty("com.sun.aas.instanceRoot");

        if (value != null) {
            return true;
        }
        else {
            return false;
        }
    }

    private boolean _isJBoss() {
        if(_detect("/org/jboss/Main.class") || _hasSystemProperty("jboss.home.dir")){
            return true;
        }
        return false;
    }

    private boolean _isJetty() {
        return _detect("/org/mortbay/jetty/Server.class");
    }

    private boolean _isJOnAS() {
        boolean jonas = _detect("/org/objectweb/jonas/server/Server.class");

        if (!_jonas && (System.getProperty("jonas.root") != null)) {
            jonas = true;
        }

        return jonas;
    }

    private boolean _isOC4J() {
        return _detect("oracle.oc4j.util.ClassUtils");
    }

    private boolean _isResin() {
        return _detect("/com/caucho/server/resin/Resin.class");
    }

    private boolean _isTomcat() {
        boolean tomcat = _detect(
            "/org/apache/catalina/startup/Bootstrap.class");

        if (!tomcat) {
            tomcat = _detect("/org/apache/catalina/startup/Embedded.class");
        }

        return tomcat;
    }

    private boolean _isWebLogic() {
        return _detect("/weblogic/Server.class");
    }

    private boolean _isWebSphere() {
        return _detect(
            "/com/ibm/websphere/product/VersionInfo.class");
    }

    
    private static ServerDetector _instance = new ServerDetector();

    private String _serverId;
    private boolean _geronimo = false;
    private boolean _glassfish= false;
    private boolean _jBoss= false;
    private boolean _jetty= false;
    private boolean _jonas= false;
    private boolean _oc4j= false;
    private boolean _resin= false;
    private boolean _supportsComet= false;
    private boolean _tomcat= false;
    private boolean _webLogic= false;
    private boolean _webSphere= false;

}