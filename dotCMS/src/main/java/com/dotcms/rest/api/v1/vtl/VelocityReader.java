package com.dotcms.rest.api.v1.vtl;

import com.dotmarketing.exception.DotDataException;
import com.dotmarketing.exception.DotSecurityException;

import java.io.IOException;
import java.io.Reader;

public interface VelocityReader {
    Reader getVelocity(final VTLResource.VelocityReaderParams params) throws DotSecurityException, IOException, DotDataException;
}
