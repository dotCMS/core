package com.dotcms.util;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertNotNull;
import static org.junit.Assert.assertTrue;

import java.io.ByteArrayInputStream;
import java.io.File;
import java.io.IOException;
import java.io.InputStreamReader;
import java.io.Reader;
import java.nio.charset.Charset;
import java.util.Date;
import java.util.HashMap;
import java.util.List;

import org.junit.BeforeClass;
import org.junit.Test;

import com.dotcms.IntegrationTestBase;
import com.dotcms.repackage.com.csvreader.CsvReader;
import com.dotcms.repackage.org.apache.commons.io.FileUtils;
import com.dotmarketing.beans.Host;
import com.dotmarketing.business.APILocator;
import com.dotmarketing.common.model.ContentletSearch;
import com.dotmarketing.exception.DotDataException;
import com.dotmarketing.exception.DotSecurityException;
import com.dotmarketing.portlets.contentlet.business.ContentletAPI;
import com.dotmarketing.portlets.contentlet.model.Contentlet;
import com.dotmarketing.portlets.languagesmanager.model.Language;
import com.dotmarketing.portlets.structure.factories.FieldFactory;
import com.dotmarketing.portlets.structure.factories.StructureFactory;
import com.dotmarketing.portlets.structure.model.Field;
import com.dotmarketing.portlets.structure.model.Structure;
import com.dotmarketing.util.ImportUtil;
import com.liferay.portal.model.User;

/**
 * Verifies that the Content Importer/Exporter feature is working as expected.
 * Users can import and export contents from the Content Search page.
 * 
 * @author Jonathan Gamba Date: 3/10/14
 */
public class ImportUtilTest extends IntegrationTestBase {

    private static User user;
    private static Host defaultSite;
    private static Language defaultLanguage;

    @BeforeClass
    public static void prepare () throws Exception {
    	//Setting web app environment
        IntegrationTestInitService.getInstance().init();
        user = APILocator.getUserAPI().getSystemUser();
        defaultSite = APILocator.getHostAPI().findDefaultHost( user, false );
        defaultLanguage = APILocator.getLanguageAPI().getDefaultLanguage();
    }

