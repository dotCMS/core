package com.dotmarketing.util;

import java.io.IOException;
import java.io.Reader;
import java.io.StringWriter;
import java.sql.Timestamp;
import java.text.DateFormat;
import java.text.ParseException;
import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.Collection;
import java.util.Date;
import java.util.HashMap;
import java.util.HashSet;
import java.util.Iterator;
import java.util.List;
import java.util.Map;
import java.util.Set;

import com.dotcms.repackage.com.csvreader.CsvReader;
import com.dotmarketing.beans.Host;
import com.dotmarketing.beans.Identifier;
import com.dotmarketing.beans.Permission;
import com.dotmarketing.business.APILocator;
import com.dotmarketing.business.CacheLocator;
import com.dotmarketing.business.DotStateException;
import com.dotmarketing.business.FactoryLocator;
import com.dotmarketing.business.PermissionAPI;
import com.dotmarketing.cache.FieldsCache;
import com.dotmarketing.common.model.ContentletSearch;
import com.dotmarketing.db.HibernateUtil;
import com.dotmarketing.exception.DotDataException;
import com.dotmarketing.exception.DotRuntimeException;
import com.dotmarketing.exception.DotSecurityException;
import com.dotmarketing.portlets.categories.business.CategoryAPI;
import com.dotmarketing.portlets.categories.model.Category;
import com.dotmarketing.portlets.contentlet.action.ImportAuditUtil;
import com.dotmarketing.portlets.contentlet.action.ImportContentletsAction;
import com.dotmarketing.portlets.contentlet.business.ContentletAPI;
import com.dotmarketing.portlets.contentlet.business.DotContentletStateException;
import com.dotmarketing.portlets.contentlet.business.DotContentletValidationException;
import com.dotmarketing.portlets.contentlet.business.HostAPI;
import com.dotmarketing.portlets.contentlet.model.Contentlet;
import com.dotmarketing.portlets.folders.business.FolderAPI;
import com.dotmarketing.portlets.folders.model.Folder;
import com.dotmarketing.portlets.languagesmanager.business.LanguageAPI;
import com.dotmarketing.portlets.languagesmanager.model.Language;
import com.dotmarketing.portlets.structure.factories.FieldFactory;
import com.dotmarketing.portlets.structure.model.ContentletRelationships;
import com.dotmarketing.portlets.structure.model.Field;
import com.dotmarketing.portlets.structure.model.Relationship;
import com.dotmarketing.portlets.structure.model.Structure;
import com.liferay.portal.language.LanguageException;
import com.liferay.portal.language.LanguageUtil;
import com.liferay.portal.model.User;

/**
 * Provides utility methods to import content into dotCMS. The data source is a
 * CSV file, which can be previously analyzed and evaluated by this utility
 * <b>before</b> importing its data as contentlets. Either analyzing or not
 * analyzing the data in a CSV file before importing it will generate a summary
 * of the operation, indicating inconsistencies, errors, warnings, or just
 * useful information for the user.
 * 
 * @author root
 * @version 1.x
 * @since Mar 22, 2012
 *
 */
public class ImportUtil {

    private static PermissionAPI permissionAPI = APILocator.getPermissionAPI();
    private final static ContentletAPI conAPI = APILocator.getContentletAPI();
    private final static CategoryAPI catAPI = APILocator.getCategoryAPI();
    private final static LanguageAPI langAPI = APILocator.getLanguageAPI();
    private final static HostAPI hostAPI = APILocator.getHostAPI();
    private final static FolderAPI folderAPI = APILocator.getFolderAPI();

    private final static String languageCodeHeader = "languageCode";
    private final static String countryCodeHeader = "countryCode";

    private final static int commitGranularity = 10;
    private final static int sleepTime = 200;

    public static final String[] IMP_DATE_FORMATS = new String[] { "d-MMM-yy", "MMM-yy", "MMMM-yy", "d-MMM", "dd-MMM-yyyy",
        "MM/dd/yy hh:mm aa", "MM/dd/yyyy hh:mm aa",	"MM/dd/yy HH:mm", "MM/dd/yyyy HH:mm", "MMMM dd, yyyy", "M/d/y", "M/d",
        "EEEE, MMMM dd, yyyy", "MM/dd/yyyy", "hh:mm:ss aa", "HH:mm:ss", "hh:mm aa", "yyyy-MM-dd" };

    private static final SimpleDateFormat DATE_FIELD_FORMAT = new SimpleDateFormat("yyyyMMdd");

