package com.dotmarketing.util;

import com.dotmarketing.logConsole.model.LogMapper;
import com.liferay.portal.model.User;
import com.liferay.util.StringUtil;


public class PushPublishLogger {

    private static String filename = "dotcms-pushpublish.log";

    public enum PushPublishHandler {
        CATEGORY("Category"),
        CONTAINER("Container"),
        CONTENT("Content"),
        CONTENT_TYPE("Content Type"),
        CONTENT_WORKFLOW("Content Workflow"),
        FOLDER("Folder"),
        HOST("Host"),
        LANGUAGE("Language"),
        LANGUAGE_VARIABLE("Language Messages"),
        LINK("Link"),
        OSGI("OSGI"),
        RELATIONSHIP("Relationship"),
        RULE("Rule"),
        STRUCTURE("Structure"),
        TEMPLATE("Template"),
        USER("User"),
        WORKFLOW("Workflow");

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
        PUBLISH("published"),
        PUBLISH_CREATE("published (new)"),
        PUBLISH_UPDATE("published (updated)"),
        UNPUBLISH("unpublished");

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
            builder.append(", ID: " + id);
        }
        if (InodeUtils.isSet(inode)) {
            if (!inode.equals(id)) {
                builder.append(", Inode: " + inode);
            }
        }
        builder.append(", Name: " + name);
        log(cl, builder.toString(), bundleId);
    }
}
