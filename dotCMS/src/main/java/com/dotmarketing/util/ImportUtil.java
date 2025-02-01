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
import com.dotmarketing.util.importer.HeaderValidationCodes;
import com.dotmarketing.util.importer.ImportResultConverter;
import com.dotmarketing.util.importer.model.AbstractSpecialHeaderInfo.SpecialHeaderType;
import com.dotmarketing.util.importer.model.AbstractValidationMessage.ValidationMessageType;
import com.dotmarketing.util.importer.model.ContentSummary;
import com.dotmarketing.util.importer.model.ContentletSearchResult;
import com.dotmarketing.util.importer.model.FieldProcessingResult;
import com.dotmarketing.util.importer.model.FileInfo;
import com.dotmarketing.util.importer.model.HeaderInfo;
import com.dotmarketing.util.importer.model.HeaderValidationResult;
import com.dotmarketing.util.importer.model.ImportResult;
import com.dotmarketing.util.importer.model.LineImportResult;
import com.dotmarketing.util.importer.model.ProcessedContentResult;
import com.dotmarketing.util.importer.model.ProcessedData;
import com.dotmarketing.util.importer.model.RelationshipProcessingResult;
import com.dotmarketing.util.importer.model.ResultData;
import com.dotmarketing.util.importer.model.SpecialHeaderInfo;
import com.dotmarketing.util.importer.model.UniqueFieldBean;
import com.dotmarketing.util.importer.model.ValidationMessage;
import com.liferay.portal.language.LanguageException;
import com.liferay.portal.language.LanguageUtil;
import com.liferay.portal.model.User;
import com.liferay.util.StringPool;
import java.io.File;
import java.io.IOException;
import java.io.Reader;
import java.net.URL;
import java.sql.Timestamp;
import java.text.DateFormat;
import java.text.ParseException;
import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.Collection;
import java.util.Collections;
import java.util.Date;
import java.util.HashMap;
import java.util.HashSet;
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
                HeaderValidationResult headerValidation;
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
                // Convert header validation results to legacy format
                ImportResultConverter.headerValidationResultsToLegacyMap(headerValidation, results);
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

                                // Get identifier from results if it exists
                                final String identifier = getIdentifierFromResults(results,
                                        csvLine);
                                // Get workflow action id column index from results if it exists
                                final int wfActionIdIndex = getWorkflowActionIdIndexFromResults(
                                        results);

                                //Importing content record...
                                final var importLineResult = importLine(csvLine, currentSiteId,
                                        contentType, preview, isMultilingual, user, identifier,
                                        wfActionIdIndex,
                                        lineNumber, languageToImport, headers, keyFields,
                                        choosenKeyField, keyContentUpdated, contentTypePermissions,
                                        uniqueFieldBeans, uniqueFields, relationships, onlyChild,
                                        onlyParent, sameKeyBatchInsert, wfActionId, request);
                                // ---
                                // Convert import results to legacy format
                                ImportResultConverter.lineImportResultToLegacyMap(
                                        importLineResult, results, counters
                                );
                                errors += results.get("errors").size();
                                // ---

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
    private static HeaderValidationResult importHeaders(final String[] headerLine,
            final Structure contentType, final String[] keyFieldsInodes,
            final boolean isMultilingual, final User user, final HashMap<Integer, Field> headers,
            final HashMap<Integer, Field> keyFields, final List<Field> uniqueFields,
            final HashMap<Integer, Relationship> relationships,
            final HashMap<Integer, Boolean> onlyChild, final HashMap<Integer, Boolean> onlyParent)
            throws Exception {

        // Create structured results for validation tracking
        final var validationBuilder = HeaderValidationResult.builder();
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
            final User user, final HeaderValidationResult.Builder validationBuilder)
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
        final var headerInfo = HeaderInfo.builder()
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
            final HeaderValidationResult.Builder validationBuilder) throws LanguageException {

        if (headerLine == null || headerLine.length == 0) {
            validationBuilder.addMessages(ValidationMessage.builder()
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
                validationBuilder.addMessages(ValidationMessage.builder()
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
            final HeaderValidationResult.Builder validationBuilder)
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
            validationBuilder.addMessages(ValidationMessage.builder()
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
            final HeaderValidationResult.Builder validationBuilder) {

        if (!isMultilingual) {
            return;
        }

        boolean hasLanguageCode = headerFields.contains(languageCodeHeader);
        boolean hasCountryCode = headerFields.contains(countryCodeHeader);

        if (!hasLanguageCode || !hasCountryCode) {
            validationBuilder.addMessages(ValidationMessage.builder()
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
            final HeaderValidationResult.Builder validationBuilder) throws LanguageException {

        if (header.equalsIgnoreCase("Identifier")) {
            validationBuilder.addMessages(
                    ValidationMessage.builder()
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
                    ValidationMessage.builder()
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
            final HeaderValidationResult.Builder validationBuilder)
            throws LanguageException, DotDataException, DotSecurityException {

        for (Field field : FieldsCache.getFieldsByStructureInode(contentType.getInode())) {
            if (!field.getVelocityVarName().equalsIgnoreCase(header)) {
                continue;
            }

            if (isNonImportableField(field)) {
                validationBuilder.addMessages(ValidationMessage.builder()
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
            final HeaderValidationResult.Builder validationBuilder) throws LanguageException {

        // Get required fields
        List<String> requiredFields = collectRequiredFields(
                FieldsCache.getFieldsByStructureInode(contentType.getInode()));

        // Find missing required fields
        List<String> missingRequired = new ArrayList<>(requiredFields);
        missingRequired.removeAll(headerFields);

        // Add errors for missing required fields
        for (String requiredField : missingRequired) {
            validationBuilder.addMessages(
                    ValidationMessage.builder()
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
            final HeaderValidationResult.Builder validationBuilder) throws LanguageException {

        if (keyFieldsInodes.length == 0) {

            validationBuilder.addMessages(
                    ValidationMessage.builder()
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
                        ValidationMessage.builder()
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
            final HeaderValidationResult.Builder validationBuilder) throws LanguageException {
        if (uniqueFields.isEmpty()) {
            return;
        }

        for (Field uniqueField : uniqueFields) {
            validationBuilder.addMessages(
                    ValidationMessage.builder()
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
            final HeaderValidationResult.Builder validationBuilder) throws LanguageException {

        if (headerCount == importableFieldCount) {

            validationBuilder.addMessages(
                    ValidationMessage.builder()
                            .type(ValidationMessageType.INFO)
                            .message(LanguageUtil.get(user, headerCount + " " +
                                    LanguageUtil.get(user, "headers-match-these-will-be-imported")))
                            .build()
            );
        } else {
            if (headerCount > 0) {
                validationBuilder.addMessages(
                        ValidationMessage.builder()
                                .type(ValidationMessageType.INFO)
                                .message(headerCount + " " +
                                        LanguageUtil.get(user,
                                                "headers-found-on-the-file-matches-all-the-structure-fields"))
                                .build()
                );
            } else {
                validationBuilder.addMessages(
                        ValidationMessage.builder()
                                .type(ValidationMessageType.INFO)
                                .message(LanguageUtil.get(user,
                                        "No-headers-found-on-the-file-that-match-any-of-the-structure-fields"))
                                .build()
                );
            }

            validationBuilder.addMessages(
                    ValidationMessage.builder()
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
                    ValidationMessage.builder()
                            .type(ValidationMessageType.INFO)
                            .message(LanguageUtil.get(user, relationshipCount + " " +
                                    LanguageUtil.get(user,
                                            "relationship-match-these-will-be-imported")))
                            .build()
            );
        }
    }

    /**
     * Processes a single line from the CSV import file. This method handles all aspects
     * of importing a content line including:
     * <ul>
     *   <li>Field validation and processing</li>
     *   <li>Content matching and updates</li>
     *   <li>Relationship processing</li>
     *   <li>Workflow execution</li>
     *   <li>Category handling</li>
     * </ul>
     *
     * @param line CSV line data as string array
     * @param currentHostId ID of the current host/site
     * @param contentType Content type structure definition
     * @param preview If true, validates without saving changes
     * @param isMultilingual If true, handles multilingual content
     * @param user User performing the import
     * @param identifier Optional identifier for content updates
     * @param wfActionIdIndex Index of workflow action ID in CSV, -1 if none
     * @param lineNumber Current line number in CSV file
     * @param language Language ID for the content
     * @param headers Map of column indices to field definitions
     * @param keyFields Map of key fields used for content matching
     * @param choosenKeyField Buffer tracking chosen key fields
     * @param keyContentUpdated Set tracking updated content keys
     * @param contentTypePermissions Content type permissions
     * @param uniqueFieldBeans List tracking unique field values
     * @param uniqueFields List of fields marked as unique
     * @param relationships Map of relationship definitions
     * @param onlyChild Map tracking child-only relationships
     * @param onlyParent Map tracking parent-only relationships
     * @param sameKeyBatchInsert True if batch contains multiple rows with same key
     * @param wfActionId Workflow action ID to execute
     * @param request HTTP request context
     * @return Line import results including validation messages and processing outcomes
     * @throws DotRuntimeException If a critical error occurs during import
     */
    private static LineImportResult importLine(
            final String[] line,
            final String currentHostId,
            final Structure contentType,
            final boolean preview,
            boolean isMultilingual,
            final User user,
            final String identifier,
            final int wfActionIdIndex,
            final int lineNumber,
            final long language,
            final HashMap<Integer, Field> headers,
            final HashMap<Integer, Field> keyFields,
            final StringBuffer choosenKeyField,
            final HashSet<String> keyContentUpdated,
            final List<Permission> contentTypePermissions,
            final List<UniqueFieldBean> uniqueFieldBeans,
            final List<Field> uniqueFields,
            final HashMap<Integer, Relationship> relationships,
            final HashMap<Integer, Boolean> onlyChild,
            final HashMap<Integer, Boolean> onlyParent,
            final boolean sameKeyBatchInsert,
            final String wfActionId,
            final HttpServletRequest request)
            throws DotRuntimeException {

        final var resultBuilder = new LineImportResultBuilder(lineNumber);

        try {
            // First validate line length
            validateLineLength(line, headers, lineNumber);

            // Process fields and collect values
            final var fieldResults = processFields(line, headers, contentType, user, currentHostId,
                    language, lineNumber, choosenKeyField);

            // Add key field info message if in preview mode
            if (preview && choosenKeyField.length() > 1) {
                resultBuilder.messages.add(ValidationMessage.builder()
                        .type(ValidationMessageType.INFO)
                        .message(LanguageUtil.get(user, "Fields-selected-as-key") + ": " +
                                choosenKeyField.substring(1))
                        .build());
            }

            fieldResults.messages().forEach(resultBuilder::addValidationMessage);
            fieldResults.keyFields().forEach(resultBuilder::addKeyField);
            fieldResults.categories().forEach(resultBuilder::addCategory);
            uniqueFieldBeans.addAll(fieldResults.uniqueFields());

            //Check if line has repeated values for a unique field, if it does then ignore the line
            if (!uniqueFieldBeans.isEmpty()) {
                boolean ignoreLine = validateUniqueFields(user, lineNumber, language,
                        uniqueFieldBeans, uniqueFields, resultBuilder);
                if (ignoreLine) {
                    resultBuilder.setIgnoreLine(true);
                    return resultBuilder.build();
                }
            }

            HashMap<Integer, Object> values = new HashMap<>();
            Set<Category> categories = new HashSet<>();
            values.putAll(fieldResults.values());
            categories.addAll(fieldResults.categories());

            if (fieldResults.ignoreLine()) {
                return resultBuilder.build();
            }

            Long existingMultilingualLanguage = null;

            Pair<Host, Folder> siteAndFolder = fieldResults.siteAndFolder().orElse(null);
            Pair<Integer, String> urlValue = fieldResults.urlValue().orElse(null);
            String urlValueAssetName = fieldResults.urlValueAssetName().orElse(null);

            // Initialize relationship maps
            final HashMap<Relationship, List<Contentlet>> csvRelationshipRecordsParentOnly = new HashMap<>();
            final HashMap<Relationship, List<Contentlet>> csvRelationshipRecordsChildOnly = new HashMap<>();
            final HashMap<Relationship, List<Contentlet>> csvRelationshipRecords = new HashMap<>();

            // Process relationships if any exist
            if (!relationships.isEmpty()) {
                RelationshipProcessingResult relationshipResults = processRelationships(
                        line, relationships, onlyChild, onlyParent, contentType,
                        language, lineNumber, user);

                relationshipResults.messages().forEach(resultBuilder::addValidationMessage);

                if (relationshipResults.parentOnlyRelationships() != null) {
                    csvRelationshipRecordsParentOnly.putAll(
                            relationshipResults.parentOnlyRelationships());
                }
                if (relationshipResults.childOnlyRelationships() != null) {
                    csvRelationshipRecordsChildOnly.putAll(
                            relationshipResults.childOnlyRelationships());
                }
                if (relationshipResults.relationships() != null) {
                    csvRelationshipRecords.putAll(relationshipResults.relationships());
                }
            }

            // Search for existing contentlets
            ContentletSearchResult searchResult = searchExistingContentlets(
                    contentType, values, keyFields, siteAndFolder, urlValue,
                    urlValueAssetName, identifier, preview, sameKeyBatchInsert, isMultilingual,
                    language, lineNumber, choosenKeyField, user);
            isMultilingual = searchResult.isMultilingual();
            searchResult.messages().forEach(resultBuilder::addValidationMessage);

            var contentlets = searchResult.contentlets().stream()
                    .filter(Objects::nonNull)
                    .collect(Collectors.toList());
            List<String> updatedInodes = searchResult.updatedInodes();
            resultBuilder.setUpdatedInodes(updatedInodes);

            String conditionValues = searchResult.conditionValues();

            // Handle contentlet creation/update with proper language handling
            resultBuilder.setNewContent(false);
            if (contentlets.isEmpty()) {
                resultBuilder.incrementNewContent();
                resultBuilder.setNewContent(true);
                Contentlet newCont = new Contentlet();
                newCont.setStructureInode(contentType.getInode());
                newCont.setLanguageId(language);
                contentlets.add(newCont);
            } else {

                if (isMultilingual || UtilMethods.isSet(identifier)) {
                    List<Contentlet> multilingualContentlets = new ArrayList<>();

                    // Check for existing contentlet in this language
                    for (Contentlet contentlet : contentlets) {
                        if (contentlet.getLanguageId() == language) {
                            multilingualContentlets.add(contentlet);
                            existingMultilingualLanguage = contentlet.getLanguageId();
                        }
                    }

                    if (multilingualContentlets.isEmpty()) {
                        String lastIdentifier = "";
                        resultBuilder.setNewContent(true);
                        for (Contentlet contentlet : contentlets) {
                            if (!contentlet.getIdentifier().equals(lastIdentifier)) {
                                resultBuilder.incrementNewContent();
                                Contentlet newCont = new Contentlet();
                                newCont.setIdentifier(contentlet.getIdentifier());
                                newCont.setStructureInode(contentType.getInode());
                                newCont.setLanguageId(language);
                                multilingualContentlets.add(newCont);

                                existingMultilingualLanguage = contentlet.getLanguageId();
                                lastIdentifier = contentlet.getIdentifier();
                            }
                        }
                    }

                    contentlets = multilingualContentlets;
                }

                if (!searchResult.isNew()) {
                    if (conditionValues.equals("") || !keyContentUpdated.contains(conditionValues)
                            || isMultilingual) {
                        resultBuilder.incrementUpdatedContent(contentlets.size());
                        if (preview) {
                            keyContentUpdated.add(conditionValues);
                        }
                    }

                    Logger.debug(ImportUtil.class, "Contentlets size: " + contentlets.size());
                    if (contentlets.size() == 1) {
                        resultBuilder.addValidationMessage(ValidationMessage.builder()
                                .type(ValidationMessageType.WARNING)
                                .message(LanguageUtil.get(user,
                                        "The-key-fields-chosen-match-one-existing-content(s)")
                                        + " - "
                                        + LanguageUtil.get(user,
                                        "more-than-one-match-suggests-key(s)-are-not-properly-unique"))
                                .lineNumber(lineNumber)
                                .build());
                    } else if (contentlets.size() > 1) {
                        resultBuilder.addValidationMessage(ValidationMessage.builder()
                                .type(ValidationMessageType.WARNING)
                                .message(LanguageUtil.get(user,
                                        "The-key-fields-choosen-match-more-than-one-content-in-this-case")
                                        + ": "
                                        + " " + LanguageUtil.get(user, "matches") + ": "
                                        + contentlets.size() + " " + LanguageUtil.get(user,
                                        "different-content-s-looks-like-the-key-fields-choosen")
                                        + " " +
                                        LanguageUtil.get(user, "aren-t-a-real-key"))
                                .lineNumber(lineNumber)
                                .build());
                    }
                }
            }

            ProcessedContentResult processResult = processContent(
                    lineNumber,
                    contentlets,
                    searchResult.isNew(),
                    existingMultilingualLanguage,
                    values,
                    siteAndFolder,
                    wfActionId,
                    csvRelationshipRecordsParentOnly,
                    csvRelationshipRecordsChildOnly,
                    csvRelationshipRecords,
                    headers,
                    categories,
                    conditionValues,
                    keyContentUpdated,
                    contentTypePermissions,
                    wfActionIdIndex,
                    preview,
                    isMultilingual,
                    user,
                    request,
                    line
            );
            // Update builder with process results
            processResult.messages().forEach(resultBuilder::addValidationMessage);
            processResult.savedInodes().forEach(resultBuilder::addSavedInode);
            resultBuilder.setLastInode(processResult.lastInode());
            resultBuilder.incrementNewContent(processResult.newContentCount());
            resultBuilder.incrementUpdatedContent(processResult.updatedContentCount());
            resultBuilder.incrementDuplicateContent(processResult.duplicateContentCount());

            return resultBuilder.build();

        } catch (final Exception e) {
            Logger.error(ImportUtil.class,
                    String.format("An error occurred when importing line # %s: %s",
                            lineNumber, e.getMessage()), e);
            throw new DotRuntimeException(e.getMessage(), e);
        }
    }

    /**
     * Validates that a CSV line contains enough columns to match all defined headers. This method
     * ensures data integrity by checking that no column references will be out of bounds when
     * processing the line.
     *
     * @param line       The array of values from the CSV line
     * @param headers    Map of column indices to their corresponding field definitions
     * @param lineNumber The current line number in the CSV file, used for error reporting
     * @throws DotRuntimeException If the line contains fewer columns than required by the headers.
     *                             The exception includes the line number for error tracking.
     */
    private static void validateLineLength(final String[] line, final Map<Integer, Field> headers,
            final int lineNumber) {
        if (line.length < headers.keySet().stream().mapToInt(i -> i).max().orElse(0)) {
            throw new DotRuntimeException(
                    LINE_NO + lineNumber + " doesn't contain all the required columns.");
        }
    }

    /**
     * Process all fields in the line and return consolidated results
     */
    /**
     * Processes and validates all fields in a CSV line, handling field-specific validation and data
     * transformations. This method coordinates the processing of different field types including:
     * <ul>
     *   <li>Basic field validation</li>
     *   <li>Field value conversion</li>
     *   <li>Category field processing</li>
     *   <li>Location (Host/Folder) field handling</li>
     *   <li>URL field processing</li>
     *   <li>Unique field validation</li>
     * </ul>
     *
     * <p>The method builds a comprehensive result object containing all processed values,
     * validation messages, and special field data like categories and site/folder information.</p>
     *
     * @param line            Array of values from the CSV line being processed
     * @param headers         Map of column indices to their corresponding field definitions
     * @param contentType     Content type structure containing field definitions
     * @param user            User performing the import
     * @param currentHostId   Current host/site identifier
     * @param language        Language ID for the content
     * @param lineNumber      Current line number in CSV file for error reporting
     * @param choosenKeyField Buffer tracking chosen key fields
     * @return FieldProcessingResult containing processed values, validation messages, and
     * and additional field data
     * @throws DotDataException     If a data access error occurs during processing
     * @throws DotSecurityException If a security violation occurs during processing
     */
    private static FieldProcessingResult processFields(
            final String[] line,
            final Map<Integer, Field> headers,
            final Structure contentType,
            final User user,
            final String currentHostId,
            final long language,
            final int lineNumber,
            final StringBuffer choosenKeyField)
            throws DotDataException, DotSecurityException {

        final var results = new FieldProcessingResultBuilder(lineNumber);

        for (Integer column : headers.keySet()) {

            Field field = headers.get(column);
            String value = line[column];

            try {
                final var fieldResult = processField(field, value, user, currentHostId,
                        language, lineNumber, column, choosenKeyField);

                if (fieldResult.ignoreLine()) {
                    return fieldResult;
                }

                if (fieldResult.siteAndFolder().isPresent()) {
                    results.setSiteAndFolder(fieldResult.siteAndFolder().get());
                }

                if (fieldResult.urlValue().isPresent()) {
                    results.setUrlValue(fieldResult.urlValue().get());
                }

                fieldResult.categories().forEach(results::addCategory);
                fieldResult.values().forEach(results::addValue);
                fieldResult.uniqueFields().forEach(results::addUniqueField);
                fieldResult.messages().forEach(results::addValidationMessage);
                fieldResult.keyFields().forEach(results::addKeyField);

            } catch (Exception e) {
                results.addError(formatFieldError(field, value, e));
                results.setIgnoreLine(true);
                return results.build();
            }
        }

        // Check if the content type is HTMLPage and the URL field is set
        final Pair<Pair<Host, Folder>, String> pathAndAssetNameForURL =
                results.urlValue != null ?
                        checkURLFieldForHTMLPage(contentType,
                                results.urlValue.getRight(), results.siteAndFolder, user) :
                        null;
        if (pathAndAssetNameForURL != null) {
            final String assetNameForURL = pathAndAssetNameForURL.getRight();
            if (UtilMethods.isSet(assetNameForURL)) {
                results.addValue(results.urlValue.getLeft(), assetNameForURL);
                final Pair<Host, Folder> parentPathForURL = pathAndAssetNameForURL.getLeft();
                if (parentPathForURL == null) {
                    throw new DotRuntimeException(
                            LINE_NO + lineNumber + " contains errors. Column: '"
                                    + HTMLPageAssetAPI.URL_FIELD + "', value: '"
                                    + results.urlValue.getRight()
                                    + "', invalid parent folder for URL. Line will be ignored.");
                } else {
                    results.setSiteAndFolder(parentPathForURL);
                    results.setUrlValueAssetName(pathAndAssetNameForURL.getRight());
                }
            }
        }

        return results.build();
    }

    /**
     * Processes a single field from a CSV line, performing type-specific validation and data
     * conversion. This method handles different field types including:
     *
     * <ul>
     *   <li>Date fields (DATE, DATE_TIME, TIME)</li>
     *   <li>Category fields</li>
     *   <li>Selection fields (CHECKBOX, SELECT, MULTI_SELECT, RADIO)</li>
     *   <li>Text and Text Area fields</li>
     *   <li>Location (Host/Folder) fields</li>
     *   <li>Binary and File fields</li>
     *   <li>URL fields</li>
     * </ul>
     *
     * <p>For each field type, the method:
     * <ul>
     *   <li>Validates the input value</li>
     *   <li>Converts the value to the appropriate data type</li>
     *   <li>Performs any required data transformations</li>
     *   <li>Handles special cases like unique fields</li>
     * </ul>
     *
     * @param field           The field definition containing type and validation rules
     * @param value           The raw value from the CSV line
     * @param user            User performing the import
     * @param currentHostId   Current host/site identifier
     * @param language        Language ID for the content
     * @param lineNumber      Current line number in CSV file for error reporting
     * @param column          Column index in the CSV line
     * @param choosenKeyField Buffer tracking chosen key fields
     * @return FieldProcessingResult containing the processed value and any validation messages
     * @throws DotDataException     If a data access error occurs during processing
     * @throws DotSecurityException If a security violation occurs during processing
     * @throws Exception            If any other error occurs during field processing
     */
    private static FieldProcessingResult processField(
            final Field field,
            final String value,
            final User user,
            final String currentHostId,
            final long language,
            final int lineNumber,
            final int column,
            final StringBuffer choosenKeyField)
            throws DotDataException, DotSecurityException, Exception {

        final var results = new FieldProcessingResultBuilder(lineNumber);
        Object processedValue;

        if (isDateField(field)) {
            processedValue = validateDateTypes(lineNumber, field, value, value);
        } else if (isCategoryField(field)) {
            Set<Category> categories = processCategoryField(field, value, user, results);
            processedValue = categories;
            results.addCategories(categories);
        } else if (isSelectionField(field)) {
            processedValue = processSelectionField(field, value);
        } else if (isTextField(field)) {
            processedValue = processTextField(value);
        } else if (isTextAreaField(field)) {
            processedValue = value;
        } else if (isLocationField(field)) {
            Pair<Host, Folder> location = processLocationField(field, value, user, results);
            processedValue = value;
            results.setSiteAndFolder(location);
        } else if (isBinaryField(field)) {
            processedValue = processBinaryField(value, results);
        } else if (isFileField(field)) {
            processedValue = processFileField(field, value, currentHostId, user, results);
        } else {
            processedValue = processDefaultField(value);
        }

        Map<Integer, Object> values = new HashMap<>();
        values.put(column, processedValue);
        results.setValues(values);

        if (field.isUnique()) {
            handleUniqueField(field, processedValue, language, lineNumber, results);
        } else {
            updateChosenKeyField(field, choosenKeyField);
        }

        if (isUrlField(field, processedValue)) {
            results.setUrlValue(processUrlValue(column, processedValue));
        }

        return results.build();
    }

    /**
     * Updates the buffer tracking chosen key fields for content matching. This method maintains a
     * comma-separated list of field names that will be used as keys when matching imported content
     * with existing content.
     *
     * <p>The method ensures each field is only added once to the list by:
     * <ul>
     *   <li>Checking if the current field is already in the list</li>
     *   <li>Only appending new fields that aren't already tracked</li>
     *   <li>Maintaining proper comma separation between field names</li>
     * </ul>
     *
     * @param field           The field being considered as a key field
     * @param choosenKeyField StringBuffer containing the comma-separated list of key field names.
     *                        May be empty if no key fields have been chosen yet.
     */
    private static void updateChosenKeyField(final Field field,
            final StringBuffer choosenKeyField) {
        if (UtilMethods.isSet(choosenKeyField.toString())) {
            int count = 1;
            String[] chosenArr = choosenKeyField.toString().split(",");
            for (String chosen : chosenArr) {
                if (UtilMethods.isSet(chosen) && !field.getVelocityVarName()
                        .equals(chosen.trim())) {
                    count++;
                }
            }
            if (chosenArr.length == count) {
                choosenKeyField.append(", ").append(field.getVelocityVarName());
            }
        } else {
            choosenKeyField.append(", ").append(field.getVelocityVarName());
        }
    }

    /**
     * Determines whether a field is a date-related type. This method checks if the field is one of
     * the following types:
     * <ul>
     *   <li>DATE - For date only fields</li>
     *   <li>DATE_TIME - For fields containing both date and time</li>
     *   <li>TIME - For time only fields</li>
     * </ul>
     *
     * @param field The field to check
     * @return true if the field is any date-related type, false otherwise
     */
    private static boolean isDateField(final Field field) {
        return field.getFieldType().equals(Field.FieldType.DATE.toString()) ||
                field.getFieldType().equals(Field.FieldType.DATE_TIME.toString()) ||
                field.getFieldType().equals(Field.FieldType.TIME.toString());
    }

    /**
     * Checks if the given field is a category field.
     * <p>
     * This method determines if the field type is either CATEGORY or CATEGORIES_TAB.
     *
     * @param field the field to check
     * @return true if the field is a category field, false otherwise
     */
    private static boolean isCategoryField(final Field field) {
        return field.getFieldType().equals(Field.FieldType.CATEGORY.toString()) ||
                field.getFieldType().equals(Field.FieldType.CATEGORIES_TAB.toString());
    }

    /**
     * Checks if the given field is a selection field.
     * <p>
     * This method determines if the field type is one of the following: CHECKBOX, SELECT,
     * MULTI_SELECT, or RADIO.
     *
     * @param field the field to check
     * @return true if the field is a selection field, false otherwise
     */
    private static boolean isSelectionField(final Field field) {
        return field.getFieldType().equals(Field.FieldType.CHECKBOX.toString()) ||
                field.getFieldType().equals(Field.FieldType.SELECT.toString()) ||
                field.getFieldType().equals(Field.FieldType.MULTI_SELECT.toString()) ||
                field.getFieldType().equals(Field.FieldType.RADIO.toString());
    }

    /**
     * Checks if the given field is a text field.
     * <p>
     * This method determines if the field type is TEXT.
     *
     * @param field the field to check
     * @return true if the field is a text field, false otherwise
     */
    private static boolean isTextField(final Field field) {
        return field.getFieldType().equals(Field.FieldType.TEXT.toString());
    }

    /**
     * Checks if the given field is a text area field.
     * <p>
     * This method determines if the field type is either TEXT_AREA or WYSIWYG.
     *
     * @param field the field to check
     * @return true if the field is a text area field, false otherwise
     */
    private static boolean isTextAreaField(final Field field) {
        return field.getFieldType().equals(Field.FieldType.TEXT_AREA.toString())
                || field.getFieldType().equals(Field.FieldType.WYSIWYG.toString());
    }

    /**
     * Checks if the given field is a location field.
     * <p>
     * This method determines if the field type is HOST_OR_FOLDER.
     *
     * @param field the field to check
     * @return true if the field is a location field, false otherwise
     */
    private static boolean isLocationField(final Field field) {
        return field.getFieldType().equals(Field.FieldType.HOST_OR_FOLDER.toString());
    }

    /**
     * Checks if the given field is a binary field.
     * <p>
     * This method determines if the field type is BinaryField.
     *
     * @param field the field to check
     * @return true if the field is a binary field, false otherwise
     */
    private static boolean isBinaryField(final Field field) {
        return new LegacyFieldTransformer(field).from().typeName()
                .equals(BinaryField.class.getName());
    }

    /**
     * Checks if the given field is a file field.
     * <p>
     * This method determines if the field type is either IMAGE or FILE.
     *
     * @param field the field to check
     * @return true if the field is a file field, false otherwise
     */
    private static boolean isFileField(final Field field) {
        return field.getFieldType().equals(Field.FieldType.IMAGE.toString()) ||
                field.getFieldType().equals(Field.FieldType.FILE.toString());
    }

    /**
     * Checks if the given field is a URL field.
     * <p>
     * This method determines if the field's velocity variable name is equal to the URL field of
     * HTMLPageAssetAPI.
     *
     * @param field the field to check
     * @param value the value to check
     * @return true if the field is a URL field, false otherwise
     */
    private static boolean isUrlField(final Field field, final Object value) {
        return value != null && field.getVelocityVarName().equals(HTMLPageAssetAPI.URL_FIELD);
    }

    /**
     * Processes a category field from a CSV line.
     * <p>
     * This method validates and processes the category field, converting the comma-separated
     * category keys into a set of Category objects. If any category key is invalid, an error is
     * added to the resultBuilder and a DotRuntimeException is thrown.
     *
     * @param field         the field definition containing type and validation rules
     * @param value         the raw value from the CSV line
     * @param user          the user performing the import
     * @param resultBuilder the builder to accumulate validation messages and results
     * @return a set of Category objects corresponding to the valid category keys
     * @throws DotDataException     if a data access error occurs during processing
     * @throws DotSecurityException if a security violation occurs during processing
     */
    private static Set<Category> processCategoryField(final Field field, final String value,
            final User user, final FieldProcessingResultBuilder resultBuilder
    ) throws DotDataException, DotSecurityException {
        Set<Category> categories = new HashSet<>();
        if (UtilMethods.isSet(value)) {
            String[] categoryKeys = value.split(",");
            for (String catKey : categoryKeys) {
                Category cat = catAPI.findByKey(catKey.trim(), user, false);
                if (cat == null) {
                    resultBuilder.addError(
                            String.format("Column: '%s', value: '%s', invalid category key found",
                                    field.getVelocityVarName(), value));
                    resultBuilder.setIgnoreLine(true);
                    throw new DotRuntimeException("Invalid category key: " + catKey);
                }
                categories.add(cat);
                resultBuilder.addCategory(cat);
            }
        }
        return categories;
    }

    /**
     * Processes a text field by truncating its value to 255 characters if necessary.
     *
     * @param value the value to process
     * @return the processed value, truncated to 255 characters if necessary
     */
    private static Object processTextField(final String value) {
        if (value != null && value.length() > 255) {
            return value.substring(0, 255);
        }
        return value;
    }

    /**
     * Processes a location field by validating and converting the value to a Pair of Host and
     * Folder.
     *
     * @param field         the field definition containing type and validation rules
     * @param value         the raw value from the CSV line
     * @param user          the user performing the import
     * @param resultBuilder the builder to accumulate validation messages and results
     * @return a Pair of Host and Folder corresponding to the valid location
     * @throws DotDataException     if a data access error occurs during processing
     * @throws DotSecurityException if a security violation occurs during processing
     */
    private static Pair<Host, Folder> processLocationField(final Field field, final String value,
            final User user, final FieldProcessingResultBuilder resultBuilder
    ) throws DotDataException, DotSecurityException {
        Pair<Host, Folder> siteAndFolder = getSiteAndFolderFromIdOrName(value, user);
        if (siteAndFolder == null) {
            resultBuilder.addError(
                    String.format("Column: '%s', value: '%s', invalid site/folder inode found",
                            field.getVelocityVarName(), value));
            resultBuilder.setIgnoreLine(true);
            throw new DotRuntimeException("Invalid site/folder: " + value);
        }
        return siteAndFolder;
    }

    /**
     * Processes a binary field by validating the URL.
     *
     * @param value         the value to process
     * @param resultBuilder the builder to accumulate validation messages and results
     * @return the processed value if the URL is valid
     * @throws DotRuntimeException if the URL is invalid
     */
    private static Object processBinaryField(final String value,
            final FieldProcessingResultBuilder resultBuilder) {
        if (UtilMethods.isSet(value) && !APILocator.getTempFileAPI().validUrl(value)) {
            resultBuilder.addError("URL is malformed or Response is not 200");
            resultBuilder.setIgnoreLine(true);
            throw new DotRuntimeException("Invalid binary URL: " + value);
        }
        return value;
    }

    /**
     * Processes a file field by validating and converting the value to a contentlet identifier.
     *
     * @param field         the field definition containing type and validation rules
     * @param value         the raw value from the CSV line
     * @param currentHostId the ID of the current host
     * @param user          the user performing the import
     * @param resultBuilder the builder to accumulate validation messages and results
     * @return the contentlet identifier if the file is valid, null otherwise
     * @throws DotDataException     if a data access error occurs during processing
     * @throws DotSecurityException if a security violation occurs during processing
     */
    private static Object processFileField(final Field field, final String value,
            final String currentHostId, final User user,
            final FieldProcessingResultBuilder resultBuilder
    ) throws DotDataException, DotSecurityException {
        String filePath = value;
        if (Field.FieldType.IMAGE.toString().equals(field.getFieldType()) && !UtilMethods.isImage(
                filePath)) {
            if (UtilMethods.isSet(filePath)) {
                resultBuilder.addWarning(String.format("The file is not an image for field: %s",
                        field.getVelocityVarName()));
            }
            return null;
        }

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
        if (id != null && InodeUtils.isSet(id.getId()) && id.getAssetType().equals("contentlet")) {
            Contentlet cont = APILocator.getContentletAPI()
                    .findContentletByIdentifier(id.getId(), true,
                            APILocator.getLanguageAPI().getDefaultLanguage().getId(),
                            user, false);
            if (cont != null && InodeUtils.isSet(cont.getInode())) {
                return cont.getIdentifier();
            }
            resultBuilder.addWarning(String.format("The file has not been found in %s:%s",
                    fileHost.getHostname(), filePath));
        }
        return null;
    }

    /**
     * Processes a default field by escaping HTML text if necessary.
     *
     * @param value the value to process
     * @return the processed value, with HTML text escaped if necessary
     */
    private static Object processDefaultField(final String value) {
        return Config.getBooleanProperty("CONTENT_ESCAPE_HTML_TEXT", true) ?
                UtilMethods.escapeUnicodeCharsForHTML(value) : value;
    }

    /**
     * Handles a unique field by adding it to the result builder.
     *
     * @param field         the field definition containing type and validation rules
     * @param value         the value to process
     * @param language      the language ID for the content
     * @param lineNumber    the line number in the CSV file
     * @param resultBuilder the builder to accumulate validation messages and results
     */
    private static void handleUniqueField(final Field field, final Object value,
            final long language,
            final int lineNumber, final FieldProcessingResultBuilder resultBuilder) {
        final var bean = UniqueFieldBean.builder().
                field(field).
                value(value).
                lineNumber(lineNumber).
                languageId(language).build();
        resultBuilder.addUniqueField(bean);
        resultBuilder.addKeyField(
                field.getVelocityVarName(), value != null ? value.toString() : null
        );
    }

    /**
     * Processes and normalizes a URL value from a CSV column to ensure consistent formatting.
     * This method:
     * <ul>
     *   <li>Strips trailing forward slashes from the URL</li>
     *   <li>Ensures the URL starts with a forward slash</li>
     *   <li>Preserves the original column index for reference</li>
     * </ul>
     *
     * @param column The CSV column index where the URL was found (0-based)
     * @param value  The URL value to process (will be converted via toString())
     * @return A {@link Pair} containing:
     *         - Left: Original column index
     *         - Right: Normalized URL path starting with '/'
     * @example
     * {@code processUrlValue(3, "about/us/") → Pair(3, "/about/us")}
     */
    private static Pair<Integer, String> processUrlValue(final int column, final Object value) {
        final String uri = StringUtils.stripEnd(
                StringUtils.strip(value.toString()), StringPool.FORWARD_SLASH);
        final StringBuilder uriBuilder = new StringBuilder();
        if (!uri.startsWith(StringPool.FORWARD_SLASH)) {
            uriBuilder.append(StringPool.FORWARD_SLASH);
        }
        uriBuilder.append(uri);
        return Pair.of(column, uriBuilder.toString());
    }

    /**
     * Formats an error message for a field.
     *
     * @param field the field definition containing type and validation rules
     * @param value the value that caused the error
     * @param e the exception that was thrown
     * @return the formatted error message
     */
    private static String formatFieldError(final Field field, final String value,
            final Exception e) {

        var exceptionMessage = e.getMessage();
        if (!UtilMethods.isSet(exceptionMessage)) {
            exceptionMessage = e.toString();
        }

        return String.format("Column: '%s', value: '%s', %s",
                field.getVelocityVarName(), value, exceptionMessage);
    }

    /**
     * Processes and validates a selection field value against predefined field entries.
     * This method handles field types: CHECKBOX, SELECT, MULTI_SELECT, and RADIO.
     *
     * <p>Matches the input value against field entries in "label|value" format. Returns the original
     * value if matched, or attempts boolean conversion for checkbox fields when no direct match exists.</p>
     *
     * @param field The {@link Field} definition containing selection options and configuration
     * @param value The raw input value from the CSV line to be processed
     * @return The validated value if matched with field entries, a boolean for checkbox values,
     *         or {@code null} if no match found and not a boolean convertible value
     *
     * @implSpec Field entries are split by newline with last segment as comparison value
     * @implNote For checkbox fields, returns Boolean.TRUE if value contains "true", "yes" or "1",
     *           Boolean.FALSE otherwise. Other field types return null for unmatched values
     */
    private static Object processSelectionField(final Field field, final String value) {
        if (UtilMethods.isSet(value)) {
            String fieldEntriesString = field.getValues() != null ? field.getValues() : "";
            String[] fieldEntries = fieldEntriesString.split("\n");

            for (String fieldEntry : fieldEntries) {
                String[] splittedValue = fieldEntry.split("\\|");
                String entryValue = splittedValue[splittedValue.length - 1].trim();

                if (entryValue.equals(value) || value.contains(entryValue)) {
                    return value;
                }
            }
            return BooleanUtils.toBoolean(value);
        }
        return null;
    }

    /**
     * Processes relationship data from a CSV line by resolving related contentlets based on
     * queries. This method handles different types of relationships (parent-only, child-only, or
     * bidirectional) and retrieves the associated contentlets using the provided queries. It
     * captures validation messages for any issues encountered during processing.
     *
     * @param line          The CSV line data containing relationship queries for each column.
     * @param relationships Map of column indices to their corresponding {@link Relationship}
     *                      definitions.
     * @param onlyChild     Map indicating if a column should only contain child relationships.
     * @param onlyParent    Map indicating if a column should only contain parent relationships.
     * @param contentType   The {@link Structure} (Content Type) of the content being imported.
     * @param language      Language ID for resolving multilingual content.
     * @param lineNumber    Current line number in the CSV for error reporting.
     * @param user          The {@link User} performing the import.
     * @return A {@link RelationshipProcessingResult} containing resolved relationships and
     * validation messages.
     * @throws LanguageException    If language resource lookups fail.
     * @throws DotDataException     If data access errors occur.
     * @throws DotSecurityException If security permissions are violated during content resolution.
     */
    private static RelationshipProcessingResult processRelationships(
            final String[] line,
            final Map<Integer, Relationship> relationships,
            final Map<Integer, Boolean> onlyChild,
            final Map<Integer, Boolean> onlyParent,
            final Structure contentType,
            final long language,
            final int lineNumber,
            final User user
    ) throws LanguageException, DotDataException, DotSecurityException {

        RelationshipProcessingBuilder builder = new RelationshipProcessingBuilder(lineNumber);

        for (Integer column : relationships.keySet()) {
            final Relationship relationship = relationships.get(column);
            final String relatedQuery = line[column];

            try {
                if (relatedQuery != null && !relatedQuery.trim().isEmpty()) {
                    List<Contentlet> relatedContentlets = RelationshipUtil
                            .getRelatedContentFromQuery(relationship,
                                    new StructureTransformer(contentType).from(), language,
                                    relatedQuery, user);

                    //If no error add the relatedContentlets
                    if (onlyChild.get(column)) {
                        builder.addChildOnlyRelationship(relationship, relatedContentlets);
                    } else if (onlyParent.get(column)) {
                        builder.addParentOnlyRelationship(relationship, relatedContentlets);
                    } else {
                        builder.addRelationship(relationship, relatedContentlets);
                    }
                } else {
                    // Empty relationship field - add empty list to clear any existing relationships
                    builder.addRelationship(relationship, List.of());
                }
            } catch (DotDataValidationException e) {
                if (null != relationship) {
                    Logger.warn(ImportUtil.class,
                            String.format("A validation error occurred with Relationship " +
                                            "'%s'[%s]: e.getMessage()", relationship.getRelationTypeValue(),
                                    relationship.getInode(), e
                                            .getMessage()), e);
                } else {
                    Logger.warn(ImportUtil.class,
                            String.format("A null relationship in column '%s' was found",
                                    column));
                }
                String structureDoesNoMatchMessage = LanguageUtil.get(user,
                        "the-structure-does-not-match-the-relationship");
                builder.addWarning(structureDoesNoMatchMessage);
            }
        }

        return builder.build();
    }

    /**
     * Searches for existing contentlets that match criteria from CSV import data.
     * Implements multiple search strategies:
     * <ol>
     *   <li><strong>Identifier lookup:</strong> Directly fetches content by identifier when provided</li>
     *   <li><strong>URL matching:</strong> Special handling for HTMLPage content types using URL field</li>
     *   <li><strong>Key field query:</strong> Constructs ElasticSearch queries using designated key fields</li>
     * </ol>
     *
     * <p>Handles complex scenarios including:
     * <ul>
     *   <li>Multilingual content detection across language variants</li>
     *   <li>Batch imports with duplicate keys in same file</li>
     *   <li>Site/folder context resolution for path-based content</li>
     *   <li>Date/time field value normalization</li>
     * </ul>
     *
     * @param contentType          Content type structure being imported
     * @param values               Map of column indexes to processed field values
     * @param keyFields            Key fields used for content matching (column index → Field)
     * @param siteAndFolder        Host/folder location context from path fields
     * @param urlValue             URL field value with column index (for HTMLPage assets)
     * @param urlValueAssetName    Processed asset name from URL field
     * @param identifier           Optional existing content identifier
     * @param preview              True for validation-only preview mode
     * @param sameKeyBatchInsert   True when multiple rows share same keys (multilingual batch)
     * @param isMultilingual       True if import contains multiple languages
     * @param language             Content language ID
     * @param lineNumber           CSV line number for error reporting
     * @param choosenKeyField      Buffer tracking user-selected key fields
     * @param user                 Authenticated user for permission checks
     *
     * @return {@link ContentletSearchResult} containing:
     *         - List of matching contentlets (empty if new content)
     *         - Updated inodes for existing content
     *         - Multilingual status flag
     *         - Search condition values used
     *
     * @throws DotDataException    If search index queries fail
     * @throws DotSecurityException If user lacks permission to access content
     *
     * @example // Search by identifier
     * searchExistingContentlets(contentType, values, keyFields, site,
     *   Pair.of(3, "/contact"), "contact-page", "1234-5678", ...)
     *
     * @see ContentletAPI#findContentletByIdentifier(String, boolean, long, User, boolean)
     * @see ESUtils#sha256(String, String, long)
     */
    private static ContentletSearchResult searchExistingContentlets(
            final Structure contentType,
            final HashMap<Integer, Object> values,
            final Map<Integer, Field> keyFields,
            final Pair<Host, Folder> siteAndFolder,
            final Pair<Integer, String> urlValue,
            final String urlValueAssetName,
            final String identifier,
            final boolean preview,
            final boolean sameKeyBatchInsert,
            final boolean isMultilingual,
            final long language,
            final int lineNumber,
            final StringBuffer choosenKeyField,
            final User user
    ) throws DotDataException, DotSecurityException {

        final var builder = ContentletSearchResult.builder();
        final List<Contentlet> contentlets = new ArrayList<>();
        final List<String> updatedInodes = new ArrayList<>();
        String conditionValues = "";
        boolean isMultilingualResult = isMultilingual;

        if (UtilMethods.isSet(identifier)) {
            contentlets.addAll(
                    searchByIdentifier(identifier, contentType, user, lineNumber, builder));
        } else if (urlValue != null && keyFields.isEmpty()) {
            // For HTMLPageAsset, we need to search by URL to math existing pages
            contentlets.addAll(
                    searchByUrl(contentType, urlValue, siteAndFolder, language, user, lineNumber,
                            builder));
        } else if (!keyFields.isEmpty()) {
            // Search by key fields
            SearchByKeyFieldsResult keyFieldResults = searchByKeyFields(
                    contentType, values, keyFields, siteAndFolder, urlValueAssetName, preview,
                    sameKeyBatchInsert, language, isMultilingual, user, lineNumber, choosenKeyField,
                    builder);
            contentlets.addAll(keyFieldResults.contentlets);
            conditionValues = keyFieldResults.conditionValues;
            isMultilingualResult = keyFieldResults.isMultilingual;
            updatedInodes.addAll(keyFieldResults.updatedInodes);
        }

        return builder
                .contentlets(contentlets)
                .updatedInodes(updatedInodes)
                .isNew(contentlets.isEmpty())
                .isMultilingual(isMultilingualResult)
                .conditionValues(conditionValues)
                .build();
    }

    /**
     * Searches for contentlets by their identifier.
     *
     * @param identifier  The unique identifier of the contentlet to search for.
     * @param contentType The structure type of the contentlet.
     * @param user        The user performing the search, used for security validation.
     * @param lineNumber  The line number in the source code where this method is called, used for
     *                    error messaging.
     * @param builder     A builder object used to accumulate search results and messages.
     * @return A list of contentlets that match the given identifier. Returns an empty list if no
     * contentlet is found.
     * @throws DotDataException     If there is a data-related exception during the search process.
     * @throws DotSecurityException If the user does not have permission to access the requested
     *                              contentlet.
     */
    private static List<Contentlet> searchByIdentifier(
            final String identifier,
            final Structure contentType,
            final User user,
            final int lineNumber,
            final ContentletSearchResult.Builder builder
    ) throws DotDataException, DotSecurityException {

        StringBuffer query = new StringBuffer()
                .append("+structureName:").append(contentType.getVelocityVarName())
                .append(" +working:true +deleted:false")
                .append(" +identifier:").append(identifier);

        List<ContentletSearch> contentsSearch = conAPI.searchIndex(query.toString(), 0, -1, null,
                user, true);

        if (contentsSearch == null || contentsSearch.isEmpty()) {
            builder.addMessages(ValidationMessage.builder()
                    .type(ValidationMessageType.ERROR)
                    .message("Content not found with identifier " + identifier)
                    .lineNumber(lineNumber)
                    .build());
            return Collections.emptyList();
        }

        return convertSearchResults(contentsSearch, lineNumber, user, builder);
    }

    /**
     * Searches for contentlets by their key fields.
     *
     * @param contentType        The structure type of the contentlet to search for.
     * @param values             A map containing values for the key fields, with field IDs as
     *                           keys.
     * @param keyFields          A map containing the key fields with their respective column
     *                           indices.
     * @param siteAndFolder      A pair representing the host and folder where the contentlet is
     *                           located.
     * @param urlValueAssetName  The URL value of the asset name.
     * @param preview            A flag indicating whether the search should consider preview mode.
     * @param sameKeyBatchInsert A flag indicating if this is part of a batch insert with the same
     *                           key.
     * @param language           The language ID for which to search contentlets.
     * @param isMultilingual     A flag indicating if the contentlet is multilingual.
     * @param user               The user performing the search, used for security validation.
     * @param lineNumber         The line number in the source code where this method is called,
     *                           used for error messaging.
     * @param choosenKeyField    A string buffer to store the chosen key field.
     * @param builder            A builder object used to accumulate search results and messages.
     * @return A container holding the search results including contentlets, updated inodes,
     * condition values, and multilingual status.
     * @throws DotDataException     If there is a data-related exception during the search process.
     * @throws DotSecurityException If the user does not have permission to access the requested
     *                              contentlet.
     */
    private static SearchByKeyFieldsResult searchByKeyFields(
            final Structure contentType,
            final HashMap<Integer, Object> values,
            final Map<Integer, Field> keyFields,
            final Pair<Host, Folder> siteAndFolder,
            final String urlValueAssetName,
            final boolean preview,
            final boolean sameKeyBatchInsert,
            final long language,
            final boolean isMultilingual,
            final User user,
            final int lineNumber,
            final StringBuffer choosenKeyField,
            final ContentletSearchResult.Builder builder
    ) throws DotDataException, DotSecurityException {

        StringBuffer query = new StringBuffer()
                .append("+structureName:").append(contentType.getVelocityVarName())
                .append(" +working:true +deleted:false");

        StringBuilder conditionValues = new StringBuilder();
        boolean appendSiteToQuery = false;
        String siteFieldValue = null;
        boolean isMultilingualResult = isMultilingual;

        // Build query from key fields
        for (Integer column : keyFields.keySet()) {
            Field field = keyFields.get(column);
            Object value = values.get(column);

            if (!UtilMethods.isSet(value)) {
                builder.addMessages(ValidationMessage.builder()
                        .type(ValidationMessageType.ERROR)
                        .message("Key field " + field.getVelocityVarName() + " is required")
                        .lineNumber(lineNumber)
                        .build());
                throw new DotRuntimeException(LINE_NO + lineNumber + " key field " +
                        field.getVelocityVarName()
                        + " is required since it was defined as a key\n");
            }

            String processedValue = value.toString();
            if (value instanceof Date || value instanceof Timestamp) {
                SimpleDateFormat formatter;
                if (field.getFieldType().equals(Field.FieldType.DATE.toString())) {
                    processedValue = DATE_FIELD_FORMAT.format((Date) value);
                } else if (field.getFieldType().equals(Field.FieldType.DATE_TIME.toString())) {
                    DateFormat df = new SimpleDateFormat("MM/dd/yyyy");
                    processedValue = df.format((Date) value);
                } else if (field.getFieldType().equals(Field.FieldType.TIME.toString())) {
                    DateFormat df = new SimpleDateFormat("HHmmss");
                    processedValue = df.format((Date) value);
                } else {
                    formatter = new SimpleDateFormat();
                    processedValue = formatter.format(value);
                    builder.addMessages(ValidationMessage.builder()
                            .type(ValidationMessageType.WARNING)
                            .message("The date format for field " + field.getVelocityVarName()
                                    + " is undetermined")
                            .lineNumber(lineNumber)
                            .build());
                    Logger.warn(ImportUtil.class,
                            "importLine: field's date format is undetermined.");
                }
            }

            if (field.getVelocityVarName().equals(HTMLPageAssetAPI.URL_FIELD)
                    && urlValueAssetName != null) {
                appendSiteToQuery = true;
                query.append(" +").append(contentType.getVelocityVarName())
                        .append(StringPool.PERIOD)
                        .append(HTMLPageAssetAPI.URL_FIELD).append(StringPool.COLON)
                        .append(urlValueAssetName);
                processedValue = getURLFromFolderAndAssetName(siteAndFolder, urlValueAssetName);
            } else if (new LegacyFieldTransformer(field).from() instanceof HostFolderField) {
                appendSiteToQuery = true;
                siteFieldValue = processedValue;
            } else {
                query.append(" +").append(contentType.getVelocityVarName())
                        .append(StringPool.PERIOD)
                        .append(field.getVelocityVarName())
                        .append(field.isUnique() ? ESUtils.SHA_256 : "_dotraw")
                        .append(StringPool.COLON)
                        .append(field.isUnique() ? ESUtils.sha256(contentType.getVelocityVarName()
                                        + StringPool.PERIOD + field.getVelocityVarName(), processedValue,
                                language)
                                : escapeLuceneSpecialCharacter(processedValue).contains(" ") ? "\""
                                        + escapeLuceneSpecialCharacter(processedValue) + "\""
                                        : escapeLuceneSpecialCharacter(processedValue));
            }

            conditionValues.append(processedValue).append("-");

            if (!field.isUnique()) {
                updateChosenKeyField(field, choosenKeyField);
            }
        }

        // Add site/folder constraints if needed
        if (appendSiteToQuery) {
            query.append(addSiteAndFolderToESQuery(siteAndFolder, siteFieldValue));
        }

        // Add language constraint unless multilingual
        String noLanguageQuery = query.toString();
        if (!isMultilingualResult) {
            query.append(" +languageId:").append(language);
        }

        List<ContentletSearch> contentsSearch = conAPI.searchIndex(query.toString(), 0,
                -1, null, user, true);

        /*
        We need to handle the case when keys are used, we could have a contentlet already saved
        with the same keys but different language so the above query is not going to find it.
         */
        if (contentsSearch == null || contentsSearch.isEmpty()) {
            if (choosenKeyField.length() > 1) {
                // Try without language constraint for multilingual content
                contentsSearch = conAPI.searchIndex(noLanguageQuery, 0, -1, null, user, true);
                if (contentsSearch != null && !contentsSearch.isEmpty()) {
                    isMultilingualResult = true;
                }
            }
        }

        List<Contentlet> contentlets = new ArrayList<>();
        List<String> updatedInodes = new ArrayList<>();

        if (contentsSearch != null && !contentsSearch.isEmpty()) {
            for (ContentletSearch contentSearch : contentsSearch) {

                Contentlet contentlet = conAPI.find(contentSearch.getInode(), user, true);
                if (contentlet != null && InodeUtils.isSet(contentlet.getInode())) {

                    boolean columnExists = false;
                    for (Integer column : keyFields.keySet()) {
                        Field field = keyFields.get(column);
                        Object value = values.get(column);
                        Object conValue = conAPI.getFieldValue(contentlet, field);

                        if (isDateField(field)) {
                            columnExists = compareDateValues(field, value, conValue);
                        } else {
                            if (field.getVelocityVarName().equals(HTMLPageAssetAPI.URL_FIELD)
                                    && urlValueAssetName != null) {
                                value = getURLFromFolderAndAssetName(siteAndFolder,
                                        urlValueAssetName);
                                conValue = getURLFromContentId(contentlet.getIdentifier());
                            }
                            if (conValue.toString().equalsIgnoreCase(value.toString())) {
                                columnExists = true;
                            } else {
                                columnExists = false;
                                break;
                            }
                        }
                    }

                    if (columnExists) {
                        contentlets.add(contentlet);
                        //Keep a register of all contentlets to be updated
                        updatedInodes.add(contentlet.getInode());
                    }
                }
            }
        }

        if (!preview) {//Don't do unnecessary calls if it is not required

            /*
           We must use an alternative search for cases when we are using the same key for batch uploads,
           for example if we have multilingual inserts for new records, the search above (searchIndex)
           can manage multilingual inserts for already stored records but not for the case when the new record and its multilingual records
           came in the same import file. They are new, we will not find them in the index.
             */
            if (sameKeyBatchInsert && contentlets.isEmpty()) {

                //Searching for all the contentlets of this structure
                List<Contentlet> foundContentlets = conAPI.findByStructure(contentType, user, true,
                        0, -1);

                for (Contentlet contentlet : foundContentlets) {

                    boolean match = true;
                    for (Integer column : keyFields.keySet()) {

                        //Getting key values
                        Field field = keyFields.get(column);
                        Object value = values.get(column);

                        //Ok, comparing our keys with the contentlets we found trying to see if there is a contentlet to update with the specified keys
                        Object conValue = conAPI.getFieldValue(contentlet, field);
                        if (field.getVelocityVarName().equals(HTMLPageAssetAPI.URL_FIELD)
                                && urlValueAssetName != null) {
                            value = getURLFromFolderAndAssetName(siteAndFolder, urlValueAssetName);
                            conValue = getURLFromContentId(contentlet.getIdentifier());
                        }
                        if (!conValue.equals(value)) {
                            match = false;
                        }
                    }

                    //Ok, we found our record
                    if (match) {
                        contentlets.add(contentlet);
                        isMultilingualResult = true;
                        break;
                    }
                }
            }
        }

        return new SearchByKeyFieldsResult(contentlets, updatedInodes,
                conditionValues.toString(), isMultilingualResult);
    }

    /**
     * Searches for contentlets by their URL.
     *
     * @param contentType   The structure type of the contentlet to search for.
     * @param urlValue      A pair containing the language ID and the URL value.
     * @param siteAndFolder A pair containing the host and folder where the contentlet is located.
     * @param language      The language ID for which to search contentlets.
     * @param user          The user performing the search, used for security validation.
     * @param lineNumber    The line number in the source code where this method is called, used for
     *                      error messaging.
     * @param builder       A builder object used to accumulate search results and messages.
     * @return A list of contentlets that match the given URL.
     * @throws DotDataException     If there is a data-related exception during the search process.
     * @throws DotSecurityException If the user does not have permission to access the requested
     *                              contentlet.
     */
    private static List<Contentlet> searchByUrl(
            final Structure contentType,
            final Pair<Integer, String> urlValue,
            final Pair<Host, Folder> siteAndFolder,
            final long language,
            final User user,
            final int lineNumber,
            final ContentletSearchResult.Builder builder
    ) throws DotDataException, DotSecurityException {

        StringBuffer query = new StringBuffer()
                .append("+structureName:").append(contentType.getVelocityVarName())
                .append(" +working:true +deleted:false")
                .append(" +languageId:").append(language)
                .append(addSiteAndFolderToESQuery(siteAndFolder, null));

        query.append(" +").append(contentType.getVelocityVarName()).append(StringPool.PERIOD)
                .append(HTMLPageAssetAPI.URL_FIELD).append(StringPool.COLON)
                .append(urlValue.getRight());

        List<ContentletSearch> contentsSearch = conAPI.searchIndex(
                query.toString(), 0, -1, null, user, true);

        if (contentsSearch == null || contentsSearch.isEmpty()) {
            builder.addMessages(ValidationMessage.builder()
                    .type(ValidationMessageType.WARNING)
                    .message("Content not found with URL " + urlValue.getRight())
                    .lineNumber(lineNumber)
                    .build());
            return Collections.emptyList();
        }

        return convertSearchResults(contentsSearch, lineNumber, user, builder);
    }

    /**
     * Compares two date values based on the field type.
     *
     * @param field    The field object containing the field type information.
     * @param value    The first date value to compare.
     * @param conValue The second date value to compare.
     * @return True if the date values are equal, false otherwise.
     */
    private static boolean compareDateValues(final Field field, final Object value,
            final Object conValue) {

        Object result = value;

        if (field.getFieldType().equals(Field.FieldType.TIME.toString())) {
            DateFormat df = new SimpleDateFormat("HHmmss");
            String conValueStr = df.format((Date) conValue);
            String valueStr = df.format((Date) result);
            return conValueStr.equals(valueStr);
        } else if (field.getFieldType().equals(Field.FieldType.DATE.toString())) {
            String valueStr = DATE_FIELD_FORMAT.format((Date) result);
            String conValueStr = DATE_FIELD_FORMAT.format((Date) conValue);
            return conValueStr.equals(valueStr);
        } else {
            if (conValue instanceof java.sql.Timestamp) {
                result = new java.sql.Timestamp(((Date) result).getTime());
            } else if (conValue instanceof Date) {
                DateFormat df = new SimpleDateFormat("MM/dd/yyyy");
                result = df.format((Date) result);
            }
            return conValue.equals(result);
        }
    }

    /**
     * Converts search results from {@link ContentletSearch} objects to {@link Contentlet} objects.
     *
     * @param contentsSearch The list of {@link ContentletSearch} objects returned by the search.
     * @param lineNumber     The line number in the source code where this method is called, used
     *                       for error messaging.
     * @param user           The user performing the search, used for security validation.
     * @param builder        A builder object used to accumulate search results and messages.
     * @return A list of {@link Contentlet} objects that match the search criteria.
     */
    private static List<Contentlet> convertSearchResults(final List<ContentletSearch> contentsSearch,
            final int lineNumber, final User user, final ContentletSearchResult.Builder builder) {

        if (contentsSearch == null || contentsSearch.isEmpty()) {
            return Collections.emptyList();
        }

        return contentsSearch.stream()
                .map(contentSearch -> {
                    try {
                        return conAPI.find(contentSearch.getInode(), user, true);
                    } catch (Exception e) {
                        Logger.warn(ImportUtil.class, "Error finding content by inode", e);

                        builder.addMessages(ValidationMessage.builder()
                                .type(ValidationMessageType.ERROR)
                                .message("Content not found with identifier "
                                        + contentSearch.getIdentifier())
                                .lineNumber(lineNumber)
                                .build());
                        return null;
                    }
                })
                .filter(Objects::nonNull)
                .filter(contentlet -> InodeUtils.isSet(contentlet.getInode()))
                .collect(Collectors.toList());
    }

    /**
     * Retrieves the identifier from the search results.
     *
     * @param results The map containing the search results.
     * @param line    The current line of CSV data being processed.
     * @return The identifier as a string, or null if not found.
     */
    private static String getIdentifierFromResults(final Map<String, List<String>> results,
            final String[] line) {
        try {
            int identifierFieldIndex = Integer.parseInt(results.get("identifiers").get(0));
            return identifierFieldIndex >= 0 ? line[identifierFieldIndex] : null;
        } catch (Exception e) {
            Logger.debug(ImportUtil.class, "No identifier field found", e);
            return null;
        }
    }

    /**
     * Retrieves the column index containing workflow action IDs from CSV import results.
     * Looks for the {@link Contentlet#WORKFLOW_ACTION_KEY} entry in the results map populated
     * during header processing.
     *
     * @param results Map containing CSV import metadata with:
     *                - Key: {@link Contentlet#WORKFLOW_ACTION_KEY}
     *                - Value: Single-element list containing column index as String
     *
     * @return Column index (0-based) of workflow action IDs in CSV, or -1 if:
     *         - No workflow action column exists
     *         - Invalid index format
     *         - Key not found in results
     *
     * @example // For CSV header: "Title,WorkflowAction,Content"
     *          // Returns 1 (second column)
     * @see ImportUtil#importFile() Header processing stores workflow action column index
     */
    private static int getWorkflowActionIdIndexFromResults(final Map<String, List<String>> results) {

        int wfActionIdIndex = -1;
        try {
            List<String> workflowActionKey = results.get(Contentlet.WORKFLOW_ACTION_KEY);
            if (UtilMethods.isSet(workflowActionKey)) {
                wfActionIdIndex = Integer.parseInt(
                        results.get(Contentlet.WORKFLOW_ACTION_KEY).get(0));
            }
        } catch (Exception e) {
            Logger.warn(ImportUtil.class, e.getMessage());
        }

        return wfActionIdIndex;
    }

    /**
     * Processes contentlets from a file and handles associated operations such as relationships,
     * validation, and workflow actions. This method processes each contentlet individually,
     * validates its data, and manages its relationships with other contentlets. It also handles
     * site and folder assignments, field value processing, and category retention for updated
     * content.
     *
     * @param lineNumber                       The current line number being processed in the file.
     * @param contentlets                      A list of Contentlet objects to be processed.
     * @param isNew                            Indicates if the import is a new operation.
     * @param existingLanguage                 The language identifier for existing content.
     * @param values                           A map containing header index and corresponding field
     *                                         values from the file.
     * @param siteAndFolder                    A pair containing the Host and Folder information for
     *                                         the contentlet.
     * @param wfActionId                       The workflow action ID associated with the
     *                                         contentlet.
     * @param csvRelationshipRecordsParentOnly Relationships where this contentlet is a parent
     *                                         only.
     * @param csvRelationshipRecordsChildOnly  Relationships where this contentlet is a child only.
     * @param csvRelationshipRecords           All relationships (both parent and child).
     * @param headers                          A map containing field definitions from the file
     *                                         header.
     * @param categories                       A set of categories associated with the contentlets.
     * @param conditionValues                  The values used for conditional processing.
     * @param keyContentUpdated                A set of keys identifying updated content.
     * @param contentTypePermissions           A list of permissions for the content type.
     * @param wfActionIdIndex                  The index of the workflow action ID in the file
     *                                         line.
     * @param preview                          Indicates if we are in preview mode (no actual
     *                                         saves).
     * @param isMultilingual                   Indicates if the contentlet is multilingual.
     * @param user                             The user performing the import operation.
     * @param request                          The HTTP request object.
     * @param line                             The current line of data being processed from the
     *                                         file.
     * @return A ProcessedContentResult object containing the results of processing, including
     * validation messages, saved inodes, and counts of new, updated, and duplicate content.
     * @throws DotDataException     If an error occurs during data processing.
     * @throws DotSecurityException If a security exception occurs.
     * @throws IOException          If an I/O error occurs.
     * @throws LanguageException    If a language-related exception occurs.
     */
    private static ProcessedContentResult processContent(
            final int lineNumber,
            final List<Contentlet> contentlets,
            final boolean isNew,
            final Long existingLanguage,
            final Map<Integer, Object> values,
            final Pair<Host, Folder> siteAndFolder,
            final String wfActionId,
            final HashMap<Relationship, List<Contentlet>> csvRelationshipRecordsParentOnly,
            final HashMap<Relationship, List<Contentlet>> csvRelationshipRecordsChildOnly,
            final HashMap<Relationship, List<Contentlet>> csvRelationshipRecords,
            final Map<Integer, Field> headers,
            final Set<Category> categories,
            final String conditionValues,
            final HashSet<String> keyContentUpdated,
            final List<Permission> contentTypePermissions,
            final int wfActionIdIndex,
            final boolean preview,
            final boolean isMultilingual,
            final User user,
            final HttpServletRequest request,
            final String[] line
    ) throws DotDataException, DotSecurityException, IOException, LanguageException {

        ProcessedContentResultBuilder resultBuilder = new ProcessedContentResultBuilder();

        for (Contentlet cont : contentlets) {

            //Clean up any existing workflow action
            cont.resetActionId();

            // Handle workflow action ID from file
            if (wfActionIdIndex >= 0) {
                String wfActionIdStr = line[wfActionIdIndex];
                if (UtilMethods.isSet(wfActionIdStr)) {
                    cont.setActionId(wfActionIdStr);
                }
            }

            // Set site and folder
            setSiteAndFolder(user, cont, siteAndFolder);

            // Set field values
            processContentFields(cont, headers, values, request, preview);

            // Retaining Categories when content updated with partial imports
            if (UtilMethods.isSet(cont.getIdentifier())) {
                retainExistingCategories(cont, headers, categories, existingLanguage, user);
            }

            // Validate relationships for the contentlet
            validateRelationships(lineNumber,
                    headers, categories, cont, csvRelationshipRecordsParentOnly,
                    csvRelationshipRecordsChildOnly, csvRelationshipRecords);

            // Process workflow
            if (!preview) {

                ContentletRelationships contentletRelationships = loadRelationshipRecords(
                        csvRelationshipRecordsParentOnly, csvRelationshipRecordsChildOnly,
                        csvRelationshipRecords, cont);

                saveContent(lineNumber, wfActionId, cont, new ArrayList<>(categories),
                        contentTypePermissions,
                        contentletRelationships, user, headers, values, siteAndFolder,
                        resultBuilder);
            }

            if (!preview && (!resultBuilder.savedInodes().isEmpty())) {
                resultBuilder.setLastInode(
                        resultBuilder.savedInodes().get(resultBuilder.savedInodes().size() - 1)
                );
            }

            // Update counters
            updateCounters(isNew, conditionValues, keyContentUpdated, isMultilingual,
                    resultBuilder);
        }

        return resultBuilder.build();
    }

    /**
     * Validates relationships for a given contentlet based on the data in the CSV file. This method
     * checks if there are any relationship fields defined in the headers, and if so, it processes
     * and validates those relationships. If no relationship fields are present, it calls a separate
     * validation method that does not include relationships.
     *
     * @param lineNumber                       The current line number being processed.
     * @param headers                          A map containing field definitions from the file
     *                                         header.
     * @param categories                       A set of categories associated with the contentlet.
     * @param cont                             The Contentlet object to be validated.
     * @param csvRelationshipRecordsParentOnly Relationships where this contentlet is a parent
     *                                         only.
     * @param csvRelationshipRecordsChildOnly  Relationships where this contentlet is a child only.
     * @param csvRelationshipRecords           All relationships (both parent and child).
     * @throws DotDataException If an error occurs during relationship validation.
     */
    private static void validateRelationships(final int lineNumber,
            final Map<Integer, Field> headers,
            final Set<Category> categories,
            final Contentlet cont,
            final HashMap<Relationship, List<Contentlet>> csvRelationshipRecordsParentOnly,
            final HashMap<Relationship, List<Contentlet>> csvRelationshipRecordsChildOnly,
            final HashMap<Relationship, List<Contentlet>> csvRelationshipRecords
    ) throws DotDataException {

        //Check the new contentlet with the validator
        final boolean skipRelationshipsValidation = headers.values().stream()
                .noneMatch((field -> field.getFieldType()
                        .equals(FieldType.RELATIONSHIP.toString())));

        try {
            if (skipRelationshipsValidation) {
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
            HashMap<String, List<Field>> errors = (HashMap<String, List<Field>>) ex.getNotValidFields();
            Set<String> keys = errors.keySet();
            for (String key : keys) {
                sb.append(key + ": ");
                List<Field> fields = errors.get(key);
                int count = 0;
                for (Field field : fields) {
                    if (count > 0) {
                        sb.append(", ");
                    }
                    sb.append(field.getVelocityVarName());
                    count++;
                }
                sb.append("\n");
            }
            throw new DotRuntimeException(sb.toString());
        }

    }

    /**
     * Processes and sets field values for a given Contentlet.
     *
     * @param cont    The Contentlet object to which field values will be set.
     * @param headers A map of column indices to their corresponding Field definitions.
     * @param values  A map of processed field values indexed by column.
     * @param request The HTTP request object (may be used for additional context).
     * @param preview Boolean flag indicating whether this is a preview operation.
     */
    private static void processContentFields(
            final Contentlet cont,
            final Map<Integer, Field> headers,
            final Map<Integer, Object> values,
            final HttpServletRequest request,
            final boolean preview
    ) throws DotDataException, IOException, DotSecurityException {

        for (Map.Entry<Integer, Field> entry : headers.entrySet()) {
            Field field = entry.getValue();
            Object value = values.get(entry.getKey());

            if (field.getFieldType().equals(Field.FieldType.HOST_OR_FOLDER.toString())) {
                // Site and Folder are already set, so we can continue
                continue;
            }

            if (UtilMethods.isSet(field.getDefaultValue()) && !UtilMethods.isSet(value)) {
                value = field.getDefaultValue();
            }

            if (isNumericField(field) && !UtilMethods.isSet(value) && !field.isRequired()) {
                value = "0";
            }

            try {
                if (isBinaryField(field)) {
                    processBinaryField(cont, field, value, request, preview);
                } else {
                    conAPI.setContentletProperty(cont, field, value);
                }
            } catch (DotContentletStateException de) {
                if (!field.isRequired() || UtilMethods.isSet(value)) {
                    throw de;
                }
            }
        }
    }

    /**
     * Retains existing categories associated with the contentlet when performing an update
     * operation. This method ensures that categories are preserved during partial imports or
     * updates of contentlets.
     *
     * @param cont             The Contentlet object being processed.
     * @param headers          Map of fields in the contentlet, indexed by column position.
     * @param categories       Set of categories to which the contentlet belongs.
     * @param existingLanguage The language ID associated with the existing contentlet.
     * @param user             The User performing the operation.
     * @throws DotDataException If an error occurs while accessing or updating categories.
     */
    private static void retainExistingCategories(final Contentlet cont,
            final Map<Integer, Field> headers, final Set<Category> categories,
            final Long existingLanguage, User user) throws DotDataException, DotSecurityException {

        final List<Field> categoryFields = getCategoryFields(cont.getStructureInode());
        final List<Field> nonHeaderCategoryFields = getNonHeaderCategoryFields(categoryFields,
                headers);

        if (!nonHeaderCategoryFields.isEmpty()) {
            List<Category> nonHeaderParentCats = new ArrayList<>();
            List<Category> nonHeaderCategories = new ArrayList<>();

            for (Field field : nonHeaderCategoryFields) {
                nonHeaderParentCats.add(catAPI.find(field.getValues(), user, false));
            }

            for (Category cat : nonHeaderParentCats) {
                nonHeaderCategories.addAll(catAPI.getAllChildren(cat, user, false));
            }

            /*
             We need to verify that we are not trying to save a contentlet that have as language the default language because that mean that
             contentlet for that default language couldn't exist, we are just saving it after all....
             */
            Long languageId = getContentLanguageId(existingLanguage);
            List<Category> existingCategories = getExistingCategories(cont, languageId, user);

            for (Category contentCategory : existingCategories) {
                for (Category nonHeaderCategory : nonHeaderCategories) {
                    if (contentCategory.getCategoryVelocityVarName()
                            .equals(nonHeaderCategory.getCategoryVelocityVarName())) {
                        categories.add(contentCategory);
                    }
                }
            }
        }
    }

    /**
     * Persists processed contentlet data to the repository with full workflow integration. Handles:
     * <ul>
     *   <li>Workflow action execution validation and triggering</li>
     *   <li>Content check-in/check-out lifecycle management</li>
     *   <li>Relationship and category associations</li>
     *   <li>Permission inheritance from content type</li>
     *   <li>Automatic publishing based on configuration</li>
     * </ul>
     *
     * <p><strong>Workflow Priority:</strong>
     * <ol>
     *   <li>Action specified in CSV's workflow action column</li>
     *   <li>Action selected in import dialog dropdown</li>
     *   <li>System default NEW/PUBLISH actions</li>
     * </ol>
     *
     * @param lineNumber            CSV line number for error tracking
     * @param wfActionId            Workflow action ID from import configuration
     * @param cont                  Contentlet with processed field values
     * @param categories            Categories to associate with content
     * @param contentTypePermissions Permissions inherited from content type
     * @param relationships         Content relationships (parent/child)
     * @param user                  Authenticated user for audit trail
     * @param headers               Map of CSV columns to content fields
     * @param values                Raw+processed field values map
     * @param siteAndFolder         Hosting location context
     * @param resultBuilder         Accumulates processing results and messages
     *
     * @throws DotDataException       If content persistence fails
     * @throws DotSecurityException   If user lacks permission for operations
     * @throws LanguageException      If workflow message localization fails
     *
     * @example // Save blog post with 'Publish' workflow
     * saveContent(42, "pub-123", blogContent, categories, permissions,
     *             relationships, user, headers, values, hostFolder, result);
     *
     * @see WorkflowAPI#findActionMappedBySystemActionContentlet(Contentlet, SystemAction, User)
     * @see ContentletAPI#checkin(Contentlet, ContentletRelationships, List, List, User, boolean)
     *
     * @commitStrategy Uses DEFER index policy during batch operations
     * @autoPublish Controlled by PUBLISH_CSV_IMPORTED_CONTENT_AUTOMATICALLY property
     */
    private static void saveContent(
            final int lineNumber,
            final String wfActionId,
            Contentlet cont,
            final List<Category> categories,
            final List<Permission> contentTypePermissions,
            final ContentletRelationships relationships,
            final User user,
            final Map<Integer, Field> headers,
            final Map<Integer, Object> values,
            final Pair<Host, Folder> siteAndFolder,
            final ProcessedContentResultBuilder resultBuilder
    ) throws DotDataException, DotSecurityException, LanguageException {

        cont.setLowIndexPriority(true);

        final var validationResult = validateWorkflowExecution(lineNumber, wfActionId, cont, user,
                resultBuilder);
        if (validationResult.getLeft()) {
            executeWorkflowAction(cont, categories, validationResult.getRight(), relationships,
                    user);
        } else {
            cont = runWorkflowIfCould(user, contentTypePermissions, categories, cont,
                    relationships);
        }

        processTagFields(cont, headers, values, siteAndFolder);

        resultBuilder.addSavedInode(cont.getInode());
    }

    /**
     * Updates processing counters in the results using the provided values.
     * <p>
     * This method updates counters related to import operations. It increments different types of
     * content based on whether the content is new or not, and under what conditions it should be
     * considered as updated or duplicated.
     *
     * @param isNew             Indicates if the content is newly created.
     * @param conditionValues   The condition value used for determining updates or duplicates.
     * @param keyContentUpdated A set tracking condition values added as updated content to avoid
     *                          duplicates.
     * @param isMultilingual    Determines if the content is multilingual, affecting update
     *                          handling.
     * @param resultBuilder     The object to update counters based on import results.
     */
    private static void updateCounters(final boolean isNew, final String conditionValues,
            final Set<String> keyContentUpdated, final boolean isMultilingual,
            final ProcessedContentResultBuilder resultBuilder) {

        if (isNew) {
            resultBuilder.incrementNewContent();
        } else {
            if (conditionValues.isEmpty()
                    || !keyContentUpdated.contains(conditionValues)
                    || isMultilingual) {
                resultBuilder.incrementUpdatedContent();
                resultBuilder.incrementDuplicateContent();
                keyContentUpdated.add(conditionValues);
            } else {
                resultBuilder.incrementDuplicateContent();
            }
        }
    }

    /**
     * Checks if the given field is a numeric type (integer or float).
     * <p>
     * This method determines if the field contentlet starts with "integer" or "float".
     *
     * @param field The field to check
     * @return true if the field is an integer or float type, false otherwise
     */

    private static boolean isNumericField(final Field field) {
        return field.getFieldContentlet().startsWith("integer")
                || field.getFieldContentlet().startsWith("float");
    }

    /**
     * Processes binary fields such as images or files and updates the results builder with the
     * processed value.
     * <p>
     * This method is responsible for handling binary data from CSV import operations. It processes
     * the binary field value.
     *
     * @param value The raw value of the binary field from the CSV line.
     */
    private static void processBinaryField(final Contentlet cont, final Field field,
            final Object value, final HttpServletRequest request, final boolean preview)
            throws IOException, DotSecurityException {
        if (preview) {
            File dummyFile = File.createTempFile("dummy", ".txt",
                    new File(ConfigUtils.getAssetTempPath()));
            cont.setBinary(field.getVelocityVarName(), dummyFile);
        } else if (value != null && UtilMethods.isSet(value.toString())) {
            DotTempFile tempFile = APILocator.getTempFileAPI()
                    .createTempFileFromUrl(null, request, new URL(value.toString()), -1);
            cont.setBinary(field.getVelocityVarName(), tempFile.file);
        }
    }

    /**
     * Retrieves all category-related fields from the specified content structure based on its
     * inode.
     *
     * @param structureInode The identifier of the content structure to fetch category fields from.
     * @return A list of Field objects representing the category-related fields (CATEGORY or
     * CATEGORIES_TAB type) from the specified structure.
     */
    private static List<Field> getCategoryFields(final String structureInode) {
        return FieldsCache.getFieldsByStructureInode(structureInode).stream()
                .filter(field -> field.getFieldType().equals(Field.FieldType.CATEGORY.toString())
                        || field.getFieldType().equals(Field.FieldType.CATEGORIES_TAB.toString()))
                .collect(Collectors.toList());
    }

    /**
     * Identifies category fields that exist in the content type but are NOT present in CSV headers.
     * Used to preserve existing category associations when updating content, ensuring categories
     * not specified in the import file are retained.
     *
     * @param categoryFields All category-type fields defined in the content type structure
     * @param headers        Map of CSV columns to content fields (key: column index, value: Field)
     *
     * @return List of {@link Field} objects representing category fields that:
     *         - Are defined in the content type
     *         - Are NOT present in CSV headers
     *         - Need their existing values preserved during update operations
     *
     * @example // Given:
     *          // categoryFields = [Topics, Audience]
     *          // headers = {0: Title, 1: Content, 2: Topics}
     *          // Returns: [Audience] field
     *
     * @implNote Comparison is done using case-insensitive inode matching
     * @see ImportUtil#importLine() Used during content update processing
     */
    private static List<Field> getNonHeaderCategoryFields(final List<Field> categoryFields,
            final Map<Integer, Field> headers) {
        return categoryFields.stream()
                .filter(field -> headers.values().stream()
                        .noneMatch(headerField -> headerField.getInode()
                                .equalsIgnoreCase(field.getInode())))
                .collect(Collectors.toList());
    }

    /**
     * Retrieves the appropriate content language identifier based on the provided existing language
     * ID. If an existing language ID is provided, it returns that value. Otherwise, it defaults to
     * the system's default language ID.
     *
     * @param existingLanguage The existing language identifier if available.
     * @return A Long representing the content language identifier (either existing or default).
     */
    private static Long getContentLanguageId(final Long existingLanguage) {
        return existingLanguage != null ?
                existingLanguage :
                langAPI.getDefaultLanguage().getId();
    }

    /**
     * Retrieves the existing categories for the specified contentlet based on its identifier and
     * language ID.
     *
     * @param cont       The Contentlet object containing the identifier of the content to find
     *                   categories for.
     * @param languageId The language identifier in which to look up the categories.
     * @param user       The User context under which the operation is being performed.
     * @return A list of Category objects representing the existing categories associated with the
     * contentlet.
     * @throws DotDataException     If an error occurs during data retrieval or processing.
     * @throws DotSecurityException If a security-related exception occurs.
     */
    private static List<Category> getExistingCategories(final Contentlet cont,
            final Long languageId, final User user) throws DotDataException, DotSecurityException {
        try {
            Contentlet workingCont = conAPI.findContentletByIdentifier(
                    cont.getIdentifier(), false, languageId, user, false);
            return catAPI.getParents(workingCont, user, false);
        } catch (DotContentletStateException e) {
            Logger.error(ImportUtil.class, "Error getting existing categories", e);
            return new ArrayList<>();
        }
    }

    /**
     * Validates if the workflow action associated with the contentlet can be executed based on user
     * permissions and the workflow step.
     * <p>
     * This method checks whether the specified workflow action is valid for execution. It verifies:
     * 1. Whether the user has the necessary permissions to execute the workflow action.
     * 2. Whether the workflow action is in the correct step of the workflow for the contentlet.
     *
     * @param lineNumber    The line number from the import file being processed.
     * @param wfActionId    The ID of the workflow action to validate (if provided).
     * @param cont          The Contentlet object associated with the workflow action.
     * @param user          The User context under which the operation is being performed.
     * @param resultBuilder A builder for collecting results and validation messages during
     *                      processing.
     * @return A Pair containing two elements: - Left: Boolean indicating whether the workflow
     * action can be executed (true = allowed, false = not allowed). - Right: WorkflowAction object
     * if the action is valid; null otherwise.
     * @throws DotDataException     If an error occurs during data retrieval or processing.
     * @throws DotSecurityException If a security-related exception occurs during permission
     *                              validation.
     */
    private static Pair<Boolean, WorkflowAction> validateWorkflowExecution(
            final int lineNumber, final String wfActionId, final Contentlet cont, final User user,
            final ProcessedContentResultBuilder resultBuilder) throws LanguageException {

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

                Logger.debug(ImportUtil.class,
                        "cont: " + cont + ", user: " + user + ", lineNumber " + lineNumber +
                                "validateWorkflowAction, message.import.contentlet.invalid.action.selected: "
                                + e.getMessage());

                resultBuilder.messages.add(ValidationMessage.builder()
                        .type(ValidationMessageType.WARNING)
                        .message(LanguageUtil.get(user,
                                "message.import.contentlet.invalid.action.found.in.csv") + " "
                                + e.getMessage())
                        .invalidValue(cont.getActionId())
                        .lineNumber(lineNumber)
                        .build());

                // if the user doesn't have access to the action then removed it from
                // the content to avoid troubles executing the action set on the
                // dropdown or on the checkin
                cont.resetActionId();
            }

            if (null != executeWfAction && UtilMethods.isSet(executeWfAction.getId())) {
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

                Logger.debug(ImportUtil.class,
                        "cont: " + cont + ", user: " + user + ", lineNumber " + lineNumber +
                                "message.import.contentlet.invalid.action.selected: "
                                + e.getMessage());

                resultBuilder.messages.add(ValidationMessage.builder()
                        .type(ValidationMessageType.WARNING)
                        .message(LanguageUtil.get(user,
                                "message.import.contentlet.invalid.action.selected") + " "
                                + e.getMessage())
                        .invalidValue(wfActionId)
                        .lineNumber(lineNumber)
                        .build());

                // if the user doesn't have access to the action then removed it from
                // the content to avoid troubles executing the action set on the
                // dropdown or on the checkin
                cont.resetActionId();
            }

            if (null != executeWfAction && UtilMethods.isSet(executeWfAction.getId())) {
                userCanExecuteAction = true;
            }
        }

        return Pair.of(userCanExecuteAction, executeWfAction);
    }

    /**
     * Executes the specified workflow action associated with the contentlet.
     * <p>
     * This method performs the execution of a validated workflow action. It sets necessary
     * properties on the contentlet and triggers the workflow process based on the provided
     * parameters.
     *
     * @param cont            The Contentlet object associated with the workflow action to execute.
     * @param categories      A list of categories associated with the contentlet.
     * @param executeWfAction The WorkflowAction object representing the validated workflow action
     *                        to be executed.
     * @param relationships   ContentletRelationships object containing relationship data for the
     *                        contentlet.
     * @param user            The User context under which the operation is being performed.
     * @throws DotDataException     If an error occurs during workflow execution or processing.
     * @throws DotSecurityException If a security-related exception occurs during execution.
     */
    private static void executeWorkflowAction(
            final Contentlet cont,
            final List<Category> categories,
            final WorkflowAction executeWfAction,
            final ContentletRelationships relationships,
            final User user
    ) throws DotDataException, DotSecurityException {

        cont.setIndexPolicy(IndexPolicy.DEFER);
        cont.setBoolProperty(Contentlet.SKIP_RELATIONSHIPS_VALIDATION,
                relationships == null || relationships.getRelationshipsRecords().isEmpty());

        workflowAPI.fireContentWorkflow(cont,
                new ContentletDependencies.Builder()
                        .respectAnonymousPermissions(Boolean.FALSE)
                        .modUser(user)
                        .relationships(relationships)
                        .workflowActionId(executeWfAction.getId())
                        .workflowActionComments("")
                        .workflowAssignKey("")
                        .categories(categories)
                        .generateSystemEvent(Boolean.FALSE)
                        .build());
    }

    /**
     * Processes tag fields from contentlets by extracting tags from their values, trimming
     * whitespace, and associating them with the appropriate contentlet.
     *
     * @param cont          The Contentlet object containing the content data.
     * @param headers       A map of field identifiers to Field objects.
     * @param values        A map of field identifiers to their corresponding values.
     * @param siteAndFolder A pair containing the Host and Folder for determining the appropriate
     *                      context when processing tags.
     */
    private static void processTagFields(
            final Contentlet cont,
            final Map<Integer, Field> headers,
            final Map<Integer, Object> values,
            final Pair<Host, Folder> siteAndFolder) {

        for (Map.Entry<Integer, Field> entry : headers.entrySet()) {
            Field field = entry.getValue();
            Object value = values.get(entry.getKey());

            if (field.getFieldType().equals(Field.FieldType.TAG.toString())
                    && value instanceof String) {
                String[] tags = ((String) value).split(",");
                String hostId = getHostId(siteAndFolder);

                for (String tagName : tags) {
                    try {
                        if (tagName != null && !tagName.trim().isEmpty()) {
                            APILocator.getTagAPI().addContentletTagInode(
                                    tagName.trim(),
                                    cont.getInode(),
                                    hostId,
                                    field.getVelocityVarName());
                        }
                    } catch (Exception e) {
                        Logger.error(ImportUtil.class, "Unable to import tags: " + e.getMessage());
                    }
                }
            }
        }
    }

    /**
     * Gets the host identifier from the given site and folder pair. If the site is null or the host
     * is the system host, returns the system host identifier. Otherwise, returns the host's
     * identifier.
     *
     * @param siteAndFolder A pair containing the Host and Folder objects
     * @return The host identifier
     */
    private static String getHostId(final Pair<Host, Folder> siteAndFolder) {
        if (siteAndFolder != null && siteAndFolder.getLeft() != null) {
            Host host = siteAndFolder.getLeft();
            return host.getIdentifier().equals(Host.SYSTEM_HOST) ?
                    Host.SYSTEM_HOST :
                    host.getIdentifier();
        }
        return Host.SYSTEM_HOST;
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
     * Constructs the URL using the specified folder and asset name.
     *
     * @param siteAndFolder A pair containing the Host and Folder objects. If not null, the folder
     *                      path is used to build the URL.
     * @param assetName     The name of the asset to append to the URL.
     * @return The constructed URL as a String.
     */
    private static String getURLFromFolderAndAssetName(
            final Pair<Host, Folder> siteAndFolder, final String assetName) {

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
                    && !UtilMethods.isSet(csvRelatedContentlet)) {
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
     * Validates unique fields by checking for duplicate values across different beans and lines.
     * <p>
     * This method iterates over each field in the provided list of unique fields. For each field,
     * it checks all associated UniqueFieldBean instances to detect any duplicate values within the
     * same language and line number. If duplicates are found, a validation message is added, and
     * the line is marked for ignoring.
     *
     * @param user             The current user for localization purposes.
     * @param lineNumber       The line number in the CSV file being processed.
     * @param language         The language ID to validate against.
     * @param uniqueFieldBeans List of UniqueFieldBean instances containing field data to validate.
     * @param uniqueFields     List of fields to check for uniqueness.
     * @param resultBuilder    A builder object to collect validation messages and results.
     * @return True if any duplicate values are found, causing the line to be ignored; otherwise,
     * false.
     * @throws LanguageException If an error occurs during language validation.
     */
    private static boolean validateUniqueFields(User user, int lineNumber, long language,
            List<UniqueFieldBean> uniqueFieldBeans, List<Field> uniqueFields,
            final LineImportResultBuilder resultBuilder) throws LanguageException {
        boolean ignoreLine = false;
        for (Field f : uniqueFields) {
            Object value = null;
            int count = 0;
            for (UniqueFieldBean bean : uniqueFieldBeans) {
                if (bean.field().equals(f) && language == bean.languageId()) {
                    if (count > 0 && value != null && value.equals(bean.value())
                            && lineNumber == bean
                            .lineNumber()) {
                        resultBuilder.incrementNewContent(-1);
                        ignoreLine = true;
                        resultBuilder.addValidationMessage(ValidationMessage.builder()
                                .type(ValidationMessageType.WARNING)
                                .message(LanguageUtil
                                        .get(user,
                                                "contains-duplicate-values-for-structure-unique-field")
                                        + " '"
                                        + f.getVelocityVarName() + "', " + LanguageUtil.get(user,
                                        "and-will-be-ignored"))
                                .field(bean.field().getVelocityVarName())
                                .invalidValue(bean.value().toString())
                                .lineNumber(lineNumber)
                                .build());
                    }
                    value = bean.value();
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
                    .fileInfo(FileInfo.builder()
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
        private static HeaderInfo initializeHeaderInfo() {
            return HeaderInfo.builder()
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
        private static ResultData initializeResultData() {
            return ResultData.builder()
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
        void addMessages(List<ValidationMessage> validationMessages) {
            if (validationMessages != null) {
                validationMessages.forEach(structuredResults::addMessages);
            }
        }

        /**
         * Adds a single validation message to the results.
         *
         * @param message Message to add
         */
        void addMessage(ValidationMessage message) {
            structuredResults.addMessages(message);
        }

        /**
         * Updates file processing information.
         *
         * @param fileInfo Updated file information
         */
        void updateFileInfo(final FileInfo fileInfo) {
            structuredResults.fileInfo(fileInfo);
        }

        /**
         * Updates header validation information.
         *
         * @param headerInfo Updated header information
         */
        void updateHeaderInfo(final HeaderInfo headerInfo) {
            FileInfo currentFileInfo = structuredResults.build().fileInfo();
            structuredResults.fileInfo(currentFileInfo.withHeaderInfo(headerInfo));
        }

        /**
         * Updates the content type name in the results.
         *
         * @param contentType Name of the content type
         */
        void updateContentType(String contentType) {
            ResultData currentData = structuredResults.build().data();
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
            structuredResults.data(ResultData.builder()
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

    /**
     * Container for key field search results
     */
    private static class SearchByKeyFieldsResult {

        final List<Contentlet> contentlets;
        final List<String> updatedInodes;
        final String conditionValues;
        final boolean isMultilingual;

        SearchByKeyFieldsResult(List<Contentlet> contentlets, List<String> updatedInodes,
                String conditionValues, boolean isMultilingual) {
            this.contentlets = contentlets;
            this.updatedInodes = updatedInodes;
            this.conditionValues = conditionValues;
            this.isMultilingual = isMultilingual;
        }
    }

    /**
     * Builder class to help construct LineImportResults during CSV line processing. Provides
     * methods to accumulate validation messages and set various result properties.
     */
    private static class LineImportResultBuilder {

        private final LineImportResult.Builder builder;
        private final List<ValidationMessage> messages;
        private final Map<String, String> keyFields;
        private final List<Category> categories;
        private final List<String> updatedInodes;
        private final List<String> savedInodes;
        private int newContentCount;
        private int updatedContentCount;
        private int duplicateContentCount;
        private String lastInode;

        public LineImportResultBuilder(int lineNumber) {
            this.builder = LineImportResult.builder();
            this.messages = new ArrayList<>();
            this.keyFields = new HashMap<>();
            this.categories = new ArrayList<>();
            this.updatedInodes = new ArrayList<>();
            this.builder.lineNumber(lineNumber).ignoreLine(false);
            this.savedInodes = new ArrayList<>();
            this.newContentCount = 0;
            this.updatedContentCount = 0;
            this.duplicateContentCount = 0;
        }

        public void addValidationMessage(ValidationMessage message) {
            messages.add(message);
        }

        public void addKeyField(String field, String value) {
            keyFields.put(field, value);
        }

        public void addCategory(Category category) {
            categories.add(category);
        }

        public void setUpdatedInodes(List<String> updatedInodes) {
            this.updatedInodes.addAll(updatedInodes);
        }

        public void setIgnoreLine(boolean ignoreLine) {
            builder.ignoreLine(ignoreLine);
        }

        public void setNewContent(boolean isNewContent) {
            builder.isNewContent(isNewContent);
        }

        void addSavedInode(String inode) {
            savedInodes.add(inode);
        }

        void incrementNewContent() {
            newContentCount++;
        }

        void incrementNewContent(final int count) {
            newContentCount += count;
        }

        void incrementUpdatedContent(final int count) {
            updatedContentCount += count;
        }

        void incrementDuplicateContent(final int count) {
            duplicateContentCount += count;
        }

        void setLastInode(String lastInode) {
            this.lastInode = lastInode;
        }

        public LineImportResult build() {
            return builder
                    .messages(messages)
                    .keyFields(keyFields)
                    .categories(categories)
                    .updatedInodes(updatedInodes)
                    .savedInodes(savedInodes)
                    .newContentCount(newContentCount)
                    .updatedContentCount(updatedContentCount)
                    .duplicateContentCount(duplicateContentCount)
                    .lastInode(lastInode)
                    .build();
        }
    }

    /**
     * Builder class to help construct FieldProcessingResults during CSV field processing. Provides
     * methods to accumulate validation messages and set various result properties.
     */
    private static class FieldProcessingResultBuilder {

        private final int lineNumber;
        private final FieldProcessingResult.Builder builder;
        private final List<ValidationMessage> messages;
        private final Map<String, String> keyFields;
        private final List<Category> categories;
        List<UniqueFieldBean> uniqueFields = new ArrayList<>();
        Map<Integer, Object> values = new HashMap<>();
        Pair<Host, Folder> siteAndFolder;
        Pair<Integer, String> urlValue;
        String urlValueAssetName;

        public FieldProcessingResultBuilder(int lineNumber) {
            this.builder = FieldProcessingResult.builder();
            this.messages = new ArrayList<>();
            this.keyFields = new HashMap<>();
            this.categories = new ArrayList<>();
            this.uniqueFields = new ArrayList<>();
            this.values = new HashMap<>();
            this.builder.lineNumber(lineNumber);
            this.lineNumber = lineNumber;
        }

        void setValues(Map<Integer, Object> values) {
            this.values = values;
        }

        void addValue(Integer index, Object value) {
            this.values.put(index, value);
        }

        void setSiteAndFolder(Pair<Host, Folder> siteAndFolder) {
            this.siteAndFolder = siteAndFolder;
        }

        public void addValidationMessage(ValidationMessage message) {
            messages.add(message);
        }

        void addUniqueField(UniqueFieldBean uniqueField) {
            this.uniqueFields.add(uniqueField);
        }

        void setUrlValue(Pair<Integer, String> urlValue) {
            this.urlValue = urlValue;
        }

        void setUrlValueAssetName(String urlValueAssetName) {
            this.urlValueAssetName = urlValueAssetName;
        }

        public void addWarning(String message) {
            addValidationMessage(ValidationMessage.builder()
                    .type(ValidationMessageType.WARNING)
                    .message(message)
                    .lineNumber(lineNumber)
                    .build());
        }

        public void addError(String message) {
            addValidationMessage(ValidationMessage.builder()
                    .type(ValidationMessageType.ERROR)
                    .message(message)
                    .lineNumber(lineNumber)
                    .build());
        }

        public void addKeyField(String field, String value) {
            keyFields.put(field, value);
        }

        public void addCategory(Category category) {
            categories.add(category);
        }

        public void addCategories(Collection<Category> categories) {
            this.categories.addAll(categories);
        }

        public void setIgnoreLine(boolean ignoreLine) {
            builder.ignoreLine(ignoreLine);
        }

        public FieldProcessingResult build() {
            return builder
                    .messages(messages)
                    .keyFields(keyFields)
                    .categories(categories)
                    .uniqueFields(uniqueFields)
                    .values(values)
                    .siteAndFolder(Optional.ofNullable(siteAndFolder))
                    .urlValue(Optional.ofNullable(urlValue))
                    .urlValueAssetName(Optional.ofNullable(urlValueAssetName))
                    .build();
        }
    }

    /**
     * Builder class to help construct RelationshipProcessingResults during CSV relationship processing.
     * Provides methods to accumulate validation messages and set various result properties.
     */
    private static class RelationshipProcessingBuilder {

        private final Map<Relationship, List<Contentlet>> parentOnlyRelationships = new HashMap<>();
        private final Map<Relationship, List<Contentlet>> childOnlyRelationships = new HashMap<>();
        private final Map<Relationship, List<Contentlet>> relationships = new HashMap<>();
        private final List<ValidationMessage> messages = new ArrayList<>();
        private final int lineNumber;

        public RelationshipProcessingBuilder(int lineNumber) {
            this.lineNumber = lineNumber;
        }

        void addParentOnlyRelationship(Relationship relationship, List<Contentlet> contentlets) {
            parentOnlyRelationships.put(relationship, contentlets);
        }

        void addChildOnlyRelationship(Relationship relationship, List<Contentlet> contentlets) {
            childOnlyRelationships.put(relationship, contentlets);
        }

        void addRelationship(Relationship relationship, List<Contentlet> contentlets) {
            relationships.put(relationship, contentlets);
        }

        void addWarning(String message) {
            messages.add(ValidationMessage.builder()
                    .type(ValidationMessageType.WARNING)
                    .message(message)
                    .lineNumber(lineNumber)
                    .build());
        }

        RelationshipProcessingResult build() {
            return RelationshipProcessingResult.builder()
                    .parentOnlyRelationships(parentOnlyRelationships)
                    .childOnlyRelationships(childOnlyRelationships)
                    .relationships(relationships)
                    .messages(messages)
                    .build();
        }
    }

    /**
     * Builder class to help construct ProcessedContentResults during CSV content processing. Provides
     * methods to accumulate validation messages and set various result properties.
     */
    private static class ProcessedContentResultBuilder {

        private final List<String> savedInodes = new ArrayList<>();
        private int newContentCount;
        private int updatedContentCount;
        private int duplicateContentCount;
        private String lastInode;
        private final List<ValidationMessage> messages = new ArrayList<>();

        // Builder methods
        void addSavedInode(String inode) {
            savedInodes.add(inode);
        }

        List<String> savedInodes() {
            return savedInodes;
        }

        void incrementNewContent() {
            newContentCount++;
        }

        void incrementUpdatedContent() {
            updatedContentCount++;
        }

        void incrementDuplicateContent() {
            duplicateContentCount++;
        }

        void addMessage(ValidationMessage message) {
            messages.add(message);
        }

        void setLastInode(String lastInode) {
            this.lastInode = lastInode;
        }

        ProcessedContentResult build() {
            return ProcessedContentResult.builder()
                    .savedInodes(savedInodes)
                    .newContentCount(newContentCount)
                    .updatedContentCount(updatedContentCount)
                    .duplicateContentCount(duplicateContentCount)
                    .lastInode(lastInode)
                    .messages(messages)
                    .build();
        }
    }

}
