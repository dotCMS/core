package com.dotcms.security.secret;


/**
 * This is a immutable wrapper class
 * That holds the Service Descriptor and additional info regarding the file that it was loaded from.
 */
class ServiceDescriptorMeta {

    private final ServiceDescriptor serviceDescriptor;

    private final String fileName;

    /**
     * The only way to instantiate it.
     * @param serviceDescriptor
     * @param fileName
     */
    ServiceDescriptorMeta(final ServiceDescriptor serviceDescriptor, final String fileName) {
        this.serviceDescriptor = serviceDescriptor;
        this.fileName = fileName;
    }

    /**
     * SD read property
     * @return
     */
    ServiceDescriptor getServiceDescriptor() {
        return serviceDescriptor;
    }

    /**
     * The yml file name the descriptor get the info from.
     * @return
     */
    public String getFileName() {
        return fileName;
    }
}
