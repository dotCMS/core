package com.dotcms.rest.api.v1.vtl;

import com.dotmarketing.exception.DotDataException;
import com.dotmarketing.exception.DotSecurityException;

import java.io.IOException;
import java.io.Reader;

/**
 * An interface for different strategies used to read velocity using data from {@link VTLResource.VelocityReaderParams}
 */

public interface VelocityReader {

    /**
     * Gets the velocity reader for a given parameters
     * @param params
     * @param pageMode
     * @return
     * @throws DotSecurityException
     * @throws IOException
     * @throws DotDataException
     */
    Reader getVelocity(final VTLResource.VelocityReaderParams params) throws DotSecurityException, IOException, DotDataException;
}