	/**
	 * Imports the data contained in a CSV file into dotCMS. The data can be
	 * either new or an update for existing content. The {@code preview}
	 * parameter determines the behavior of this method:
	 * <ul>
	 * <li>{@code preview == true}: This is the ideal approach. The data
	 * contained in the CSV file is previously analyzed and evaluated
	 * <b>BEFORE</b> actually committing any changes to existing contentlets or
	 * adding new ones. This way, users can perform the appropriate corrections
	 * (if needed) before submitting the new contents.</li>
	 * <li>{@code preview == false}: Setting the parameter this way will make
	 * the system try to import the contents right away. The method will also
	 * return a summary with the status of the operation.</li>
	 * </ul>
	 * 
	 * @param importId
	 *            - The ID of this data import.
	 * @param currentSiteId
	 *            - The ID of the Site where the content will be added/updated.
	 * @param contentTypeInode
	 *            - The Inode of the Content Type that the content is associated
	 *            to.
	 * @param keyfields
	 *            - The Inodes of the fields used to associated existing dotCMS
	 *            contentlets with the information in this file. Can be empty.
	 * @param preview
	 *            - Set to {@code true} if an analysis and evaluation of the
	 *            imported data will be generated <b>before</b> actually
	 *            importing the data. Otherwise, set to {@code false}.
	 * @param isMultilingual
	 *            - If set to {@code true}, the CSV file will import contents in
	 *            more than one language. Otherwise, set to {@code false}.
	 * @param user
	 *            - The {@link User} performing this action.
	 * @param language
	 *            - The language ID for the contents. If the ID equals -1, the
	 *            columns for language code and country code will be used to
	 *            infer the language ID.
	 * @param csvHeaders
	 *            - The headers for each column in the CSV file.
	 * @param csvreader
	 *            - The actual data contained in the CSV file.
	 * @param languageCodeHeaderColumn
	 *            - The column name containing the language code.
	 * @param countryCodeHeaderColumn
	 *            - The column name containing the country code.
	 * @param reader
	 *            - The character streams reader.
	 * @return The resulting analysis performed on the CSV file. This provides
	 *         information regarding inconsistencies, errors, warnings and/or
	 *         precautions to the user.
	 * @throws DotRuntimeException
	 *             An error occurred when analyzing the CSV file.
	 * @throws DotDataException
	 *             An error occurred when analyzing the CSV file.
	 */
    public static HashMap<String, List<String>> importFile(Long importId, String currentSiteId, String contentTypeInode, String[] keyfields, boolean preview, boolean isMultilingual, User user, long language, String[] csvHeaders, CsvReader csvreader, int languageCodeHeaderColumn, int countryCodeHeaderColumn, Reader reader)
            throws DotRuntimeException, DotDataException {

        HashMap<String, List<String>> results = new HashMap<String, List<String>>();
        results.put("warnings", new ArrayList<String>());
        results.put("errors", new ArrayList<String>());
        results.put("messages", new ArrayList<String>());
        results.put("results", new ArrayList<String>());
        results.put("counters", new ArrayList<String>());
        results.put("identifiers", new ArrayList<String>());
        results.put("updatedInodes", new ArrayList<String>());
        results.put("lastInode", new ArrayList<String>());

        Structure contentType = CacheLocator.getContentTypeCache().getStructureByInode (contentTypeInode);
        List<Permission> contentTypePermissions = permissionAPI.getPermissions(contentType);
        List<UniqueFieldBean> uniqueFieldBeans = new ArrayList<UniqueFieldBean>();
        List<Field> uniqueFields = new ArrayList<Field>();

        //Initializing variables
        int lines = 0;
        int errors = 0;
        int lineNumber = 0;

        Counters counters = new Counters();
        HashSet<String> keyContentUpdated = new HashSet<String>();
        StringBuffer choosenKeyField = new StringBuffer();

        HashMap<Integer, Field> headers = new HashMap<Integer, Field>();
        HashMap<Integer, Field> keyFields = new HashMap<Integer, Field>();
        HashMap<Integer, Relationship> relationships = new HashMap<Integer, Relationship>();

        //Get unique fields for structure
        for(Field field : FieldsCache.getFieldsByStructureInode(contentType.getInode())){
            if(field.isUnique()){
                uniqueFields.add(field);
            }
        }

        //Parsing the file line per line
        try {
            if ((csvHeaders != null) || (csvreader.readHeaders())) {
                //Importing headers from the first file line
                HashMap<Integer,Boolean> onlyParent=new HashMap<Integer,Boolean>();
                HashMap<Integer,Boolean> onlyChild=new HashMap<Integer,Boolean>();
                if (csvHeaders != null) {
                    importHeaders(csvHeaders, contentType, keyfields, preview, isMultilingual, user, results, headers, keyFields, uniqueFields,relationships,onlyChild,onlyParent);
                } else {
                    importHeaders(csvreader.getHeaders(), contentType, keyfields, preview, isMultilingual, user, results, headers, keyFields, uniqueFields,relationships,onlyChild,onlyParent);
                }
                lineNumber++;

                //Reading the whole file
                if (headers.size() > 0) {
                    if (!preview) {
                        HibernateUtil.startTransaction();
                    }
                    String[] csvLine;
                    while (csvreader.readRecord()) {
                        if(ImportAuditUtil.cancelledImports.containsKey(importId)){
                            break;
                        }
                        lineNumber++;
                        csvLine = csvreader.getValues();
                        try {
                            lines++;
                            Logger.debug(ImportUtil.class, "Line " + lines + ": (" + csvreader.getRawRecord() + ").");

                            //Importing a line
                            Long languageToImport = language;
                            if ( language == -1 ) {
                                if ( languageCodeHeaderColumn != -1 && countryCodeHeaderColumn != -1 ) {
                                    Language dotCMSLanguage = langAPI.getLanguage( csvLine[languageCodeHeaderColumn], csvLine[countryCodeHeaderColumn] );
                                    languageToImport = dotCMSLanguage.getId();
                                }
                            }

                            if ( languageToImport != -1 ) {
                                /*
                                Verifies if there was already imported a record with the same keys.
                                Useful to know if we have batch uploads with the same keys, mostly visible for batch content uploads with multiple languages
                                 */
                                boolean sameKeyBatchInsert = true;
                                if ( keyFields != null && !keyFields.isEmpty() ) {
                                    for ( Integer column : keyFields.keySet() ) {
                                        Field keyField = keyFields.get( column );
										if (!counters.matchKey(keyField.getVelocityVarName(), csvLine[column])) {
                                            sameKeyBatchInsert = false;
                                            break;
                                        }
                                    }
                                }

                                //Importing content record...
                                importLine( csvLine, currentSiteId, contentType, preview, isMultilingual, user, results, lineNumber, languageToImport, headers, keyFields, choosenKeyField,
                                        counters, keyContentUpdated, contentTypePermissions, uniqueFieldBeans, uniqueFields, relationships, onlyChild, onlyParent, sameKeyBatchInsert );

                                //Storing the record keys we just imported for a later reference...
                                if ( keyFields != null && !keyFields.isEmpty() ) {
                                    for ( Integer column : keyFields.keySet() ) {
                                        Field keyField = keyFields.get( column );
										counters.addKey(keyField.getVelocityVarName(), csvLine[column]);
                                    }
                                }
                            } else {
                                results.get( "errors" ).add( LanguageUtil.get( user, "Line--" ) + lineNumber + LanguageUtil.get( user, "Locale-not-found-for-languageCode" ) + " ='" + csvLine[languageCodeHeaderColumn] + "' countryCode='" + csvLine[countryCodeHeaderColumn] + "'" );
                                errors++;
                            }

                            if ( !preview && (lineNumber % commitGranularity == 0) ) {
                                HibernateUtil.closeAndCommitTransaction();
                                Thread.sleep( sleepTime );
                                HibernateUtil.startTransaction();
                            }
                        } catch ( DotRuntimeException ex ) {
                            String errorMessage = ex.getMessage();
                            if(errorMessage.indexOf("Line #") == -1){
                                errorMessage = "Line #"+lineNumber+" "+errorMessage;
                            }
                            results.get("errors").add(errorMessage);
                            errors++;
                            Logger.info(ImportUtil.class, "Error line: " + lines + " (" + csvreader.getRawRecord()
                                    + "). Line Ignored.");
                        }
                    }

                    if(!preview){
                        results.get("counters").add("linesread="+lines);
                        results.get("counters").add("errors="+errors);
                        results.get("counters").add("newContent="+counters.getNewContentCounter());
                        results.get("counters").add("contentToUpdate="+counters.getContentToUpdateCounter());
                        HibernateUtil.closeAndCommitTransaction();
                    }

                    results.get("messages").add(lines + " "+LanguageUtil.get(user, "lines-of-data-were-read" ));
                    if (errors > 0) {
                        results.get("errors").add(errors + " " + LanguageUtil.get(user, "input-lines-had-errors" ));
                    }
                    if(preview && choosenKeyField.length() > 1) {
                        results.get("messages").add( LanguageUtil.get(user, "Fields-selected-as-key")+": "+choosenKeyField.substring(1).toString()+".");
                    }
                    if (counters.getNewContentCounter() > 0) {
                        results.get("messages").add(LanguageUtil.get(user, "Attempting-to-create") + " " + (counters.getNewContentCounter()) + " contentlets - " + LanguageUtil.get(user, "check-below-for-errors"));
                    }
                    if (counters.getContentToUpdateCounter() > 0) {
                        results.get("messages").add(LanguageUtil.get(user, "Approximately") + " " + (counters.getContentToUpdateCounter()) + " " + LanguageUtil.get(user, "old-content-will-be-updated"));
                    }

                    results.get("results").add(counters.getContentCreated() + " "+LanguageUtil.get(user, "new")+" "+"\"" + contentType.getName() + "\" "+ LanguageUtil.get(user, "were-created"));
                    results.get("results").add(counters.getContentUpdatedDuplicated() + " \"" + contentType.getName() + "\" "+ LanguageUtil.get(user, "contentlets-updated-corresponding-to")+" "+ counters.getContentUpdated() +" "+ LanguageUtil.get(user, "repeated-contents-based-on-the-key-provided"));

                    if (errors > 0) {
                        results.get("results").add(errors + " "+ LanguageUtil.get(user, "contentlets-were-ignored-due-to-invalid-information"));
                    }
                } else {
                    results.get("errors").add(LanguageUtil.get(user, "No-headers-found-on-the-file-nothing-will-be-imported"));
                }
            }
        } catch (Exception e) {
            Logger.error(ImportContentletsAction.class,e.getMessage());

        } finally {
            if (reader != null) {
                try {
                    reader.close();
                } catch (IOException e) {
                	// Reader could not be closed. Continue
                }
            }
        }
        Logger.info(ImportUtil.class, lines + " lines read correctly. " + errors + " errors found.");
        return results;
    }

