package com.dotcms.integritycheckers;

import java.io.File;
import java.io.IOException;

import com.dotcms.rest.IntegrityResource;
import com.dotmarketing.exception.DotDataException;
import com.dotmarketing.exception.DotSecurityException;

/**
 * This is a list of common methods that any integrity checker implementation
 * must has.
 * <p>
 * Any checker class must have a IntegrityType (enum) and implementation to how
 * generate cvs file, check integrity of the data and fix the conflicts is exist
 * any
 * </p>
 * <p>
 * If you want to create a new checker implementation without using the abstract
 * integrity checker class, you need to implement this interface. Otherwise, you
 * can take advantage of some commons methods need it to do integrity checks by
 * extending {@link AbstractIntegrityChecker} class (recommended)
 * </p>
 * <p>
 * The IntegrityType (enum) is located at {@link IntegrityResource} class.
 * </p>
 *
 * @author Rogelio Blanco
 * @version 1.0
 * @since 06-10-2015
 *
 */
public interface IntegrityChecker {

    /**
     * Get the integrity type of the checker implementation
     * 
     * @return IntegrityType
     */
    public IntegrityType getIntegrityType();

    /**
     * Generates a .csv file with all the information need it to check
     * integrity.
     * 
     * @param outputPath
     *            location to store cvs files; for example outputPath =
     *            ConfigUtils.getIntegrityPath() + File.separator + endpointId;
     * @return generated csv file
     * @throws DotDataException
     * @throws IOException
     */
    public File generateCSVFile(final String outputPath) throws DotDataException, IOException;

    /**
     * Checks the integrity of the information generated using the
     * implementation of each checker. Also, it generates and populates the
     * table results to check conflicts
     * 
     * @param endpointId
     *            Server identifier were we need generate conflicts report
     * @return
     * @throws Exception
     */
    public boolean generateIntegrityResults(final String endpointId) throws Exception;

    /**
     * When there is conflicts, this method will fix them depending of the
     * implementation of every checker
     * 
     * @param endpointId
     *            Server identifier were we need to fix conflicts
     * @throws DotDataException
     * @throws DotSecurityException
     */
    public void executeFix(final String endpointId) throws DotDataException, DotSecurityException;

    /**
     * Get temporal table name for an specific endpoint id.
     * <p>
     * Note: the integrity type it will be pick up using getIntegrityType()
     * method of the checker implementation
     * </p>
     * 
     * @param endpointId
     * @return temporal table name
     */
    public String getTempTableName(final String endpointId);

    /**
     * Discard conflicts from an specific endpoint id.
     * <p>
     * Note: the integrity type it will be pick up using getIntegrityType()
     * method of the checker implementation
     * </p>
     * 
     * @param endpointId
     * @throws DotDataException
     */
    public void discardConflicts(final String endpointId) throws DotDataException;

    /**
     * Check if there is integrity conflicts data. This method checks if the
     * result table of an integrity type is not empty
     * 
     * @param endpointId
     * @return true if the results table is NOT empty, otherwise returns false
     * @throws Exception
     */
    public boolean doesIntegrityConflictsDataExist(final String endpointId) throws Exception;
}
