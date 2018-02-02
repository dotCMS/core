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
        LANGUAGE_FILE("Language File"),
        LINK("Link"),
        OSGI("OSGI Bundle"),
        RELATIONSHIP("Relationship"),
        RULE("Rule"),
        STRUCTURE("Structure"),
        TEMPLATE("Template"),
        USER("User"),
        WORKFLOW("Workflow");

        private final String handler;

        PushPublishHandler(final String handler) {
            this.handler = handler;
        }

        @Override
        public String toString() {
            return this.handler;
        }
    }

    public enum PushPublishAction {
        PUBLISH("publish"),
        PUBLISH_CREATE("publish (new)"),
        PUBLISH_UPDATE("publish (update)"),
        UNPUBLISH("unpublish");

        private final String action;

        PushPublishAction(final String action) {
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

    public static void error ( final Class cl, final String msg, final String bundleId ) {

        if ( LogMapper.getInstance().isLogEnabled( filename ) ) {
            Logger.error( PushPublishLogger.class, cl.toString() + " : [BundleID: "+bundleId+"] " + msg );
        }
    }

    /**
     * Method to log Push Publishing information
     * @param cl class
     * @param handler The PushPublish IHandler instance executing the publish action
     * @param action Publish action type
     * @param id Id of the object being published
     * @param bundleId Bundle ID containing the object being published
     */
    public static void log (final Class cl, final PushPublishHandler handler, final PushPublishAction action,
                            final String id, final String bundleId) {
        log(cl, handler, action, id, null, null, bundleId);
    }

    /**
     * Method to log Push Publishing information
     * @param cl class
     * @param handler The PushPublish IHandler instance executing the publish action
     * @param action Publish action type
     * @param id Id of the object being published
     * @param inode Inode of the object being published
     * @param name Name of the object being published
     * @param bundleId Bundle ID containing the object being published
     */
    public static void log (final Class cl, final PushPublishHandler handler, final PushPublishAction action,
                            final String id, final String inode, final String name, final String bundleId) {
        final StringBuilder builder = new StringBuilder();
        builder.append(handler.toString())
            .append(' ')
            .append(action.toString())
            .append(" success");
        if (InodeUtils.isSet(id)) {
            builder.append(", ID: ").append(id);
        }
        if (InodeUtils.isSet(inode) && !inode.equals(id)) {
            builder.append(", Inode: ").append(inode);
        }
        if (StringUtils.isSet(name)) {
            builder.append(", Name: ").append(name);
        }
        log(cl, builder.toString(), bundleId);
    }

    /**
     * Method to log Push Publishing Errors
     * @param cl class
     * @param handler The PushPublish IHandler instance executing the publish action
     * @param action Publish action type
     * @param id Id of the object being published
     * @param inode Inode of the object being published
     * @param name Name of the object being published
     * @param bundleId Bundle ID containing the object being published
     * @param errorMessage Custom error message to be logged
     * @param ex Exception if any
     */
    public static void error (final Class cl, final PushPublishHandler handler, final PushPublishAction action,
                                final String id, final String inode, final String name, final String bundleId,
                                final String errorMessage, final Throwable ex) {
        final StringBuilder builder = new StringBuilder();
        builder.append("Failed to ")
            .append(action.toString())
            .append(' ')
            .append(handler.toString());
        if (InodeUtils.isSet(id)) {
            builder.append(", ID: ").append(id);
        }
        if (InodeUtils.isSet(inode) && !inode.equals(id)) {
            builder.append(", Inode: ").append(inode);
        }
        if (StringUtils.isSet(name)) {
            builder.append(", Name: ").append(name);
        }
        if (StringUtils.isSet(errorMessage)) {
            builder.append(", Error: ").append(errorMessage);
        }
        if (null != ex) {
            builder.append(", ").append(ex.getMessage());
        }
        error(cl, builder.toString(), bundleId);
    }
}
