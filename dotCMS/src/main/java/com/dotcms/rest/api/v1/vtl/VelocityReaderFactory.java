package com.dotcms.rest.api.v1.vtl;

public class VelocityReaderFactory {

    public static VelocityReader getVelocityReader(final boolean folderExists) {
        if(folderExists) {
            return new FileVelocityReader();
        } else {
            return new RequestBodyVelocityReader();
        }
    }
}
