package com.dotmarketing.portlets.contentlet.business.json.types;

import java.io.Serializable;

public interface DataTypeSerializer <S extends Serializable, T extends Serializable> {

    T write(S in);

    S read(T in);

}
