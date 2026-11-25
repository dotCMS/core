package com.dotcms.security.multipart;

import com.dotcms.business.SystemTable;
import com.dotcms.util.IntegrationTestInitService;
import com.dotmarketing.business.APILocator;
import com.dotmarketing.exception.DotDataException;
import com.dotmarketing.exception.DotSecurityException;
import org.junit.Assert;
import org.junit.BeforeClass;
import org.junit.Test;

/**
 * {@link SecureFileValidator} Test
 * @author jsanca
 */
public class SecureFileValidatorTest {

    private static final String ILLEGAL_EXTENSIONS_PROPERTY = "MULTI_PART_ILLEGAL_FILE_EXTENSIONS";

    @BeforeClass
    public static void prepare() throws Exception {
        IntegrationTestInitService.getInstance().init();
    }

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
    public void Test_Validate_Illegal_File_ext() {

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
    public void Test_Validate_Illegal_Path_ext() {

        final SecureFileValidator secureFileValidator = new IllegalTraversalFilePathValidator();
        final String filenameHeader = "Content-Disposition: attachment; filename=\"../../../filename.sh\"";
        final String filename = ContentDispositionFileNameParser.parse(filenameHeader);

        secureFileValidator.validate(filename);
    }


    /**
     * This method test validate
     * Given scenario: the file path is valid but contains series of ..
     * Expected Result: Test should pass
     * @throws DotDataException
     * @throws DotSecurityException
     */
    @Test
    public void Test_Validate_Legal_FileNameWith_N_Dots() {
        final SecureFileValidator secureFileValidator = new IllegalFileExtensionsValidator();
        final String[] files = {
            "Screenshot 2025-03-18 at 4.09.02 p.m..png",
            "Meeting notes 3.15.24 ..draft.docx",
            "Report Q1..Q2 2024.pdf",
            "lol.....png",
            "test...again...png",
            ".....png"
        };
        for (String filename : files) {
            final String filenameHeader = String.format("Content-Disposition: attachment; filename=\"%s\"",filename);
            final String parsed = ContentDispositionFileNameParser.parse(filenameHeader);
            secureFileValidator.validate(parsed);
        }
    }

    /**
     * Method to test: {@link IllegalFileExtensionsValidator#validate(String)}
     * Given scenario: the block list is written to the system table, and later removed, while the
     * same validator (as the upload paths keep one in a static field) is in use
     * Expected Result: the written list applies to the next check without a restart, and removing
     * it puts the default list back
     */
    @Test
    public void Test_Validate_system_table_list_applies_without_restart() {
        final SecureFileValidator secureFileValidator = new IllegalFileExtensionsValidator();
        final SystemTable systemTable = APILocator.getSystemAPI().getSystemTable();
        try {
            secureFileValidator.validate("before.php");
            Assert.assertThrows(IllegalArgumentException.class, () -> secureFileValidator.validate("before.jsp"));

            systemTable.set(ILLEGAL_EXTENSIONS_PROPERTY, ".EXE,.Php");
            Assert.assertThrows(IllegalArgumentException.class, () -> secureFileValidator.validate("configured.php"));
            Assert.assertThrows(IllegalArgumentException.class, () -> secureFileValidator.validate("configured.exe"));
            secureFileValidator.validate("configured.jsp");

            systemTable.delete(ILLEGAL_EXTENSIONS_PROPERTY);
            Assert.assertThrows(IllegalArgumentException.class, () -> secureFileValidator.validate("restored.jsp"));
            secureFileValidator.validate("restored.php");
        } finally {
            systemTable.delete(ILLEGAL_EXTENSIONS_PROPERTY);
        }
    }

    /**
     * Method to test: {@link IllegalFileExtensionsValidator#validate(String)}
     * Given scenario: the system table holds a list without leading dots, then an empty value
     * Expected Result: the entries match all the same, and the empty value leaves the default list
     * in force
     */
    @Test
    public void Test_Validate_system_table_list_without_dots_or_empty() {
        final SecureFileValidator secureFileValidator = new IllegalFileExtensionsValidator();
        final SystemTable systemTable = APILocator.getSystemAPI().getSystemTable();
        try {
            systemTable.set(ILLEGAL_EXTENSIONS_PROPERTY, "jsp,exe");
            Assert.assertThrows(IllegalArgumentException.class, () -> secureFileValidator.validate("nodots.JSP"));
            Assert.assertThrows(IllegalArgumentException.class, () -> secureFileValidator.validate("nodots.exe"));
            secureFileValidator.validate("nodots.bat");

            systemTable.set(ILLEGAL_EXTENSIONS_PROPERTY, "");
            Assert.assertThrows(IllegalArgumentException.class, () -> secureFileValidator.validate("empty.jsp"));
            Assert.assertThrows(IllegalArgumentException.class, () -> secureFileValidator.validate("empty.sh"));
            secureFileValidator.validate("empty.txt");
        } finally {
            systemTable.delete(ILLEGAL_EXTENSIONS_PROPERTY);
        }
    }
}
