package com.dotmarketing.business.portal;

import java.io.IOException;

interface XMLSerializable {


    default String toXml() throws IOException {
        return SerializationHelper.toXml(this);
    }

}