	/**
	 * Reads the CSV file headers in order to find inconsistencies or errors.
	 * Such situations will be saved in the {@code results} list.
	 * 
	 * @param headerLine
	 *            - The line in the CSV file containing the data headers.
	 * @param contentType
	 *            - The Content Type that the data in this file is associated
	 *            to.
	 * @param keyFieldsInodes
	 *            - The Inodes of the fields used to associated existing dotCMS
	 *            contentlets with the information in this file. Can be empty.
	 * @param preview
	 *            - Set to {@code true} if an analysis and evaluation of the
	 *            imported data will be generated <b>before</b> actually
	 *            importing the data. Otherwise, set to {@code false}.
	 * @param isMultilingual
	 *            - If set to {@code true}, the CSV file will import contents in
	 *            more than one language. Otherwise, set to {@code false}.
	 * @param user
	 *            - The {@link User} performing this action.
	 * @param results
	 *            - The status object that keeps track of potential errors,
	 *            inconsistencies, or warnings.
	 * @param headers
	 * @param keyFields
	 *            - The fields used to associated existing dotCMS contentlets
	 *            with the information in this file. Can be empty.
	 * @param uniqueFields
	 *            - The list of fields that are unique (if any).
	 * @param relationships
	 *            - Content relationships (if any).
	 * @param onlyChild
	 *            - Contains content relationships that are only child
	 *            relationships (header name ends with {@code "-RELCHILD"}).
	 * @param onlyParent
	 *            - Contains content relationships that are only parent
	 *            relationships (header name ends with {@code "-RELPARENT"}).
	 * @throws Exception
	 *             An error occurred when validating the CSV data.
	 */
    private static void importHeaders(String[] headerLine, Structure contentType, String[] keyFieldsInodes, boolean preview, boolean isMultilingual, User user, HashMap<String, List<String>> results, HashMap<Integer, Field> headers, HashMap<Integer, Field> keyFields, List<Field> uniqueFields, HashMap<Integer, Relationship> relationships,HashMap<Integer,Boolean> onlyChild, HashMap<Integer,Boolean> onlyParent) throws Exception  {

        int importableFields = 0;

        //Importing headers and storing them in a hashmap to be reused later in the whole import process
        List<Field> fields = FieldsCache.getFieldsByStructureInode(contentType.getInode());
        List<Relationship> contentTypeRelationships = FactoryLocator.getRelationshipFactory().byContentType(contentType);
        List<String> requiredFields = new ArrayList<String>();
        List<String> headerFields = new ArrayList<String>();
        for(Field field:fields){
            if(field.isRequired()){
            	requiredFields.add(field.getVelocityVarName());
            }
        }
        for (int i = 0; i < headerLine.length; i++) {
            boolean found = false;
            String header = headerLine[i].replaceAll("'", "");

            if (header.equalsIgnoreCase("Identifier")) {
                results.get("messages").add(LanguageUtil.get(user, "identifier-field-found-in-import-contentlet-csv-file"));
                results.get("identifiers").add("" + i);
                continue;
            }

            headerFields.add(header);

            for (Field field : fields) {
            	if (field.getVelocityVarName().equalsIgnoreCase(header)) {
                    if (field.getFieldType().equals(Field.FieldType.BUTTON.toString())){
                        found = true;

                        results.get("warnings").add(
                                LanguageUtil.get(user, "Header")+": \"" + header

                                +"\" "+ LanguageUtil.get(user, "matches-a-field-of-type-button-this-column-of-data-will-be-ignored"));
                    }
                    else if (field.getFieldType().equals(Field.FieldType.BINARY.toString())){
                        found = true;
                        results.get("warnings").add(
                                LanguageUtil.get(user, "Header")+": \"" + header
                                + "\" "+ LanguageUtil.get(user, "matches-a-field-of-type-binary-this-column-of-data-will-be-ignored"));
                    }
                    else if (field.getFieldType().equals(Field.FieldType.LINE_DIVIDER.toString())){
                        found = true;
                        results.get("warnings").add(
                                LanguageUtil.get(user, "Header")+": \"" + header
                                + "\" "+LanguageUtil.get(user, "matches-a-field-of-type-line-divider-this-column-of-data-will-be-ignored"));
                    }
                    else if (field.getFieldType().equals(Field.FieldType.TAB_DIVIDER.toString())){
                        found = true;
                        results.get("warnings").add(
                                LanguageUtil.get(user, "Header")+": \"" + header
                                + "\" "+LanguageUtil.get(user, "matches-a-field-of-type-tab-divider-this-column-of-data-will-be-ignored"));
                    }
                    else {
                        found = true;
                        headers.put(i, field);
                        for (String fieldInode : keyFieldsInodes) {
                            if (fieldInode.equals(field.getInode()))
                                keyFields.put(i, field);
                        }
                        break;
                    }
                }
            }

            /*
             * We gonna delete -RELPARENT -RELCHILD so we can
             * search for the relation name. No problem as
             * we put relationships.put(i,relationship) instead
             * of header.
             */
            boolean onlyP=false;
            if(header.endsWith("-RELPARENT")) {
                header = header.substring(0,header.lastIndexOf("-RELPARENT"));
                onlyP=true;
            }

            boolean onlyCh=false;
            if(header.endsWith("-RELCHILD")) {
                header = header.substring(0,header.lastIndexOf("-RELCHILD"));
                onlyCh=true;
            }

            //Check if the header is a relationship
            for(Relationship relationship : contentTypeRelationships)
            {
                if(relationship.getRelationTypeValue().equalsIgnoreCase(header))
                {
                    found = true;
                    relationships.put(i,relationship);
                    onlyParent.put(i, onlyP);
                    onlyChild.put(i, onlyCh);

                    // special case when the relationship has the same structure for parent and child, set only as child
                    if(relationship.getChildStructureInode().equals(relationship.getParentStructureInode()) && !onlyCh && !onlyP) {
                        onlyChild.put(i, true);
                    }
                }
            }

            if ((!found) && !(isMultilingual && (header.equals(languageCodeHeader) || header.equals(countryCodeHeader)))) {
                results.get("warnings").add(
                        LanguageUtil.get(user, "Header")+": \"" + header
                        + "\""+ " "+ LanguageUtil.get(user, "doesn-t-match-any-structure-field-this-column-of-data-will-be-ignored"));
            }
        }

        requiredFields.removeAll(headerFields);

        for(String requiredField: requiredFields){
            results.get("errors").add(LanguageUtil.get(user, "Field")+": \"" + requiredField+ "\" "+LanguageUtil.get(user, "required-field-not-found-in-header"));
        }

        for (Field field : fields) {
            if (isImportableField(field)){
                importableFields++;
            }
        }

        //Checking keyField selected by the user against the headers
        for (String keyField : keyFieldsInodes) {
            boolean found = false;
            for (Field headerField : headers.values()) {
                if (headerField.getInode().equals(keyField)) {
                    found = true;
                    break;
                }
            }
            if (!found) {
                results.get("errors").add(
                        LanguageUtil.get(user, "Key-field")+": \"" + FieldFactory.getFieldByInode(keyField).getVelocityVarName()
                        + "\" "+LanguageUtil.get(user, "choosen-doesn-t-match-any-of-theh-eaders-found-in-the-file"));
            }
        }

        if (keyFieldsInodes.length == 0)
            results.get("warnings").add(
                    LanguageUtil.get(user, "No-key-fields-were-choosen-it-could-give-to-you-duplicated-content"));

        if(!uniqueFields.isEmpty()){
            for(Field f : uniqueFields){
                results.get("warnings").add(LanguageUtil.get(user, "the-structure-field")+ " " + f.getVelocityVarName() +  " " +LanguageUtil.get(user, "is-unique"));
            }
        }

        //Adding some messages to the results
        if (importableFields == headers.size()) {
            results.get("messages").add(
                    LanguageUtil.get(user,  headers.size() + " "+LanguageUtil.get(user, "headers-match-these-will-be-imported")));
        } else {
            if (headers.size() > 0) {
                results.get("messages").add(headers.size() + " " + LanguageUtil.get(user, "headers-found-on-the-file-matches-all-the-structure-fields"));
            } else {
                results
                .get("messages")
                .add(
                        LanguageUtil.get(user, "No-headers-found-on-the-file-that-match-any-of-the-structure-fields"));
            }
            results
            .get("warnings")
            .add(LanguageUtil.get(user, "Not-all-the-structure-fields-were-matched-against-the-file-headers-Some-content-fields-could-be-left-empty"));
        }
        //Adding the relationship messages
        if(relationships.size() > 0)
        {
            results.get("messages").add(LanguageUtil.get(user,  relationships.size() + " "+LanguageUtil.get(user, "relationship-match-these-will-be-imported")));
        }
    }

