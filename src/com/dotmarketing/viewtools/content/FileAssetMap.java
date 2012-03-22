package com.dotmarketing.viewtools.content;

import org.apache.commons.lang.builder.ToStringBuilder;

import com.dotmarketing.exception.DotDataException;
import com.dotmarketing.portlets.fileassets.business.FileAsset;

public class FileAssetMap extends FileAsset {
    @Override
    public String toString() {
        return  ToStringBuilder.reflectionToString(this);
    }
    
    public String getUri() throws DotDataException {
        return super.getURI();
    }
}
