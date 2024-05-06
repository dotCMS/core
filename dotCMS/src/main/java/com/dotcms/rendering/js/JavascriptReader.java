package com.dotcms.rendering.js;

import com.dotcms.rendering.util.ScriptingReaderParams;
import com.dotmarketing.exception.DotDataException;
import com.dotmarketing.exception.DotSecurityException;

import java.io.IOException;
import java.io.Reader;

/**
 * An interface for different strategies used to read javascript using data from {@link JsResource.JavascriptReaderParams}
 */

public interface JavascriptReader {

    /**
     * Gets the javascript reader for a given parameters
     * @param params JsResource.JavascriptReaderParams
     * @return
     * @throws DotSecurityException
     * @throws IOException
     * @throws DotDataException
     */
    Reader getJavaScriptReader(final ScriptingReaderParams params) throws DotSecurityException, IOException, DotDataException;
}