	/**
	 * Imports content extracted from a CSV upload file. This method will
	 * receive and handle line by line of the import file.
	 * 
	 * @param line
	 *            - Represents the data line read from the CSV file.
	 * @param contentType
	 *            - The Content Type that the data in this file is associated
	 *            to.
	 * @param preview
	 *            - Set to {@code true} if an analysis and evaluation of the
	 *            imported data will be generated <b>before</b> actually
	 *            importing the data. Otherwise, set to {@code false}.
	 * @param isMultilingual
	 *            - If set to {@code true}, the CSV file will import contents in
	 *            more than one language. Otherwise, set to {@code false}.
	 * @param user
	 *            - The {@link User} performing this action.
	 * @param results
	 *            - The status object that keeps track of potential errors,
	 *            inconsistencies, or warnings.
	 * @param lineNumber
	 *            - The line number in the CSV file for this record.
	 * @param language
	 *            - The language ID used to create the contentlet.
	 * @param headers
	 *            - The {@link Field} objects representing the CSV file headers.
	 * @param keyFields
	 *            - The fields used to associated existing dotCMS contentlets
	 *            with the information in this file. Can be empty.
	 * @param choosenKeyField
	 * @param counters
	 * @param keyContentUpdated
	 * @param contentTypePermissions
	 *            - The list of permissions associated to this Content Type.
	 * @param uniqueFieldBeans
	 *            - Tracking object used to identify repeated values for unique
	 *            fields. If repeated fields are found, the line of the CSV file
	 *            will be ignored.
	 * @param uniqueFields
	 *            - The list of fields that are unique (if any).
	 * @param relationships
	 *            - Content relationships (if any).
	 * @param onlyChild
	 *            - Contains content relationships that are only child
	 *            relationships (header name ends with {@code "-RELCHILD"}).
	 * @param onlyParent
	 *            - Contains content relationships that are only parent
	 *            relationships (header name ends with {@code "-RELPARENT"}).
	 * @param sameKeyBatchInsert
	 *            Indicates if the keys for this row had been use them in this
	 *            batch upload, help us to see if there is a batch content
	 *            upload with multiple records and the same key, mostly used for
	 *            content with multiple languages.
	 * @throws DotRuntimeException
	 *             An error was detected when importing a line from the CSV
	 *             file.
	 */
    private static void importLine ( String[] line, String currentHostId, Structure contentType, boolean preview, boolean isMultilingual, User user, HashMap<String, List<String>> results, int lineNumber, long language,
            HashMap<Integer, Field> headers, HashMap<Integer, Field> keyFields, StringBuffer choosenKeyField, Counters counters,
            HashSet<String> keyContentUpdated, List<Permission> contentTypePermissions, List<UniqueFieldBean> uniqueFieldBeans, List<Field> uniqueFields, HashMap<Integer, Relationship> relationships, HashMap<Integer, Boolean> onlyChild, HashMap<Integer, Boolean> onlyParent,
            boolean sameKeyBatchInsert ) throws DotRuntimeException {

        try {
            //Building a values HashMap based on the headers/columns position
            HashMap<Integer, Object> values = new HashMap<Integer, Object>();
            Set<Category> categories = new HashSet<Category>();
            boolean headersIncludeHostField = false;
            for ( Integer column : headers.keySet() ) {
                Field field = headers.get( column );
                if ( line.length < column ) {
                    throw new DotRuntimeException( "Incomplete line found, the line #" + lineNumber +
                            " doesn't contain all the required columns." );
                }
                String value = line[column];
                Object valueObj = value;
                if (field.getFieldType().equals(Field.FieldType.DATE.toString())) {
                    if (field.getFieldContentlet().startsWith("date")) {
                        if(UtilMethods.isSet(value)) {
                            try { valueObj = parseExcelDate(value) ;} catch (ParseException e) {
                                throw new DotRuntimeException("Line #" + lineNumber + " contains errors, Column: " + field.getVelocityVarName() +
                                        ", value: " + value + ", couldn't be parsed as any of the following supported formats: " +
                                        printSupportedDateFormats());
                            }
                        } else {
                            valueObj = null;
                        }
                    }
                } else if (field.getFieldType().equals(Field.FieldType.DATE_TIME.toString())) {
                    if (field.getFieldContentlet().startsWith("date")) {
                        if(UtilMethods.isSet(value)) {
                            try { valueObj = parseExcelDate(value) ;} catch (ParseException e) {
                                throw new DotRuntimeException("Line #" + lineNumber + " contains errors, Column: " + field.getVelocityVarName() +
                                        ", value: " + value + ", couldn't be parsed as any of the following supported formats: " +
                                        printSupportedDateFormats());
                            }
                        } else {
                            valueObj = null;
                        }
                    }
                } else if (field.getFieldType().equals(Field.FieldType.TIME.toString())) {
                    if (field.getFieldContentlet().startsWith("date")) {
                        if(UtilMethods.isSet(value)) {
                            try { valueObj = parseExcelDate(value) ;} catch (ParseException e) {
                                throw new DotRuntimeException("Line #" + lineNumber + " contains errors, Column: " + field.getVelocityVarName() +
                                        ", value: " + value + ", couldn't be parsed as any of the following supported formats: " +
                                        printSupportedDateFormats());
                            }
                        } else {
                            valueObj = null;
                        }
                    }
                } else if (field.getFieldType().equals(Field.FieldType.CATEGORY.toString()) || field.getFieldType().equals(Field.FieldType.CATEGORIES_TAB.toString())) {
                    valueObj = value;
                    if(UtilMethods.isSet(value)) {
                        String[] categoryKeys = value.split(",");
                        for(String catKey : categoryKeys) {
                            Category cat = catAPI.findByKey(catKey.trim(), user, false);
                            if(cat == null)
                                throw new DotRuntimeException("Line #" + lineNumber + " contains errors, Column: " + field.getVelocityVarName() +
                                        ", value: " + value + ", invalid category key found, line will be ignored.");
                            categories.add(cat);
                        }
                    }
                }
                else if (field.getFieldType().equals(Field.FieldType.CHECKBOX.toString()) ||
                        field.getFieldType().equals(Field.FieldType.SELECT.toString()) ||
                        field.getFieldType().equals(Field.FieldType.MULTI_SELECT.toString()) ||
                        field.getFieldType().equals(Field.FieldType.RADIO.toString())
                        ) {
                    valueObj = value;
                    if(UtilMethods.isSet(value))
                    {
                        String fieldEntriesString = field.getValues()!=null ? field.getValues() : "";
                        String[] fieldEntries = fieldEntriesString.split("\n");
                        boolean found = false;
                        for(String fieldEntry : fieldEntries)
                        {
                            String[] splittedValue = fieldEntry.split("\\|");
                            String entryValue = splittedValue[splittedValue.length - 1].trim();

                            if(entryValue.equals(value) || value.contains(entryValue))
                            {
                                found = true;
                                break;
                            }
                        }
                        if(!found)
                        {
                            throw new DotRuntimeException("Line #" + lineNumber + " contains errors, Column: " + field.getVelocityVarName() +
                                    ", value: " + value + ", invalid value found, line will be ignored.");
                        }
                    }
                    else {
                        valueObj = null;
                    }
                }
                else if (field.getFieldType().equals(Field.FieldType.TEXT.toString())) {
                    if (value.length() > 255) {
                        valueObj = value.substring(0, 255);
                    }
                } else if (field.getFieldType().equals(Field.FieldType.TEXT_AREA.toString()) || field.getFieldType().equals(Field.FieldType.WYSIWYG.toString())) {
                    valueObj = value;
                }
                else if (field.getFieldType().equals(Field.FieldType.HOST_OR_FOLDER.toString())) {
                    Identifier identifier = null;
                    valueObj = null;
                    try{
                        identifier = APILocator.getIdentifierAPI().findFromInode(value);
                    }
                    catch(DotStateException dse){
                        Logger.debug(ImportUtil.class, dse.getMessage());
                    }
                    if(identifier != null && InodeUtils.isSet(identifier.getInode())){
                        valueObj = value;
                        headersIncludeHostField = true;
                    }else if(value.contains("//")){
                        String hostName=null;
                        StringWriter path = null;

                        String[] arr = value.split("/");
                        path = new StringWriter().append("/");
                        for(String y : arr){
                            if(UtilMethods.isSet(y) && hostName == null){
                                hostName = y;
                            }
                            else if(UtilMethods.isSet(y)){
                                path.append(y);
                                path.append("/");
                            }
                        }
                        Host host = APILocator.getHostAPI().findByName(hostName, user, false);
                        if(UtilMethods.isSet(host)){
                            valueObj=host.getIdentifier();
                            Folder f = APILocator.getFolderAPI().findFolderByPath(path.toString(), host, user, false);
                            if(UtilMethods.isSet(f)) {
                                valueObj=f.getInode();
                            }
                            headersIncludeHostField = true;
                        }
                    }
                    else{
                        Host h = APILocator.getHostAPI().findByName(value, user, false);
                        if(UtilMethods.isSet(h)){
                            valueObj=h.getIdentifier();	
                            headersIncludeHostField = true;
                        }
                    }

                    if(valueObj ==null){
                        throw new DotRuntimeException("Line #" + lineNumber + " contains errors, Column: " + field.getVelocityVarName() +
                                ", value: " + value + ", invalid host/folder inode found, line will be ignored.");
                    }
                }else if(field.getFieldType().equals(Field.FieldType.IMAGE.toString()) || field.getFieldType().equals(Field.FieldType.FILE.toString())) {
                    String filePath = value;
                    if(field.getFieldType().equals(Field.FieldType.IMAGE.toString()) && !UtilMethods.isImage(filePath))
                    {
                        //Add Warning the File isn't is an image
                        if(UtilMethods.isSet(filePath)){
                            String localLineMessage = LanguageUtil.get(user, "Line--");
                            String noImageFileMessage = LanguageUtil.get(user, "the-file-is-not-an-image");
                            results.get("warnings").add(localLineMessage + lineNumber + ". " + noImageFileMessage);
                        }
                        valueObj = null;
                    }
                    else
                    {
                        //check if the path is relative to this host or not
                        Host fileHost = hostAPI.find(currentHostId, user, false);
                        if(filePath.indexOf(":") > -1)
                        {
                            String[] fileInfo = filePath.split(":");
                            if(fileInfo.length == 2)
                            {
                                Host fileHostAux = hostAPI.findByName(fileInfo[0], user, false);
                                fileHost = (UtilMethods.isSet(fileHostAux) ? fileHostAux : fileHost);
                                filePath = fileInfo[1];
                            }
                        }

                        Identifier id = APILocator.getIdentifierAPI().find(fileHost, filePath);
                        if(id!=null && InodeUtils.isSet(id.getId()) && id.getAssetType().equals("contentlet")){
                            Contentlet cont = APILocator.getContentletAPI().findContentletByIdentifier(id.getId(), true, APILocator.getLanguageAPI().getDefaultLanguage().getId(), user, false);
                            if(cont!=null && InodeUtils.isSet(cont.getInode())){
                                valueObj = cont.getIdentifier();
                            }else{
                                String localLineMessage = LanguageUtil.get(user, "Line--");
                                String noFileMessage = LanguageUtil.get(user, "The-file-has-not-been-found");
                                results.get("warnings").add(localLineMessage + lineNumber + ". " + noFileMessage + ": " + fileHost.getHostname() + ":" + filePath);
                                valueObj = null;
                            }
                        }
                    }
                }
                else {
                    valueObj = Config.getBooleanProperty("CONTENT_ESCAPE_HTML_TEXT",true) ? UtilMethods.escapeUnicodeCharsForHTML(value) : value;
                }
                values.put(column, valueObj);

                if(field.isUnique()){
                    UniqueFieldBean bean = new UniqueFieldBean();
                    bean.setField(field);
                    bean.setValue(valueObj);
                    bean.setLineNumber(lineNumber);
                    bean.setLanguageId(language);
                    uniqueFieldBeans.add(bean);
                }
            }

            //Find the relationships and their related contents
            HashMap<Relationship,List<Contentlet>> csvRelationshipRecordsParentOnly = new HashMap<Relationship,List<Contentlet>>();
            HashMap<Relationship,List<Contentlet>> csvRelationshipRecordsChildOnly = new HashMap<Relationship,List<Contentlet>>();
            HashMap<Relationship,List<Contentlet>> csvRelationshipRecords = new HashMap<Relationship,List<Contentlet>>();
            for (Integer column : relationships.keySet()) {
                Relationship relationship = relationships.get(column);
                String relatedQuery = line[column];
                List<Contentlet> relatedContentlets = new ArrayList<Contentlet>();
                boolean error = false;
                if(UtilMethods.isSet(relatedQuery))
                {
                    relatedContentlets = conAPI.checkoutWithQuery(relatedQuery, user, false);

                    //validate if the contenlet retrieved are from the correct typ
                    if(FactoryLocator.getRelationshipFactory().isParent(relationship,contentType))
                    {
                        for(Contentlet contentlet : relatedContentlets)
                        {
                            Structure relatedStructure = contentlet.getStructure();
                            if(!(FactoryLocator.getRelationshipFactory().isChild(relationship,relatedStructure)))
                            {
                                error = true;
                                break;
                            }
                        }
                    }
                    if(FactoryLocator.getRelationshipFactory().isChild(relationship,contentType))
                    {
                        for(Contentlet contentlet : relatedContentlets)
                        {
                            Structure relatedStructure = contentlet.getStructure();
                            if(!(FactoryLocator.getRelationshipFactory().isParent(relationship,relatedStructure)))
                            {
                                error = true;
                                break;
                            }
                        }
                    }
                }
                if(!error)
                {
                    //If no error add the relatedContentlets
                    if(onlyChild.get(column)) {
                        csvRelationshipRecordsChildOnly.put(relationship, relatedContentlets);
                    } else if(onlyParent.get(column)) {
                        csvRelationshipRecordsParentOnly.put(relationship, relatedContentlets);
                    } else {
                        csvRelationshipRecords.put(relationship, relatedContentlets);
                    }
                }
                else
                {
                    //else add the error message
                    String localLineMessage = LanguageUtil.get(user, "Line--");
                    String structureDoesNoMatchMessage = LanguageUtil.get(user, "the-structure-does-not-match-the-relationship");
                    results.get("warnings").add(localLineMessage + lineNumber + ". " + structureDoesNoMatchMessage);
                }
            }

            //Searching contentlets to be updated by key fields
            List<Contentlet> contentlets = new ArrayList<Contentlet>();
            String conditionValues = "";

            int identifierFieldIndex = -1;
            try {
                identifierFieldIndex = Integer.parseInt( results.get( "identifiers" ).get( 0 ) );
            } catch ( Exception e ) {
            }

            String identifier = null;
            if ( -1 < identifierFieldIndex ) {
                identifier = line[identifierFieldIndex];
            }

            StringBuffer buffy = new StringBuffer();
            buffy.append( "+structureName:" + contentType.getVelocityVarName() + " +working:true +deleted:false" );

            if ( UtilMethods.isSet( identifier ) ) {
                buffy.append( " +identifier:" + identifier );

                List<ContentletSearch> contentsSearch = conAPI.searchIndex( buffy.toString(), 0, -1, null, user, true );

                if ( (contentsSearch == null) || (contentsSearch.size() == 0) ) {
                    throw new DotRuntimeException( "Line #" + lineNumber + ": Content not found with identifier " + identifier + "\n" );
                } else {
                    Contentlet contentlet;
                    for ( ContentletSearch contentSearch : contentsSearch ) {
                        contentlet = conAPI.find( contentSearch.getInode(), user, true );
                        if ( (contentlet != null) && InodeUtils.isSet( contentlet.getInode() ) ) {
                            contentlets.add( contentlet );
                        } else {
                            throw new DotRuntimeException( "Line #" + lineNumber + ": Content not found with identifier " + identifier + "\n" );
                        }
                    }
                }
            } else if (keyFields.size() > 0) {
                for (Integer column : keyFields.keySet()) {
                    Field field = keyFields.get(column);
                    Object value = values.get(column);
                    String text;
                    if (value instanceof Date || value instanceof Timestamp) {
                        SimpleDateFormat formatter = null;
                        if(field.getFieldType().equals(Field.FieldType.DATE.toString())){
                            text = DATE_FIELD_FORMAT.format((Date)value);
                        }else if(field.getFieldType().equals(Field.FieldType.DATE_TIME.toString())){
                            DateFormat df = new SimpleDateFormat("MM/dd/yyyy");
                            text = df.format((Date)value);
                        }else if(field.getFieldType().equals(Field.FieldType.TIME.toString())) {
                            DateFormat df = new SimpleDateFormat("HHmmss");
                            text =  df.format((Date)value);
                        } else {
                            formatter = new SimpleDateFormat();
                            text = formatter.format(value);
                            Logger.warn(ImportUtil.class,"importLine: field's date format is undetermined.");
                        }
                    } else {
                        text = value.toString();
                    }
                    if(!UtilMethods.isSet(text)){
                        throw new DotRuntimeException("Line #" + lineNumber + " key field " + field.getVelocityVarName() + " is required since it was defined as a key\n");
                    }else{
                        if(field.getFieldType().equals(Field.FieldType.HOST_OR_FOLDER.toString())) {
                            buffy.append(" +(conhost:" + text + " conFolder:" + text+")");
                        } else {
                            buffy.append(" +" + contentType.getVelocityVarName() + "." + field.getVelocityVarName() + ":" + (escapeLuceneSpecialCharacter(text).contains(" ")?"\""+escapeLuceneSpecialCharacter(text)+"\"":escapeLuceneSpecialCharacter(text)));
                        }
                        conditionValues += conditionValues + value + "-";
                    }

                    if(!field.isUnique()){
                        if(UtilMethods.isSet(choosenKeyField.toString())){
                            int count = 1;
                            String[] chosenArr = choosenKeyField.toString().split(",");
                            for(String chosen : chosenArr){
                                if(UtilMethods.isSet(chosen) && !field.getVelocityVarName().equals(chosen.trim())){
                                    count++;
                                }
                            }
                            if(chosenArr.length==count){
                                choosenKeyField.append(", " + field.getVelocityVarName());
                            }
                        }else{
                            choosenKeyField.append(", " + field.getVelocityVarName());
                        }
                    }

                }

                String noLanguageQuery = buffy.toString();
                if ( !isMultilingual && !UtilMethods.isSet( identifier ) ) {
                    buffy.append( " +languageId:" ).append( language );
                }

                List<ContentletSearch> cons = conAPI.searchIndex( buffy.toString(), 0, -1, null, user, true );
                /*
                We need to handle the case when keys are used, we could have a contentlet already saved with the same keys but different language
                so the above query is not going to find it.
                 */
                if ( cons == null || cons.isEmpty() ) {
                    if ( choosenKeyField.length() > 1 ) {
                        cons = conAPI.searchIndex( noLanguageQuery, 0, -1, null, user, true );
                        if (cons != null && !cons.isEmpty()) {
                            isMultilingual = true;
                        }
                    }
                }

                Contentlet con;
                for (ContentletSearch contentletSearch: cons) {
                    con = conAPI.find(contentletSearch.getInode(), user, true);
                    if ((con != null) && InodeUtils.isSet(con.getInode())) {
                        boolean columnExists = false;
                        for (Integer column : keyFields.keySet()) {
                            Field field = keyFields.get(column);
                            Object value = values.get(column);
                            Object conValue = conAPI.getFieldValue(con, field);
                            if(field.getFieldType().equals(Field.FieldType.DATE.toString())
                                    || field.getFieldType().equals(Field.FieldType.DATE_TIME.toString())
                                    || field.getFieldType().equals(Field.FieldType.TIME.toString())){
                                if(field.getFieldType().equals(Field.FieldType.TIME.toString())){
                                    DateFormat df = new SimpleDateFormat("HHmmss");
                                    conValue = df.format((Date)conValue);
                                    value = df.format((Date)value);
                                }else if(field.getFieldType().equals(Field.FieldType.DATE.toString())){
                                    value = DATE_FIELD_FORMAT.format((Date)value);
                                    conValue = DATE_FIELD_FORMAT.format((Date)conValue);
                                }else{
                                    if(conValue instanceof java.sql.Timestamp){
                                        value = new java.sql.Timestamp(((Date)value).getTime());
                                    }else if(conValue instanceof Date){
                                        DateFormat df = new SimpleDateFormat("MM/dd/yyyy");
                                        value = df.format((Date)value);
                                    }
                                }
                                if(conValue.equals(value)){
                                    columnExists = true;
                                }else{
                                    columnExists = false;
                                    break;
                                }
                            }else{
                                if(conValue.toString().equalsIgnoreCase(value.toString())){
                                    columnExists = true;
                                }else{
                                    columnExists = false;
                                    break;
                                }
                            }
                        }
                        if(columnExists) {
                            contentlets.add(con);
                            //Keep a register of all contentlets to be updated
                            results.get("updatedInodes").add(con.getInode());
                        }
                    }
                }

                if ( !preview ) {//Don't do unnecessary calls if it is not required

                    /*
                   We must use an alternative search for cases when we are using the same key for batch uploads,
                   for example if we have multilingual inserts for new records, the search above (searchIndex)
                   can manage multilingual inserts for already stored records but not for the case when the new record and its multilingual records
                   came in the same import file. They are new, we will not find them in the index.
                     */
                    if ( sameKeyBatchInsert && contentlets.isEmpty() ) {

                        //Searching for all the contentlets of this structure
                        List<Contentlet> foundContentlets = conAPI.findByStructure( contentType, user, true, 0, -1 );

                        for ( Contentlet contentlet : foundContentlets ) {

                            boolean match = true;
                            for ( Integer column : keyFields.keySet() ) {

                                //Getting key values
                                Field field = keyFields.get( column );
                                Object value = values.get( column );

                                //Ok, comparing our keys with the contentlets we found trying to see if there is a contentlet to update with the specified keys
                                Object conValue = conAPI.getFieldValue( contentlet, field );
                                if ( !conValue.equals( value ) ) {
                                    match = false;
                                }
                            }

                            //Ok, we found our record
                            if ( match ) {
                                contentlets.add( contentlet );
                                isMultilingual = true;
                                break;
                            }
                        }
                    }
                }
            }

            //Creating/updating content
            boolean isNew = false;
            Long existingMultilingualLanguage = null;//For multilingual batch imports we need the language of an existing contentlet if there is any
            if ( contentlets.size() == 0 ) {
                counters.setNewContentCounter( counters.getNewContentCounter() + 1 );
                isNew = true;
                Contentlet newCont = new Contentlet();
                newCont.setStructureInode( contentType.getInode() );
                newCont.setLanguageId( language );
                contentlets.add( newCont );
            } else {
                if ( isMultilingual || UtilMethods.isSet( identifier ) ) {
                    List<Contentlet> multilingualContentlets = new ArrayList<Contentlet>();

                    for ( Contentlet contentlet : contentlets ) {
                        if ( contentlet.getLanguageId() == language ) {
                            multilingualContentlets.add( contentlet );
                            existingMultilingualLanguage = contentlet.getLanguageId();
                        }
                    }

                    if ( multilingualContentlets.size() == 0 ) {
                        String lastIdentifier = "";
                        isNew = true;
                        for ( Contentlet contentlet : contentlets ) {
                            if ( !contentlet.getIdentifier().equals( lastIdentifier ) ) {
                                counters.setNewContentCounter( counters.getNewContentCounter() + 1 );
                                Contentlet newCont = new Contentlet();
                                newCont.setIdentifier( contentlet.getIdentifier() );
                                newCont.setStructureInode( contentType.getInode() );
                                newCont.setLanguageId( language );
                                multilingualContentlets.add( newCont );

                                existingMultilingualLanguage = contentlet.getLanguageId();
                                lastIdentifier = contentlet.getIdentifier();
                            }
                        }
                    }

                    contentlets = multilingualContentlets;
                }

                if ( !isNew ) {
                    if ( conditionValues.equals( "" ) || !keyContentUpdated.contains( conditionValues ) || isMultilingual ) {
                        counters.setContentToUpdateCounter( counters.getContentToUpdateCounter() + contentlets.size() );
                        if ( preview ) {
                            keyContentUpdated.add( conditionValues );
                        }
                    }
                    if ( contentlets.size() == 1 ) {
                        results.get( "warnings" ).add(
                                LanguageUtil.get( user, "Line--" ) + lineNumber + ". " + LanguageUtil.get( user, "The-key-fields-chosen-match-one-existing-content(s)" ) + " - "
                                        + LanguageUtil.get( user, "more-than-one-match-suggests-key(s)-are-not-properly-unique" ) );
                    } else if ( contentlets.size() > 1 ) {
                        results.get( "warnings" ).add(
                                LanguageUtil.get( user, "Line--" ) + lineNumber + ". " + LanguageUtil.get( user, "The-key-fields-choosen-match-more-than-one-content-in-this-case" ) + ": "
                                        + " " + LanguageUtil.get( user, "matches" ) + ": " + contentlets.size() + " " + LanguageUtil.get( user, "different-content-s-looks-like-the-key-fields-choosen" ) + " " +
                                        LanguageUtil.get( user, "aren-t-a-real-key" ) );
                    }
                }
            }

            for (Contentlet cont : contentlets)
            {
                //Fill the new contentlet with the data
                for (Integer column : headers.keySet()) {
                    Field field = headers.get(column);
                    Object value = values.get(column);

                    if (field.getFieldType().equals(Field.FieldType.HOST_OR_FOLDER.toString())) { // DOTCMS-4484												

                        //Verify if the value belongs to a Host or to a Folder
                        Folder folder = null;
                        Host host = hostAPI.find( value.toString(), user, false );
                        //If a host was not found using the given value (identifier) it must be a folder
                        if ( !UtilMethods.isSet( host ) || !InodeUtils.isSet( host.getInode() ) ) {
                            folder = folderAPI.find( value.toString(), user, false );
                        }

                        if (folder != null && folder.getInode().equalsIgnoreCase(value.toString())) {
                            if (!permissionAPI.doesUserHavePermission(folder,PermissionAPI.PERMISSION_CAN_ADD_CHILDREN,user)) {
                                throw new DotSecurityException( "User has no Add Children Permissions on selected folder" );
                            }
                            cont.setHost(folder.getHostId());
                            cont.setFolder(value.toString());
                        }
                        else if(host != null) {
                            if (!permissionAPI.doesUserHavePermission(host,PermissionAPI.PERMISSION_CAN_ADD_CHILDREN,user)) {
                                throw new DotSecurityException("User has no Add Children Permissions on selected host");
                            }
                            cont.setHost(value.toString());
                            cont.setFolder(FolderAPI.SYSTEM_FOLDER);
                        }
                        continue;
                    }

                    if(UtilMethods.isSet(field.getDefaultValue()) && (!UtilMethods.isSet(String.valueOf(value)) || value==null)){
                        value = field.getDefaultValue();
                    }

                    if(field.getFieldContentlet().startsWith("integer") || field.getFieldContentlet().startsWith("float")){
                        if(!UtilMethods.isSet(String.valueOf(value)) && !field.isRequired()){
                            value = "0";
                        }
                    }
                    try{
                        conAPI.setContentletProperty(cont, field, value);
                    }catch(DotContentletStateException de){
                        if(!field.isRequired() || (value!=null && UtilMethods.isSet(String.valueOf(value)))){
                            throw de;
                        }
                    }
                }

                // Retaining Categories when content updated with partial imports
                if(UtilMethods.isSet(cont.getIdentifier())){

                    List<Field> structureFields = FieldsCache.getFieldsByStructureInode(contentType.getInode());
                    List<Field> categoryFields = new ArrayList<Field>();
                    List<Field> nonHeaderCategoryFields = new ArrayList<Field>();
                    List<Category> nonHeaderParentCats = new ArrayList<Category>();
                    List<Category> categoriesToRetain = new ArrayList<Category>();
                    List<Category> categoriesOnWorkingContent = new ArrayList<Category>();

                    for(Field field : structureFields){
                        if(field.getFieldType().equals(Field.FieldType.CATEGORY.toString()) || field.getFieldType().equals(Field.FieldType.CATEGORIES_TAB.toString()))
                            categoryFields.add(field);
                    }

                    for (Integer column : headers.keySet()) {
                        Field headerField = headers.get(column);
                        Iterator<Field> itr = categoryFields.iterator();
                        while(itr.hasNext()){
                            Field field = itr.next();
                            if(headerField.getInode().equalsIgnoreCase(field.getInode())){
                                itr.remove();
                            }
                        }
                    }

                    nonHeaderCategoryFields.addAll(categoryFields);

                    for(Field field : nonHeaderCategoryFields){
                        nonHeaderParentCats.add(catAPI.find(field.getValues(), user, false));
                    }

                    for(Category cat : nonHeaderParentCats){
                        categoriesToRetain.addAll(catAPI.getAllChildren(cat, user, false));
                    }

                    /*
                     We need to verify that we are not trying to save a contentlet that have as language the default language because that mean that
                     contentlet for that default language couldn't exist, we are just saving it after all....
                     */
                    Long languageId = langAPI.getDefaultLanguage().getId();
                    if ( existingMultilingualLanguage != null ) {
                        languageId = existingMultilingualLanguage;//Using the language another an existing contentlet with the same identifier
                    }

                    Contentlet workingCont;
                    try{
                        workingCont = conAPI.findContentletByIdentifier( cont.getIdentifier(), false, languageId, user, false );
                        categoriesOnWorkingContent = catAPI.getParents( workingCont, user, false );
                    }catch(DotContentletStateException dse){
                        Logger.error(ImportContentletsAction.class,dse.getMessage());
                    }

                    for(Category existingCat : categoriesOnWorkingContent){
                        for(Category retainCat :categoriesToRetain){
                            if(existingCat.compareTo(retainCat) == 0){
                                categories.add(existingCat);
                            }
                        }
                    }
                }

                //Check if line has repeated values for a unique field, if it does then ignore the line
                boolean ignoreLine = false;
                if(!uniqueFieldBeans.isEmpty()){
                    ignoreLine =
                        validateUniqueFields(user, results, lineNumber, language, counters, uniqueFieldBeans,
                            uniqueFields);
                }

                if(!ignoreLine){
                    //Check the new contentlet with the validator
                    try
                    {
                        conAPI.validateContentlet(cont,new ArrayList<Category>(categories));
                    }
                    catch(DotContentletValidationException ex)
                    {
                        StringBuffer sb = new StringBuffer("Line #" + lineNumber + " contains errors\n");
                        HashMap<String,List<Field>> errors = (HashMap<String,List<Field>>) ex.getNotValidFields();
                        Set<String> keys = errors.keySet();
                        for(String key : keys)
                        {
                            sb.append(key + ": ");
                            List<Field> fields = errors.get(key);
                            int count = 0;
                            for(Field field : fields){
                                if(count>0){
                                    sb.append(", ");
                                }
                                sb.append(field.getVelocityVarName());
                                count++;
                            }
                            sb.append("\n");
                        }
                        throw new DotRuntimeException(sb.toString());
                    }

                    //If not preview save the contentlet
                    if (!preview)
                    {
                        cont.setInode(null);
                        cont.setLowIndexPriority(true);
                        //Load the old relationShips and add the new ones
                        ContentletRelationships contentletRelationships = conAPI.getAllRelationships(cont);
                        List<ContentletRelationships.ContentletRelationshipRecords> relationshipRecords = contentletRelationships.getRelationshipsRecords();
                        for(ContentletRelationships.ContentletRelationshipRecords relationshipRecord : relationshipRecords) {
                            List<Contentlet> csvRelatedContentlet = csvRelationshipRecords.get(relationshipRecord.getRelationship());
                            if(UtilMethods.isSet(csvRelatedContentlet)) {
                                relationshipRecord.getRecords().addAll(csvRelatedContentlet);
                            }
                            csvRelatedContentlet = csvRelationshipRecordsChildOnly.get(relationshipRecord.getRelationship());
                            if(UtilMethods.isSet(csvRelatedContentlet) && relationshipRecord.isHasParent()) {
                                relationshipRecord.getRecords().addAll(csvRelatedContentlet);
                            }
                            csvRelatedContentlet = csvRelationshipRecordsParentOnly.get(relationshipRecord.getRelationship());
                            if(UtilMethods.isSet(csvRelatedContentlet) && !relationshipRecord.isHasParent()) {
                                relationshipRecord.getRecords().addAll(csvRelatedContentlet);
                            }
                        }
                        //END Load the old relationShips and add the new ones
                        cont = conAPI.checkin(cont,contentletRelationships, new ArrayList<Category>(categories), contentTypePermissions, user, false);

                        if(Config.getBooleanProperty("PUBLISH_CSV_IMPORTED_CONTENT_AUTOMATICALLY", false)){
                            APILocator.getContentletAPI().publish(cont, user, false);
                        }
                        for (Integer column : headers.keySet()) {
                            Field field = headers.get(column);
                            Object value = values.get(column);
                            if (field.getFieldType().equals(Field.FieldType.TAG.toString()) &&
                                    value instanceof String) {
                                String[] tags = ((String)value).split(",");
                                Host host = null;
                                String hostId = "";
                                if(headersIncludeHostField){
                                    //the CSV File has a Host Or Field Column, with a valid value
                                    try{
                                        host = APILocator.getHostAPI().find(cont.getHost(), user, true);
                                    }catch(Exception e){
                                        Logger.error(ImportUtil.class, "Unable to get host from content: " + e.getMessage());
                                    }
                                    if(UtilMethods.isSet(host)){
                                        if(host.getIdentifier().equals(Host.SYSTEM_HOST))
                                            hostId = Host.SYSTEM_HOST;
                                        else
                                            hostId = host.getIdentifier();
                                    }
                                    else{
                                        hostId = Host.SYSTEM_HOST;
                                    }
                                    for (String tagName : tags) {
                                        try {
                                            if ( tagName != null && !tagName.trim().isEmpty() ) {
                                                APILocator.getTagAPI().addContentletTagInode(tagName.trim(), cont.getInode(), hostId, field.getVelocityVarName());
                                            }
                                        } catch (Exception e) {
                                            Logger.error(ImportUtil.class, "Unable to import tags: " + e.getMessage());
                                        }
                                    }
                                }
                                else {
                                    for (String tagName : tags)
                                        try {
                                            if ( tagName != null && !tagName.trim().isEmpty() ) {
                                                APILocator.getTagAPI().addContentletTagInode( tagName.trim(), cont.getInode(), Host.SYSTEM_HOST, field.getVelocityVarName() );
                                            }
                                        } catch (Exception e) {
                                            Logger.error(ImportUtil.class, "Unable to import tags: " + e.getMessage());
                                        }
                                }
                            }
                        }
                        results.get("lastInode").clear();
                        List<String> l = results.get("lastInode");
                        l.add(cont.getInode());
                        results.put("lastInode", l);
                    }

                    if (isNew){
                        counters.setContentCreated(counters.getContentCreated() + 1);
                    }else{
                        if (conditionValues.equals("") || !keyContentUpdated.contains(conditionValues)) {
                            counters.setContentUpdated(counters.getContentUpdated() + 1);
                            counters.setContentUpdatedDuplicated(counters.getContentUpdatedDuplicated() + 1);
                            keyContentUpdated.add(conditionValues);
                        }else{
                            counters.setContentUpdatedDuplicated(counters.getContentUpdatedDuplicated() + 1);
                        }

                    }
                }
            }
        } catch (Exception e) {
            Logger.error(ImportUtil.class,e.getMessage(),e);
            throw new DotRuntimeException(e.getMessage());
        }
    }

