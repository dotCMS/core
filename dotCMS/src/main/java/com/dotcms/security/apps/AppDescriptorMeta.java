package com.dotcms.security.apps;


/**
 * This is a immutable wrapper class
 * That holds the Service Descriptor and additional info regarding the file that it was loaded from.
 */
class AppDescriptorMeta {

    private final AppDescriptor appDescriptor;

    private final String fileName;

    /**
     * The only way to instantiate it.
     * @param appDescriptor
     * @param fileName
     */
    AppDescriptorMeta(final AppDescriptor appDescriptor, final String fileName) {
        this.appDescriptor = appDescriptor;
        this.fileName = fileName;
    }

    /**
     * SD read property
     * @return
     */
    AppDescriptor getAppDescriptor() {
        return appDescriptor;
    }

    /**
     * The yml file name the descriptor get the info from.
     * @return
     */
    public String getFileName() {
        return fileName;
    }
}
