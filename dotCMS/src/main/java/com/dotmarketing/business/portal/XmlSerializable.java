package com.dotmarketing.business.portal;

import com.fasterxml.jackson.annotation.JsonIgnore;
import java.io.IOException;

public interface XmlSerializable<T> {


    @JsonIgnore
    default String toXml() throws IOException {
        return XmlUtil.toXml(this);
    }

}
