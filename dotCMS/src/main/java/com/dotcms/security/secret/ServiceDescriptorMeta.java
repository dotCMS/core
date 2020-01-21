package com.dotcms.security.secret;

class ServiceDescriptorMeta {

    private final ServiceDescriptor serviceDescriptor;

    private final String fileName;

    ServiceDescriptorMeta(final ServiceDescriptor serviceDescriptor, final String fileName) {
        this.serviceDescriptor = serviceDescriptor;
        this.fileName = fileName;
    }

    public ServiceDescriptor getServiceDescriptor() {
        return serviceDescriptor;
    }

    public String getFileName() {
        return fileName;
    }
}
