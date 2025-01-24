package com.dotmarketing.util;

import com.dotcms.content.elasticsearch.util.ESUtils;
import com.dotcms.contenttype.model.field.BinaryField;
import com.dotcms.contenttype.model.field.HostFolderField;
import com.dotcms.contenttype.model.type.BaseContentType;
import com.dotcms.contenttype.model.type.ContentType;
import com.dotcms.contenttype.transform.contenttype.StructureTransformer;
import com.dotcms.contenttype.transform.field.LegacyFieldTransformer;
import com.dotcms.repackage.com.csvreader.CsvReader;
import com.dotcms.rest.api.v1.temp.DotTempFile;
import com.dotcms.util.LowerKeyMap;
import com.dotcms.util.RelationshipUtil;
import com.dotmarketing.beans.Host;
import com.dotmarketing.beans.Identifier;
import com.dotmarketing.beans.Permission;
import com.dotmarketing.business.APILocator;
import com.dotmarketing.business.CacheLocator;
import com.dotmarketing.business.DotValidationException;
import com.dotmarketing.business.PermissionAPI;
import com.dotmarketing.cache.FieldsCache;
import com.dotmarketing.common.model.ContentletSearch;
import com.dotmarketing.db.HibernateUtil;
import com.dotmarketing.exception.DotDataException;
import com.dotmarketing.exception.DotDataValidationException;
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
import com.dotmarketing.portlets.contentlet.model.ContentletDependencies;
import com.dotmarketing.portlets.contentlet.model.IndexPolicy;
import com.dotmarketing.portlets.folders.business.FolderAPI;
import com.dotmarketing.portlets.folders.model.Folder;
import com.dotmarketing.portlets.htmlpageasset.business.HTMLPageAssetAPI;
import com.dotmarketing.portlets.languagesmanager.business.LanguageAPI;
import com.dotmarketing.portlets.languagesmanager.model.Language;
import com.dotmarketing.portlets.structure.factories.FieldFactory;
import com.dotmarketing.portlets.structure.model.ContentletRelationships;
import com.dotmarketing.portlets.structure.model.ContentletRelationships.ContentletRelationshipRecords;
import com.dotmarketing.portlets.structure.model.Field;
import com.dotmarketing.portlets.structure.model.Field.FieldType;
import com.dotmarketing.portlets.structure.model.Relationship;
import com.dotmarketing.portlets.structure.model.Structure;
import com.dotmarketing.portlets.workflows.business.WorkflowAPI;
import com.dotmarketing.portlets.workflows.model.WorkflowAction;
import com.dotmarketing.util.importer.AbstractImportValidationMessage.ValidationMessageType;
import com.dotmarketing.util.importer.AbstractSpecialHeaderInfo.SpecialHeaderType;
import com.dotmarketing.util.importer.ContentSummary;
import com.dotmarketing.util.importer.HeaderValidationCodes;
import com.dotmarketing.util.importer.ImportFileInfo;
import com.dotmarketing.util.importer.ImportHeaderInfo;
import com.dotmarketing.util.importer.ImportHeaderValidationResult;
import com.dotmarketing.util.importer.ImportResult;
import com.dotmarketing.util.importer.ImportResultConverter;
import com.dotmarketing.util.importer.ImportResultData;
import com.dotmarketing.util.importer.ImportValidationMessage;
import com.dotmarketing.util.importer.ProcessedData;
import com.dotmarketing.util.importer.SpecialHeaderInfo;
import com.liferay.portal.language.LanguageException;
import com.liferay.portal.language.LanguageUtil;
import com.liferay.portal.model.User;
import com.liferay.util.StringPool;
import java.io.File;
import java.io.Reader;
import java.net.URL;
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
import java.util.Objects;
import java.util.Optional;
import java.util.Set;
import java.util.function.Function;
import java.util.function.LongConsumer;
import java.util.stream.Collectors;
import javax.servlet.http.HttpServletRequest;
import org.apache.commons.lang3.BooleanUtils;
import org.apache.commons.lang3.StringUtils;
import org.apache.commons.lang3.tuple.ImmutablePair;
import org.apache.commons.lang3.tuple.Pair;
import org.jetbrains.annotations.NotNull;

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

    public static final String LINE_NO = "Line #";
    private static PermissionAPI permissionAPI = APILocator.getPermissionAPI();
    private final static ContentletAPI conAPI = APILocator.getContentletAPI();
    private final static CategoryAPI catAPI = APILocator.getCategoryAPI();
    private final static LanguageAPI langAPI = APILocator.getLanguageAPI();
    private final static HostAPI hostAPI = APILocator.getHostAPI();
    private final static FolderAPI folderAPI = APILocator.getFolderAPI();
    private final static WorkflowAPI workflowAPI = APILocator.getWorkflowAPI();

    private final static String languageCodeHeader = "languageCode";
    private final static String countryCodeHeader = "countryCode";

    private final static int commitGranularity = 100;
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
     * @param wfActionId
     *            - The workflow Action Id to execute on the import
     * @param request
     *            - The request object.
     * @return The resulting analysis performed on the CSV file. This provides
     *         information regarding inconsistencies, errors, warnings and/or
     *         precautions to the user.
     * @throws DotRuntimeException
     *             An error occurred when analyzing the CSV file.
     * @throws DotDataException
     *             An error occurred when analyzing the CSV file.
     */
    public static HashMap<String, List<String>> importFile(
            Long importId, String currentSiteId, String contentTypeInode, String[] keyfields,
            boolean preview, boolean isMultilingual, User user, long language,
            String[] csvHeaders, CsvReader csvreader, int languageCodeHeaderColumn,
            int countryCodeHeaderColumn, Reader reader, String wfActionId,
            final HttpServletRequest request) throws DotRuntimeException, DotDataException {

        return importFile(importId, currentSiteId, contentTypeInode, keyfields, preview,
                isMultilingual, user, language, csvHeaders, csvreader, languageCodeHeaderColumn,
                countryCodeHeaderColumn, wfActionId, request, null);
    }

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
     * @param wfActionId
     *            - The workflow Action Id to execute on the import
     * @param request
     *            - The request object.
     * @param progressCallback
     *           - A callback function to report progress.
	 * @return The resulting analysis performed on the CSV file. This provides
	 *         information regarding inconsistencies, errors, warnings and/or
	 *         precautions to the user.
	 * @throws DotRuntimeException
	 *             An error occurred when analyzing the CSV file.
	 * @throws DotDataException
	 *             An error occurred when analyzing the CSV file.
	 */
    public static HashMap<String, List<String>> importFile(Long importId, String currentSiteId,
            String contentTypeInode, String[] keyfields, boolean preview, boolean isMultilingual,
            User user, long language, String[] csvHeaders, CsvReader csvreader,
            int languageCodeHeaderColumn, int countryCodeHeaderColumn, String wfActionId,
            final HttpServletRequest request, final LongConsumer progressCallback)
            throws DotRuntimeException, DotDataException {

        HashMap<String, List<String>> results = new HashMap<>();
        results.put("warnings", new ArrayList<>());
        results.put("errors", new ArrayList<>());
        results.put("messages", new ArrayList<>());
        results.put("results", new ArrayList<>());
        results.put("counters", new ArrayList<>());
        results.put("identifiers", new ArrayList<>());
        results.put("updatedInodes", new ArrayList<>());
        results.put("lastInode", new ArrayList<>());
        results.put(Contentlet.WORKFLOW_ACTION_KEY, new ArrayList<>());

        Structure contentType = CacheLocator.getContentTypeCache().getStructureByInode (contentTypeInode);
        List<Permission> contentTypePermissions = permissionAPI.getPermissions(contentType);
        List<UniqueFieldBean> uniqueFieldBeans = new ArrayList<>();
        List<Field> uniqueFields = new ArrayList<>();

        //Initializing variables
        int lines = 1;
        int errors = 0;
        int lineNumber = 0;

        Counters counters = new Counters();
        HashSet<String> keyContentUpdated = new HashSet<>();
        StringBuffer choosenKeyField = new StringBuffer();

        // Data structures to be populated by header validation
        HashMap<Integer, Field> headers = new HashMap<>();
        HashMap<Integer, Field> keyFields = new HashMap<>();
        HashMap<Integer, Relationship> relationships = new HashMap<>();
        HashMap<Integer, Boolean> onlyParent = new HashMap<>();
        HashMap<Integer, Boolean> onlyChild = new HashMap<>();

        //Get unique fields for structure
        for(Field field : FieldsCache.getFieldsByStructureInode(contentType.getInode())){
            if(field.isUnique()){
                uniqueFields.add(field);
            }
        }

        //Parsing the file line per line
        try {
            if ((csvHeaders != null) || (csvreader.readHeaders())) {

                // Process headers and get validation result
                ImportHeaderValidationResult headerValidation;
                if (csvHeaders != null) {
                    headerValidation = importHeaders(csvHeaders, contentType, keyfields,
                            isMultilingual, user, headers, keyFields, uniqueFields, relationships,
                            onlyChild, onlyParent);
                } else {
                    headerValidation = importHeaders(csvreader.getHeaders(), contentType, keyfields,
                            isMultilingual, user, headers, keyFields, uniqueFields, relationships,
                            onlyChild, onlyParent);
                }

                // ---
                // Convert validation result to legacy format
                ImportResultConverter.addValidationResultsToLegacyMap(headerValidation, results);
                errors += results.get("errors").size();
                // ---

                lineNumber++;

                // Log preview/import status every 100 processed records
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
                                        counters, keyContentUpdated, contentTypePermissions, uniqueFieldBeans, uniqueFields, relationships, onlyChild, onlyParent, sameKeyBatchInsert, wfActionId, request );

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

                            if (lineNumber % commitGranularity == 0) {
                                final String action = preview ? "previewed." : "imported.";
                                Logger.info(ImportUtil.class, String.format("-> %d entries have been %s", lineNumber, action));
                                if(!preview) {
                                    HibernateUtil.closeAndCommitTransaction();
                                    HibernateUtil.startTransaction();
                                }
                            }
                        } catch (final DotRuntimeException ex) {
                            String errorMessage = getErrorMsgFromException(user, ex);
                            if(errorMessage.indexOf(LINE_NO) == -1){
                                errorMessage = LINE_NO + lineNumber + ": " + errorMessage;
                            }
                            results.get("errors").add(errorMessage);
                            errors++;
                            Logger.warn(ImportUtil.class, "Error line: " + lines + " (" + csvreader.getRawRecord()
                                    + "). Line Ignored.");
                        } finally {
                            // Progress callback
                            if (progressCallback != null) {
                                progressCallback.accept(lines);
                            }
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
        } catch (final Exception e) {
            Logger.error(ImportContentletsAction.class, String.format("An error occurred when parsing CSV file in " +
                    "line #%s: %s", lineNumber, e.getMessage()), e);
        }
        final String action = preview ? "Content preview" : "Content import";
        String statusMsg = String.format("%s has finished, %d lines were read correctly.", action, lines);
        statusMsg = errors > 0 ? statusMsg + String.format(" However, %d errors were found.", errors) : StringPool.BLANK;
        Logger.info(ImportUtil.class, statusMsg);
        return results;
    }

    /**
     * Validates and processes the headers from a CSV import file. This method performs
     * comprehensive validation of the header line including:
     * <ul>
     *   <li>Basic format validation</li>
     *   <li>Content type field matching</li>
     *   <li>Relationship field processing</li>
     *   <li>Multilingual requirements</li>
     *   <li>Key fields validation</li>
     *   <li>Unique fields processing</li>
     * </ul>
     *
     * @param headerLine      CSV file header line to validate
     * @param contentType     Content Type structure to validate against
     * @param keyFieldsInodes Array of field inodes used as keys for content matching
     * @param isMultilingual  Whether the import supports multiple languages
     * @param user            User performing the import
     * @param headers         Map to store validated header-to-field mappings
     * @param keyFields       Map to store validated key field mappings
     * @param uniqueFields    List of fields marked as unique in the content type
     * @param relationships   Map to store relationship field mappings
     * @param onlyChild       Map tracking child-only relationships
     * @param onlyParent      Map tracking parent-only relationships
     * @return Validation result containing header information and validation messages
     * @throws Exception if validation fails or processing encounters errors
     */
    private static ImportHeaderValidationResult importHeaders(final String[] headerLine,
            final Structure contentType, final String[] keyFieldsInodes,
            final boolean isMultilingual, final User user, final HashMap<Integer, Field> headers,
            final HashMap<Integer, Field> keyFields, final List<Field> uniqueFields,
            final HashMap<Integer, Relationship> relationships,
            final HashMap<Integer, Boolean> onlyChild, final HashMap<Integer, Boolean> onlyParent)
            throws Exception {

        // Create structured results for validation tracking
        final var validationBuilder = ImportHeaderValidationResult.builder();
        final List<String> headerFields = new ArrayList<>();

        // Validate basic header format
        validateHeaderLineFormat(headerLine, user, validationBuilder);

        // Get content type info and create relationship map
        final ContentTypeInfo typeInfo = getContentTypeInfo(contentType);
        final Map<String, Relationship> relationshipsMap = createRelationshipMap(
                typeInfo.relationships
        );

        // Process and validate headers
        processHeaders(
                headerLine, contentType, keyFieldsInodes, isMultilingual,
                relationshipsMap, headerFields, headers, keyFields,
                relationships, onlyChild, onlyParent, user, validationBuilder);

        // Validate multilingual requirements if needed
        validateMultilingualHeaders(isMultilingual, headerFields, validationBuilder);

        // Validate key fields and unique fields
        validateKeyFields(keyFieldsInodes, headers, user, validationBuilder);
        processUniqueFields(uniqueFields, user, validationBuilder);

        // Generate summary messages
        addSummaryMessages(headers.size(), typeInfo.importableFields,
                relationships.size(), user, validationBuilder);

        // Add context information
        Map<String, Object> context = new HashMap<>();
        context.put("headers", headers);
        context.put("keyFields", keyFields);
        context.put("relationships", relationships);
        context.put("onlyChild", onlyChild);
        context.put("onlyParent", onlyParent);
        validationBuilder.context(context);

        return validationBuilder.build();
    }

    /**
     * Processes and validates header entries from the CSV file. This method handles the detailed
     * validation of each header column and populates various data structures with the results.
     *
     * @param headerLine        Array of header strings to process
     * @param contentType       Content Type structure to validate against
     * @param keyFieldsInodes   Array of field inodes used as keys
     * @param isMultilingual    Whether import is multilingual
     * @param relationshipsMap  Map of available relationships
     * @param headerFields      List to store processed header names
     * @param headers           Map to store header-to-field mappings
     * @param keyFields         Map to store key field mappings
     * @param relationships     Map to store relationship mappings
     * @param onlyChild         Map for child-only relationships
     * @param onlyParent        Map for parent-only relationships
     * @param user              User performing the import
     * @param validationBuilder Builder for validation result
     * @throws Exception if processing encounters errors
     */
    private static void processHeaders(final String[] headerLine,
            final Structure contentType,
            final String[] keyFieldsInodes, boolean isMultilingual,
            final Map<String, Relationship> relationshipsMap, final List<String> headerFields,
            final HashMap<Integer, Field> headers, final HashMap<Integer, Field> keyFields,
            final HashMap<Integer, Relationship> relationships,
            final HashMap<Integer, Boolean> onlyChild, final HashMap<Integer, Boolean> onlyParent,
            final User user, final ImportHeaderValidationResult.Builder validationBuilder)
            throws Exception {

        List<String> validHeaders = new ArrayList<>();
        List<String> invalidHeaders = new ArrayList<>();
        List<SpecialHeaderInfo> specialHeaders = new ArrayList<>();

        // Process each header
        for (int i = 0; i < headerLine.length; i++) {
            String header = headerLine[i].replaceAll("'", "");
            headerFields.add(header);

            // Handle special headers first
            final var specialHeaderInfo = isSpecialHeader(header, i, user, validationBuilder);
            if (specialHeaderInfo.type() != SpecialHeaderType.NONE) {
                validHeaders.add(header);
                specialHeaders.add(specialHeaderInfo);
                continue;
            }

            // Process and validate header
            processAndValidateHeader(
                    header, i, contentType, headers, keyFieldsInodes, keyFields,
                    onlyChild, onlyParent, relationshipsMap, relationships,
                    validHeaders, invalidHeaders, isMultilingual, user, validationBuilder);
        }

        // Validate required fields
        List<String> missingHeaders = validateRequiredFields(headerFields, user, contentType,
                validationBuilder);

        // Create headerInfo
        final var headerInfo = ImportHeaderInfo.builder()
                .totalHeaders(headerLine.length)
                .validHeaders(validHeaders.toArray(new String[0]))
                .invalidHeaders(invalidHeaders.toArray(new String[0]))
                .missingHeaders(missingHeaders.toArray(new String[0]))
                .validationDetails(new HashMap<>())  // Add validation details if needed
                .specialHeaders(specialHeaders)
                .build();
        validationBuilder.headerInfo(headerInfo);
    }

    /**
     * Validates the basic format of header lines from the CSV file. Checks for:
     * <ul>
     *   <li>Non-empty header line</li>
     *   <li>No duplicate header names</li>
     * </ul>
     *
     * @param headerLine        Array of header strings to validate
     * @param user              User performing the import
     * @param validationBuilder Builder to accumulate validation messages
     * @throws LanguageException      If language key lookup fails
     * @throws DotValidationException If headers are invalid or empty
     */
    private static void validateHeaderLineFormat(final String[] headerLine, final User user,
            final ImportHeaderValidationResult.Builder validationBuilder) throws LanguageException {

        if (headerLine == null || headerLine.length == 0) {
            validationBuilder.addMessages(ImportValidationMessage.builder()
                    .type(ValidationMessageType.ERROR)
                    .code(HeaderValidationCodes.INVALID_HEADER_FORMAT.name())
                    .message(LanguageUtil.get(user,
                            "No-headers-found-on-the-file-nothing-will-be-imported"))
                    .build());
            throw new DotValidationException("Invalid header format");
        }

        // Validate no duplicate headers
        Set<String> uniqueHeaders = new HashSet<>();
        for (int i = 0; i < headerLine.length; i++) {
            String header = headerLine[i].replaceAll("'", "").toLowerCase();
            if (!uniqueHeaders.add(header)) {
                validationBuilder.addMessages(ImportValidationMessage.builder()
                        .type(ValidationMessageType.ERROR)
                        .code(HeaderValidationCodes.DUPLICATE_HEADER.name())
                        .field(headerLine[i])
                        .lineNumber(1)
                        .message(LanguageUtil.get(user, "Duplicate-header-found") + ": "
                                + headerLine[i])
                        .build());
            }
        }
    }

    /**
     * Processes and validates an individual header entry. This method attempts to match the header
     * with content type fields or relationships and records the validation result.
     *
     * @param header            Header string to process
     * @param columnIndex       Index of the header in the CSV file
     * @param contentType       Content Type structure to validate against
     * @param headers           Map to populate with validated header-to-field mappings
     * @param keyFieldsInodes   Array of field inodes used as keys
     * @param keyFields         Map to populate with key field mappings
     * @param relationships     Map to populate with relationship mappings
     * @param onlyChild         Map tracking child-only relationships
     * @param onlyParent        Map tracking parent-only relationships
     * @param relationshipsMap  Map of available relationships
     * @param validHeaders      List to store valid header names
     * @param invalidHeaders    List to store invalid header names
     * @param isMultilingual    Whether import supports multiple languages
     * @param user              User performing the import
     * @param validationBuilder Builder to accumulate validation messages
     * @throws LanguageException If language key lookup fails
     */
    private static void processAndValidateHeader(
            final String header, final int columnIndex, final Structure contentType,
            final HashMap<Integer, Field> headers, final String[] keyFieldsInodes,
            final HashMap<Integer, Field> keyFields,
            final HashMap<Integer, Boolean> onlyChild,
            final HashMap<Integer, Boolean> onlyParent,
            final Map<String, Relationship> relationshipsMap,
            final HashMap<Integer, Relationship> relationships,
            final List<String> validHeaders, final List<String> invalidHeaders,
            final boolean isMultilingual, final User user,
            final ImportHeaderValidationResult.Builder validationBuilder)
            throws LanguageException, DotDataException, DotSecurityException {

        // First try content type fields
        boolean found = processContentTypeField(header, columnIndex, contentType, headers,
                keyFieldsInodes, keyFields, onlyChild, onlyParent, relationshipsMap, relationships,
                user, validationBuilder);
        if (found) {
            validHeaders.add(header);
        }

        // Validate if the header is a relationship header
        boolean foundAsRelationship = processRelationshipHeader(header, columnIndex,
                relationshipsMap, relationships,
                onlyChild, onlyParent);
        if (foundAsRelationship) {
            validHeaders.add(header);
        }

        // If not found and not a language header, mark as invalid
        if (!(found || foundAsRelationship) && !(isMultilingual && isLanguageHeader(header))) {
            invalidHeaders.add(header);
            validationBuilder.addMessages(ImportValidationMessage.builder()
                    .type(ValidationMessageType.WARNING)
                    .code(HeaderValidationCodes.INVALID_HEADER.name())
                    .field(header)
                    .lineNumber(1)
                    .context(Map.of("columnIndex", columnIndex))
                    .message(LanguageUtil.get(user, "Header") + " \"" + header + "\" " +
                            LanguageUtil.get(user,
                                    "doesn-t-match-any-structure-field-this-column-of-data-will-be-ignored"))
                    .build());
        }
    }

    /**
     * Validates headers required for multilingual imports. Checks for presence of language code and
     * country code headers when multilingual import is enabled.
     *
     * @param isMultilingual    Whether multilingual import is enabled
     * @param headerFields      List of processed header fields
     * @param validationBuilder Builder to accumulate validation messages
     */
    private static void validateMultilingualHeaders(final boolean isMultilingual,
            final List<String> headerFields,
            final ImportHeaderValidationResult.Builder validationBuilder) {

        if (!isMultilingual) {
            return;
        }

        boolean hasLanguageCode = headerFields.contains(languageCodeHeader);
        boolean hasCountryCode = headerFields.contains(countryCodeHeader);

        if (!hasLanguageCode || !hasCountryCode) {
            validationBuilder.addMessages(ImportValidationMessage.builder()
                    .type(ValidationMessageType.ERROR)
                    .code(HeaderValidationCodes.INVALID_LANGUAGE.name())
                    .message("languageCode and countryCode fields are mandatory in the CSV file" +
                            " when importing multilingual content")
                    .lineNumber(1)
                    .build());
        }
    }

    /**
     * Retrieves and processes Content Type information including fields and relationships.
     *
     * @param contentType Content Type structure to process
     * @return ContentTypeInfo containing fields, relationships and count of importable fields
     */
    private static ContentTypeInfo getContentTypeInfo(final Structure contentType) {
        final List<Field> fields = FieldsCache.getFieldsByStructureInode(contentType.getInode());
        final List<Relationship> relationships = APILocator.getRelationshipAPI()
                .byContentType(contentType);

        int importableFields = 0;
        for (Field field : fields) {
            if (isImportableField(field)) {
                importableFields++;
            }
        }

        return new ContentTypeInfo(fields, relationships, importableFields);
    }

    /**
     * Creates a map of relationships for efficient lookup during header validation. Uses the
     * relationship type value as the key for quick access.
     *
     * @param relationships List of relationships to map
     * @return Map of relationships keyed by their type value
     */
    private static Map<String, Relationship> createRelationshipMap(
            final List<Relationship> relationships) {
        final Map<String, Relationship> relationshipsMap = new LowerKeyMap<>();
        relationshipsMap.putAll(relationships.stream()
                .collect(
                        Collectors.toMap(
                                Relationship::getRelationTypeValue,
                                Function.identity()
                        )));
        return relationshipsMap;
    }

    /**
     * Collects all required fields from a list of content type fields.
     *
     * @param fields List of fields to process
     * @return List of velocity variable names for required fields
     */
    private static List<String> collectRequiredFields(final List<Field> fields) {
        return fields.stream()
                .filter(Field::isRequired)
                .map(Field::getVelocityVarName)
                .collect(Collectors.toList());
    }

    /**
     * Checks if a header represents a special system field like Identifier or Workflow Action. Adds
     * appropriate validation messages for recognized system headers.
     *
     * @param header            Header to check
     * @param columnIndex       Index of the header in the CSV file
     * @param user              User performing the import
     * @param validationBuilder Builder to accumulate validation messages
     * @return SpecialHeaderInfo containing type and column index if header is special, NONE otherwise
     * @throws LanguageException If language key lookup fails
     */
    private static SpecialHeaderInfo isSpecialHeader(final String header, final int columnIndex,
            final User user,
            final ImportHeaderValidationResult.Builder validationBuilder) throws LanguageException {

        if (header.equalsIgnoreCase("Identifier")) {
            validationBuilder.addMessages(
                    ImportValidationMessage.builder()
                            .type(ValidationMessageType.INFO)
                            .code(HeaderValidationCodes.SYSTEM_HEADER.name())
                            .field("Identifier")
                            .lineNumber(1)
                            .message(LanguageUtil.get(user,
                                    "identifier-field-found-in-import-contentlet-csv-file"))
                            .context(Map.of("columnIndex", columnIndex))
                            .build()
            );

            return SpecialHeaderInfo.builder()
                    .type(SpecialHeaderType.IDENTIFIER)
                    .columnIndex(columnIndex)
                    .build();
        }

        if (header.equalsIgnoreCase(Contentlet.WORKFLOW_ACTION_KEY)) {
            validationBuilder.addMessages(
                    ImportValidationMessage.builder()
                            .type(ValidationMessageType.INFO)
                            .code(HeaderValidationCodes.SYSTEM_HEADER.name())
                            .field(Contentlet.WORKFLOW_ACTION_KEY)
                            .lineNumber(1)
                            .message(LanguageUtil.get(user,
                                    "workflow-action-id-field-found-in-import-contentlet-csv-file"))
                            .context(Map.of("columnIndex", columnIndex))
                            .build()
            );

            return SpecialHeaderInfo.builder()
                    .type(SpecialHeaderType.WORKFLOW_ACTION)
                    .columnIndex(columnIndex)
                    .build();
        }

        return SpecialHeaderInfo.builder()
                .type(SpecialHeaderType.NONE)
                .columnIndex(-1)
                .build();
    }

    /**
     * Processes and validates a header against content type fields. Handles field mapping, key
     * field identification, and relationship setup.
     *
     * @param header            Header to process
     * @param columnIndex       Index of the header in the CSV file
     * @param contentType       Content Type structure to validate against
     * @param headers           Map to populate with header-to-field mappings
     * @param keyFieldsInodes   Array of field inodes used as keys
     * @param keyFields         Map to populate with key field mappings
     * @param relationships     Map to populate with relationship mappings
     * @param onlyChild         Map tracking child-only relationships
     * @param onlyParent        Map tracking parent-only relationships
     * @param relationshipsMap  Map of available relationships
     * @param user              User performing the import
     * @param validationBuilder Builder to accumulate validation messages
     * @return true if header was successfully processed as a content type field
     * @throws LanguageException If language key lookup fails
     */
    private static boolean processContentTypeField(final String header, final int columnIndex,
            final Structure contentType, final HashMap<Integer, Field> headers,
            final String[] keyFieldsInodes, final HashMap<Integer, Field> keyFields,
            final HashMap<Integer, Boolean> onlyChild, final HashMap<Integer, Boolean> onlyParent,
            final Map<String, Relationship> relationshipsMap,
            final HashMap<Integer, Relationship> relationships, final User user,
            final ImportHeaderValidationResult.Builder validationBuilder)
            throws LanguageException, DotDataException, DotSecurityException {

        for (Field field : FieldsCache.getFieldsByStructureInode(contentType.getInode())) {
            if (!field.getVelocityVarName().equalsIgnoreCase(header)) {
                continue;
            }

            if (isNonImportableField(field)) {
                validationBuilder.addMessages(ImportValidationMessage.builder()
                        .type(ValidationMessageType.WARNING)
                        .code(HeaderValidationCodes.INVALID_HEADER.name())
                        .field(header)
                        .lineNumber(1)
                        .message(formatNonImportableFieldMessage(field, header, user))
                        .context(Map.of("columnIndex", columnIndex))
                        .build());
                return true;
            }

            // Add field to headers
            headers.put(columnIndex, field);

            // Check if field matches any key field inodes and add to keyFields if it does
            for (String fieldInode : keyFieldsInodes) {
                if (fieldInode.equals(field.getInode())) {
                    keyFields.put(columnIndex, field);
                }
            }

            // Add relationships if field is relationship type
            if (field.getFieldType().equals(FieldType.RELATIONSHIP.toString())) {
                processRelationshipField(field, columnIndex, user, relationshipsMap, relationships,
                        onlyParent, onlyChild);
            }

            return true;
        }

        return false;
    }

    /**
     * Checks if a field is non-importable based on its type. Non-importable fields include buttons,
     * line dividers, and tab dividers.
     *
     * @param field Field to check
     * @return true if field is non-importable, false otherwise
     */
    private static boolean isNonImportableField(final Field field) {
        return field.getFieldType().equals(Field.FieldType.BUTTON.toString()) ||
                field.getFieldType().equals(Field.FieldType.LINE_DIVIDER.toString()) ||
                field.getFieldType().equals(Field.FieldType.TAB_DIVIDER.toString());
    }

    /**
     * Formats an error message for non-importable field types.
     *
     * @param field  Field that is non-importable
     * @param header Header name from CSV
     * @param user   User performing the import
     * @return Formatted error message
     * @throws LanguageException If language key lookup fails
     */
    private static String formatNonImportableFieldMessage(final Field field, final String header,
            final User user) throws LanguageException {
        return LanguageUtil.get(user, "Header") + " \"" + header + "\" " +
                LanguageUtil.get(user, "matches-a-field-of-type-" +
                        field.getFieldType().toLowerCase() +
                        "-this-column-of-data-will-be-ignored");
    }

    /**
     * Processes relationship headers that explicitly define relationships through suffixes
     * like -RELPARENT or -RELCHILD.
     *
     * @param header Header string to process
     * @param columnIndex Index of the header in CSV
     * @param relationshipsMap Map containing available relationships keyed by type value
     * @param relationships Stores relationships found in headers keyed by column index
     * @param onlyChild Tracks which columns contain child-only relationships
     * @param onlyParent Tracks which columns contain parent-only relationships
     * @return true if header was processed as a relationship, false otherwise
     */
    private static boolean processRelationshipHeader(final String header, final int columnIndex,
            final Map<String, Relationship> relationshipsMap,
            final HashMap<Integer, Relationship> relationships,
            final HashMap<Integer, Boolean> onlyChild, final HashMap<Integer, Boolean> onlyParent) {

        String relationshipHeader = header;
        boolean onlyP = false;
        boolean onlyCh = false;

        if (header.endsWith("-RELPARENT")) {
            relationshipHeader = header.substring(0, header.lastIndexOf("-RELPARENT"));
            onlyP = true;
        } else if (header.endsWith("-RELCHILD")) {
            relationshipHeader = header.substring(0, header.lastIndexOf("-RELCHILD"));
            onlyCh = true;
        }

        //Check if the header is a relationship
        final Relationship relationship = relationshipsMap.get(relationshipHeader.toLowerCase());
        if (relationship == null) {
            return false;
        }

        relationships.put(columnIndex, relationship);

        if (!onlyParent.containsKey(columnIndex)) {
            onlyParent.put(columnIndex, onlyP);
        }

        if (!onlyChild.containsKey(columnIndex)) {
            // special case when the relationship has the same structure for parent and child, set only as child
            if (relationship.getChildStructureInode().equals(relationship.getParentStructureInode())
                    && !onlyCh && !onlyP) {
                onlyChild.put(columnIndex, true);
            } else {
                onlyChild.put(columnIndex, onlyCh);
            }
        }

        return true;
    }

    /**
     * Processes fields that are relationship type fields and sets up their relationship mappings.
     * Unlike relationship headers, these are fields that are defined as relationships in the
     * content type.
     *
     * @param field The field to process
     * @param columnIndex Index of the field in CSV
     * @param user Current user
     * @param relationshipsMap Map storing relationships by type value
     * @param relationships Stores relationships found in fields by column index
     * @param onlyParent Tracks which columns contain parent-only relationships
     * @param onlyChild Tracks which columns contain child-only relationships
     * @throws DotDataException If error accessing relationships
     * @throws DotSecurityException If user lacks permissions
     */
    private static void processRelationshipField(
            final Field field, final int columnIndex, final User user,
            final Map<String, Relationship> relationshipsMap,
            final HashMap<Integer, Relationship> relationships,
            final HashMap<Integer, Boolean> onlyParent,
            final HashMap<Integer, Boolean> onlyChild)
            throws DotDataException, DotSecurityException {

        final Relationship fieldRelationship = APILocator.getRelationshipAPI()
                .getRelationshipFromField(field, user);
        relationships.put(columnIndex, fieldRelationship);
        relationshipsMap.put(field.getVelocityVarName().toLowerCase(), fieldRelationship);
        relationshipsMap.remove(fieldRelationship.getRelationTypeValue().toLowerCase());

        // Considering case when importing self-related content
        if (fieldRelationship.getChildStructureInode()
                .equals(fieldRelationship.getParentStructureInode())) {
            if (fieldRelationship.getParentRelationName() != null &&
                    fieldRelationship.getParentRelationName().equals(field.getVelocityVarName())) {
                onlyParent.put(columnIndex, true);
                onlyChild.put(columnIndex, false);
            } else if (fieldRelationship.getChildRelationName() != null &&
                    fieldRelationship.getChildRelationName().equals(field.getVelocityVarName())) {
                onlyParent.put(columnIndex, false);
                onlyChild.put(columnIndex, true);
            }
        }
    }

    /**
     * Checks if a header is one of the special language-related fields.
     *
     * @param header Header to check
     * @return true if header is a language or country code field
     */
    private static boolean isLanguageHeader(final String header) {
        return header.equals(languageCodeHeader) || header.equals(countryCodeHeader);
    }

    /**
     * Validates required fields against the headers found in the CSV. Identifies missing required
     * fields and adds appropriate error messages.
     *
     * @param headerFields      List of headers found in CSV
     * @param user              User performing the import
     * @param contentType       Content Type structure to validate against
     * @param validationBuilder Builder to accumulate validation messages
     * @return List of required fields that were not found in headers
     * @throws LanguageException If language key lookup fails
     */
    private static List<String> validateRequiredFields(final List<String> headerFields,
            final User user, final Structure contentType,
            final ImportHeaderValidationResult.Builder validationBuilder) throws LanguageException {

        // Get required fields
        List<String> requiredFields = collectRequiredFields(
                FieldsCache.getFieldsByStructureInode(contentType.getInode()));

        // Find missing required fields
        List<String> missingRequired = new ArrayList<>(requiredFields);
        missingRequired.removeAll(headerFields);

        // Add errors for missing required fields
        for (String requiredField : missingRequired) {
            validationBuilder.addMessages(
                    ImportValidationMessage.builder()
                            .type(ValidationMessageType.ERROR)
                            .code(HeaderValidationCodes.REQUIRED_FIELD_MISSING.name())
                            .field(requiredField)
                            .lineNumber(1)
                            .message(
                                    LanguageUtil.get(user, "Field") +
                                            ": \"" + requiredField + "\" " +
                                            LanguageUtil.get(user,
                                                    "required-field-not-found-in-header"))
                            .build()
            );
        }

        return missingRequired;
    }

    /**
     * Validates key fields specified for the import. Ensures all specified key fields are present
     * in the headers.
     *
     * @param keyFieldsInodes   Array of field inodes used as keys
     * @param headers           Map of validated header-to-field mappings
     * @param user              User performing the import
     * @param validationBuilder Builder to accumulate validation messages
     * @throws LanguageException If language key lookup fails
     */
    private static void validateKeyFields(final String[] keyFieldsInodes,
            final HashMap<Integer, Field> headers, final User user,
            final ImportHeaderValidationResult.Builder validationBuilder) throws LanguageException {

        if (keyFieldsInodes.length == 0) {

            validationBuilder.addMessages(
                    ImportValidationMessage.builder()
                            .type(ValidationMessageType.WARNING)
                            .code(HeaderValidationCodes.NO_KEY_FIELDS.name())
                            .lineNumber(1)
                            .message(LanguageUtil.get(user,
                                    "No-key-fields-were-choosen-it-could-give-to-you-duplicated-content"))
                            .build()
            );
            return;
        }

        for (String keyFieldInode : keyFieldsInodes) {
            boolean found = false;
            for (Field headerField : headers.values()) {
                if (headerField.getInode().equals(keyFieldInode)) {
                    found = true;
                    break;
                }
            }

            if (!found) {
                Field keyField = FieldFactory.getFieldByInode(keyFieldInode);
                validationBuilder.addMessages(
                        ImportValidationMessage.builder()
                                .type(ValidationMessageType.ERROR)
                                .code(HeaderValidationCodes.INVALID_KEY_FIELD.name())
                                .field(keyField.getVelocityVarName())
                                .lineNumber(1)
                                .message(LanguageUtil.get(user, "Key-field") + ": \"" +
                                        keyField.getVelocityVarName() + "\" " +
                                        LanguageUtil.get(user,
                                                "choosen-doesn-t-match-any-of-theh-eaders-found-in-the-file"))
                                .build()
                );
            }
        }
    }

    /**
     * Processes unique fields and adds appropriate warning messages. Alerts users about fields that
     * require unique values.
     *
     * @param uniqueFields      List of fields marked as unique
     * @param user              User performing the import
     * @param validationBuilder Builder to accumulate validation messages
     * @throws LanguageException If language key lookup fails
     */
    private static void processUniqueFields(final List<Field> uniqueFields, final User user,
            final ImportHeaderValidationResult.Builder validationBuilder) throws LanguageException {
        if (uniqueFields.isEmpty()) {
            return;
        }

        for (Field uniqueField : uniqueFields) {
            validationBuilder.addMessages(
                    ImportValidationMessage.builder()
                            .type(ValidationMessageType.WARNING)
                            .code(HeaderValidationCodes.UNIQUE_FIELD.name())
                            .field(uniqueField.getVelocityVarName())
                            .message(LanguageUtil.get(user, "the-structure-field") + " " +
                                    uniqueField.getVelocityVarName() + " " +
                                    LanguageUtil.get(user, "is-unique"))
                            .build()
            );
        }
    }

    /**
     * Adds summary messages about the header validation process. Provides information about:
     * <ul>
     *   <li>Number of matched headers</li>
     *   <li>Completeness of field coverage</li>
     *   <li>Relationship matches</li>
     * </ul>
     *
     * @param headerCount          Number of valid headers found
     * @param importableFieldCount Number of fields that can be imported
     * @param relationshipCount    Number of relationships found
     * @param user                 User performing the import
     * @param validationBuilder    Builder to accumulate validation messages
     * @throws LanguageException If language key lookup fails
     */
    private static void addSummaryMessages(final int headerCount, final int importableFieldCount,
            final int relationshipCount, final User user,
            final ImportHeaderValidationResult.Builder validationBuilder) throws LanguageException {

        if (headerCount == importableFieldCount) {

            validationBuilder.addMessages(
                    ImportValidationMessage.builder()
                            .type(ValidationMessageType.INFO)
                            .message(LanguageUtil.get(user, headerCount + " " +
                                    LanguageUtil.get(user, "headers-match-these-will-be-imported")))
                            .build()
            );
        } else {
            if (headerCount > 0) {
                validationBuilder.addMessages(
                        ImportValidationMessage.builder()
                                .type(ValidationMessageType.INFO)
                                .message(headerCount + " " +
                                        LanguageUtil.get(user,
                                                "headers-found-on-the-file-matches-all-the-structure-fields"))
                                .build()
                );
            } else {
                validationBuilder.addMessages(
                        ImportValidationMessage.builder()
                                .type(ValidationMessageType.INFO)
                                .message(LanguageUtil.get(user,
                                        "No-headers-found-on-the-file-that-match-any-of-the-structure-fields"))
                                .build()
                );
            }

            validationBuilder.addMessages(
                    ImportValidationMessage.builder()
                            .type(ValidationMessageType.WARNING)
                            .code(HeaderValidationCodes.INCOMPLETE_HEADERS.name())
                            .message(LanguageUtil.get(user,
                                    "Not-all-the-structure-fields-were-matched-against-the-file-headers"
                                            +
                                            "-Some-content-fields-could-be-left-empty"))
                            .lineNumber(1)
                            .build()
            );
        }

        if (relationshipCount > 0) {
            validationBuilder.addMessages(
                    ImportValidationMessage.builder()
                            .type(ValidationMessageType.INFO)
                            .message(LanguageUtil.get(user, relationshipCount + " " +
                                    LanguageUtil.get(user,
                                            "relationship-match-these-will-be-imported")))
                            .build()
            );
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
     * @param wfActionId
     *            - represent the Workflow Action Id to execute
	 * @throws DotRuntimeException
	 *             An error was detected when importing a line from the CSV
	 *             file.
	 */
    private static void importLine ( String[] line, String currentHostId, Structure contentType, boolean preview, boolean isMultilingual, User user, HashMap<String, List<String>> results, int lineNumber, long language,
            HashMap<Integer, Field> headers, HashMap<Integer, Field> keyFields, StringBuffer choosenKeyField, Counters counters,
            HashSet<String> keyContentUpdated, List<Permission> contentTypePermissions, List<UniqueFieldBean> uniqueFieldBeans, List<Field> uniqueFields, HashMap<Integer, Relationship> relationships, HashMap<Integer, Boolean> onlyChild, HashMap<Integer, Boolean> onlyParent,
            boolean sameKeyBatchInsert, String wfActionId, final HttpServletRequest request ) throws DotRuntimeException {

        try {
            //Building a values HashMap based on the headers/columns position
            HashMap<Integer, Object> values = new HashMap<>();
            Set<Category> categories = new HashSet<>();
            Pair<Host, Folder> siteAndFolder = null;
            Pair<Integer, String> urlValue = null;
            for ( Integer column : headers.keySet() ) {
                Field field = headers.get( column );
                if ( line.length < column ) {
                    throw new DotRuntimeException(LINE_NO + lineNumber + "doesn't contain all the required columns.");
                }
                String value = line[column];
                Object valueObj = value;
                if (field.getFieldType().equals(Field.FieldType.DATE.toString())) {
                    valueObj = validateDateTypes(lineNumber, field, value, valueObj);
                } else if (field.getFieldType().equals(Field.FieldType.DATE_TIME.toString())) {
                    valueObj = validateDateTypes(lineNumber, field, value, valueObj);
                } else if (field.getFieldType().equals(Field.FieldType.TIME.toString())) {
                    valueObj = validateDateTypes(lineNumber, field, value, valueObj);
                } else if (field.getFieldType().equals(Field.FieldType.CATEGORY.toString()) || field.getFieldType().equals(Field.FieldType.CATEGORIES_TAB.toString())) {
                    valueObj = value;
                    if(UtilMethods.isSet(value)) {
                        String[] categoryKeys = value.split(",");
                        for(String catKey : categoryKeys) {
                            Category cat = catAPI.findByKey(catKey.trim(), user, false);
                            if(cat == null)
                                throw new DotRuntimeException(LINE_NO + lineNumber + " contains errors. Column: '" + field.getVelocityVarName() +
                                        "', value: '" + value + "', invalid category key found. Line will be ignored.");
                            categories.add(cat);
                        }
                    }
                } else if (field.getFieldType().equals(Field.FieldType.CHECKBOX.toString()) ||
                        field.getFieldType().equals(Field.FieldType.SELECT.toString()) ||
                        field.getFieldType().equals(Field.FieldType.MULTI_SELECT.toString()) ||
                        field.getFieldType().equals(Field.FieldType.RADIO.toString())
                        ) {
                    valueObj = value;
                    if (UtilMethods.isSet(value)) {
                        String fieldEntriesString = field.getValues()!=null ? field.getValues() : "";
                        String[] fieldEntries = fieldEntriesString.split("\n");
                        boolean found = false;
                        for (String fieldEntry : fieldEntries) {
                            String[] splittedValue = fieldEntry.split("\\|");
                            String entryValue = splittedValue[splittedValue.length - 1].trim();

                            if (entryValue.equals(value) || value.contains(entryValue)) {
                                found = true;
                                break;
                            }
                        }
                        if (!found) {
                            valueObj = BooleanUtils.toBoolean(value);
                        }
                    } else {
                        valueObj = null;
                    }
                } else if (field.getFieldType().equals(Field.FieldType.TEXT.toString())) {
                    if (value.length() > 255) {
                        valueObj = value.substring(0, 255);
                    }
                } else if (field.getFieldType().equals(Field.FieldType.TEXT_AREA.toString()) || field.getFieldType().equals(Field.FieldType.WYSIWYG.toString())) {
                    valueObj = value;
                } else if (field.getFieldType().equals(Field.FieldType.HOST_OR_FOLDER.toString())) {
                    siteAndFolder = getSiteAndFolderFromIdOrName(value, user);
                    if (siteAndFolder == null) {
                        throw new DotRuntimeException(LINE_NO + lineNumber + " contains errors. Column: '" + field.getVelocityVarName() +
                                "', value: '" + value + "', invalid site/folder inode found. Line will be ignored.");
                    } else {
                        valueObj = value;
                    }
                } else if (new LegacyFieldTransformer(field).from().typeName().equals(BinaryField.class.getName())){
                    if(UtilMethods.isSet(value) && !APILocator.getTempFileAPI().validUrl(value)){
                        throw new DotRuntimeException(LINE_NO + lineNumber + " contains errors. URL is malformed or Response is not 200");
                    }
                }else if(field.getFieldType().equals(Field.FieldType.IMAGE.toString()) || field.getFieldType().equals(Field.FieldType.FILE.toString())) {
                    String filePath = value;
                    if (Field.FieldType.IMAGE.toString().equals(field.getFieldType())
                            && !UtilMethods.isImage(filePath)) {
                        //Add Warning the File isn't is an image
                        if(UtilMethods.isSet(filePath)){
                            String localLineMessage = LanguageUtil.get(user, "Line--");
                            String noImageFileMessage = LanguageUtil.get(user, "the-file-is-not-an-image", field.getVelocityVarName());
                            results.get("warnings").add(localLineMessage + lineNumber + ": " + noImageFileMessage);
                        }
                        valueObj = null;
                    } else {
                        //check if the path is relative to this host or not
                        Host fileHost = hostAPI.find(currentHostId, user, false);
                        if (filePath.contains(":")) {
                            String[] fileInfo = filePath.split(":");
                            if (fileInfo.length == 2) {
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
                                results.get("warnings").add(localLineMessage + lineNumber + ": " + noFileMessage + " in " + fileHost.getHostname() + ":" + filePath);
                                valueObj = null;
                            }
                        }
                    }
                } else {
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

                if (valueObj != null
                        && field.getVelocityVarName().equals(HTMLPageAssetAPI.URL_FIELD)) {
                    final String uri = StringUtils.stripEnd(
                            StringUtils.strip(valueObj.toString()), StringPool.FORWARD_SLASH);
                    final StringBuilder uriBuilder = new StringBuilder();
                    if (!uri.startsWith(StringPool.FORWARD_SLASH)) {
                        uriBuilder.append(StringPool.FORWARD_SLASH);
                    }
                    uriBuilder.append(uri);
                    urlValue = Pair.of(column, uriBuilder.toString());
                }
            }

            // Check if the content type is HTMLPage and the URL field is set
            final Pair<Pair<Host, Folder>, String> pathAndAssetNameForURL =
                    urlValue != null ?
                        checkURLFieldForHTMLPage(contentType,
                            urlValue.getRight(), siteAndFolder, user) :
                    null;
            if (pathAndAssetNameForURL != null) {
                final String assetNameForURL = pathAndAssetNameForURL.getRight();
                if (UtilMethods.isSet(assetNameForURL)) {
                    values.put(urlValue.getLeft(), assetNameForURL);
                    final Pair<Host, Folder> parentPathForURL = pathAndAssetNameForURL.getLeft();
                    if (parentPathForURL == null) {
                        throw new DotRuntimeException(LINE_NO + lineNumber + " contains errors. Column: '"
                                + HTMLPageAssetAPI.URL_FIELD + "', value: '" + urlValue.getRight()
                                + "', invalid parent folder for URL. Line will be ignored.");
                    } else {
                        siteAndFolder = parentPathForURL;
                    }
                }
            }

            //Find the relationships and their related contents
            final HashMap<Relationship, List<Contentlet>> csvRelationshipRecordsParentOnly = new HashMap<>();
            final HashMap<Relationship, List<Contentlet>> csvRelationshipRecordsChildOnly = new HashMap<>();
            final HashMap<Relationship, List<Contentlet>> csvRelationshipRecords = new HashMap<>();
            for (Integer column : relationships.keySet()) {
                final Relationship relationship = relationships.get(column);
                final String relatedQuery = line[column];
                List<Contentlet> relatedContentlets = null;
                try{
                    if(relatedQuery !=null && !relatedQuery.trim().isEmpty()) {
                        relatedContentlets = RelationshipUtil
                                .getRelatedContentFromQuery(relationship,
                                        new StructureTransformer(contentType).from(), language,
                                        relatedQuery, user);

                        //If no error add the relatedContentlets
                        if (onlyChild.get(column)) {
                            csvRelationshipRecordsChildOnly.put(relationship, relatedContentlets);
                        } else if (onlyParent.get(column)) {
                            csvRelationshipRecordsParentOnly.put(relationship, relatedContentlets);
                        } else {
                            csvRelationshipRecords.put(relationship, relatedContentlets);
                        }
                    } else{
                        csvRelationshipRecords.put(relationship, relatedContentlets);
                    }
                } catch (final DotDataValidationException e) {
                    //add the error message
                    if (null != relationship) {
                        Logger.warn(ImportUtil.class, String.format("A validation error occurred with Relationship " +
                                "'%s'[%s]: e.getMessage()", relationship.getRelationTypeValue(), relationship.getInode(), e
                                .getMessage()), e);
                    } else {
                        Logger.warn(ImportUtil.class, String.format("A null relationship in column '%s' was found",
                                column));
                    }
                    String localLineMessage = LanguageUtil.get(user, "Line--");
                    String structureDoesNoMatchMessage = LanguageUtil.get(user, "the-structure-does-not-match-the-relationship");
                    results.get("warnings").add(localLineMessage + lineNumber + ": " + structureDoesNoMatchMessage);
                }
            }

            //Searching contentlets to be updated by key fields
            List<Contentlet> contentlets = new ArrayList<>();
            String conditionValues = "";

            int identifierFieldIndex = -1;
            try {
                identifierFieldIndex = Integer.parseInt( results.get( "identifiers" ).get( 0 ) );
            } catch ( Exception e ) {
                Logger.debug(ImportUtil.class, e.getMessage(), e);
            }

            Logger.debug(ImportUtil.class, "identifierFieldIndex: " + identifierFieldIndex);

            String identifier = null;
            if ( -1 < identifierFieldIndex ) {
                identifier = line[identifierFieldIndex];
            }

            StringBuffer buffy = new StringBuffer();
            buffy.append("+structureName:").append(contentType.getVelocityVarName())
                    .append(" +working:true +deleted:false");

            Logger.debug(ImportUtil.class,"Identifier is set: " + UtilMethods.isSet( identifier ));
            Logger.debug(ImportUtil.class,"Keyfields size: " + keyFields.size());
            if ( UtilMethods.isSet( identifier ) ) {
                buffy.append(" +identifier:").append(identifier);

                List<ContentletSearch> contentsSearch = conAPI.searchIndex(buffy.toString(), 0, -1, null, user, true);

                if ((contentsSearch == null) || (contentsSearch.size() == 0)) {

                    Logger.warn(ImportUtil.class, LINE_NO + lineNumber + ": Content not found with identifier " + identifier + "\n");
                    throw new DotRuntimeException(LINE_NO + lineNumber + ": Content not found with identifier " + identifier + "\n");
                } else {

                    Contentlet contentlet;
                    for (ContentletSearch contentSearch : contentsSearch) {
                        contentlet = conAPI.find(contentSearch.getInode(), user, true);
                        if ((contentlet != null) && InodeUtils.isSet(contentlet.getInode())) {
                            contentlets.add(contentlet);
                        } else {
                            Logger.warn(ImportUtil.class, LINE_NO + lineNumber + ": Content not found with identifier " + identifier + "\n");
                            throw new DotRuntimeException(LINE_NO + lineNumber + ": Content not found with identifier " + identifier + "\n");
                        }
                    }
                }
            } else if (pathAndAssetNameForURL != null && keyFields.isEmpty()) {
                // For HTMLPageAsset, we need to search by URL to math existing pages
                buffy.append( " +languageId:" ).append( language );
                buffy.append(addSiteAndFolderToESQuery(siteAndFolder, null));
                buffy.append(" +").append(contentType.getVelocityVarName()).append(StringPool.PERIOD)
                        .append(HTMLPageAssetAPI.URL_FIELD).append(StringPool.COLON)
                        .append(pathAndAssetNameForURL.getRight());
                List<ContentletSearch> contentsSearch = conAPI.searchIndex(
                        buffy.toString(), 0, -1, null, user, true);
                if (contentsSearch != null && !contentsSearch.isEmpty()) {
                    Contentlet contentlet;
                    for (ContentletSearch contentSearch : contentsSearch) {
                        contentlet = conAPI.find(contentSearch.getInode(), user, true);
                        if ((contentlet != null) && InodeUtils.isSet(contentlet.getInode())) {
                            contentlets.add(contentlet);
                        } else {
                            Logger.warn(ImportUtil.class, LINE_NO + lineNumber + ": Content not found with URL " + urlValue.getRight() + "\n");
                            throw new DotRuntimeException(LINE_NO + lineNumber + ": Content not found with URL " + urlValue.getRight() + "\n");
                        }
                    }
                }
            } else if (keyFields.size() > 0) {
                boolean appendSiteToQuery = false;
                String siteFieldValue = null;
                for (Integer column : keyFields.keySet()) {
                    Field field = keyFields.get(column);
                    Object value = values.get(column);
                    String text;
                    if (value instanceof Date || value instanceof Timestamp) {
                        SimpleDateFormat formatter;
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
                        throw new DotRuntimeException(LINE_NO + lineNumber + " key field " + field.getVelocityVarName() + " is required since it was defined as a key\n");
                    }else{
                        if (field.getVelocityVarName().equals(HTMLPageAssetAPI.URL_FIELD)
                                && pathAndAssetNameForURL != null) {
                            appendSiteToQuery = true;
                            buffy.append(" +").append(contentType.getVelocityVarName()).append(StringPool.PERIOD)
                                    .append(HTMLPageAssetAPI.URL_FIELD).append(StringPool.COLON)
                                    .append(pathAndAssetNameForURL.getRight());
                            value = getURLFromFolderAndAssetName(pathAndAssetNameForURL);
                        } else if(new LegacyFieldTransformer(field).from() instanceof HostFolderField) {
                            appendSiteToQuery = true;
                            siteFieldValue = text;
                        } else {
                            buffy.append(" +").append(contentType.getVelocityVarName()).append(StringPool.PERIOD)
                                    .append(field.getVelocityVarName()).append(field.isUnique()? ESUtils.SHA_256: "_dotraw")
                                    .append(StringPool.COLON)
                                    .append(field.isUnique()? ESUtils.sha256(contentType.getVelocityVarName()
                                                    + StringPool.PERIOD + field.getVelocityVarName(), text,
                                            language): escapeLuceneSpecialCharacter(text).contains(" ") ? "\""
                                            + escapeLuceneSpecialCharacter(text) + "\""
                                            : escapeLuceneSpecialCharacter(text));

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
                                choosenKeyField.append(", ").append(field.getVelocityVarName());
                            }
                        }else{
                            choosenKeyField.append(", ").append(field.getVelocityVarName());
                        }
                    }

                }

                if (appendSiteToQuery) {
                    buffy.append(addSiteAndFolderToESQuery(siteAndFolder, siteFieldValue));
                }

                String noLanguageQuery = buffy.toString();
                if ( !isMultilingual && !UtilMethods.isSet( identifier ) ) {
                    buffy.append( " +languageId:" ).append( language );
                }

                Logger.debug(ImportUtil.class, "buffy: " + buffy.toString());
                List<ContentletSearch> cons = conAPI.searchIndex( buffy.toString(), 0, -1, null, user, true );
                Logger.debug(ImportUtil.class,"Cons: " + cons.size());
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
                Logger.debug(ImportUtil.class,"Cons: " + cons.size());
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
                                if (field.getVelocityVarName().equals(HTMLPageAssetAPI.URL_FIELD)
                                        && pathAndAssetNameForURL != null) {
                                    value = getURLFromFolderAndAssetName(pathAndAssetNameForURL);
                                    conValue = getURLFromContentId(con.getIdentifier());
                                }
                                Logger.debug(ImportUtil.class,"conValue: " + conValue.toString());
                                Logger.debug(ImportUtil.class,"Value: " + value.toString());
                                if(conValue.toString().equalsIgnoreCase(value.toString())){
                                    columnExists = true;
                                }else{
                                    columnExists = false;
                                    break;
                                }
                            }
                        }
                        Logger.debug(ImportUtil.class, "column exists: " + columnExists);
                        if(columnExists) {//aca entra
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
                                if (field.getVelocityVarName().equals(HTMLPageAssetAPI.URL_FIELD)
                                        && pathAndAssetNameForURL != null) {
                                    value = getURLFromFolderAndAssetName(pathAndAssetNameForURL);
                                    conValue = getURLFromContentId(contentlet.getIdentifier());
                                }
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
            Logger.debug(ImportUtil.class,"Contentlets Size: " + contentlets.size());
            if ( contentlets.size() == 0 ) {
                counters.setNewContentCounter( counters.getNewContentCounter() + 1 );
                isNew = true;
                Contentlet newCont = new Contentlet();
                newCont.setStructureInode( contentType.getInode() );
                newCont.setLanguageId( language );
                contentlets.add( newCont );
            } else {
                if ( isMultilingual || UtilMethods.isSet( identifier ) ) {
                    List<Contentlet> multilingualContentlets = new ArrayList<>();

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
                Logger.debug(ImportUtil.class,"isNew: " + isNew);

                if ( !isNew ) {
                    if ( conditionValues.equals( "" ) || !keyContentUpdated.contains( conditionValues ) || isMultilingual ) {
                        counters.setContentToUpdateCounter( counters.getContentToUpdateCounter() + contentlets.size() );
                        if ( preview ) {
                            keyContentUpdated.add( conditionValues );
                        }
                    }
                    Logger.debug(ImportUtil.class,"Contentlets size: " + contentlets.size());
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

            for (Contentlet cont : contentlets) {

                //Clean up any existing workflow action
                cont.resetActionId();

                int wfActionIdIndex = -1;
                try {
                    List<String> workflowActionKey = results.get(Contentlet.WORKFLOW_ACTION_KEY);
                    if (UtilMethods.isSet(workflowActionKey)) {
                        wfActionIdIndex = Integer
                                .parseInt(results.get(Contentlet.WORKFLOW_ACTION_KEY).get(0));
                    }
                } catch (Exception e) {
                    Logger.warn(ImportUtil.class, e.getMessage());
                }

                String wfActionIdStr;
                if ( -1 < wfActionIdIndex ) {
                    wfActionIdStr = line[wfActionIdIndex];
                    if(UtilMethods.isSet(wfActionIdStr)) {
                        cont.setActionId(wfActionIdStr);
                    }
                }

                //Set the site and folder
                setSiteAndFolder(user, cont, siteAndFolder);

                //Fill the new contentlet with the data
                for (Integer column : headers.keySet()) {
                    Field field = headers.get(column);
                    Object value = values.get(column);

                    if (field.getFieldType().equals(Field.FieldType.HOST_OR_FOLDER.toString())) { // DOTCMS-4484
                        // Site and Folder are already set, so we can continue
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
                        if (new LegacyFieldTransformer(field).from().typeName().equals(BinaryField.class.getName())){
                            if(preview){
                                //To avoid creating temp files for preview, we just create a dummy file and assign it to the contentlet
                                final File dummyFile = File.createTempFile("dummy", ".txt", new File(ConfigUtils.getAssetTempPath()));
                                cont.setBinary(field.getVelocityVarName(), dummyFile);
                            } else {
                                if (null != value && UtilMethods.isSet(value.toString())) {
                                    final DotTempFile tempFile = APILocator.getTempFileAPI().createTempFileFromUrl(null, request, new URL(value.toString()), -1);
                                    cont.setBinary(field.getVelocityVarName(), tempFile.file);
                                }
                            }
                        } else {
                            conAPI.setContentletProperty(cont, field, value);
                        }
                    }catch(DotContentletStateException de){
                        if(!field.isRequired() || (value!=null && UtilMethods.isSet(String.valueOf(value)))){
                            throw de;
                        }
                    }
                }

                // Retaining Categories when content updated with partial imports
                if(UtilMethods.isSet(cont.getIdentifier())){

                    final List<Field> structureFields = FieldsCache.getFieldsByStructureInode(contentType.getInode());
                    final List<Field> categoryFields = new ArrayList<>();
                    for(Field field : structureFields){
                        if(field.getFieldType().equals(Field.FieldType.CATEGORY.toString()) || field.getFieldType().equals(Field.FieldType.CATEGORIES_TAB.toString()))
                            categoryFields.add(field);
                    }
                    for (Integer column : headers.keySet()) {
                        Field headerField = headers.get(column);
                        Iterator<Field> itr = categoryFields.iterator();
                        while (itr.hasNext()) {
                            Field field = itr.next();
                            if (headerField.getInode().equalsIgnoreCase(field.getInode())) {
                                itr.remove();
                            }
                        }
                    }

                    //Only run if there is Category Field missing from the Headers
                    if(!categoryFields.isEmpty()) {
                        final List<Field> nonHeaderCategoryFields = new ArrayList<>(categoryFields);
                        final List<Category> nonHeaderParentCats = new ArrayList<>();
                        final List<Category> nonHeaderCategories = new ArrayList<>();
                        List<Category> categoriesOnWorkingContent = new ArrayList<>();

                        //Find the Parent Category for each of the fields
                        for (final Field field : nonHeaderCategoryFields) {
                            nonHeaderParentCats.add(catAPI.find(field.getValues(), user, false));
                        }

                        //Get All the children of All the Parent Categories
                        for (final Category cat : nonHeaderParentCats) {
                            nonHeaderCategories.addAll(catAPI.getAllChildren(cat, user, false));
                        }

                    /*
                     We need to verify that we are not trying to save a contentlet that have as language the default language because that mean that
                     contentlet for that default language couldn't exist, we are just saving it after all....
                     */
                        Long languageId = langAPI.getDefaultLanguage().getId();
                        if (existingMultilingualLanguage != null) {
                            languageId = existingMultilingualLanguage;//Using the language another an existing contentlet with the same identifier
                        }

                        try {
                            final Contentlet workingCont = conAPI.findContentletByIdentifier(cont.getIdentifier(), false, languageId, user, false);
                            categoriesOnWorkingContent = catAPI.getParents(workingCont, user, false);
                        } catch (DotContentletStateException dse) {
                            Logger.error(ImportContentletsAction.class, dse.getMessage());
                        }

                        //We do this to only add the categories from non Header Categories Field, could be that there is
                        //more than one Category field, but only some of them are being excluded in the headers.
                        for (final Category contentCategory : categoriesOnWorkingContent) {
                            for (final Category nonHeaderCategory : nonHeaderCategories) {
                                if (contentCategory.getCategoryVelocityVarName().equals(nonHeaderCategory.getCategoryVelocityVarName())) {
                                    categories.add(contentCategory);
                                }
                            }
                        }
                    }
                }

                //Check if line has repeated values for a unique field, if it does then ignore the line
                boolean ignoreLine = false;
                if(!uniqueFieldBeans.isEmpty()){
                    ignoreLine =
                            validateUniqueFields(user, results, lineNumber, language, counters,
                                    uniqueFieldBeans,
                                    uniqueFields);
                }

                if(!ignoreLine){
                    //Check the new contentlet with the validator
                    final boolean skipRelationshipsValidation = headers.values().stream()
                            .noneMatch((field -> field.getFieldType()
                                    .equals(FieldType.RELATIONSHIP.toString())));

                    try {
                        if(skipRelationshipsValidation) {
                            conAPI.validateContentletNoRels(cont, new ArrayList<>(categories));

                        } else {
                            ContentletRelationships contentletRelationships = loadRelationshipRecords(
                                    csvRelationshipRecordsParentOnly, csvRelationshipRecordsChildOnly,
                                    csvRelationshipRecords, cont);

                            conAPI.validateContentlet(cont, contentletRelationships,
                                    new ArrayList<>(categories));
                        }
                    } catch (DotContentletValidationException ex) {
                        StringBuffer sb = new StringBuffer(LINE_NO + lineNumber + " contains errors\n");
                        HashMap<String,List<Field>> errors = (HashMap<String,List<Field>>) ex.getNotValidFields();
                        Set<String> keys = errors.keySet();
                        for (String key : keys) {
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

                    /*
                    Validating the action to execute
                     */
                    WorkflowAction executeWfAction = null;
                    boolean userCanExecuteAction = false;
                    // Check if the CSV file have set an actionId to execute and if the user
                    // have permission to execute the action
                    if (UtilMethods
                            .isSet(cont.getActionId())) {

                        try {
                            executeWfAction = validateWorkflowAction(user, cont);
                        } catch (Exception e) {

                            Logger.debug(ImportUtil.class, results + ", cont: " + cont + ", user: " +  user + ", lineNumber " +  lineNumber +
                                    "validateWorkflowAction, message.import.contentlet.invalid.action.selected: " + e.getMessage());
                            setActionWarning(results, cont, user, lineNumber,
                                    "message.import.contentlet.invalid.action.found.in.csv",
                                    e.getMessage());
                        }

                        if (null != executeWfAction && UtilMethods
                                .isSet(executeWfAction.getId())) {
                            userCanExecuteAction = true;
                        }
                    }

                    //If the CSV line doesn't have set a wfActionId or the user doesn't have
                    // permission to execute this action then check if and action was set int
                    // the import dropdown
                    if (!userCanExecuteAction && UtilMethods.isSet(wfActionId)) {

                        try {
                            cont.setActionId(wfActionId);
                            executeWfAction = validateWorkflowAction(user, cont);
                        } catch (Exception e) {

                            Logger.debug(ImportUtil.class, results + ", cont: " + cont + ", user: " +  user + ", lineNumber " +  lineNumber +
                                    "message.import.contentlet.invalid.action.selected: " + e.getMessage());
                            setActionWarning(results, cont, user, lineNumber,
                                    "message.import.contentlet.invalid.action.selected",
                                    e.getMessage());
                        }

                        if (null != executeWfAction && UtilMethods
                                .isSet(executeWfAction.getId())) {
                            userCanExecuteAction = true;
                        }
                    }

                    //If not preview save the contentlet
                    if (!preview) {
                        cont.setLowIndexPriority(true);

                        ContentletRelationships contentletRelationships = loadRelationshipRecords(
                                csvRelationshipRecordsParentOnly, csvRelationshipRecordsChildOnly,
                                csvRelationshipRecords, cont);

                        if (userCanExecuteAction) {
                          cont.setIndexPolicy(IndexPolicy.DEFER);

                          cont.setBoolProperty(Contentlet.SKIP_RELATIONSHIPS_VALIDATION, skipRelationshipsValidation);

                          Logger.debug(ImportUtil.class, "fireContentWorkflow: " + executeWfAction.getName() + ", id: " + executeWfAction.getId());
                            cont = workflowAPI.fireContentWorkflow(cont,
                                    new ContentletDependencies.Builder()
                                            .respectAnonymousPermissions(Boolean.FALSE)
                                            .modUser(user)
                                            .relationships(contentletRelationships)
                                            .workflowActionId(executeWfAction.getId())
                                            .workflowActionComments("")
                                            .workflowAssignKey("")
                                            .categories(new ArrayList<>(categories))
                                            .generateSystemEvent(Boolean.FALSE).build());
                        } else {
                            Logger.debug(ImportUtil.class, "runWorkflowIfCould");
                            cont = runWorkflowIfCould(user, contentTypePermissions,
                                    new ArrayList<>(categories), cont, contentletRelationships);
                        }

                        for (Integer column : headers.keySet()) {
                            Field field = headers.get(column);
                            Object value = values.get(column);
                            if (field.getFieldType().equals(Field.FieldType.TAG.toString()) &&
                                    value instanceof String) {
                                String[] tags = ((String)value).split(",");
                                if(siteAndFolder != null){
                                    //the CSV File has a Host Or Field Column, with a valid value
                                    final Host host = siteAndFolder.getLeft();
                                    String hostId = "";
                                    if(UtilMethods.isSet(host)){
                                        if(host.getIdentifier().equals(Host.SYSTEM_HOST))
                                            hostId = Host.SYSTEM_HOST;
                                        else
                                            hostId = host.getIdentifier();
                                    } else {
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
                                } else {
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
        } catch (final Exception e) {
            Logger.error(ImportUtil.class, String.format("An error occurred when importing line # %s: %s",
                    lineNumber, e.getMessage()), e);
            throw new DotRuntimeException(e.getMessage(), e);
        }
    }

    /**
     * Get the site and folder from the given identifier or name
     * @param idOrName the identifier or name
     *                 it can be a host identifier, a folder identifier, a path to a folder or a host name
     * @param user current user
     * @return a pair with the host and folder
     */
    private static Pair<Host, Folder> getSiteAndFolderFromIdOrName(
            final String idOrName, final User user)
            throws DotDataException, DotSecurityException {

        if (!UtilMethods.isSet(idOrName)) {
            return null;
        }

        // Verify if the value belongs to a site identifier
        Host site = hostAPI.find(idOrName, user, false);

        if (!isSiteSet(site)) {
            // If a site was not found using the given value (identifier) it can be a folder
            Folder folder = folderAPI.find(idOrName, user, false);

            if (!isFolderSet(folder)) {
                // If a site or folder was not found using the given value (identifier)
                // it can be a path to a folder or a site name
                final Pair<String, String> hostNameAndPath = getHostNameAndPath(idOrName);
                final String hostName = hostNameAndPath.getLeft();
                final String path = hostNameAndPath.getRight();
                site = hostAPI.findByName(hostName, user, false);
                if (isSiteSet(site) && UtilMethods.isSet(path)) {
                    folder = folderAPI.findFolderByPath(path, site, user, false);
                }
            }

            if (isFolderSet(folder)) {
                return ImmutablePair.of(folder.getHost(), folder);
            }
        }

        if (isSiteSet(site)) {
            return ImmutablePair.of(site, APILocator.getFolderAPI().findSystemFolder());
        }

        return null;

    }

    private static boolean isFolderSet(Folder folder) {
        return UtilMethods.isSet(folder) && InodeUtils.isSet(folder.getInode());
    }

    private static boolean isSiteSet(final Host site) {
        return UtilMethods.isSet(site) && UtilMethods.isSet(site.getIdentifier());
    }

    /**
     * Get the host name and the path from the given string
     * If the string contains a double slash, it is used to include the site name followed by the path
     * Otherwise, the value is considered a site name
     * @param hostNameOrPath the host name or host name and path
     * @return a pair with the host name and the path (if any)
     */
    private static Pair<String, String> getHostNameAndPath(final String hostNameOrPath) {
        if (hostNameOrPath.contains(StringPool.DOUBLE_SLASH)) {
            // double slash is used to include the site name followed by the path
            final String[] arr = hostNameOrPath.split(StringPool.FORWARD_SLASH);
            final StringBuilder path = new StringBuilder().append(StringPool.FORWARD_SLASH);
            String hostName = null;
            // first path part is the site name, the rest is the path
            for (String pathPart : arr) {
                if (UtilMethods.isSet(pathPart) && hostName == null) {
                    hostName = pathPart;
                } else if (UtilMethods.isSet(pathPart)) {
                    path.append(pathPart);
                    path.append(StringPool.FORWARD_SLASH);
                }
            }
            return ImmutablePair.of(hostName, path.toString());
        }
        // no double slash present, so return the given string as the site name
        return ImmutablePair.left(hostNameOrPath);
    }

    /**
     * Add the site and folder to the ES query to check unique fields
     * @param siteAndFolder the site and folder pair
     * @param fieldValue the field value from the CSV line
     * @return the ES query to check unique fields by site and folder
     */
    private static String addSiteAndFolderToESQuery(
            final Pair<Host, Folder> siteAndFolder, final String fieldValue) {
        final StringBuilder siteAndFolderQuery = new StringBuilder();
        if (siteAndFolder != null) {
            if (siteAndFolder.getLeft() != null) {
                final Host host = siteAndFolder.getLeft();
                siteAndFolderQuery.append(" +conhost:").append(host.getIdentifier());
            }
            if (siteAndFolder.getRight() != null) {
                final Folder folder = siteAndFolder.getRight();
                siteAndFolderQuery.append(" +conFolder:").append(folder.getInode());
            }
        } else if (UtilMethods.isSet(fieldValue)) {
            siteAndFolderQuery.append(" +(conhost:").append(fieldValue).append(" conFolder:")
                    .append(fieldValue).append(")");
        }
        return siteAndFolderQuery.toString();
    }

    /**
     * Set the site and folder for the given contentlet
     * @param user current user
     * @param cont the contentlet
     * @param siteAndFolder the site and folder pair
     * @throws DotDataException if an dotCMS data error occurs
     * @throws DotSecurityException if a security error occurs
     */
    private static void setSiteAndFolder(final User user,
            final Contentlet cont, final Pair<Host, Folder> siteAndFolder)
            throws DotDataException, DotSecurityException {
        if (siteAndFolder != null) {
            final Host host = siteAndFolder.getLeft();
            final Folder folder = siteAndFolder.getRight();
            if (UtilMethods.isSet(folder) && !folder.isSystemFolder() &&
                    !permissionAPI.doesUserHavePermission(folder,
                            PermissionAPI.PERMISSION_CAN_ADD_CHILDREN, user)) {
                throw new DotSecurityException( "User has no Add Children Permissions on selected folder" );
            } else if (UtilMethods.isSet(host) && (!permissionAPI.doesUserHavePermission(
                    host,PermissionAPI.PERMISSION_CAN_ADD_CHILDREN, user))) {
                throw new DotSecurityException("User has no Add Children Permissions on selected host");

            }
            if (UtilMethods.isSet(host) && UtilMethods.isSet(folder)) {
                cont.setHost(host.getIdentifier());
                cont.setFolder(folder.getInode());
            }
        }
    }

    /**
     * Check if the URL field for an HTMLPage content type is set correctly
     * @param contentType the content type
     * @param urlValue the URL field value
     * @param siteAndFolderFromLine the site and folder from the site field in the import line
     *                              it can be null if the site field is not set
     * @param user the current user
     */
    private static Pair<Pair<Host,Folder>, String> checkURLFieldForHTMLPage(
            final Structure contentType, final String urlValue,
            final Pair<Host, Folder> siteAndFolderFromLine, final User user)
            throws DotDataException, DotSecurityException {

        final ContentType targetContentType = new StructureTransformer(contentType).from();
        if (UtilMethods.isSet(urlValue) &&
                BaseContentType.HTMLPAGE.getType() == targetContentType.baseType().getType()) {

            // Check if the URL field value includes parent folder and asset name
            String assetName = urlValue;
            String parentPath = null;
            if (StringUtils.lastIndexOf(urlValue, StringPool.FORWARD_SLASH) >= 0) {
                assetName = StringUtils.substringAfterLast(urlValue, StringPool.FORWARD_SLASH);
                parentPath = StringUtils.substringBeforeLast(urlValue, StringPool.FORWARD_SLASH);
            }

            // If there is parent path and also a site without folder was already read
            // from the site field in the import line, check if the parent folder exists and return it
            // If there is a parent path and a site was not read from the site field,
            // check if the parent folder exists in the default site instead
            final Host site = siteAndFolderFromLine == null ?
                    hostAPI.findDefaultHost(user, false) :
                    siteAndFolderFromLine.getLeft();
            final Folder folder = siteAndFolderFromLine == null ?
                    folderAPI.findSystemFolder() : siteAndFolderFromLine.getRight();
            final Pair<Host, Folder> siteAndFolder = ImmutablePair.of(site, folder);

            if (UtilMethods.isSet(parentPath)) {
                return ImmutablePair.of(
                        getHostAndFolderFromParentPathOrSiteField(
                                siteAndFolder, user, parentPath),
                        assetName);
            }

            // If there is no parent path, return the site and folder from the site field
            return ImmutablePair.of(siteAndFolder, assetName);
        }

        return null;
    }

    /**
     * Get the host and folder from given parent path.
     * Returns the host and folder from the site field in the import line if already set
     * @param siteAndFolder the site and folder from the site field in the import line
     * @param user the current user
     * @param parentPath the parent path for the URL field value
     * @return the host and folder from the parent path or the site field
     */
    private static Pair<Host, Folder> getHostAndFolderFromParentPathOrSiteField(
            final Pair<Host, Folder> siteAndFolder,
            final User user, final String parentPath)
            throws DotDataException, DotSecurityException {

        final Host site = siteAndFolder.getLeft();
        final Folder siteFolder = siteAndFolder.getRight();
        final Folder parentFolder = folderAPI.findFolderByPath(
                parentPath, site, user, false);
        if (isFolderSet(parentFolder)) {
            if ((siteFolder == null || siteFolder.isSystemFolder())) {
                return ImmutablePair.of(site, parentFolder);
            } else {
                if (isFolderSet(siteFolder) && !parentFolder.getInode().equals(siteFolder.getInode())) {
                    Logger.warn(ImportUtil.class, String.format(
                            "Folder from site field %s doesn't match parent folder for URL: %s",
                            siteFolder.getPath(), parentFolder.getPath()));
                }
                return siteAndFolder;
            }
        } else {
            Logger.warn(ImportUtil.class, String.format(
                    "Parent folder not found for URL field value: %s", parentPath));
            return null;
        }

    }

    /**
     * Get the URL from the folder path and asset name
     * @param folderAndAssetName the folder and asset name pair
     * @return the URL for the given folder path and asset name
     */
    private static String getURLFromFolderAndAssetName(
            final Pair<Pair<Host, Folder>,String> folderAndAssetName) {
        final Pair<Host, Folder> siteAndFolder = folderAndAssetName.getLeft();
        final String assetName = folderAndAssetName.getRight();
        final StringBuilder url = new StringBuilder();
        if (siteAndFolder != null) {
            final Folder folder = siteAndFolder.getRight();
            if (isFolderSet(folder)) {
                url.append(folder.getPath());
                if (UtilMethods.isSet(folder.getPath())
                        && !folder.getPath().endsWith(StringPool.FORWARD_SLASH)) {
                    url.append(StringPool.FORWARD_SLASH);
                }
            }
        }
        url.append(assetName);
        return url.toString();
    }

    /**
     * Get the URL from the content identifier
     * @param contentId the content identifier
     * @return the URL for the given content identifier
     */
    private static String getURLFromContentId(final String contentId) {
        StringBuilder url = new StringBuilder();
        if (contentId != null ) {
            try {
                final Identifier identifier = APILocator.getIdentifierAPI().find(contentId);
                if (UtilMethods.isSet(identifier) && UtilMethods.isSet(identifier.getId())) {
                    url.append(identifier.getURI());
                }
            } catch (DotDataException e) {
                Logger.error(ImportUtil.class, "Unable to get Identifier with id ["
                        + contentId + "]. Could not get the url", e );
            }
        }
        return url.toString();
    }

    private static Contentlet runWorkflowIfCould(final User user, final List<Permission> contentTypePermissions,
                                                 final List<Category> categories, final Contentlet contentlet,
                                                 final ContentletRelationships contentletRelationships) throws DotDataException, DotSecurityException {
        // If the User doesn't have permissions to execute the wfActionId or
        // not action Id is set on the CSV/Import select box then use the old
        // checking method
        final boolean live = Config.getBooleanProperty(
                "PUBLISH_CSV_IMPORTED_CONTENT_AUTOMATICALLY", false);
        final Optional<WorkflowAction> workflowActionSaveOpt =
                workflowAPI.findActionMappedBySystemActionContentlet
                        (contentlet, WorkflowAPI.SystemAction.NEW, user);

        if (workflowActionSaveOpt.isPresent()) {

            if (workflowActionSaveOpt.get().hasSaveActionlet()) {

                Logger.debug(ImportUtil.class,
                        ()-> "Importing a contentlet with the save action: "
                        + workflowActionSaveOpt.get().getName());
                final Contentlet savedContent = workflowAPI.fireContentWorkflow
                        (contentlet, new ContentletDependencies.Builder()
                                .workflowActionId(workflowActionSaveOpt.get().getId())
                                .relationships(contentletRelationships).categories(categories)
                                .permissions(contentTypePermissions).modUser(user).build());
                return live && !workflowActionSaveOpt.get().hasPublishActionlet()?
                        runWorkflowPublishIfCould(contentletRelationships,
                                categories, contentTypePermissions, user, savedContent):
                        savedContent;
            } else {

                contentlet.setActionId(workflowActionSaveOpt.get().getId());
            }
        }

        if (null == contentlet.getActionId()) {
            contentlet.setProperty(Contentlet.DISABLE_WORKFLOW, true); // it is needed to avoid recursive call
        }
        contentlet.setProperty(Contentlet.DONT_VALIDATE_ME, true);
        final Contentlet contentletSaved = conAPI.checkin(contentlet, contentletRelationships,
                categories, contentTypePermissions,
                user, false);

        if (live) {
            APILocator.getContentletAPI().publish(contentletSaved, user, false);
        }
        return contentletSaved;
    }

    private static Contentlet runWorkflowPublishIfCould(final ContentletRelationships contentletRelationships,
                                                 final List<Category>   categories,
                                                 final List<Permission> permissions,
                                                 final User user,
                                                 final Contentlet savedContent) throws DotDataException, DotSecurityException {

        final Optional<WorkflowAction> workflowActionPublishOpt =
                workflowAPI.findActionMappedBySystemActionContentlet
                        (savedContent, WorkflowAPI.SystemAction.PUBLISH, user);

        if (workflowActionPublishOpt.isPresent()) {

            if (workflowActionPublishOpt.get().hasPublishActionlet()) {

                Logger.debug(ImportUtil.class,
                        () -> "Importing a contentlet with the publish action: "
                        + workflowActionPublishOpt.get().getName());
                return workflowAPI.fireContentWorkflow
                        (savedContent, new ContentletDependencies.Builder()
                                .workflowActionId(workflowActionPublishOpt.get().getId())
                                .relationships(contentletRelationships).categories(categories)
                                .permissions(permissions).modUser(user).build());
            } else {

                savedContent.setActionId(workflowActionPublishOpt.get().getId());
            }
        }

        if (null == savedContent.getActionId()) {
            savedContent.setProperty(Contentlet.DISABLE_WORKFLOW, true); // it is needed to avoid recursive call
        }

        conAPI.publish(savedContent, user, false);
        return savedContent;
    }
    /**
     *
     * @param csvRelationshipRecordsParentOnly
     * @param csvRelationshipRecordsChildOnly
     * @param csvRelationshipRecords
     * @param cont
     * @return
     * @throws DotDataException
     */
    @NotNull
    private static ContentletRelationships loadRelationshipRecords(
            HashMap<Relationship, List<Contentlet>> csvRelationshipRecordsParentOnly,
            HashMap<Relationship, List<Contentlet>> csvRelationshipRecordsChildOnly,
            HashMap<Relationship, List<Contentlet>> csvRelationshipRecords, Contentlet cont)
            throws DotDataException {
        //Load the old relationShips and add the new ones
        ContentletRelationships contentletRelationships = conAPI.getAllRelationships(cont);
        List<ContentletRelationships.ContentletRelationshipRecords> relationshipRecords = contentletRelationships
                .getRelationshipsRecords();
        for (ContentletRelationships.ContentletRelationshipRecords relationshipRecord : relationshipRecords) {
            List<Contentlet> csvRelatedContentlet = csvRelationshipRecords
                    .get(relationshipRecord.getRelationship());

            //Relationship must be wiped out when the relationship field is sent as empty
            if (csvRelationshipRecords.containsKey(relationshipRecord.getRelationship())
                    && csvRelatedContentlet == null) {
                relationshipRecord.setRecords(new ArrayList<>());
            }

            if (UtilMethods.isSet(csvRelatedContentlet)) {
                addRelatedContent(relationshipRecord, csvRelatedContentlet);
            }
            csvRelatedContentlet = csvRelationshipRecordsChildOnly
                    .get(relationshipRecord.getRelationship());
            if (UtilMethods.isSet(csvRelatedContentlet) && relationshipRecord.isHasParent()) {
                addRelatedContent(relationshipRecord, csvRelatedContentlet);
            }
            csvRelatedContentlet = csvRelationshipRecordsParentOnly
                    .get(relationshipRecord.getRelationship());
            if (UtilMethods.isSet(csvRelatedContentlet) && !relationshipRecord.isHasParent()) {
                addRelatedContent(relationshipRecord, csvRelatedContentlet);
            }
        }
        //END Load the old relationShips and add the new ones
        return contentletRelationships;
    }

    /**
     *
     * @param relationshipRecord
     * @param csvRelatedContentlet
     */
    private static void addRelatedContent(final ContentletRelationshipRecords relationshipRecord,
            final List<Contentlet> csvRelatedContentlet) {
        relationshipRecord.getRecords().addAll(csvRelatedContentlet.stream()
                .filter(related -> relationshipRecord.getRecords().stream().noneMatch(
                        record -> record.getIdentifier().equals(related.getIdentifier())))
                .collect(Collectors.toList()));
    }

    /**
     * Validates if Action associated to the Contentlet can be executed validating permissions
     * and the step of the Contentlet
     */
    private static WorkflowAction validateWorkflowAction(final User user,
            final Contentlet contentlet)
            throws DotDataException {

        WorkflowAction executeWfAction;
        String actionId = contentlet.getActionId();
        try {

            //Validate the permissions over the action to execute
            executeWfAction = workflowAPI
                    .findActionRespectingPermissions(actionId, contentlet, user);

            //Validate if the action we want to execute is in the right step
            workflowAPI.validateActionStepAndWorkflow(contentlet, user);
        } catch (final DotSecurityException e) {
            throw new DotDataException(String.format("User '%s' doesn't have permissions to execute Workflow Action " +
                    "'%s': %s", user.getUserId(), actionId, e.getMessage()), e);
        } catch (final DotDataException | IllegalArgumentException e) {
            throw new DotDataException(String.format("An error occurred when validating Workflow Action '%s' on " +
                    "content '%s': %s", actionId, contentlet.getIdentifier(), e.getMessage()), e);
        }

        return executeWfAction;
    }

    /**
     * Sets a warning message related to the Action associated to the Contentlet
     */
    private static void setActionWarning(HashMap<String, List<String>> results,
            Contentlet contentlet,
            final User user, final int lineNumber, final String generalErrorKey,
            final String specificErrorMessage)
            throws LanguageException {

        results.get("warnings")
                .add(LanguageUtil.get(user, "Line--") + " " + lineNumber + ". "
                        + LanguageUtil.get(user, generalErrorKey) + " "
                        + specificErrorMessage);
        // if the user doesn't have access to the action then removed it from
        // the content to avoid troubles executing the action set on the
        // dropdown or on the checkin
        contentlet.resetActionId();
    }

    private static Object validateDateTypes(final int lineNumber, final Field field,
            final String value,
            Object valueObj) {
        if (field.getFieldContentlet().startsWith("date")) {
            if (UtilMethods.isSet(value)) {
                try {
                    valueObj = parseExcelDate(value);
                } catch (ParseException e) {
                    throw new DotRuntimeException(
                            LINE_NO + lineNumber + " contains errors, Column: " + field
                                    .getVelocityVarName() +
                                    ", value: " + value
                                    + ", couldn't be parsed as any of the following supported formats: "
                                    +
                                    printSupportedDateFormats());
                }
            } else {
                valueObj = null;
            }
        }
        return valueObj;
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
                            LanguageUtil.get(user, "Line--") + lineNumber + ": " + LanguageUtil
                                .get(user, "contains-duplicate-values-for-structure-unique-field") + " '" + f
                                .getVelocityVarName() + "', " + LanguageUtil.get(user, "and-will-be-ignored"));
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

        private Collection<Map<String, String>> keys = new ArrayList<>();

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

                Map<String, String> keyMap = new HashMap<>();
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
                field.getFieldType().equals( Field.FieldType.PERMISSIONS_TAB.toString()) ||
                field.getFieldType().equals(Field.FieldType.COLUMN.toString())      ||  
                field.getFieldType().equals(Field.FieldType.ROW.toString())    
                        
                );
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

    /**
     * Utility method that retrieves the appropriate error message from an exception stack trace. This error might come
     * from either the Content Preview, or the Content Import step.
     *
     * @param user      The {@link User} calling the CSV Content Import tool.
     * @param exception The exception that was thrown because of the error.
     *
     * @return The appropriate error message that will be displayed to the user.
     *
     * @throws LanguageException An error occurred when accessing the i18n resource files.
     */
    private static String getErrorMsgFromException(final User user, final DotRuntimeException exception) throws
            LanguageException {
        if (!UtilMethods.isSet(exception.getMessage())) {
            return exception.getCause().getClass().getSimpleName();
        } else {
            return LanguageUtil.get(user, exception.getMessage());
        }
    }

    /**
     * Container class holding content type information needed for header validation. This includes
     * fields, relationships, and a count of importable fields.
     */
    private static class ContentTypeInfo {

        /**
         * List of fields defined in the content type
         */
        final List<Field> fields;

        /**
         * List of relationships associated with the content type
         */
        final List<Relationship> relationships;

        /**
         * Count of fields that can be imported
         */
        final int importableFields;

        /**
         * Creates a new ContentTypeInfo instance.
         *
         * @param fields           List of fields in the content type
         * @param relationships    List of relationships for the content type
         * @param importableFields Count of fields that can be imported
         */
        ContentTypeInfo(final List<Field> fields, final List<Relationship> relationships,
                final int importableFields) {
            this.fields = fields;
            this.relationships = relationships;
            this.importableFields = importableFields;
        }
    }

    /**
     * Container class for managing structured import results during the validation process. This
     * class handles building and updating the structured result format.
     */
    private static class ImportResults {

        /**
         * Builder for the structured results
         */
        private final ImportResult.Builder structuredResults;

        /**
         * Creates a new ImportResults instance with initialized data structures.
         */
        ImportResults() {
            this.structuredResults = initializeStructuredResults();
        }

        /**
         * Initializes the base structure for import results.
         *
         * @return Builder configured with default values
         */
        private static ImportResult.Builder initializeStructuredResults() {
            return ImportResult.builder()
                    .fileInfo(ImportFileInfo.builder()
                            .totalRows(0)
                            .parsedRows(0)
                            .headerInfo(initializeHeaderInfo())
                            .build())
                    .data(initializeResultData());
        }

        /**
         * Initializes header information with empty arrays.
         *
         * @return HeaderInfo builder with default values
         */
        private static ImportHeaderInfo initializeHeaderInfo() {
            return ImportHeaderInfo.builder()
                    .validHeaders(new String[0])
                    .invalidHeaders(new String[0])
                    .missingHeaders(new String[0])
                    .validationDetails(new HashMap<>())
                    .build();
        }

        /**
         * Initializes result data with zero counters.
         *
         * @return ResultData builder with default values
         */
        private static ImportResultData initializeResultData() {
            return ImportResultData.builder()
                    .processed(ProcessedData.builder()
                            .valid(0)
                            .invalid(0)
                            .build())
                    .summary(ContentSummary.builder()
                            .created(0)
                            .updated(0)
                            .contentType("")
                            .build())
                    .build();
        }

        /**
         * Adds multiple validation messages to the results.
         *
         * @param validationMessages List of messages to add
         */
        void addMessages(List<ImportValidationMessage> validationMessages) {
            if (validationMessages != null) {
                validationMessages.forEach(structuredResults::addMessages);
            }
        }

        /**
         * Adds a single validation message to the results.
         *
         * @param message Message to add
         */
        void addMessage(ImportValidationMessage message) {
            structuredResults.addMessages(message);
        }

        /**
         * Updates file processing information.
         *
         * @param fileInfo Updated file information
         */
        void updateFileInfo(final ImportFileInfo fileInfo) {
            structuredResults.fileInfo(fileInfo);
        }

        /**
         * Updates header validation information.
         *
         * @param headerInfo Updated header information
         */
        void updateHeaderInfo(final ImportHeaderInfo headerInfo) {
            ImportFileInfo currentFileInfo = structuredResults.build().fileInfo();
            structuredResults.fileInfo(currentFileInfo.withHeaderInfo(headerInfo));
        }

        /**
         * Updates the content type name in the results.
         *
         * @param contentType Name of the content type
         */
        void updateContentType(String contentType) {
            ImportResultData currentData = structuredResults.build().data();
            structuredResults.data(currentData.withSummary(
                    currentData.summary().withContentType(contentType)
            ));
        }

        /**
         * Updates processing counters in the results.
         *
         * @param counts Updated counter values
         */
        void updateCounters(ImportCounts counts) {
            structuredResults.data(ImportResultData.builder()
                    .processed(ProcessedData.builder()
                            .valid(counts.getValid())
                            .invalid(counts.getInvalid())
                            .build())
                    .summary(ContentSummary.builder()
                            .created(counts.getCreated())
                            .updated(counts.getUpdated())
                            .contentType(structuredResults.build().data().summary().contentType())
                            .build())
                    .build());
        }

        /**
         * Builds and returns the final structured results.
         *
         * @return Complete structured import results
         */
        ImportResult getResults() {
            return structuredResults.build();
        }
    }

    /**
     * Value object holding count information for import processing. Tracks valid/invalid records
     * and created/updated content counts.
     */
    private static class ImportCounts {

        private final int valid;
        private final int invalid;
        private final int created;
        private final int updated;

        /**
         * Creates a new ImportCounts instance.
         *
         * @param valid   Count of valid records
         * @param invalid Count of invalid records
         * @param created Count of newly created content
         * @param updated Count of updated content
         */
        private ImportCounts(int valid, int invalid, int created, int updated) {
            this.valid = valid;
            this.invalid = invalid;
            this.created = created;
            this.updated = updated;
        }

        /**
         * Factory method to create an ImportCounts instance.
         *
         * @param valid   Count of valid records
         * @param invalid Count of invalid records
         * @param created Count of newly created content
         * @param updated Count of updated content
         * @return New ImportCounts instance
         */
        static ImportCounts of(int valid, int invalid, int created, int updated) {
            return new ImportCounts(valid, invalid, created, updated);
        }

        public int getValid() {
            return valid;
        }

        public int getInvalid() {
            return invalid;
        }

        public int getCreated() {
            return created;
        }

        public int getUpdated() {
            return updated;
        }

        @Override
        public boolean equals(Object o) {
            if (this == o) {
                return true;
            }
            if (o == null || getClass() != o.getClass()) {
                return false;
            }
            ImportCounts that = (ImportCounts) o;
            return valid == that.valid &&
                    invalid == that.invalid &&
                    created == that.created &&
                    updated == that.updated;
        }

        @Override
        public int hashCode() {
            return Objects.hash(valid, invalid, created, updated);
        }
    }

}
