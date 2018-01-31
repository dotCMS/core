package com.dotmarketing.util;

import com.dotmarketing.logConsole.model.LogMapper;
import com.liferay.portal.model.User;
import com.liferay.util.StringUtil;


public class PushPublishLogger {

    private static String filename = "dotcms-pushpublish.log";

    public enum PushPublishHandler {
        CATEGORY("Category");

        private final String handler;

        PushPublishHandler(String handler) {
            this.handler = handler;
        }

        @Override
        public String toString() {
            return this.handler;
        }
    }

    public enum PushPublishAction {
        PUBLISH("Published"),
        PUBLISH_CREATE("Published (new)"),
        PUBLISH_UPDATE("Published (updated)"),
        UNPUBLISH("Unpublished");

        private final String action;

        PushPublishAction(String action) {
            this.action = action;
        }

        @Override
        public String toString() {
            return this.action;
        }
    }

    @SuppressWarnings("rawtypes")
	public static void log ( Class cl, String msg ) {

        if ( LogMapper.getInstance().isLogEnabled( filename ) ) {
            Logger.info( PushPublishLogger.class, cl.toString() + " : " + msg );
        }
    }

    @SuppressWarnings("rawtypes")
	public static void log ( Class cl, String msg, String bundleId ) {

        if ( LogMapper.getInstance().isLogEnabled( filename ) ) {
            Logger.info( PushPublishLogger.class, cl.toString() + " : [BundleID: "+bundleId+"] " + msg );
        }
    }

    @SuppressWarnings("rawtypes")
	public static void log ( Class cl, String msg, String bundleId, User user ) {

        if ( user == null || user.getUserId() == null ) {
            log( cl, msg , bundleId);
        } else {
            if ( LogMapper.getInstance().isLogEnabled( filename ) ) {
                Logger.info( PushPublishLogger.class, cl.toString() + " : [BundleID: "+bundleId+"] " + msg + ", User: " + user.getUserId());
            }
        }
    }

    public static void log (Class cl, PushPublishHandler handler, PushPublishAction action, String id, String inode, String name, String bundleId) {
        StringBuilder builder = new StringBuilder();
        builder.append(handler.toString());
        builder.append(" ");
        builder.append(action.toString());
        if (InodeUtils.isSet(id)) {
            builder.append(" ID: " + id);
        }
        if (InodeUtils.isSet(inode)) {
            if (!inode.equals(id)) {
                builder.append(" Inode: " + inode);
            }
        }
        builder.append(" Name: " + name);
        log(cl, builder.toString(), bundleId);
    }
}
