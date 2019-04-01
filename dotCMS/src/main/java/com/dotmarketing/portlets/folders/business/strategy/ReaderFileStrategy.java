package com.dotmarketing.portlets.folders.business.strategy;

import java.io.IOException;
import java.io.Reader;

/**
 * Strategy to read a file from source
 * @author jsanca
 */
public interface ReaderFileStrategy {

    public final static String SITE_BROWSER_SYSTEM_PREFIX = "//";
    public final static String FILE_SYSTEM_PREFIX         = "file://";
    public final static String S3_SYSTEM_PREFIX           = "s3://";
    public final static String GITHUB_SYSTEM_PREFIX       = "github://";
    public final static String GITLAB_SYSTEM_PREFIX       = "githlab://";

    /**
     * Test if the Strategy could applies for the arguments
     * @param file {@link String}
     * @return boolean
     */
    boolean test(final String file);

    /**
     * Applies the strategy
     * @param file
     * @return Reader
     */
    Reader apply (final String file) throws IOException;

    /**
     * Returns the source associated to the type of file
     * @return Source
     */
    Source    source();

    public enum Source {

        SITE_BROWSER, FILE_SYSTEM, S3, GITHUB, GITLAB
    }
}