    /**
     *
     * @param user
     * @param results
     * @param lineNumber
     * @param language
     * @param counters
     * @param uniqueFieldBeans
     * @param uniqueFields
     * @return
     * @throws LanguageException
     */
    private static boolean validateUniqueFields(User user, HashMap<String, List<String>> results, int lineNumber,
                                                long language, Counters counters,
                                                List<UniqueFieldBean> uniqueFieldBeans,
                                                List<Field> uniqueFields) throws LanguageException {
        boolean ignoreLine = false;
        for (Field f : uniqueFields) {
            Object value = null;
            int count = 0;
            for (UniqueFieldBean bean : uniqueFieldBeans) {
                if (bean.getField().equals(f) && language == bean.getLanguageId()) {
                    if (count > 0 && value != null && value.equals(bean.getValue()) && lineNumber == bean
                        .getLineNumber()) {
                        counters.setNewContentCounter(counters.getNewContentCounter() - 1);
                        ignoreLine = true;
                        results.get("warnings").add(
                            LanguageUtil.get(user, "Line--") + " " + lineNumber + " " + LanguageUtil
                                .get(user, "contains-duplicate-values-for-structure-unique-field") + " " + f
                                .getVelocityVarName() + " " + LanguageUtil.get(user, "and-will-be-ignored"));
                    }
                    value = bean.getValue();
                    count++;

                }
            }
        }
        return ignoreLine;
    }

