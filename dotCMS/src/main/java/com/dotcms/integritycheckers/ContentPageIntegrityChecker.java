package com.dotcms.integritycheckers;

import java.io.File;
import java.io.IOException;

import com.dotmarketing.exception.DotDataException;
import com.dotmarketing.exception.DotSecurityException;
import com.dotmarketing.portlets.structure.model.Structure;
import com.dotmarketing.util.Logger;

/**
 * Contentlet page integrity checker implementation.
 * <p>
 * Note: Only generateCSVFile method is implemented because it has a different
 * output than {@link HtmlPageIntegrityChecker} class. The methods that are not
 * implemented please use them from {@link HtmlPageIntegrityChecker} class.
 * </p>
 * <p>
 * This class is a workaround because we need to support legacy html pages and
 * the new implementation using contentlets type "page"
 * </p>
 * 
 * @author Rogelio Blanco
 * @version 1.0
 * @since 06-10-2015
 * 
 */
public class ContentPageIntegrityChecker extends AbstractIntegrityChecker {

    @Override
    public final IntegrityType getIntegrityType() {
        // Legacy HTML pages and contentlet pages share the same result table
        return IntegrityType.CONTENTPAGES;
    }

    /**
     * Creates CSV file for contenlet HTML Pages information from End Point
     * server.
     * <p>
     * NOTE: This method is required because we need to generate a different
     * .csv file for content pages
     * </p>
     *
     * @param outputPath
     *            - The file containing the list of pages.
     * @return a {@link File} with the page information.
     * @throws DotDataException
     *             An error occurred when querying the database.
     * @throws IOException
     *             An error occurred when writing to the file.
     */
    @Override
    public File generateCSVFile(final String outputPath) throws DotDataException, IOException {
        final String outputFile = outputPath + File.separator
                + getIntegrityType().getDataToCheckCSVName();

        return generateContentletsCSVFile(outputFile, Structure.STRUCTURE_TYPE_HTMLPAGE);
    }

    @Override
    public boolean generateIntegrityResults(final String endpointId) throws Exception {
        Logger.warn(this,
                "Method not implemented, because this process is handle by HtmlPageIntegrityChecker.java");
        return false;
    }

    @Override
    public void executeFix(final String endpointId) throws DotDataException, DotSecurityException {
        Logger.warn(this,
                "Method not implemented, because this process is handle by HtmlPageIntegrityChecker.java");
    }
}
