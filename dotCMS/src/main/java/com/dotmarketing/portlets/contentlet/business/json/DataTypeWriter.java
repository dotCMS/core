package com.dotmarketing.portlets.contentlet.business.json;

import java.io.Serializable;

public interface DataTypeWriter {

    Serializable write(Serializable in);

}