    /**
     * 
     * @return
     */
    private static String printSupportedDateFormats () {
        StringBuffer ret = new StringBuffer("[ ");
        for (String pattern : IMP_DATE_FORMATS) {
            ret.append(pattern + ", ");
        }
        ret.append(" ] ");
        return ret.toString();
    }

	/**
	 * Escape lucene reserved characters
	 * 
	 * @param text
	 * @return String
	 */
    private static String escapeLuceneSpecialCharacter(String text){
        text = text.replaceAll("\\[","\\\\[").replaceAll("\\]","\\\\]");
        text = text.replaceAll("\\{","\\\\{").replaceAll("\\}","\\\\}");
        text = text.replaceAll("\\+","\\\\+").replaceAll(":","\\\\:");
        text = text.replaceAll("\\*","\\\\*").replaceAll("\\?","\\\\?");
        text = text.replaceAll("\\(","\\\\(").replaceAll("\\)","\\\\)");
        text = text.replaceAll("&&","\\\\&&").replaceAll("\\|\\|","\\\\||");
        text = text.replaceAll("!","\\\\!").replaceAll("\\^","\\\\^");
        text = text.replaceAll("-","\\\\-").replaceAll("~","\\\\~");
        text = text.replaceAll("\"","\\\"");

        return text;
    }

