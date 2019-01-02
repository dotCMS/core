package com.dotcms.rest.api.v1.vtl;

class VelocityReaderFactory {

    private VelocityReaderFactory() {}

    static VelocityReader getVelocityReader(final boolean folderExists) {
        if(folderExists) {
            return new FileVelocityReader();
        } else {
            return new RequestBodyVelocityReader();
        }
    }
}
