package com.dotcms.security.multipart;

import com.dotmarketing.exception.DotDataException;
import com.dotmarketing.exception.DotSecurityException;
import org.junit.Assert;
import org.junit.Test;

/**
 * {@link SecureFileValidator} Test
 * @author jsanca
 */
public class SecureFileValidatorTest {

    /**
     * This method test validate
     * Given scenario: Happy path the name is ok
     * Expected Result: recovers the filename and nothing happens
     *
     * @throws DotDataException
     * @throws DotSecurityException
     */
    @Test
    public void Test_Validate_happy_path()
            throws DotDataException, DotSecurityException {

        final SecureFileValidator secureFileValidator = new IllegalFileExtensionsValidator();
        final String filenameHeader = "Content-Disposition: attachment; filename=\"filename.jpg\"";
        final String filename = ContentDispositionFileNameParser.parse(filenameHeader);

        Assert.assertEquals("filename.jpg", filename);

        secureFileValidator.validate(filename);
    }

    /**
     * This method test validate
     * Given scenario: the file extension is invalid
     * Expected Result: expected IllegalArgumentException exception
     *
     * @throws DotDataException
     * @throws DotSecurityException
     */
    @Test(expected = IllegalArgumentException.class)
    public void Test_Validate_Illegal_File_ext()
            throws DotDataException, DotSecurityException {

        final SecureFileValidator secureFileValidator = new IllegalFileExtensionsValidator();
        final String filenameHeader = "Content-Disposition: attachment; filename=\"filename.sh\"";
        final String filename = ContentDispositionFileNameParser.parse(filenameHeader);

        Assert.assertEquals("filename.sh", filename);

        secureFileValidator.validate(filename);
    }


    /**
     * This method test validate
     * Given scenario: the file path is invalid
     * Expected Result: expected IllegalArgumentException exception
     *
     * @throws DotDataException
     * @throws DotSecurityException
     */
    @Test(expected = IllegalArgumentException.class)
    public void Test_Validate_Illegal_Path_ext()
            throws DotDataException, DotSecurityException {

        final SecureFileValidator secureFileValidator = new IllegalTraversalFilePathValidator();
        final String filenameHeader = "Content-Disposition: attachment; filename=\"../../../filename.sh\"";
        final String filename = ContentDispositionFileNameParser.parse(filenameHeader);

        secureFileValidator.validate(filename);
    }

}