    /**
     * Testing the {@link ImportUtil#importFile(Long, String, String, String[], boolean, boolean, com.liferay.portal.model.User, long, String[], com.dotcms.repackage.javacsv.com.csvreader.CsvReader, int, int, java.io.Reader)} method
     *
     * @throws DotDataException
     * @throws DotSecurityException
     * @throws IOException
     * @throws InterruptedException
     */
    @Test
    public void importFile () throws DotDataException, DotSecurityException, IOException, InterruptedException {

        ContentletAPI contentletAPI = APILocator.getContentletAPI();

        //Create a test Content Type
        String contentTypeSuffix = String.valueOf( new Date().getTime() );
        Structure contentType = new Structure();
        contentType.setName( "Import Test " + contentTypeSuffix );
        contentType.setVelocityVarName( "ImportTest_" + contentTypeSuffix );
        contentType.setDescription( "Testing import of csv files" );

        StructureFactory.saveStructure( contentType );

        //Create test fields
        Field textField = new Field( "Title", Field.FieldType.TEXT, Field.DataType.TEXT, contentType, true, true, true, 1, false, false, true );
        textField = FieldFactory.saveField( textField );
        final String textFieldVarName = textField.getVelocityVarName();

        Field siteField = new Field( "Host", Field.FieldType.HOST_OR_FOLDER, Field.DataType.TEXT, contentType, true, true, true, 2, false, false, true );
        siteField = FieldFactory.saveField( siteField );
        final String siteFieldVarName = siteField.getVelocityVarName();

        //----------------PREVIEW = TRUE------------------------------------------
        //------------------------------------------------------------------------
        //Create the csv file to import
        Reader reader = createTempFile( textFieldVarName + ", " + siteFieldVarName + "\r\n" +
                "Test1, " + defaultSite.getIdentifier() + "\r\n" +
                "Test2, " + defaultSite.getIdentifier() + "\r\n" +
                "Test3, " + defaultSite.getIdentifier() + "\r\n" +
                "Test4, " + defaultSite.getIdentifier() + "\r\n" );
        CsvReader csvreader = new CsvReader( reader );
        csvreader.setSafetySwitch( false );
        String[] csvHeaders = csvreader.getHeaders();

        //Preview=true
        HashMap<String, List<String>> results = ImportUtil.importFile( 0L, defaultSite.getInode(), contentType.getInode(), new String[]{}, true, false, user, defaultLanguage.getId(), csvHeaders, csvreader, -1, -1, reader );
        //Validations
        validate( results, true, false, true );

        //As it was a preview nothing should be saved
        List<Contentlet> savedData = contentletAPI.findByStructure( contentType.getInode(), user, false, 0, 0 );
        //Validations
        assertNotNull( savedData );
        assertEquals( savedData.size(), 0 );

        //----------------PREVIEW = FALSE-----------------------------------------
        //------------------------------------------------------------------------
        //Create the csv file to import
        reader = createTempFile( textFieldVarName + ", " + siteFieldVarName + "\r\n" +
                "Test1, " + defaultSite.getIdentifier() + "\r\n" +
                "Test2, " + defaultSite.getIdentifier() + "\r\n" +
                "Test3, " + defaultSite.getIdentifier() + "\r\n" +
                "Test4, " + defaultSite.getIdentifier() + "\r\n" );
        csvreader = new CsvReader( reader );
        csvreader.setSafetySwitch( false );
        csvHeaders = csvreader.getHeaders();

        //Preview=false
        results = ImportUtil.importFile( 0L, defaultSite.getInode(), contentType.getInode(), new String[]{}, false, false, user, defaultLanguage.getId(), csvHeaders, csvreader, -1, -1, reader );
        //Validations
        validate( results, false, false, true );

        //Now we should have saved data
        savedData = contentletAPI.findByStructure( contentType.getInode(), user, false, 0, 0 );
        //Validations
        assertNotNull( savedData );
        assertEquals( savedData.size(), 4 );

        //----------------USING WRONG HOST IDENTIFIERS----------------------------
        //------------------------------------------------------------------------
        //Create the csv file to import
        reader = createTempFile( textFieldVarName + ", " + siteFieldVarName + "\r\n" +
                "Test5, " + defaultSite.getIdentifier() + "\r\n" +
                "Test6, " + "999-99999999-99999999-00000" + "\r\n" +
                "Test7, " + "44444444-5555555555-2222" + "\r\n" );
        csvreader = new CsvReader( reader );
        csvreader.setSafetySwitch( false );
        csvHeaders = csvreader.getHeaders();

        //Preview=true
        results = ImportUtil.importFile( 0L, defaultSite.getInode(), contentType.getInode(), new String[]{}, true, false, user, defaultLanguage.getId(), csvHeaders, csvreader, -1, -1, reader );
        //Validations
        validate( results, true, true, true );

        //We should have the same amount on data
        savedData = contentletAPI.findByStructure( contentType.getInode(), user, false, 0, 0 );
        //Validations
        assertNotNull( savedData );
        assertEquals( savedData.size(), 4 );

        //---------------USING KEY FIELDS-----------------------------------------
        //------------------------------------------------------------------------

        //Making sure the contentlets are in the indexes
        List<ContentletSearch> contentletSearchResults;
        int x = 0;
        do {
            Thread.sleep( 200 );
            //Verify if it was added to the index
            contentletSearchResults = contentletAPI.searchIndex( "+structureName:" + contentType.getVelocityVarName() + " +working:true +deleted:false +" + contentType.getVelocityVarName() + ".title:Test1 +languageId:1", 0, -1, null, user, true );
            x++;
        } while ( (contentletSearchResults == null || contentletSearchResults.isEmpty()) && x < 100 );

        //Create the csv file to import
        reader = createTempFile( textFieldVarName + ", " + siteFieldVarName + "\r\n" +
                "Test1, " + defaultSite.getIdentifier() + "\r\n" +
                "Test2, " + defaultSite.getIdentifier() + "\r\n" );
        csvreader = new CsvReader( reader );
        csvreader.setSafetySwitch( false );
        csvHeaders = csvreader.getHeaders();

        //Preview=false
        results = ImportUtil.importFile( 0L, defaultSite.getInode(), contentType.getInode(), new String[]{textField.getInode()}, false, false, user, defaultLanguage.getId(), csvHeaders, csvreader, -1, -1, reader );
        //Validations
        validate( results, false, false, true );//We should expect warnings: Line #X. The key fields chosen match 1 existing content(s) - more than one match suggests key(s) are not properly unique

        //We used the key fields, so the import process should update instead to add new records
        savedData = contentletAPI.findByStructure( contentType.getInode(), user, false, 0, 0 );
        //Validations
        assertNotNull( savedData );
        assertEquals( savedData.size(), 4 );

        //---------------USING IDENTIFIER COLUMN----------------------------------
        //------------------------------------------------------------------------
        //Create the csv file to import

        String id1 = null;
        String id2 = null;
        for ( Contentlet content : savedData ) {
            if ( content.getMap().get( "title" ).equals( "Test1" ) ) {
                id1 = content.getIdentifier();
            } else if ( content.getMap().get( "title" ).equals( "Test2" ) ) {
                id2 = content.getIdentifier();
            }
        }

        reader = createTempFile( "Identifier, title, host" + "\r\n" +
                id1 + ", Test1_edited, " + defaultSite.getIdentifier() + "\r\n" +
                id2 + ", Test2_edited, " + defaultSite.getIdentifier() + "\r\n" );
        csvreader = new CsvReader( reader );
        csvreader.setSafetySwitch( false );
        csvHeaders = csvreader.getHeaders();

        //Preview=false
        results = ImportUtil.importFile( 0L, defaultSite.getInode(), contentType.getInode(), new String[]{}, false, false, user, defaultLanguage.getId(), csvHeaders, csvreader, -1, -1, reader );
        //Validations
        validate( results, false, false, true );

        //We used a identifier column, so the import process should update instead to add new records
        savedData = contentletAPI.findByStructure( contentType.getInode(), user, false, 0, 0 );
        //Validations
        assertNotNull( savedData );
        assertEquals( savedData.size(), 4 );

        //-------------------------LANGUAGE AND KEY FIELDS------------------------
        //------------------------------------------------------------------------
        //Create the csv file to import
        reader = createTempFile( "languageCode, countryCode, title, host" + "\r\n" +
                "es, ES, Test1_edited, " + defaultSite.getIdentifier() + "\r\n" +
                "es, ES, Test2_edited, " + defaultSite.getIdentifier() + "\r\n" );
        csvreader = new CsvReader( reader );
        csvreader.setSafetySwitch( false );
        csvHeaders = csvreader.getHeaders();

        int languageCodeHeaderColumn = 0;
        int countryCodeHeaderColumn = 1;
        //Preview=false
        results = ImportUtil.importFile( 0L, defaultSite.getInode(), contentType.getInode(), new String[]{textField.getInode()}, false, true, user, -1, csvHeaders, csvreader, languageCodeHeaderColumn, countryCodeHeaderColumn, reader );
        //Validations
        validate( results, false, false, false );

        //We used the key fields, so the import process should update instead to add new records
        savedData = contentletAPI.findByStructure( contentType.getInode(), user, false, 0, 0 );
        //Validations
        assertNotNull( savedData );
        assertEquals( savedData.size(), 6 );

        //Validate we saved the contentlets on spanish
        int spanishFound = 0;
        for ( Contentlet contentlet : savedData ) {
            if ( contentlet.getLanguageId() == 2 ) {
                spanishFound++;
            }
        }
        assertEquals( spanishFound, 2 );
    }