    /**
     * 
     * @author root
     * @version 1.x
     * @since Mar 22, 2012
     *
     */
    public static class Counters {

        public int newContentCounter = 0;
        public int contentToUpdateCounter = 0;
        public int contentCreated = 0;
        public int contentUpdated = 0;
        public int contentUpdatedDuplicated = 0;

        private Collection<Map<String, String>> keys = new ArrayList<Map<String, String>>();

        /**
         * @return the newContentCounter
         */
        public int getNewContentCounter () {
            return newContentCounter;
        }

        /**
         * @param newContentCounter the newContentCounter to set
         */
        public void setNewContentCounter ( int newContentCounter ) {
            this.newContentCounter = newContentCounter;
        }

        /**
         * @return the contentToUpdateCounter
         */
        public int getContentToUpdateCounter () {
            return contentToUpdateCounter;
        }

        /**
         * @param contentToUpdateCounter the contentToUpdateCounter to set
         */
        public void setContentToUpdateCounter(int contentToUpdateCounter) {
            this.contentToUpdateCounter = contentToUpdateCounter;
        }
        /**
         * @return the contentCreated
         */
        public int getContentCreated() {
            return contentCreated;
        }
        /**
         * @param contentCreated the contentCreated to set
         */
        public void setContentCreated(int contentCreated) {
            this.contentCreated = contentCreated;
        }
        /**
         * @return the contentUpdated
         */
        public int getContentUpdated() {
            return contentUpdated;
        }
        /**
         * @param contentUpdated the contentUpdated to set
         */
        public void setContentUpdated(int contentUpdated) {
            this.contentUpdated = contentUpdated;
        }
        /**
         * @return the contentUpdatedDuplicated
         */
        public int getContentUpdatedDuplicated() {
            return contentUpdatedDuplicated;
        }
        /**
         * @param contentUpdatedDuplicated the contentUpdatedDuplicated to set
         */
        public void setContentUpdatedDuplicated(int contentUpdatedDuplicated) {
            this.contentUpdatedDuplicated = contentUpdatedDuplicated;
        }

        /**
         * Stores unique keys per line, useful to know if we have batch uploads with the same keys, mostly use it for batch content uploads with multiple languages
         *
         * @param key
         * @param value
         */
        public void addKey ( String key, String value ) {

            if ( !matchKey( key, value ) ) {

                Map<String, String> keyMap = new HashMap<String, String>();
                keyMap.put( key, value );

                keys.add( keyMap );
            }
        }

        /**
         * Verifies if a key with a given value was already used
         *
         * @param key
         * @param value
         * @return
         */
        public boolean matchKey ( String key, String value ) {

            for ( Map<String, String> keyMap : keys ) {

                String match = keyMap.get( key );
                if ( match != null && match.equals( value ) ) {
                    return true;
                }
            }

            return false;
        }

        public int uniqueKeysCount () {
            return keys.size();
        }

    }

    /**
     * 
     * @param field
     * @return
     */
    public static boolean isImportableField ( Field field ) {
        return !(
                field.getFieldType().equals( Field.FieldType.BUTTON.toString() ) ||
                field.getFieldType().equals( Field.FieldType.LINE_DIVIDER.toString() ) ||
                field.getFieldType().equals( Field.FieldType.TAB_DIVIDER.toString() ) ||
                field.getFieldType().equals( Field.FieldType.BINARY.toString() ) ||
                field.getFieldType().equals( Field.FieldType.PERMISSIONS_TAB.toString() ));
    }

    /**
     * 
     * @param date
     * @return
     * @throws ParseException
     */
    private static Date parseExcelDate ( String date ) throws ParseException {
        return DateUtil.convertDate( date, IMP_DATE_FORMATS );
    }

    /**
     * 
     * @author root
     * @version 1.x
     * @since Mar 22, 2012
     *
     */
    private static class UniqueFieldBean {

        private Field field;

        private Object value;

        private Integer lineNumber;

        private long languageId;

        /**
         * 
         * @return
         */
        public Field getField() {
            return field;
        }

        /**
         * 
         * @param field
         */
        public void setField(Field field) {
            this.field = field;
        }

        /**
         * 
         * @return
         */
        public Object getValue() {
            return value;
        }

        /**
         * 
         * @param value
         */
        public void setValue(Object value) {
            this.value = value;
        }

        /**
         * 
         * @return
         */
        public Integer getLineNumber() {
            return lineNumber;
        }

        /**
         * 
         * @param lineNumber
         */
        public void setLineNumber(Integer lineNumber) {
            this.lineNumber = lineNumber;
        }

        public long getLanguageId() {
            return languageId;
        }

        public void setLanguageId(long languageId) {
            this.languageId = languageId;
        }

    }

}