    /**
     * Creates a temporal file using a given content
     *
     * @param content
     * @return
     * @throws IOException
     */
    private Reader createTempFile ( String content ) throws IOException {

        File tempTestFile = File.createTempFile( "csvTest_" + String.valueOf( new Date().getTime() ), ".txt" );
        FileUtils.writeStringToFile( tempTestFile, content );
        byte[] bytes = com.liferay.util.FileUtil.getBytes( tempTestFile );

        return new InputStreamReader( new ByteArrayInputStream( bytes ), Charset.forName( "UTF-8" ) );
    }

    /**
     * Validates a given result generated by the ImportUtil.importFile method
     *
     * @param results
     * @param preview
     * @param expectingErrors
     * @param expectingWarnings
     */
    private void validate ( HashMap<String, List<String>> results, Boolean preview, Boolean expectingErrors, Boolean expectingWarnings ) {

        //Reading the results

        if ( expectingErrors ) {
            List<String> errors = results.get( "errors" );
            assertNotNull( errors );//Expected warnings as no key fields were chosen
            assertTrue( !errors.isEmpty() );
        } else {
            List<String> errors = results.get( "errors" );
            assertTrue( errors == null || errors.isEmpty() );//No errors should be found
        }

        if ( expectingWarnings ) {
            List<String> warnings = results.get( "warnings" );
            assertNotNull( warnings );//Expected warnings as no key fields were chosen
            assertTrue( !warnings.isEmpty() );
        } else {
            List<String> warnings = results.get( "warnings" );
            assertTrue( warnings == null || warnings.isEmpty() );
        }

        List<String> finalResults = results.get( "results" );
        assertNotNull( finalResults );//Expected final results messages
        assertTrue( !finalResults.isEmpty() );

        List<String> messages = results.get( "messages" );
        assertNotNull( messages );//Expected return messages
        assertTrue( !messages.isEmpty() );

        if ( !preview ) {

            List<String> lastInode = results.get( "lastInode" );
            assertNotNull( lastInode );
            assertTrue( !lastInode.isEmpty() );

            List<String> counters = results.get( "counters" );
            assertNotNull( counters );
            assertTrue( !counters.isEmpty() );
        }
    }

}
