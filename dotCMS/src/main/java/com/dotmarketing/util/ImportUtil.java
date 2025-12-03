package com.dotmarketing.util;

import static com.dotmarketing.portlets.contentlet.model.Contentlet.IDENTIFIER_KEY;
import static com.dotmarketing.portlets.contentlet.model.Contentlet.STRUCTURE_INODE_KEY;
import static com.dotmarketing.util.importer.HeaderValidationCodes.HEADERS_NOT_FOUND;
import static com.dotmarketing.util.importer.ImportLineValidationCodes.LANGUAGE_NOT_FOUND;
import static com.liferay.util.StringPool.FORWARD_SLASH;

import com.dotcms.content.elasticsearch.util.ESUtils;
import com.dotcms.contenttype.business.ContentTypeAPI;
import com.dotcms.contenttype.model.field.BinaryField;
import com.dotcms.contenttype.model.field.HostFolderField;
import com.dotcms.contenttype.model.type.BaseContentType;
import com.dotcms.contenttype.model.type.ContentType;
import com.dotcms.contenttype.transform.contenttype.StructureTransformer;
import com.dotcms.contenttype.transform.field.LegacyFieldTransformer;
import com.dotcms.repackage.com.csvreader.CsvReader;
import com.dotcms.rest.api.v1.temp.DotTempFile;
import com.dotcms.rest.api.v1.temp.TempFileAPI;
import com.dotcms.util.LowerKeyMap;
import com.dotcms.util.RelationshipUtil;
import com.dotmarketing.beans.Host;
import com.dotmarketing.beans.Identifier;
import com.dotmarketing.beans.Permission;
import com.dotmarketing.business.APILocator;
import com.dotmarketing.business.DotValidationException;
import com.dotmarketing.business.PermissionAPI;
import com.dotmarketing.cache.FieldsCache;
import com.dotmarketing.common.model.ContentletSearch;
import com.dotmarketing.db.HibernateUtil;
import com.dotmarketing.exception.DotDataException;
import com.dotmarketing.exception.DotDataValidationException;
import com.dotmarketing.exception.DotHibernateException;
import com.dotmarketing.exception.DotRuntimeException;
import com.dotmarketing.exception.DotSecurityException;
import com.dotmarketing.portlets.categories.business.CategoryAPI;
import com.dotmarketing.portlets.categories.model.Category;
import com.dotmarketing.portlets.contentlet.action.ImportAuditUtil;
import com.dotmarketing.portlets.contentlet.business.ContentletAPI;
import com.dotmarketing.portlets.contentlet.business.DotContentletStateException;
import com.dotmarketing.portlets.contentlet.business.DotDateFieldException;
import com.dotmarketing.portlets.contentlet.business.HostAPI;
import com.dotmarketing.portlets.contentlet.model.Contentlet;
import com.dotmarketing.portlets.contentlet.model.ContentletDependencies;
import com.dotmarketing.portlets.contentlet.model.IndexPolicy;
import com.dotmarketing.portlets.fileassets.business.FileAsset;
import com.dotmarketing.portlets.fileassets.business.FileAssetAPI;
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
import com.dotmarketing.util.importer.ImportLineValidationCodes;
import com.dotmarketing.util.importer.ImportResultConverter;
import com.dotmarketing.util.importer.exception.HeaderValidationException;
import com.dotmarketing.util.importer.exception.ImportLineError;
import com.dotmarketing.util.importer.exception.ImportLineException;
import com.dotmarketing.util.importer.exception.ValidationMessageException;
import com.dotmarketing.util.importer.model.AbstractImportResult.OperationType;
import com.dotmarketing.util.importer.model.AbstractValidationMessage.ValidationMessageType;
import com.dotmarketing.util.importer.model.ContentSummary;
import com.dotmarketing.util.importer.model.ContentletSearchResult;
import com.dotmarketing.util.importer.model.FieldProcessingResult;
import com.dotmarketing.util.importer.model.FieldsProcessingResult;
import com.dotmarketing.util.importer.model.FileInfo;
import com.dotmarketing.util.importer.model.FileInfo.Builder;
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
import io.vavr.Lazy;
import io.vavr.control.Try;
import java.io.File;
import java.io.IOException;
import java.io.Reader;
import java.net.URI;
import java.net.URL;
import java.sql.Savepoint;
import java.sql.Timestamp;
import java.text.DateFormat;
import java.text.ParseException;
import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.Collection;
import java.util.Collections;
import java.util.Comparator;
import java.util.Date;
import java.util.HashMap;
import java.util.HashSet;
import java.util.Iterator;
import java.util.List;
import java.util.Map;
import java.util.Map.Entry;
import java.util.Objects;
import java.util.Optional;
import java.util.Set;
import java.util.stream.Collectors;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.locks.ReentrantLock;
import java.util.function.Function;
import javax.servlet.http.HttpServletRequest;
import org.apache.commons.lang3.BooleanUtils;
import org.apache.commons.lang3.StringUtils;
import org.apache.commons.lang3.tuple.ImmutablePair;
import org.apache.commons.lang3.tuple.Pair;

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

    private static final PermissionAPI permissionAPI = APILocator.getPermissionAPI();
    private static final ContentletAPI conAPI = APILocator.getContentletAPI();
    private static final CategoryAPI catAPI = APILocator.getCategoryAPI();
    private static final LanguageAPI langAPI = APILocator.getLanguageAPI();
    private static final HostAPI hostAPI = APILocator.getHostAPI();
    private static final FolderAPI folderAPI = APILocator.getFolderAPI();
    private static final WorkflowAPI workflowAPI = APILocator.getWorkflowAPI();
    private static final ContentTypeAPI contentTypeAPI = APILocator.getContentTypeAPI(APILocator.systemUser());
    private static final FileAssetAPI fileAssetAPI = APILocator.getFileAssetAPI();
    private static final TempFileAPI tempFileAPI = APILocator.getTempFileAPI();

    public static final String KEY_WARNINGS = "warnings";
    public static final String KEY_ERRORS = "errors";
    public static final String KEY_MESSAGES = "messages";
    public static final String KEY_RESULTS = "results";
    public static final String KEY_COUNTERS = "counters";
    public static final String KEY_IDENTIFIERS = "identifiers";
    public static final String KEY_UPDATED_INODES = "updatedInodes";
    public static final String KEY_LAST_INODE = "lastInode";

    private static final int TEXT_FIELD_MAX_LENGTH = 255;

    private static final String languageCodeHeader = "languageCode";
    private static final String countryCodeHeader = "countryCode";
    public static final String identifierHeader = "Identifier";

    public static final int COMMIT_GRANULARITY = Lazy.of(
            () -> Config.getIntProperty("COMMIT_GRANULARITY", 100)
    ).get();

    protected static final String[] IMP_DATE_FORMATS = new String[] { "d-MMM-yy", "MMM-yy", "MMMM-yy", "d-MMM", "dd-MMM-yyyy",
        "MM/dd/yy hh:mm aa", "MM/dd/yyyy hh:mm aa",	"MM/dd/yy HH:mm", "MM/dd/yyyy HH:mm", "MMMM dd, yyyy", "M/d/y", "M/d",
        "EEEE, MMMM dd, yyyy", "MM/dd/yyyy", "hh:mm:ss aa", "HH:mm:ss", "hh:mm aa", "yyyy-MM-dd" };

    /**
     * European date formats to support international date parsing
     */
    protected static final String[] EUROPEAN_DATE_FORMATS = new String[] {
        "d/M/y", "dd/MM/yyyy", "d/M/yyyy", "dd/MM/yy", "dd-MM-yyyy"
    };

    /**
     * Combined date formats including both US and European patterns for comprehensive date parsing
     */
    protected static final String[] ALL_DATE_FORMATS = combineArrays(IMP_DATE_FORMATS, EUROPEAN_DATE_FORMATS);

    /**
     * Helper method to combine two string arrays
     */
    private static String[] combineArrays(String[] array1, String[] array2) {
        String[] combined = new String[array1.length + array2.length];
        System.arraycopy(array1, 0, combined, 0, array1.length);
        System.arraycopy(array2, 0, combined, array1.length, array2.length);
        return combined;
    }

    /**
     * Date format patterns for different field types
     */
    private static final String DATE_FIELD_FORMAT_PATTERN = "yyyyMMdd";
    private static final String DATE_TIME_FIELD_FORMAT_PATTERN = "MM/dd/yyyy";
    private static final String TIME_FIELD_FORMAT_PATTERN = "HHmmss";

    private ImportUtil() {
        // Prevent instantiation
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
     * For new implementations, consider using {@link #importFileResult} which provides
     * a more structured response format.
     * <p>
     * This method maintains backwards compatibility with existing code.
     * It SO is IMPORTANT to notice thar on this method {@code stopOnError} is set to false
     * as it didn't exist before and the default value of the {@code stopOnError} flag is false.
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
        final ImmutableImportFileParams importFileParams = ImmutableImportFileParams.builder()
                .importId(importId)
                .siteId(currentSiteId)
                .contentTypeInode(contentTypeInode)
                .keyFields(keyfields)
                .preview(preview)
                .isMultilingual(isMultilingual)
                .user(user)
                .language(language)
                .csvHeaders(csvHeaders)
                .csvReader(csvreader)
                .languageCodeHeaderColumn(languageCodeHeaderColumn)
                .countryCodeHeaderColumn(countryCodeHeaderColumn)
                .request(request)
                .workflowActionId(wfActionId)
                .build();
        final ImportResult result = importFileResult(importFileParams);
        return ImportResultConverter.toLegacyFormat(result, user);
    }

    /**
     * Imports the data contained in a CSV file into dotCMS. This method is the preferred way to
     * handle imports as it provides a structured result object containing detailed validation,
     * error tracking, and processing information.
     *
     * <p>The preview parameter determines the behavior:
     * <ul>
     *   <li>{@code preview == true}: Analyzes and validates data before committing changes</li>
     *   <li>{@code preview == false}: Attempts immediate import with result summary</li>
     * </ul>
     *
     * @param importId                 ID of this data import
     * @param currentSiteId            ID of the Site for content
     * @param contentTypeInode         Content Type Inode
     * @param keyfields                Field Inodes for content matching
     * @param preview                  If true, validates without saving
     * @param isMultilingual           If true, handles multiple languages
     * @param user                     User performing the import
     * @param language                 Language ID (-1 for language from CSV)
     * @param csvHeaders               Headers from CSV file
     * @param csvreader                CSV file reader
     * @param languageCodeHeaderColumn Language code column index
     * @param countryCodeHeaderColumn  Country code column index
     * @param wfActionId               Workflow action ID
     * @param fileTotalLines           The total lines count of the CSV file
     * @param request                  HTTP request context
     * @param progressCallback         Optional callback for progress updates
     * @return Structured ImportResult containing validation and processing details
     * @throws DotRuntimeException If a critical error occurs
     * @throws DotDataException    If a data access error occurs
     */
    /**
     * Modified version of the importFileResult method that maintains consistent
     * commit granularity even when errors and rollbacks occur.
     */
    public static ImportResult importFileResult(final ImportFileParams params)
            throws DotRuntimeException, DotDataException {

        final ContentType type = Try.of(()->contentTypeAPI.find(params.contentTypeInode())).getOrNull();
        if (type == null) {
            throw new DotDataValidationException("Content type not found for inode: " + params.contentTypeInode());
        }
        Structure contentType = new StructureTransformer(type).asStructure();

        List<Permission> contentTypePermissions = permissionAPI.getPermissions(contentType);
        List<UniqueFieldBean> uniqueFieldBeans = new ArrayList<>();

        // Initialize processing variables
        int failedRows = 0;
        int lineNumber = 0;

        int stoppedOnErrorAtLine = -1;

        // Track successful imports separately from line number for consistent commit granularity
        int successfulImports = 0;

        // Initialize counters
        final Counters counters = new Counters();
        final HashSet<String> keyContentUpdated = new HashSet<>();
        final Set<String> chosenKeyFields = new HashSet<>();

        // Processing maps
        final Map<Integer, Field> headers = new HashMap<>();
        final Map<Integer, Field> keyFields = new HashMap<>();
        final Map<Integer, Relationship> relationships = new HashMap<>();
        final Map<Integer, Boolean> onlyParent = new HashMap<>();
        final Map<Integer, Boolean> onlyChild = new HashMap<>();

        // Get unique fields
        final List<Field> uniqueFields = FieldsCache.getFieldsByStructureInode(contentType.getInode())
                .stream()
                .filter(Field::isUnique)
                .collect(Collectors.toList());

        // Results builder
        final FileInfo.Builder fileInfoBuilder = FileInfo.builder();
        HeaderValidationResult headerValidation;
        final List<ValidationMessage> messages = new ArrayList<>();
        final List<String> savedInodes = new ArrayList<>();
        final List<String> updatedInodes = new ArrayList<>();

        try {
            if ((params.csvHeaders() != null) || (params.csvReader().readHeaders())) {

                // Process headers
                if (params.csvHeaders() != null) {
                    headerValidation = importHeaders(params.csvHeaders(), contentType, params.keyFields(),
                            params.isMultilingual(), params.user(), headers, keyFields, uniqueFields, relationships,
                            onlyChild, onlyParent);
                } else {
                    headerValidation = importHeaders(params.csvReader().getHeaders(), contentType, params.keyFields(),
                            params.isMultilingual(), params.user(), headers, keyFields, uniqueFields, relationships,
                            onlyChild, onlyParent);
                }

                // Add header validation results
                fileInfoBuilder.headerInfo(headerValidation.headerInfo());
                messages.addAll(headerValidation.messages());

                int identifierColumnIndex = -1;
                int workflowActionIdColumnIndex = -1;
                for (SpecialHeaderInfo specialHeader : headerValidation.headerInfo()
                        .specialHeaders()) {
                    if (specialHeader.header().equalsIgnoreCase(identifierHeader)) {
                        identifierColumnIndex = specialHeader.columnIndex();
                    } else if (specialHeader.header().
                            equalsIgnoreCase(Contentlet.WORKFLOW_ACTION_KEY)) {
                        workflowActionIdColumnIndex = specialHeader.columnIndex();
                    }
                }

                lineNumber++;
                Savepoint savepoint = null;
                // Log preview/import status every 100 processed records
                // Reading the whole file
                if (!headers.isEmpty()) {

                    if (!params.preview()) {
                        HibernateUtil.startTransaction();
                    }

                    String[] csvLine;
                    while (params.csvReader().readRecord()) {

                        lineNumber++;
                        csvLine = params.csvReader().getValues();
                        LineImportResultBuilder resultBuilder = null;

                        // Check for cancellation
                        if (ImportAuditUtil.cancelledImports.containsKey(params.importId())) {
                            messages.add(ValidationMessage.builder()
                                    .type(ValidationMessageType.INFO)
                                    .lineNumber(lineNumber)
                                    .message("Import cancelled by user")
                                    .build());
                            break;
                        }

                        try {

                            Logger.debug(ImportUtil.class,
                                    "Line " + lineNumber + ": (" + params.csvReader().getRawRecord() + ").");

                            // Process language for line
                            final Long languageToImport = processLanguage(params.language(),
                                    params.languageCodeHeaderColumn(),
                                    params.countryCodeHeaderColumn(), csvLine);

                            if (languageToImport != -1) {

                                // Verifies if there was already imported a record with the same keys.
                                // Useful to know if we have batch uploads with the same keys,
                                // mostly visible for batch content uploads with multiple languages
                                boolean sameKeyBatchInsert = checkBatchKeyMatches(keyFields,
                                        csvLine, counters);

                                // Get identifier from results if it exists
                                final String identifier = getIdentifierFromResults(
                                        identifierColumnIndex, csvLine
                                );

                                savepoint = HibernateUtil.setSavepoint();
                                //Importing content record...
                                resultBuilder = new LineImportResultBuilder(lineNumber);
                                importLine(csvLine, params.siteId(),
                                        contentType, params.preview(), params.stopOnError(),
                                        params.isMultilingual(), params.user(), identifier,
                                        workflowActionIdColumnIndex, lineNumber, languageToImport,
                                        headers, keyFields, chosenKeyFields, keyContentUpdated,
                                        contentTypePermissions, uniqueFieldBeans, uniqueFields,
                                        relationships, onlyChild, onlyParent, sameKeyBatchInsert,
                                        params.workflowActionId(), params.request(), resultBuilder);
                                final var lineResult = resultBuilder.build();
                                parseLineResults(counters, lineResult, messages, savedInodes,
                                        updatedInodes, true);

                                //Storing the record keys we just imported for a later reference...
                                if (!keyFields.isEmpty()) {
                                    storeKeyFieldValues(keyFields, csvLine, counters);
                                }

                                // Increment the successful imports counter
                                successfulImports++;

                                // Handle transaction commits based on successful imports instead of line number
                                if (successfulImports % params.commitGranularityOverride() == 0) {
                                    handleBatchCommit(params.preview(), lineNumber);
                                    counters.incCommits();
                                    savepoint = null;

                                    Logger.debug(ImportUtil.class,
                                            "Committed batch at line " + lineNumber + " after " +
                                                    successfulImports + " successful imports.");
                                }
                            } else {
                                messages.add(ValidationMessage.builder()
                                        .type(ValidationMessageType.ERROR)
                                        .code(LANGUAGE_NOT_FOUND.name())
                                        .message(LanguageUtil.get(params.user(),
                                                "Locale-not-found-for-languageCode") + " ='"
                                                + csvLine[params.languageCodeHeaderColumn()]
                                                + "' countryCode='"
                                                + csvLine[params.countryCodeHeaderColumn()] + "'")
                                        .context(Map.of(
                                                "Language code", csvLine[params.languageCodeHeaderColumn()],
                                                "Country code", csvLine[params.countryCodeHeaderColumn()]
                                        ))
                                        .lineNumber(lineNumber)
                                        .build());

                                // Count language errors as failures
                                failedRows++;
                            }

                        } catch (Exception ex) {

                            failedRows++;

                            // Roll back to savepoint on error
                            if (!params.preview() && savepoint != null && !params.stopOnError()) {
                                HibernateUtil.rollbackSavepoint(savepoint);
                                counters.incRollbacks();

                                Logger.debug(ImportUtil.class,
                                        "Rolled back to savepoint at line " + lineNumber);
                            }

                            // If we failed importing a line we need to make sure we track
                            // everything that was done so far
                            if (ex instanceof ImportLineException) {
                                if (resultBuilder != null) {
                                    final var lineResult = resultBuilder.build();
                                    parseLineResults(counters, lineResult, messages, savedInodes,
                                            updatedInodes, false);
                                }
                            }

                            handleException(ex, lineNumber, messages);

                            Logger.warn(ImportUtil.class, "Error line: " + lineNumber +
                                    " (" + params.csvReader().getRawRecord() + "). Line Ignored.");

                            // Stop on first error if configured
                            if (params.stopOnError()) {
                                Logger.info(ImportUtil.class, "Per given configuration the Import process Stopped on Error at line " + lineNumber);
                                stoppedOnErrorAtLine = lineNumber;
                                break;
                            }

                        } finally {
                            // Update progress
                            if (params.progressCallback() != null) {
                                params.progressCallback().accept(lineNumber);
                            }
                        }
                    } // End of while loop that reads the CSV file

                    // Perform a final commit for any remaining records
                    if (!params.preview() && successfulImports > 0 &&
                            successfulImports % params.commitGranularityOverride() != 0) {
                        handleBatchCommit(params.preview(), lineNumber);
                        counters.incCommits();
                        Logger.debug(ImportUtil.class,
                                "Final commit at line " + lineNumber + " for remaining " +
                                        (successfulImports % params.commitGranularityOverride()) + " records.");
                    }

                    // Finalize transaction if not preview
                    if (!params.preview()) {
                        HibernateUtil.closeAndCommitTransaction();
                    }

                    if (!params.preview()) {
                        if (counters.getContentUpdatedDuplicated() > 0 &&
                                counters.getContentUpdatedDuplicated()
                                        > counters.getContentUpdated()) {
                            messages.add(ValidationMessage.builder()
                                    .type(ValidationMessageType.INFO)
                                    .message(counters.getContentUpdatedDuplicated() + " \""
                                            + contentType.getName() + "\" " + LanguageUtil.get(params.user(),
                                            "contentlets-updated-corresponding-to") + " "
                                            + counters.getContentUpdated() + " " + LanguageUtil.get(
                                            params.user(),
                                            "repeated-contents-based-on-the-key-provided"))
                                    .build()
                            );
                        }
                    }

                } else {
                    messages.add(ValidationMessage.builder()
                            .type(ValidationMessageType.ERROR)
                            .code(HEADERS_NOT_FOUND.name())
                            .message(LanguageUtil.get(params.user(),
                                    "No-headers-found-on-the-file-nothing-will-be-imported"))
                            .build()
                    );
                }
            }
        } catch (Exception e) {
            Logger.error(ImportUtil.class,
                    String.format("An error occurred when parsing CSV file in " +
                            "line #%s: %s", lineNumber, e.getMessage()), e);
            handleException(e, lineNumber, messages);
            failedRows++;
        }

        // Add import statistics to messages
        messages.add(ValidationMessage.builder()
                .type(ValidationMessageType.INFO)
                .message("Import statistics: " + successfulImports + " successful imports, " +
                        failedRows + " failed rows, " +
                        counters.getCommits() + " commits, " +
                        counters.getRollbacks() + " rollbacks")
                .build()
        );

        // Preparing the response
        return generateImportResult(params, lineNumber, failedRows, stoppedOnErrorAtLine,
                messages, fileInfoBuilder, counters, chosenKeyFields, contentType);
    }

    /**
     * Comparator for validation messages that sorts by line number first, then by error code.
     * Messages without line numbers or error codes are sorted to the end.
     */
    private static Comparator<ValidationMessage> validationMessageComparator() {
        return Comparator.comparing((ValidationMessage m) -> m.lineNumber().orElse(Integer.MAX_VALUE))
                .thenComparing(m -> m.code().orElse("\uFFFF")); // Use high Unicode character to sort nulls last
    }

    /**
     * Generates an import result from the provided input parameters after processing and analyzing
     * the file data and performing the necessary content operations.
     * @param params         the parameters containing details about the import operation,
     * @param lineNumber      the number of lines successfully processed
     * @param failedRows      the number of rows that encountered errors during processing
     * @param messages        a list of validation messages containing information, warnings, or
     *                        errors for the import operation
     * @param fileInfoBuilder a builder for constructing file information details
     * @param counters        an object containing counters to track content creation, updating, and
     *                        other metrics
     * @param chosenKeyFields a set of key fields initially chosen for the operation
     * @param contentType     the structure representing the type of content being imported
     * @return an {@link ImportResult} object containing details about the operation, processed
     * data, validation messages, and summary information
     */
    private static ImportResult generateImportResult(
            final ImportFileParams params, final int lineNumber,
            final int failedRows, final int stoppedOnErrorAtLine,
            final List<ValidationMessage> messages,
            final Builder fileInfoBuilder, final Counters counters,
            final Set<String> chosenKeyFields, final Structure contentType) {

        fileInfoBuilder.totalRows((int) params.fileTotalLines());
        final var fileInfo = fileInfoBuilder.build();

        final var infoMessages = messages.stream()
                .filter(message -> message.type() == ValidationMessageType.INFO)
                .sorted(validationMessageComparator())
                .collect(Collectors.toList());

        final var warningMessages = messages.stream()
                .filter(message -> message.type() == ValidationMessageType.WARNING)
                .sorted(validationMessageComparator())
                .collect(Collectors.toList());

        final var errorMessages = messages.stream()
                .filter(message -> message.type() == ValidationMessageType.ERROR)
                .sorted(validationMessageComparator())
                .collect(Collectors.toList());

        final String action = params.preview() ? "Content preview" : "Content import";
        String statusMsg = String.format("%s has finished, %d lines were read correctly.", action,
                lineNumber);
        statusMsg = !errorMessages.isEmpty() ? statusMsg + String.format(
                " However, %d errors were found.",
                errorMessages.size()) : StringPool.BLANK;
        Logger.info(ImportUtil.class, statusMsg);

        // Calculate the values for the report display
        final var createdContent = Math.max(counters.getContentToCreate(),
                counters.getContentCreated());
        final var updatedContent = Math.max(counters.getContentToUpdate(),
                counters.getContentUpdated());

        // Calculate the displayed key fields, if an identifier column exist, that one will be
        // the actual key field
        var calculatedKeyFields = chosenKeyFields;
        if (!fileInfo.headerInfo().specialHeaders().isEmpty()) {
            for (SpecialHeaderInfo specialHeader : fileInfo.headerInfo().specialHeaders()) {
                if (specialHeader.header().equalsIgnoreCase(identifierHeader)) {
                    calculatedKeyFields = new HashSet<>();
                    calculatedKeyFields.add(specialHeader.header());
                    break;
                }
            }
        }

        final var resultBuilder = ImportResult.builder();
        return resultBuilder
                .type(params.preview() ? OperationType.PREVIEW : OperationType.PUBLISH)
                .keyFields(calculatedKeyFields)
                .workflowActionId(Optional.ofNullable(params.workflowActionId()))
                .contentTypeName(contentType.getName())
                .contentTypeVariableName(contentType.getVelocityVarName())
                .lastInode(Optional.ofNullable(counters.getLastInode()))
                .fileInfo(fileInfo)
                .stoppedOnErrorAtLine( stoppedOnErrorAtLine > 0 ? Optional.of(stoppedOnErrorAtLine) : Optional.empty() )
                .data(ResultData.builder()
                        .processed(ProcessedData.builder()
                                .parsedRows(lineNumber)
                                .failedRows(failedRows)
                                .build())
                        .summary(ContentSummary.builder()
                                .toCreateContent(counters.getContentToCreate())
                                .toUpdateContent(counters.getContentToUpdate())
                                .createdContent(counters.getContentCreated())
                                .updatedContent(counters.getContentUpdated())
                                .duplicateContent(counters.getContentUpdatedDuplicated())
                                .createdDisplay(createdContent)
                                .updatedDisplay(updatedContent)
                                .failedDisplay(failedRows)
                                .commits(counters.getCommits())
                                .rollbacks(counters.getRollbacks())
                                .build())
                        .build())
                .info(infoMessages)
                .warning(warningMessages)
                .error(errorMessages)
                .build();
    }

    /**
     * Processes the results of a line import operation and updates the provided counters, messages
     * list, and inode lists with the relevant data from the line result.
     *
     * @param counters       an instance of {@code Counters} to update with values from the line
     *                       result
     * @param lineResult     an instance of {@code LineImportResult} containing the results of a
     *                       single line processing
     * @param messages       a list of {@code ValidationMessage} to which validation messages from
     *                       the line result are added
     * @param savedInodes    a list of strings representing the inodes saved during processing; this
     *                       list is updated with values from the line result
     * @param updatedInodes  a list of strings representing the inodes updated during processing;
     *                       this list is updated with values from the line result
     * @param updateCounters If the counters needs or not to be updated. if we are colling the
     *                       parseLineResults after an error we don't need to update them.
     */
    private static void parseLineResults(Counters counters, LineImportResult lineResult,
            List<ValidationMessage> messages, List<String> savedInodes,
            List<String> updatedInodes, final boolean updateCounters) {

        if (updateCounters) {
            counters.incContentToCreate(lineResult.contentToCreate());
            counters.incContentCreated(lineResult.createdContent());
            counters.incContentToUpdate(lineResult.contentToUpdate());
            counters.incContentUpdated(lineResult.updatedContent());
            counters.incContentUpdatedDuplicated(lineResult.duplicateContent());
        }

        // Update results from line processing
        counters.setLastInode(lineResult.lastInode());
        messages.addAll(lineResult.messages());
        savedInodes.addAll(lineResult.savedInodes());
        updatedInodes.addAll(lineResult.updatedInodes());
    }

    /**
     * Processes the language based on the given parameters. If the provided language value is -1,
     * attempts to retrieve a language ID using the language and country code columns from a CSV
     * line.
     *
     * @param language                 the language identifier, where a value of -1 triggers lookup
     *                                 using the columns
     * @param languageCodeHeaderColumn the column index in the CSV line for the language code
     * @param countryCodeHeaderColumn  the column index in the CSV line for the country code
     * @param csvLine                  the array representing a line from a CSV file, containing
     *                                 language details
     * @return the language ID derived from the provided parameters or the original language value
     * if valid
     */
    private static Long processLanguage(long language, int languageCodeHeaderColumn,
            int countryCodeHeaderColumn, String[] csvLine) {

        if (language == -1) {
            if (languageCodeHeaderColumn != -1 && countryCodeHeaderColumn != -1) {
                Language dotCMSLanguage = langAPI.getLanguage(
                        csvLine[languageCodeHeaderColumn],
                        csvLine[countryCodeHeaderColumn]
                );
                return dotCMSLanguage.getId();
            }
        }

        return language;
    }

    /**
     * Handles the batch commit process by logging the number of entries processed and, if not in
     * preview mode, committing and starting a new transaction.
     *
     * @param preview    A boolean indicating whether the operation is in preview mode. If true, no
     *                   transaction is committed.
     * @param lineNumber The number of entries processed during the batch operation.
     * @throws DotHibernateException If an error occurs while handling the Hibernate transaction.
     */
    private static void handleBatchCommit(final boolean preview, final int lineNumber)
            throws DotHibernateException {

        final String action = preview ? "previewed." : "imported.";
        Logger.info(ImportUtil.class,
                String.format("-> %d entries have been %s", lineNumber, action)
        );

        if (!preview) {
            HibernateUtil.closeAndCommitTransaction();
            HibernateUtil.startTransaction();
        }
    }

    /**
     * Handles an exception by building a validation message and adding it to the provided list.
     *
     * @param ex         The exception to be handled. If the exception is an instance of
     *                   ValidationMessageException, additional details will be extracted.
     * @param lineNumber The line number associated with the exception, typically where it
     *                   occurred.
     * @param messages   A list of validation messages to which the constructed validation message
     *                   will be added.
     */
    private static void handleException(Exception ex, int lineNumber,
            List<ValidationMessage> messages) {

        final ValidationMessage.Builder messageBuilder = ValidationMessage.builder()
                .type(ValidationMessageType.ERROR)
                .lineNumber(lineNumber);

        if (ex instanceof ValidationMessageException) {
            final var validationMessageException = (ValidationMessageException) ex;
            messageBuilder
                    .code(validationMessageException.getCode())
                    .field(validationMessageException.getField())
                    .invalidValue(validationMessageException.getInvalidValue())
                    .context(validationMessageException.getContext());
        }

        if(ex instanceof ImportLineError){
            final var importLineError = (ImportLineError) ex;
            messageBuilder
                    .code(importLineError.getCode())
                    .field(importLineError.getField())
                    .invalidValue(importLineError.getValue())
                    .context(importLineError.getContext().orElseGet(Map::of));
        }

        messageBuilder.message(ex.getMessage());
        messages.add(messageBuilder.build());
    }

    /**
     * Checks if the current line has matches with batch keys from previous lines.
     */
    private static boolean checkBatchKeyMatches(final Map<Integer, Field> keyFields,
            final String[] csvLine, final Counters counters) {

        if (keyFields == null || keyFields.isEmpty()) {
            return true;
        }

        boolean sameKeyBatchInsert = true;
        for (Integer column : keyFields.keySet()) {
            Field keyField = keyFields.get(column);
            if (!counters.matchKey(keyField.getVelocityVarName(), csvLine[column])) {
                sameKeyBatchInsert = false;
                break;
            }
        }

        return sameKeyBatchInsert;
    }

    /**
     * Stores key field values for tracking duplicates in batch processing.
     */
    private static void storeKeyFieldValues(final Map<Integer, Field> keyFields,
            final String[] csvLine, final Counters counters) {

        for (Integer column : keyFields.keySet()) {
            Field keyField = keyFields.get(column);
            counters.addKey(keyField.getVelocityVarName(), csvLine[column]);
        }
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
            final boolean isMultilingual, final User user, final Map<Integer, Field> headers,
            final Map<Integer, Field> keyFields, final List<Field> uniqueFields,
            final Map<Integer, Relationship> relationships,
            final Map<Integer, Boolean> onlyChild, final Map<Integer, Boolean> onlyParent)
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
                relationshipsMap, headerFields, headers, keyFields, uniqueFields,
                typeInfo.importableFields, relationships, onlyChild, onlyParent, user,
                validationBuilder);

        // Generate summary messages
        addHeadersSummaryMessages(headers.size(), typeInfo.importableFields,
                relationshipsMap, user, validationBuilder);

        return validationBuilder.build();
    }

    /**
     * Processes and validates header entries from the CSV file. This method handles the detailed
     * validation of each header column and populates various data structures with the results.
     *
     * @param headerLine           Array of header strings to process
     * @param contentType          Content Type structure to validate against
     * @param keyFieldsInodes      Array of field inodes used as keys
     * @param isMultilingual       Whether import is multilingual
     * @param relationshipsMap     Map of available relationships
     * @param headerFields         List to store processed header names
     * @param headers              Map to store header-to-field mappings
     * @param keyFields            Map to store key field mappings
     * @param uniqueFields         List of fields marked as unique in the content type
     * @param importableFieldCount Number of fields that can be imported
     * @param relationships        Map to store relationship mappings
     * @param onlyChild            Map for child-only relationships
     * @param onlyParent           Map for parent-only relationships
     * @param user                 User performing the import
     * @param validationBuilder    Builder for validation result
     * @throws Exception if processing encounters errors
     */
    private static void processHeaders(final String[] headerLine,
            final Structure contentType,
            final String[] keyFieldsInodes, boolean isMultilingual,
            final Map<String, Relationship> relationshipsMap, final List<String> headerFields,
            final Map<Integer, Field> headers, final Map<Integer, Field> keyFields,
            final List<Field> uniqueFields, final int importableFieldCount,
            final Map<Integer, Relationship> relationships, final Map<Integer, Boolean> onlyChild,
            final Map<Integer, Boolean> onlyParent, final User user,
            final HeaderValidationResult.Builder validationBuilder) throws Exception {

        List<String> invalidHeaders = new ArrayList<>();
        List<SpecialHeaderInfo> specialHeaders = new ArrayList<>();

        // Process each header
        for (int i = 0; i < headerLine.length; i++) {
            String header = headerLine[i].replaceAll("'", "");
            headerFields.add(header);

            // Handle special headers first
            final var specialHeaderInfo = isSpecialHeader(header, i, isMultilingual);
            if (specialHeaderInfo.isPresent()) {
                specialHeaders.add(specialHeaderInfo.get());
                continue;
            }

            // Process and validate header
            processAndValidateHeader(
                    header, i, contentType, headers, keyFieldsInodes, keyFields,
                    onlyChild, onlyParent, relationshipsMap, relationships,
                    invalidHeaders, isMultilingual, user, validationBuilder);
        }

        // Validate required fields
        final List<String> missingHeaders = validateRequiredFields(headerFields, user, contentType,
                validationBuilder);

        // Validate multilingual requirements if needed
        validateMultilingualHeaders(isMultilingual, headerFields, missingHeaders,
                validationBuilder);

        // Validate key fields and unique fields
        validateKeyFields(keyFieldsInodes, headers, user, validationBuilder);
        printUniqueFieldsWarning(uniqueFields, user, validationBuilder);

        final String[] headersNames = headers.values()
                .stream()
                .map(Field::getVelocityVarName)
                .toArray(String[]::new);

        // Add context information
        Map<String, Object> context = new HashMap<>();
        context.put("importableFields", importableFieldCount);
        context.put("headers", headerLine);
        context.put("keyFields", keyFields);
        context.put("relationships", relationships);
        context.put("onlyChild", onlyChild);
        context.put("onlyParent", onlyParent);

        // Create headerInfo
        final var headerInfo = HeaderInfo.builder()
                .validHeaders(headersNames)
                .invalidHeaders(invalidHeaders.toArray(new String[0]))
                .missingHeaders(missingHeaders.toArray(new String[0]))
                .specialHeaders(specialHeaders)
                .context(context)
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
            throw HeaderValidationException.builder()
                    .message(LanguageUtil.get(user,
                            "No-headers-found-on-the-file-nothing-will-be-imported"))
                    .code(HeaderValidationCodes.HEADERS_NOT_FOUND.name())
                    .build();
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
     * @param onlyChild         Map tracking child-only relationships
     * @param onlyParent        Map tracking parent-only relationships
     * @param relationshipsMap  Map of available relationships
     * @param relationships     Map to populate with relationship mappings
     * @param invalidHeaders    List to store invalid header names
     * @param isMultilingual    Whether import supports multiple languages
     * @param user              User performing the import
     * @param validationBuilder Builder to accumulate validation messages
     * @throws LanguageException If language key lookup fails
     */
    private static void processAndValidateHeader(
            final String header, final int columnIndex, final Structure contentType,
            final Map<Integer, Field> headers, final String[] keyFieldsInodes,
            final Map<Integer, Field> keyFields,
            final Map<Integer, Boolean> onlyChild,
            final Map<Integer, Boolean> onlyParent,
            final Map<String, Relationship> relationshipsMap,
            final Map<Integer, Relationship> relationships,
            final List<String> invalidHeaders,
            final boolean isMultilingual, final User user,
            final HeaderValidationResult.Builder validationBuilder)
            throws LanguageException, DotDataException, DotSecurityException {

        // First try content type fields
        boolean found = processContentTypeField(header, columnIndex, contentType, headers,
                keyFieldsInodes, keyFields, onlyChild, onlyParent, relationshipsMap, relationships,
                user, validationBuilder);

        // Validate if the header is a relationship header
        boolean foundAsRelationship = processRelationshipHeader(header, columnIndex,
                relationshipsMap, relationships,
                onlyChild, onlyParent);

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
     * @param missingHeaders    List of missing header fields
     * @param validationBuilder Builder to accumulate validation messages
     */
    private static void validateMultilingualHeaders(final boolean isMultilingual,
            final List<String> headerFields, final List<String> missingHeaders,
            final HeaderValidationResult.Builder validationBuilder) {

        if (!isMultilingual) {
            return;
        }

        boolean hasLanguageCode = headerFields.contains(languageCodeHeader);
        boolean hasCountryCode = headerFields.contains(countryCodeHeader);

        if (!hasLanguageCode || !hasCountryCode) {

            if (!hasLanguageCode) {
                missingHeaders.add(languageCodeHeader);
            } else {
                missingHeaders.add(countryCodeHeader);
            }

            validationBuilder.addMessages(ValidationMessage.builder()
                    .type(ValidationMessageType.ERROR)
                    .code(HeaderValidationCodes.MISSING_HEADER.name())
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
     * Determines if a given header qualifies as a special header based on certain conditions.
     *
     * @param header         the name of the header to evaluate
     * @param columnIndex    the index of the column where the header is located
     * @param isMultilingual a flag indicating if multilingual support is enabled
     * @return an Optional containing the SpecialHeaderInfo object if the header is special;
     * otherwise, an empty Optional
     */
    private static Optional<SpecialHeaderInfo> isSpecialHeader(final String header,
            final int columnIndex,
            final boolean isMultilingual) {

        if (header.equalsIgnoreCase(identifierHeader)
                || header.equalsIgnoreCase(Contentlet.WORKFLOW_ACTION_KEY)
                || (isLanguageHeader(header) && isMultilingual)) {

            return Optional.of(SpecialHeaderInfo.builder()
                    .header(header)
                    .columnIndex(columnIndex)
                    .build());
        }

        return Optional.empty();
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
            final Structure contentType, final Map<Integer, Field> headers,
            final String[] keyFieldsInodes, final Map<Integer, Field> keyFields,
            final Map<Integer, Boolean> onlyChild, final Map<Integer, Boolean> onlyParent,
            final Map<String, Relationship> relationshipsMap,
            final Map<Integer, Relationship> relationships, final User user,
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
            final Map<Integer, Relationship> relationships,
            final Map<Integer, Boolean> onlyChild, final Map<Integer, Boolean> onlyParent) {

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
            final Map<Integer, Relationship> relationships,
            final Map<Integer, Boolean> onlyParent,
            final Map<Integer, Boolean> onlyChild)
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
        return header.equalsIgnoreCase(languageCodeHeader)
                || header.equalsIgnoreCase(countryCodeHeader);
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
            final Map<Integer, Field> headers, final User user,
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
     * require unique values. Creates a single consolidated warning message for all unique fields.
     *
     * @param uniqueFields      List of fields marked as unique
     * @param user              User performing the import (unused, kept for compatibility)
     * @param validationBuilder Builder to accumulate validation messages
     */
    private static void printUniqueFieldsWarning(final List<Field> uniqueFields, final User user,
            final HeaderValidationResult.Builder validationBuilder) {
        if (uniqueFields.isEmpty()) {
            return;
        }

        // Create comma-separated list of unique field names
        String fieldNames = uniqueFields.stream()
                .map(Field::getVelocityVarName)
                .collect(Collectors.joining(", "));

        validationBuilder.addMessages(
                ValidationMessage.builder()
                        .type(ValidationMessageType.WARNING)
                        .code(HeaderValidationCodes.UNIQUE_FIELD.name())
                        .field(fieldNames) // Comma-separated list of unique fields
                        .lineNumber(1)
                        .message("There are unique fields in this Content Type. Duplicate values are rejected during import.")
                        .build()
        );
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
     * @param relationshipsMap     Map of relationships found
     * @param user                 User performing the import
     * @param validationBuilder    Builder to accumulate validation messages
     * @throws LanguageException If language key lookup fails
     */
    private static void addHeadersSummaryMessages(final int headerCount,
            final int importableFieldCount,
            final Map<String, Relationship> relationshipsMap, final User user,
            final HeaderValidationResult.Builder validationBuilder) throws LanguageException {

        if (headerCount != importableFieldCount) {
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
                                .type(ValidationMessageType.ERROR)
                                .code(HeaderValidationCodes.HEADERS_NOT_FOUND.name())
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

        if (!relationshipsMap.isEmpty()) {
            StringBuilder relationships = new StringBuilder();
            final int count = relationshipsMap.size();
            final Iterator<Entry<String, Relationship>> iterator = relationshipsMap.entrySet()
                    .iterator();
            while (iterator.hasNext()) {
                final Entry<String, Relationship> entry = iterator.next();
                final Relationship value = entry.getValue();
                if (value != null) {
                    relationships.append(value.getRelationTypeValue());
                }
                if (iterator.hasNext()) {
                    relationships.append(", ");
                }
            }
            validationBuilder.addMessages(
                    ValidationMessage.builder()
                            .type(ValidationMessageType.INFO)
                            .message(count + " Relationship field" + (count > 1 ? "(s) were" : "was") + " found and will be used for import." )
                            .field(relationships.toString())
                            .build()
            );
        }
    }

    /**
     * Processes a single line from the CSV import file. This method handles all aspects of
     * importing a content line including:
     * <ul>
     *   <li>Field validation and processing</li>
     *   <li>Content matching and updates</li>
     *   <li>Relationship processing</li>
     *   <li>Workflow execution</li>
     *   <li>Category handling</li>
     * </ul>
     *
     * @param line                   CSV line data as string array
     * @param currentHostId          ID of the current host/site
     * @param contentType            Content type structure definition
     * @param preview                If true, validates without saving changes
     * @param isMultilingual         If true, handles multilingual content
     * @param user                   User performing the import
     * @param identifier             Optional identifier for content updates
     * @param wfActionIdIndex        Index of workflow action ID in CSV, -1 if none
     * @param lineNumber             Current line number in CSV file
     * @param language               Language ID for the content
     * @param headers                Map of column indices to field definitions
     * @param keyFields              Map of key fields used for content matching
     * @param chosenKeyFields        Buffer tracking chosen key fields
     * @param keyContentUpdated      Set tracking updated content keys
     * @param contentTypePermissions Content type permissions
     * @param uniqueFieldBeans       List tracking unique field values
     * @param uniqueFields           List of fields marked as unique
     * @param relationships          Map of relationship definitions
     * @param onlyChild              Map tracking child-only relationships
     * @param onlyParent             Map tracking parent-only relationships
     * @param sameKeyBatchInsert     True if batch contains multiple rows with same key
     * @param wfActionId             Workflow action ID to execute
     * @param request                HTTP request context
     * @param resultBuilder          Builder to accumulate import results
     * @throws DotRuntimeException If a critical error occurs during import
     */
    private static void importLine(
            final String[] line,
            final String currentHostId,
            final Structure contentType,
            final boolean preview,
            final boolean stopOnError,
            boolean isMultilingual,
            final User user,
            final String identifier,
            final int wfActionIdIndex,
            final int lineNumber,
            final long language,
            final Map<Integer, Field> headers,
            final Map<Integer, Field> keyFields,
            final Set<String> chosenKeyFields,
            final HashSet<String> keyContentUpdated,
            final List<Permission> contentTypePermissions,
            final List<UniqueFieldBean> uniqueFieldBeans,
            final List<Field> uniqueFields,
            final Map<Integer, Relationship> relationships,
            final Map<Integer, Boolean> onlyChild,
            final Map<Integer, Boolean> onlyParent,
            final boolean sameKeyBatchInsert,
            final String wfActionId,
            final HttpServletRequest request,
            final LineImportResultBuilder resultBuilder
    ) throws Exception {

        // First validate line length
        validateLineLength(line, headers, lineNumber);

        // Process fields and collect values
        final FieldsProcessingResult fieldResults = processFields(
                line, headers, contentType, user, currentHostId, language, lineNumber
        );

        fieldResults.messages().forEach(resultBuilder::addValidationMessage);
        fieldResults.categories().forEach(resultBuilder::addCategory);
        uniqueFieldBeans.addAll(fieldResults.uniqueFields());

        final Map<Integer, Object> values = new HashMap<>(fieldResults.values());
        final Set<Category> categories = new HashSet<>(fieldResults.categories());

        if (fieldResults.ignoreLine()) {
            resultBuilder.setIgnoreLine(true);
            return;
        }
        //Check if line has repeated values for a unique field, if it does then ignore the line
        if (!uniqueFieldBeans.isEmpty()) {
            final boolean ignoreLine = validateUniqueFields(user, lineNumber, language, stopOnError,
                    uniqueFieldBeans, uniqueFields, resultBuilder);
            if (ignoreLine && !stopOnError) { //Do not ignore the line if the stopOnError flag is on so an exception will be thrown
                resultBuilder.setIgnoreLine(true);
                return;
            }
        }

        Long existingMultilingualLanguage = null;

        Pair<Host, Folder> siteAndFolder = fieldResults.siteAndFolder().orElse(null);
        Pair<Integer, String> urlValue = fieldResults.urlValue().orElse(null);
        String urlValueAssetName = fieldResults.urlValueAssetName().orElse(null);

        // Initialize relationship maps
        final Map<Relationship, List<Contentlet>> csvRelationshipRecordsParentOnly = new HashMap<>();
        final Map<Relationship, List<Contentlet>> csvRelationshipRecordsChildOnly = new HashMap<>();
        final Map<Relationship, List<Contentlet>> csvRelationshipRecords = new HashMap<>();

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
        final var searchResult = searchExistingContentlets(
                contentType, values, keyFields, siteAndFolder, urlValue,
                urlValueAssetName, identifier, preview, sameKeyBatchInsert, isMultilingual,
                language, lineNumber, chosenKeyFields, user);
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
            resultBuilder.incrementContentToCreate();
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
                            resultBuilder.incrementContentToCreate();
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

            if (!resultBuilder.isNewContent()) {
                if (conditionValues.isEmpty()
                        || !keyContentUpdated.contains(conditionValues)
                        || isMultilingual) {
                    resultBuilder.incrementContentToUpdate(contentlets.size());
                    if (preview) {
                        keyContentUpdated.add(conditionValues);
                    }
                }

                Logger.debug(ImportUtil.class, "Contentlets size: " + contentlets.size());
                if (contentlets.size() > 1) {
                    resultBuilder.addValidationMessage(ValidationMessage.builder()
                            .type(ValidationMessageType.WARNING)
                            .code(ImportLineValidationCodes.DUPLICATE_UNIQUE_VALUE.name())
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
         //Here we save the contentlet(s) and process the results
        final ProcessedContentResult processResult = processContent(
                lineNumber,
                contentlets,
                resultBuilder.isNewContent(),
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
                currentHostId,
                user,
                request,
                line
        );
        // Update builder with process results
        processResult.messages().forEach(resultBuilder::addValidationMessage);
        processResult.savedInodes().forEach(resultBuilder::addSavedInode);
        resultBuilder.setLastInode(processResult.lastInode());
        resultBuilder.incrementContentToCreate(processResult.contentToCreate());
        resultBuilder.incrementCreatedContent(processResult.createdContent());
        resultBuilder.incrementContentToUpdate(processResult.contentToUpdate());
        resultBuilder.incrementUpdatedContent(processResult.updatedContent());
        resultBuilder.incrementDuplicateContent(processResult.duplicateContent());
    }

    /**
     * Validates that a CSV line contains enough columns to match all defined headers. This method
     * ensures data integrity by checking that no column references will be out of bounds when
     * processing the line.
     *
     * @param line       The array of values from the CSV line
     * @param headers    Map of column indices to their corresponding field definitions
     * @param lineNumber The current line number in the CSV file, used for error reporting
     * @throws ImportLineException If the line contains fewer columns than required by the headers.
     *                             The exception includes the line number for error tracking.
     */
    private static void validateLineLength(final String[] line, final Map<Integer, Field> headers,
            final int lineNumber) {
        if (line.length < headers.keySet().stream().mapToInt(i -> i).max().orElse(0)) {
            throw ImportLineException.builder()
                    .message("Doesn't contain all the required columns")
                    .code(ImportLineValidationCodes.INCOMPLETE_LINE.name())
                    .lineNumber(lineNumber)
                    .build();
        }
    }

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
     * @return FieldsProcessingResult containing processed values, validation messages, and
     * and additional field data
     * @throws DotDataException     If a data access error occurs during processing
     * @throws DotSecurityException If a security violation occurs during processing
     */
    private static FieldsProcessingResult processFields(
            final String[] line,
            final Map<Integer, Field> headers,
            final Structure contentType,
            final User user,
            final String currentHostId,
            final long language,
            final int lineNumber
    ) throws DotDataException, DotSecurityException {

        final var results = new FieldsProcessingResultBuilder(lineNumber);

        for (Integer column : headers.keySet()) {

            final Field field = headers.get(column);
            final String value = line[column];

            try {
                final var fieldResult = processField(field, value, user, currentHostId,
                        language, lineNumber, column);

                if (fieldResult.siteAndFolder().isPresent()) {
                    results.setSiteAndFolder(fieldResult.siteAndFolder().get());
                }

                if (fieldResult.urlValue().isPresent()) {
                    results.setUrlValue(fieldResult.urlValue().get());
                }

                fieldResult.categories().forEach(results::addCategory);

                if (fieldResult.value().isPresent()) {
                    results.addValue(column, fieldResult.value().get());
                }

                if (fieldResult.uniqueField().isPresent()) {
                    results.addUniqueField(fieldResult.uniqueField().get());
                }

                fieldResult.messages().forEach(results::addValidationMessage);

            } catch (Exception e) {
                Logger.debug(ImportUtil.class, "Error processing field, line number: " + lineNumber, e);
                throw e;
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
                    throw ImportLineException.builder()
                            .message("Invalid parent folder for URL")
                            .code(ImportLineValidationCodes.INVALID_URL_FOLDER.name())
                            .lineNumber(lineNumber)
                            .field(HTMLPageAssetAPI.URL_FIELD)
                            .invalidValue(results.urlValue.getRight())
                            .build();
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
            final int column
    ) throws DotDataException, DotSecurityException {

        final var results = new FieldProcessingResultBuilder(lineNumber);
        Object processedValue;

        if (isDateField(field)) {
            processedValue = validateDateTypes(field, value, value);
        } else if (isCategoryField(field)) {
            Set<Category> categories = validateCategoryField(field, value, user, results);
            processedValue = categories;
            results.addCategories(categories);
        } else if (isSelectionField(field)) {
            processedValue = validateSelectionField(field, value);
        } else if (isTextField(field)) {
            processedValue = validateTextField(value);
        } else if (isTextAreaField(field)) {
            processedValue = value;
        } else if (isLocationField(field)) {
            Pair<Host, Folder> location = validateLocationField(field, value, user);
            processedValue = value;
            results.setSiteAndFolder(location);
        } else if (isBinaryField(field)) {
            //Binaries are loaded from an external source
            processedValue = validateBinaryField(field, value);
        } else if (isFileField(field)) {
            // Files fields are images or file fields, they can be loaded from an external source or referenced by an internal path
            processedValue = validateFileField(field, value, currentHostId, user);
        } else {
            processedValue = processDefaultField(value);
        }

        results.setValue(processedValue);

        if (field.isUnique()) {
            results.setUniqueField(handleUniqueField(field, processedValue, language, lineNumber));
        }

        if (isUrlField(field, processedValue)) {
            results.setUrlValue(processUrlValue(column, processedValue));
        }

        return results.build();
    }

    /**
     * Formats a date value using the specified pattern.
     *
     * @param value   The date value to format
     * @param pattern The format pattern to apply
     * @return The formatted date string
     */
    private static String formatDate(final Date value, final String pattern) {
        DateFormat df = new SimpleDateFormat(pattern);
        return df.format(value);
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
     * category keys into a set of Category objects.
     * Each category must:
     * <ul>
     *   <li>Exist in the system</li>
     *   <li>Be a descendant of the configured root category (i.e. within the allowed hierarchy)</li>
     * </ul>
     * If any category key is invalid, an error is
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
    private static Set<Category> validateCategoryField(
            final Field field,
            final String value,
            final User user,
            final FieldProcessingResultBuilder resultBuilder
    ) throws DotDataException, DotSecurityException {

        Set<Category> categories = new HashSet<>();
        if (!UtilMethods.isSet(value)) {
            return categories;
        }

        final Category configuredRootCategory = findConfiguredRootCategory(field, user);

        // Defensive check — should not normally occur
        if (configuredRootCategory == null) {
            throw ImportLineException.builder()
                    .message(String.format(
                            "Root category configured for field '%s' could not be found. Please check the field configuration.",
                            field.getVelocityVarName()
                    ))
                    .code(ImportLineValidationCodes.INVALID_CATEGORY_KEY.name())
                    .field(field.getVelocityVarName())
                    .invalidValue(value)
                    .build();
        }

        String[] categoryKeys = value.split(",");
        for (String catKey : categoryKeys) {
            String key = catKey.trim();
            Category cat = validateCategoryKey(key, configuredRootCategory, field, user);
            categories.add(cat);
            resultBuilder.addCategory(cat);
        }

        return categories;
    }


    /**
     * Given a field previously determined to be of type Category, this method retrieves
     * the configured root category associated with that field.
     * <p>
     * The root category defines the top-level constraint under which all assigned
     * categories must reside. If no such category is found, the method returns null.
     *
     * @param categoryField the field whose associated root category is to be retrieved
     * @param user          the user performing the operation (for permission checks)
     * @return the configured root Category for the field, or null if not found or inaccessible
     */
    private static Category findConfiguredRootCategory(final Field categoryField, final User user) {
        Category category = null;
        try {
            category = catAPI.find(categoryField.getValues(), user, false);
        } catch (final DotSecurityException | DotDataException e) {
            Logger.error(ImportUtil.class, String.format(
                    "User '%s' couldn't get the configured root Category from field '%s': %s",
                    user != null ? user.getUserId() : null,
                    categoryField.getCategoryId(),
                    e.getMessage()), e);
        }
        return category;
    }


    /**
     * Validates a single category key against the configured root category for a field.
     * <p>
     * This method ensures that:
     * <ul>
     *   <li>The category identified by the given key exists in the system</li>
     *   <li>The category is a descendant (child, grandchild, etc.) of the specified root category</li>
     * </ul>
     * If the key is invalid, an {@link ImportLineException} is thrown with context for debugging.
     *
     * @param key           the raw category key to validate
     * @param rootCategory  the root category that all valid categories must descend from
     * @param field         the field definition this key is associated with (used for error reporting)
     * @param user          the user performing the operation (used for permission checks)
     * @return the valid {@link Category} object corresponding to the key
     * @throws DotDataException     if a data access error occurs
     * @throws DotSecurityException if a security or permission error occurs
     * @throws ImportLineException  if the category does not exist or is not under the root category
     */
    private static Category validateCategoryKey(final String key, final Category rootCategory, final Field field, final User user) throws DotDataException, DotSecurityException {
        Category cat = catAPI.findByKey(key, user, false);
        if (cat == null || !catAPI.isParent(cat, rootCategory, user)) {
            throw ImportLineException.builder()
                    .message(String.format(
                            "Invalid category key found: '%s'. It must exist and be a child of '%s'.",
                            key, rootCategory.getCategoryName()
                    ))
                    .code(ImportLineValidationCodes.INVALID_CATEGORY_KEY.name())
                    .field(field.getVelocityVarName())
                    .invalidValue(key)
                    .build();
        }
        return cat;
    }

    /**
     * Processes a text field by truncating its value to 255 characters if necessary.
     *
     * @param value the value to process
     * @return the processed value, truncated to 255 characters if necessary
     */
    private static Object validateTextField(final String value) {
        if (value != null && value.length() > TEXT_FIELD_MAX_LENGTH) {
            return value.substring(0, TEXT_FIELD_MAX_LENGTH);
        }
        return value;
    }

    /**
     * Processes a location field by validating and converting the value to a Pair of Host and
     * Folder.
     *
     * @param field the field definition containing type and validation rules
     * @param value the raw value from the CSV line
     * @param user  the user performing the import
     * @return a Pair of Host and Folder corresponding to the valid location
     * @throws DotDataException     if a data access error occurs during processing
     * @throws DotSecurityException if a security violation occurs during processing
     */
    private static Pair<Host, Folder> validateLocationField(final Field field, final String value,
            final User user) throws DotDataException, DotSecurityException {

        Pair<Host, Folder> siteAndFolder = getSiteAndFolderFromIdOrName(value, user);
        if (siteAndFolder == null) {
            throw ImportLineException.builder()
                    .message("The provided inode/path does not exist or is not associated with a valid Site or Folder.")
                    .code(ImportLineValidationCodes.INVALID_SITE_FOLDER_REF.name())
                    .field(field.getVelocityVarName())
                    //Add context here
                    .context(Map.of("errorHint", "The value must be a valid site-name folder path or their respective inodes."))
                    .invalidValue(value)
                    .build();
        }

        return siteAndFolder;
    }

    /**
     * Processes a binary field by validating the URL.
     *
     * @param value the value to process
     * @return the processed value if the URL is valid
     * @throws ImportLineException if the URL is invalid
     */
    private static Object validateBinaryField(final Field field, final String value) {
        if (UtilMethods.isNotSet(value)) {
            // If the value is not set, return as REQUIRED_FIELD_MISSING is handled by contentlet checkin
            return value;
        }
        
        //Here we need to throw an exception if the value is not set and the value is required
        final boolean validURL = UtilMethods.isValidStrictURL(value);
        if(!validURL) {
            
            // If the value is not a valid URL, we throw an exception
            throw ImportLineException.builder()
                    .message("The provided value is not a syntactically valid URL")
                    .code(ImportLineValidationCodes.INVALID_BINARY_URL.name())
                    .field(field.getVelocityVarName())
                    .invalidValue(value)
                    .context(Map.of("errorHint","invalid URL format."))
                    .build();
        }
        if (UtilMethods.isSet(value)) {
            final boolean validUrl = Try.of(()->tempFileAPI.validUrl(value)).getOrElse(false);
            if (!validUrl) {
                
                throw ImportLineException.builder()
                        .message("URL is syntactically valid but returned a non-success HTTP response")
                        .code(ImportLineValidationCodes.UNREACHABLE_URL_CONTENT.name())
                        .field(field.getVelocityVarName())
                        .invalidValue(value)
                        .context(Map.of(
                                "errorHint", "The server responded with an error (e.g. 4xx or 5xx). " +
                                   "This may indicate the resource was not found, access was denied, " +
                                   "or the server is unavailable."
                        ))
                        .build();
            }
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
     * @return the contentlet identifier if the file is valid, null otherwise
     * @throws DotDataException     if a data access error occurs during processing
     * @throws DotSecurityException if a security violation occurs during processing
     */
    private static Object validateFileField(final Field field, final String value,
            final String currentHostId, final User user
    ) throws DotDataException, DotSecurityException {
        if(UtilMethods.isNotSet(value)) {
            // If the value is not set return as REQUIRED_FIELD_MISSING is handled by contentlet checkin
           return value;
        }
        //Here we need to determine if the value is a valid internal file path or an external URL
        final boolean dotCMSPath = UtilMethods.isValidDotCMSPath(value);
        if (dotCMSPath) {
            final Optional<String> internal = matchWithInternalIdentifier(value, currentHostId, user);
            if (internal.isPresent()) {
                // We found a matching object
                return internal.get();
            } else {
               //Throw validation error as failed to match the given path with an internal object
                throw ImportLineException.builder()
                        .message("Unable to match the given path with a file stored in dotCMS")
                        .code(ImportLineValidationCodes.INVALID_FILE_PATH.name())
                        .field(field.getVelocityVarName())
                        .context(Map.of("errorHint","The provided value must be a valid file /folder/file path in dotCMS or an external URL."))
                        .invalidValue(value)
                        .build();
            }
        } else {
            final boolean validURL = UtilMethods.isValidStrictURL(value);
            if(!validURL) {
                // If the value is not a valid URL, we throw an exception
                throw ImportLineException.builder()
                        .message("The provided value is not a syntactically valid URL nor a valid dotCMS path")
                        .code(ImportLineValidationCodes.INVALID_BINARY_URL.name())
                        .field(field.getVelocityVarName())
                        .context(Map.of("errorHint","The provided value must be a valid URL or a valid dotCMS path."))
                        .invalidValue(value)
                        .build();
            }
            // If it's a URL, we need to validate if we can access it
            if (!tempFileAPI.validUrl(value)) {
                throw ImportLineException.builder()
                        .message("URL is syntactically valid but returned a non-success HTTP response")
                        .code(ImportLineValidationCodes.UNREACHABLE_URL_CONTENT.name())
                        .field(field.getVelocityVarName())
                        .context(Map.of("errorHint","There's a problem accessing the content at the provided URL."))
                        .invalidValue(value)
                        .build();
            }

            // if the URL is valid, we can return it as is
            return value;
        }
    }

    /**
     * Matches a given value with an internal identifier.
     * @param value the value to match, which can be a file path or URL
     * @param currentHostId the ID of the current host
     * @param user the user performing the import
     * @return an Optional containing the contentlet identifier if a match is found,
     * @throws DotDataException if a data access error occurs during processing
     * @throws DotSecurityException if a security violation occurs during processing
     */
    private static Optional<String> matchWithInternalIdentifier(final String value, final String currentHostId, final User user) throws DotDataException, DotSecurityException {
        //Here we need to determine if the value is a valid file path or URL
        String filePath = value;
        Host fileHost = hostAPI.find(currentHostId, user, false);
        if (filePath.contains(StringPool.COLON)) {
            String[] fileInfo = filePath.split(StringPool.COLON);
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
                return Optional.of(cont.getIdentifier());
            }
        }
        return Optional.empty();
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
     * @param field      the field definition containing type and validation rules
     * @param value      the value to process
     * @param language   the language ID for the content
     * @param lineNumber the line number in the CSV file
     */
    private static UniqueFieldBean handleUniqueField(final Field field, final Object value,
            final long language, final int lineNumber) {
        return UniqueFieldBean.builder().
                field(field).
                value(value).
                lineNumber(lineNumber).
                languageId(language).build();
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
    private static Object validateSelectionField(final Field field, final String value) {
        if (UtilMethods.isSet(value)) {
            String fieldEntriesString = field.getValues() != null ? field.getValues() : "";
            String[] fieldEntries = fieldEntriesString.split("\n");

            for (String fieldEntry : fieldEntries) {
                String[] splitValue = fieldEntry.split("\\|");
                String entryValue = splitValue[splitValue.length - 1].trim();

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
                                            "'%s'[%s]: %s", relationship.getRelationTypeValue(),
                                    relationship.getInode(), e
                                            .getMessage()), e);
                } else {
                    Logger.warn(ImportUtil.class,
                            String.format("A null relationship in column '%s' was found",
                                    column));
                }
                String structureDoesNoMatchMessage = LanguageUtil.get(user,
                        "the-structure-does-not-match-the-relationship");
                builder.addWarning(structureDoesNoMatchMessage,
                        ImportLineValidationCodes.RELATIONSHIP_VALIDATION_ERROR.name());
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
     * @param chosenKeyFields      Buffer tracking user-selected key fields
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
     */
    private static ContentletSearchResult searchExistingContentlets(
            final Structure contentType,
            final Map<Integer, Object> values,
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
            final Set<String> chosenKeyFields,
            final User user
    ) throws DotDataException, DotSecurityException {

        final var builder = ContentletSearchResult.builder();
        final List<Contentlet> contentlets = new ArrayList<>();
        final List<String> updatedInodes = new ArrayList<>();
        String conditionValues = "";
        boolean isMultilingualResult = isMultilingual;

        if (UtilMethods.isSet(identifier)) {
            contentlets.addAll(
                    searchByIdentifierFromDB(identifier, contentType, user));
        } else if (urlValue != null && keyFields.isEmpty()) {
            // For HTMLPageAsset, we need to search by URL to math existing pages
            contentlets.addAll(searchByUrl(contentType, urlValue, siteAndFolder, language, user));
        } else if (!keyFields.isEmpty()) {
            // Search by key fields
            SearchByKeyFieldsResult keyFieldResults = searchByKeyFields(
                    contentType, values, keyFields, siteAndFolder, urlValueAssetName, preview,
                    sameKeyBatchInsert, language, isMultilingual, user, lineNumber, chosenKeyFields,
                    builder);
            contentlets.addAll(keyFieldResults.contentlets);
            conditionValues = keyFieldResults.conditionValues;
            isMultilingualResult = keyFieldResults.isMultilingual;
            updatedInodes.addAll(keyFieldResults.updatedInodes);
        }

        return builder
                .contentlets(contentlets)
                .updatedInodes(updatedInodes)
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
     * @return A list of contentlets that match the given identifier. Returns an empty list if no
     * contentlet is found.
     * @throws DotDataException     If there is a data-related exception during the search process.
     * @throws DotSecurityException If the user does not have permission to access the requested
     *                              contentlet.
     */
    private static List<Contentlet> searchByIdentifierFromDB(
            final String identifier,
            final Structure contentType,
            final User user
    ) throws DotDataException, DotSecurityException {

        final Contentlet contentlet = new Contentlet(Map.of(
                IDENTIFIER_KEY, identifier,
                STRUCTURE_INODE_KEY, contentType.getInode()
        ));

        final List<Contentlet> allLanguages = conAPI.getAllLanguages(contentlet, false, user, true);
        if (allLanguages == null || allLanguages.isEmpty()) {
            throw ImportLineException.builder()
                    .message("Content not found with identifier")
                    .code(ImportLineValidationCodes.CONTENT_NOT_FOUND.name())
                    .invalidValue(identifier)
                    .build();
        }

        return allLanguages.stream().map(Contentlet::new).collect(Collectors.toList());
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
     * @param chosenKeyFields    A string buffer to store the chosen key field.
     * @param builder            A builder object used to accumulate search results and messages.
     * @return A container holding the search results including contentlets, updated inodes,
     * condition values, and multilingual status.
     * @throws DotDataException     If there is a data-related exception during the search process.
     * @throws DotSecurityException If the user does not have permission to access the requested
     *                              contentlet.
     */
    private static SearchByKeyFieldsResult searchByKeyFields(
            final Structure contentType,
            final Map<Integer, Object> values,
            final Map<Integer, Field> keyFields,
            final Pair<Host, Folder> siteAndFolder,
            final String urlValueAssetName,
            final boolean preview,
            final boolean sameKeyBatchInsert,
            final long language,
            final boolean isMultilingual,
            final User user,
            final int lineNumber,
            final Set<String> chosenKeyFields,
            final ContentletSearchResult.Builder builder
    ) throws DotDataException, DotSecurityException {

        StringBuilder query = new StringBuilder()
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
                throw ImportLineException.builder()
                        .message("Key field " + field.getVelocityVarName()
                                + " is required since it was defined as a key")
                        .code(ImportLineValidationCodes.MISSING_KEY_FIELD.name())
                        .lineNumber(lineNumber)
                        .build();
            }

            String processedValue = value.toString();
            if (value instanceof Date || value instanceof Timestamp) {
                SimpleDateFormat formatter;
                if (field.getFieldType().equals(Field.FieldType.DATE.toString())) {
                    processedValue = formatDate((Date) value, DATE_FIELD_FORMAT_PATTERN);
                } else if (field.getFieldType().equals(Field.FieldType.DATE_TIME.toString())) {
                    processedValue = formatDate((Date) value, DATE_TIME_FIELD_FORMAT_PATTERN);
                } else if (field.getFieldType().equals(Field.FieldType.TIME.toString())) {
                    processedValue = formatDate((Date) value, TIME_FIELD_FORMAT_PATTERN);
                } else {
                    formatter = new SimpleDateFormat();
                    processedValue = formatter.format(value);
                    builder.addMessages(ValidationMessage.builder()
                            .type(ValidationMessageType.WARNING)
                            .message("The date format for field " + field.getVelocityVarName()
                                    + " is undetermined")
                            .code(ImportLineValidationCodes.INVALID_DATE_FORMAT.name())
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
                chosenKeyFields.add(field.getVelocityVarName());
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

        // We need to handle the case when keys are used, we could have a contentlet already saved
        // with the same keys but different language so the above query is not going to find it.
        if (contentsSearch == null || contentsSearch.isEmpty()) {
            if (chosenKeyFields.size() > 1) {
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

                        if (new LegacyFieldTransformer(field).from() instanceof HostFolderField) {
                            final Pair<Host, Folder> siteOrFolder = getSiteAndFolderFromIdOrName((String) value, user);
                            final String valueAsHostId = siteOrFolder != null ? siteOrFolder.getLeft().getIdentifier() : null;
                            final String valueAsFolderId = siteOrFolder != null ? siteOrFolder.getRight().getIdentifier() : null;

                            // Check if either host or folder matches
                            if (!conValue.equals(valueAsHostId) && !conValue.equals(valueAsFolderId)) {
                                match = false;
                            }
                        } else if (!conValue.equals(value)) {
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
            final User user
    ) throws DotDataException, DotSecurityException {

        StringBuilder query = new StringBuilder()
                .append("+structureName:").append(contentType.getVelocityVarName())
                .append(" +working:true +deleted:false")
                .append(" +languageId:").append(language)
                .append(addSiteAndFolderToESQuery(siteAndFolder, null));

        query.append(" +").append(contentType.getVelocityVarName()).append(StringPool.PERIOD)
                .append(HTMLPageAssetAPI.URL_FIELD).append(StringPool.COLON)
                .append(urlValue.getRight());

        List<ContentletSearch> contentsSearch = conAPI.searchIndex(
                query.toString(), 0, -1, null, user, true);

        if (contentsSearch != null && !contentsSearch.isEmpty()) {
            return convertSearchResults(contentsSearch, user);
        }

        return Collections.emptyList();
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
            String conValueStr = formatDate((Date) conValue, TIME_FIELD_FORMAT_PATTERN);
            String valueStr = formatDate((Date) result, TIME_FIELD_FORMAT_PATTERN);
            return conValueStr.equals(valueStr);
        } else if (field.getFieldType().equals(Field.FieldType.DATE.toString())) {
            String valueStr = formatDate((Date) result, DATE_FIELD_FORMAT_PATTERN);
            String conValueStr = formatDate((Date) conValue, DATE_FIELD_FORMAT_PATTERN);
            return conValueStr.equals(valueStr);
        } else {
            if (conValue instanceof java.sql.Timestamp) {
                result = new java.sql.Timestamp(((Date) result).getTime());
            } else if (conValue instanceof Date) {
                result = formatDate((Date) result, DATE_TIME_FIELD_FORMAT_PATTERN);
            }
            return conValue.equals(result);
        }
    }

    /**
     * Converts search results from {@link ContentletSearch} objects to {@link Contentlet} objects.
     *
     * @param contentsSearch The list of {@link ContentletSearch} objects returned by the search.
     * @param user           The user performing the search, used for security validation.
     * @return A list of {@link Contentlet} objects that match the search criteria.
     */
    private static List<Contentlet> convertSearchResults(final List<ContentletSearch> contentsSearch,
            final User user) {

        if (contentsSearch == null || contentsSearch.isEmpty()) {
            return Collections.emptyList();
        }

        return contentsSearch.stream()
                .map(contentSearch -> {
                    try {
                        return conAPI.find(contentSearch.getInode(), user, true);
                    } catch (Exception e) {
                        Logger.warn(ImportUtil.class, "Error finding content by inode", e);
                        throw ImportLineException.builder()
                                .message("Content not found with identifier")
                                .code(ImportLineValidationCodes.CONTENT_NOT_FOUND.name())
                                .invalidValue(contentSearch.getIdentifier())
                                .build();
                    }
                })
                .filter(Objects::nonNull)
                .filter(contentlet -> InodeUtils.isSet(contentlet.getInode()))
                .collect(Collectors.toList());
    }

    private static String getIdentifierFromResults(final int identifierFieldIndex,
            final String[] line) {
        try {
            return identifierFieldIndex >= 0 ? line[identifierFieldIndex] : null;
        } catch (Exception e) {
            Logger.debug(ImportUtil.class, "No identifier field found", e);
            return null;
        }
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
            final Map<Relationship, List<Contentlet>> csvRelationshipRecordsParentOnly,
            final Map<Relationship, List<Contentlet>> csvRelationshipRecordsChildOnly,
            final Map<Relationship, List<Contentlet>> csvRelationshipRecords,
            final Map<Integer, Field> headers,
            final Set<Category> categories,
            final String conditionValues,
            final HashSet<String> keyContentUpdated,
            final List<Permission> contentTypePermissions,
            final int wfActionIdIndex,
            final boolean preview,
            final String siteId,
            final User user,
            final HttpServletRequest request,
            final String[] line
    ) throws DotDataException, DotSecurityException, IOException, LanguageException {

        final ProcessedContentResultBuilder resultBuilder = new ProcessedContentResultBuilder();

        for (Contentlet cont : contentlets) {

            setWorkflowAction(wfActionIdIndex, line, cont);

            // Set site and folder
            setSiteAndFolder(user, cont, siteAndFolder);

            // Set field values
            processContentFields(cont, headers, values, request, siteId, user, preview);

            // Retaining Categories when content updated with partial imports
            if (UtilMethods.isSet(cont.getIdentifier())) {
                retainExistingCategories(cont, headers, categories, existingLanguage, user);
            }

            // Validate the contentlet, it routes to the appropriate validation strategy weather or not it might have relationships
            validateContentlet(
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
            updateCounters(isNew, conditionValues, keyContentUpdated, resultBuilder);
        }

        return resultBuilder.build();
    }

    /**
     * Sets the workflow action ID for a contentlet based on the provided index and line data.
     * @param wfActionIdIndex The index of the workflow action ID in the line data.
     * @param line
     * @param cont
     */
    private static void setWorkflowAction(int wfActionIdIndex, String[] line, Contentlet cont) {
        //Clean up any existing workflow action
        cont.resetActionId();
        // Handle workflow action ID from file
        if (wfActionIdIndex >= 0) {
            String wfActionIdStr = line[wfActionIdIndex];
            if (UtilMethods.isSet(wfActionIdStr)) {
                cont.setActionId(wfActionIdStr);
            }
        }
    }

    /**
     * Validates a contentlet and its fields using the appropriate validation strategy.
     * If relationship fields are defined in the headers, a comprehensive validation is performed
     * that includes relationship validation. Otherwise, a standard validation without
     * relationship checks is applied.
     *
     * @param headers Map of column positions to their corresponding field definitions
     * @param categories Set of categories associated with this contentlet
     * @param cont The contentlet to validate
     * @param csvRelationshipRecordsParentOnly Map of parent-only relationships
     * @param csvRelationshipRecordsChildOnly Map of child-only relationships
     * @param csvRelationshipRecords Map of bidirectional relationships
     * @throws DotDataException If validation fails or other data access issues occur
     */
    private static void validateContentlet(
            final Map<Integer, Field> headers,
            final Set<Category> categories,
            final Contentlet cont,
            final Map<Relationship, List<Contentlet>> csvRelationshipRecordsParentOnly,
            final Map<Relationship, List<Contentlet>> csvRelationshipRecordsChildOnly,
            final Map<Relationship, List<Contentlet>> csvRelationshipRecords
    ) throws DotDataException {

        //Check the new contentlet with the validator
        final boolean hasRelationships = headers.values().stream()
                .anyMatch((field -> field.getFieldType()
                        .equals(FieldType.RELATIONSHIP.toString())));
            //if we have relationships, we need to validate them
            if (hasRelationships) {
                ContentletRelationships contentletRelationships = loadRelationshipRecords(
                        csvRelationshipRecordsParentOnly, csvRelationshipRecordsChildOnly,
                        csvRelationshipRecords, cont);

                conAPI.validateContentlet(cont, contentletRelationships,
                        new ArrayList<>(categories), true);
            } else {
                //Otherwise, we call standard validation
                conAPI.validateContentlet(cont, null, new ArrayList<>(categories), true);
            }

    }

    /**
     * Processes and sets field values for a given Contentlet.
     *
     * @param cont    The Contentlet object to which field values will be set.
     * @param headers A map of column indices to their corresponding Field definitions.
     * @param values  A map of processed field values indexed by column.
     * @param request The HTTP request object (maybe used for additional context).
     * @param preview Boolean flag indicating whether this is a preview operation.
     */
    private static void processContentFields(
            final Contentlet cont,
            final Map<Integer, Field> headers,
            final Map<Integer, Object> values,
            final HttpServletRequest request,
            final String siteId,
            final User user,
            final boolean preview
    ) throws IOException, DotSecurityException {

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
                    fetchAndSetBinaryField(cont, field, value, request, preview);
                } else if (isFileField(field)) {
                    fetchAndSetFileField(cont, field, value, request, siteId, user, preview);
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
     * Persists processed contentlet data to the repository with full workflow integration.
     * Handles:
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
     * @param lineNumber             CSV line number for error tracking
     * @param wfActionId             Workflow action ID from import configuration
     * @param cont                   Contentlet with processed field values
     * @param categories             Categories to associate with content
     * @param contentTypePermissions Permissions inherited from content type
     * @param relationships          Content relationships (parent/child)
     * @param user                   Authenticated user for audit trail
     * @param headers                Map of CSV columns to content fields
     * @param values                 Raw+processed field values map
     * @param siteAndFolder          Hosting location context
     * @param resultBuilder          Accumulates processing results and messages
     * @throws DotDataException     If content persistence fails
     * @throws DotSecurityException If user lacks permission for operations
     * @throws LanguageException    If workflow message localization fails
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

        final var validationResult = validateWorkflowExecution(lineNumber, wfActionId, cont, user, resultBuilder);
        if (Boolean.TRUE.equals(validationResult.getLeft())) {
            cont = executeWorkflowAction(cont, categories, validationResult.getRight(), relationships, user);
        } else {
            cont = runWorkflowIfCould(user, contentTypePermissions, categories, cont, relationships);
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
     * @param resultBuilder     The object to update counters based on import results.
     */
    private static void updateCounters(final boolean isNew, final String conditionValues,
            final Set<String> keyContentUpdated,
            final ProcessedContentResultBuilder resultBuilder) {

        if (isNew) {
            resultBuilder.incrementCreatedContent();
        } else {
            if (conditionValues.isEmpty() || !keyContentUpdated.contains(conditionValues)) {
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
    private static void fetchAndSetBinaryField(final Contentlet cont, final Field field,
            final Object value, final HttpServletRequest request, final boolean preview)
            throws IOException, DotSecurityException {
        // At this point if we got this far with an empty value it's because it was determined that the field is not required
        // so no need to re-check
        if (preview) {
            File dummyFile = File.createTempFile("dummy", ".txt",
                    new File(ConfigUtils.getAssetTempPath()));
            cont.setBinary(field.getVelocityVarName(), dummyFile);
        } else if (value != null && UtilMethods.isSet(value.toString())) {
            final URL url = URI.create(value.toString()).toURL();
            final DotTempFile tempFile = tempFileAPI
                    .createTempFileFromUrl(null, request, url, -1);
            cont.setBinary(field.getVelocityVarName(), tempFile.file);
        }
    }

    /**
     * Fetches and sets an image field in the contentlet.
     * @param cont Contentlet with processed field values
     * @param field Field to check
     * @param value Value to set for the image field
     * @param request HTTP request object, used for context
     * @param preview Boolean flag indicating if this is a preview operation
     */
    private static void fetchAndSetFileField(final Contentlet cont, final Field field,
            final Object value, final HttpServletRequest request, final String siteId, final User user, final boolean preview) {
        // At this point if we got this far with an empty value it's because it was determined that the field is not required So No need to re-check
        // But we check if its set and not empty, so we don't try to process empty values.
        // Otherwise, we might end-up throwing an exception for a non-required field
        if (value != null && UtilMethods.isSet(value.toString())) {
            final String uriOrIdentifier = value.toString();
            // First we need to determine if we're looking at an internal Path or an external URL
            // if we're looking at an url we attempt a fetch
            if (UtilMethods.isValidStrictURL(uriOrIdentifier)) {
                try {
                    final Host currentHost = Host.SYSTEM_HOST.equals(siteId) ?
                            hostAPI.findDefaultHost(APILocator.systemUser(),false) :
                            hostAPI.find(siteId, APILocator.systemUser(), false);
                    final ContentType contentType = contentTypeAPI.find(
                            FileAssetAPI.DEFAULT_FILE_ASSET_STRUCTURE_VELOCITY_VAR_NAME);
                    final URI uri = URI.create(uriOrIdentifier);

                    // Create a file asset from the temporary file or retrieve the existing one
                    final Contentlet fileAsset = getFileAsset(uri, request, contentType, currentHost, user);

                    // Set the image field in the contentlet to the identifier of the file asset
                    // That's how image fields are constructed
                    cont.setProperty(field.getVelocityVarName(), fileAsset.getIdentifier());

                } catch (Exception e) {
                    Logger.error(ImportUtil.class, "Error setting image field", e);
                    throw ImportLineException.builder()
                            .message("Error processing file asset from URL: " + uriOrIdentifier + " under site: " + siteId)
                            .code(ImportLineValidationCodes.INVALID_SITE_FOLDER_REF.name())
                            .invalidValue(uriOrIdentifier)
                            .build();
                }
            } else {
                cont.setProperty(field.getVelocityVarName(), uriOrIdentifier);
            }
        }
    }

    /**
     * Retrieves or creates a file asset from a given URI.
     * @param uri The URI of the file to be processed.
     * @param request The HTTP request object, used to fetch the file.
     * @param contentType The content type for the file asset, used to define its structure.
     * @param site The default host where the file asset will be stored.
     * @return A Contentlet object representing the file asset, either retrieved or created.
     * @throws DotDataException If there is a data-related exception during the process.
     * @throws DotSecurityException If the user does not have permission to access the requested
     * @throws IOException If an I/O error occurs while fetching the file.
     */
    private static Contentlet getFileAsset(final URI uri, HttpServletRequest request, final ContentType contentType,
            final Host site, final User user)
            throws DotDataException, DotSecurityException, IOException {
        final long langId = langAPI.getDefaultLanguage().getId();

        final String fileName = UtilMethods.fileName(uri);

        // Use filename + host as key since we're checking for filename existence on that host
        final String fileKey = fileName + ":" + site.getIdentifier();
        final ReentrantLock reentrantLock = fileLocks.computeIfAbsent(fileKey, k -> new ReentrantLock());

        reentrantLock.lock();
        try {
            final Folder root = folderAPI.findFolderByPath(FORWARD_SLASH,
                    site, APILocator.systemUser(), false);

            final boolean exists = fileAssetAPI.fileNameExists(site, root, fileName);
            if (exists) {
                //Check if user has permission to access the file
                final FileAsset file = fileAssetAPI.getFileByPath(FORWARD_SLASH + fileName,
                        site, langId, false);
                if (!permissionAPI.doesUserHavePermission(file, PermissionAPI.PERMISSION_READ, user)) {
                    throw new DotSecurityException("User does not have permission to read the existing file: " + fileName);
                }
                return file;
            }
            // if we determine that the file does not exist, we proceed to fetch it
            final DotTempFile tempFile = APILocator.getTempFileAPI().createTempFileFromUrl(null, request, uri.toURL(), -1);
            final File file = tempFile.file;

            // And create a new file asset from the temporary file we fetched
            final Contentlet fileAsset = new Contentlet();
            fileAsset.setContentType(contentType);
            fileAsset.setLanguageId(langId);
            fileAsset.setHost(site.getIdentifier());
            fileAsset.setFolder(root.getInode());
            fileAsset.setProperty(FileAssetAPI.TITLE_FIELD, file.getName());
            fileAsset.setProperty(FileAssetAPI.FILE_NAME_FIELD, file.getName());
            fileAsset.setProperty(FileAssetAPI.BINARY_FIELD, file);

            final Contentlet savedFileAsset = conAPI.checkin(fileAsset, user, false);
            conAPI.publish(savedFileAsset, user, false);

            return savedFileAsset;

        } finally {
            reentrantLock.unlock();
            // Cleanup if no threads are waiting
            if (!reentrantLock.hasQueuedThreads()) {
                fileLocks.remove(fileKey, reentrantLock);
            }
        }
    }

    private static final ConcurrentHashMap<String, ReentrantLock> fileLocks = new ConcurrentHashMap<>();

    /**
     * Creates a Contentlet object representing a file asset from the provided temporary file.
     * @param tempFile The temporary file containing the uploaded file data.
     * @param contentType The content type for the file asset, used to define its structure.
     * @param defaultHost The default host where the file asset will be stored.
     * @return A Contentlet object representing the file asset, populated with necessary properties.
     */


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

                final var messageBuilder = ValidationMessage.builder()
                        .type(ValidationMessageType.WARNING)
                        .message(LanguageUtil.get(user,
                                "message.import.contentlet.invalid.action.found.in.csv") + " "
                                + e.getMessage())
                        .invalidValue(cont.getActionId())
                        .lineNumber(lineNumber);

                if (e instanceof ImportLineException) {
                    final ImportLineException le = (ImportLineException) e;
                    messageBuilder.code(le.getCode());
                    messageBuilder.context(le.getContext());
                }

                resultBuilder.messages.add(messageBuilder.build());

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

                final var messageBuilder = ValidationMessage.builder()
                        .type(ValidationMessageType.WARNING)
                        .message(LanguageUtil.get(user,
                                "message.import.contentlet.invalid.action.selected")
                                + System.lineSeparator()
                                + e.getMessage())
                        .invalidValue(wfActionId)
                        .lineNumber(lineNumber);

                if (e instanceof ImportLineException) {
                    final ImportLineException le = (ImportLineException)e;
                    messageBuilder.code(le.getCode());
                    messageBuilder.context(le.getContext());
                }

                resultBuilder.messages.add(messageBuilder.build());

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
    private static Contentlet executeWorkflowAction(
            final Contentlet cont,
            final List<Category> categories,
            final WorkflowAction executeWfAction,
            final ContentletRelationships relationships,
            final User user
    ) throws DotDataException, DotSecurityException {

        cont.setIndexPolicy(IndexPolicy.DEFER);
        cont.setBoolProperty(Contentlet.SKIP_RELATIONSHIPS_VALIDATION,
                relationships == null || relationships.getRelationshipsRecords().isEmpty());

        return workflowAPI.fireContentWorkflow(cont,
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

    /**
     * Determines if the given folder is set and its associated inode is also set.
     *
     * @param folder the Folder object to be checked
     * @return true if the folder and its inode are set, false otherwise
     */
    private static boolean isFolderSet(Folder folder) {
        return UtilMethods.isSet(folder) && InodeUtils.isSet(folder.getInode());
    }

    /**
     * Checks if the given site is set and has a valid identifier.
     *
     * @param site the Host object to check
     * @return true if the site and its identifier are both set, false otherwise
     */
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
     */
    private static void setSiteAndFolder(final User user,
            final Contentlet cont, final Pair<Host, Folder> siteAndFolder)
            throws DotDataException {
        if (siteAndFolder != null) {
            final Host host = siteAndFolder.getLeft();
            final Folder folder = siteAndFolder.getRight();
            if (UtilMethods.isSet(folder) && !folder.isSystemFolder() &&
                    !permissionAPI.doesUserHavePermission(folder,
                            PermissionAPI.PERMISSION_CAN_ADD_CHILDREN, user)) {
                throw ImportLineException.builder()
                        .message("User has no Add Children Permissions on selected folder")
                        .code(ImportLineValidationCodes.PERMISSION_ERROR.name())
                        .context(Map.of(
                                "Identifier", cont.getIdentifier(),
                                "Folder", folder.getPath()
                        ))
                        .build();
            } else if (UtilMethods.isSet(host) && (!permissionAPI.doesUserHavePermission(
                    host,PermissionAPI.PERMISSION_CAN_ADD_CHILDREN, user))) {
                throw ImportLineException.builder()
                        .message("User has no Add Children Permissions on selected host")
                        .code(ImportLineValidationCodes.PERMISSION_ERROR.name())
                        .context(Map.of(
                                "Identifier", cont.getIdentifier(),
                                "Site", host.getIdentifier()
                        ))
                        .build();
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

    /**
     * Executes a workflow action on a contentlet, if applicable, based on permissions, categories,
     * and relationships provided. This method determines the appropriate workflow action, and if
     * valid, performs it with the given user and contentlet details. If no workflow action is
     * applicable, it falls back to saving or publishing the contentlet based on system
     * configurations.
     *
     * @param user                    The user attempting to run the workflow for the contentlet.
     * @param contentTypePermissions  The list of permissions associated with the content type of
     *                                the contentlet.
     * @param categories              The list of categories assigned to the contentlet.
     * @param contentlet              The contentlet object that the workflow or save operation will
     *                                be applied to.
     * @param contentletRelationships The relationships associated with the contentlet.
     * @return The contentlet object after the workflow action or save operation has been applied.
     * @throws DotDataException     If a failure occurs during data access or database operations.
     * @throws DotSecurityException If the user does not have the necessary permissions to execute
     *                              the workflow.
     */
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

    /**
     * Executes a workflow publish action on the provided contentlet if associated with a publish
     * workflow action. If not, the contentlet will be directly published. The contentlet may also
     * be updated with relationships, categories, and permissions before being published.
     *
     * @param contentletRelationships an object representing the relationships associated with the
     *                                contentlet
     * @param categories              a list of categories to be associated with the contentlet
     * @param permissions             a list of permissions to be associated with the contentlet
     * @param user                    the user performing the operation
     * @param savedContent            the contentlet to be published or have its workflow processed
     * @return the contentlet after the workflow publish action (or direct publish) is completed
     * @throws DotDataException     if there is an issue accessing or modifying content data
     * @throws DotSecurityException if the user does not have the necessary permissions to perform
     *                              the operation
     */
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
    private static ContentletRelationships loadRelationshipRecords(
            Map<Relationship, List<Contentlet>> csvRelationshipRecordsParentOnly,
            Map<Relationship, List<Contentlet>> csvRelationshipRecordsChildOnly,
            Map<Relationship, List<Contentlet>> csvRelationshipRecords, Contentlet cont)
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
            final Contentlet contentlet) throws ImportLineException {

        WorkflowAction executeWfAction;
        String actionId = contentlet.getActionId();
        try {

            //Validate the permissions over the action to execute
            executeWfAction = workflowAPI
                    .findActionRespectingPermissions(actionId, contentlet, user);

            //Validate if the action we want to execute is in the right step
            workflowAPI.validateActionStepAndWorkflow(contentlet, user);
        } catch (final DotSecurityException e) {
            throw ImportLineException.builder()
                    .message(String.format(
                            "User '%s' doesn't have permissions to execute Workflow Action " +
                                    "'%s'", user.getUserId(), actionId))
                    .code(ImportLineValidationCodes.WORKFLOW_PERMISSION_ERROR.name())
                    .context(Map.of(
                            "Identifier", contentlet.getIdentifier(),
                            "WorkflowActionId", actionId,
                            "Error", e.getMessage()
                    ))
                    .build();
        } catch (final DotDataException | IllegalArgumentException e) {
            final String identifier = StringUtils.defaultIfEmpty(contentlet.getIdentifier(),"N/A");
            final String title = StringUtils.abbreviate(
                       StringUtils.defaultIfEmpty(contentlet.getTitle(),"N/A"),
                    30);
            throw ImportLineException.builder()
                    .message(String.format(
                            "An error occurred executing Workflow Action '%s' on content with identifier '%s' and title '%s' ",
                            actionId, identifier, title))
                    .code(ImportLineValidationCodes.INVALID_WORKFLOW_ACTION.name())
                    .context(Map.of(
                            "Identifier", identifier,
                            "WorkflowActionId", actionId,
                            "Error", e.getMessage()
                    ))
                    .build();
        }

        return executeWfAction;
    }

    /**
     * Validates and parses the date types in a given field and value, returning the parsed object
     * or null if the value is not set or invalid.
     *
     * @param field    The field object containing metadata about the type of the data.
     * @param value    The string representation of the value to be validated and parsed.
     * @param valueObj The original value object, which will be updated if parsing is successful.
     * @return The parsed date object if the value is successfully parsed, null if the value is not
     * set, or the original value object on validation failure.
     */
    private static Object validateDateTypes(final Field field, final String value,
            Object valueObj) {
        if (field.getFieldContentlet().startsWith("date")) {
            if (UtilMethods.isSet(value)) {
                try {
                    valueObj = parseExcelDate(value);
                } catch (ParseException e) {
                    throw DotDateFieldException.conversionErrorBuilder(field.getVelocityVarName(), value)
                            .fieldType(field.getFieldType())
                            .acceptedFormats(IMP_DATE_FORMATS)
                            .addContext("errorMessage", e.getMessage())
                            .build();
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
    private static boolean validateUniqueFields(User user, int lineNumber, long language, boolean stopOnError,
            List<UniqueFieldBean> uniqueFieldBeans, List<Field> uniqueFields,
            final LineImportResultBuilder resultBuilder) throws LanguageException {
        boolean ignoreLine = false;
        for (Field f : uniqueFields) {
            Object value = null;
            int count = 0;
            for (UniqueFieldBean bean : uniqueFieldBeans) {
                if (bean.field().equals(f) && language == bean.languageId()) {
                    if (count > 0 && value != null && value.equals(bean.value()) && lineNumber == bean.lineNumber()) {
                        resultBuilder.incrementContentToCreate(-1);
                        ignoreLine = true;
                        if(!stopOnError) {
                            //this is clearly an error that will cause an exception that will be thrown downstream
                            //Therefore we don't need it reported twice so only log it as a warning when stopOnError is not on
                            resultBuilder.addValidationMessage(dupeWarning(user, lineNumber, f, bean));
                        }
                    }
                    value = bean.value();
                    count++;
                }
            }
        }
        return ignoreLine;
    }

    /**
     * Build the warning message describing the offending dupe value encountered situation
     * @param user
     * @param lineNumber
     * @param f
     * @param bean
     * @return
     * @throws LanguageException
     */
    private static ValidationMessage dupeWarning(User user, int lineNumber, Field f,
            UniqueFieldBean bean) throws LanguageException {
        return ValidationMessage.builder()
                .type(ValidationMessageType.WARNING)
                .message(dupeUniqueFieldMessage(user, f.getVelocityVarName()))
                .code(ImportLineValidationCodes.DUPLICATE_UNIQUE_VALUE.name())
                .field(bean.field().getVelocityVarName())
                .invalidValue(bean.value().toString())
                .lineNumber(lineNumber)
                .build();
    }

    /**
     * Builds a duplicate unique field validation message using String.format
     *
     * @param user the user for localization
     * @param velocityVarName the field variable name
     * @return the formatted message
     */
    private static String dupeUniqueFieldMessage(User user, String velocityVarName)
            throws LanguageException {
        String basePattern = LanguageUtil.get(user, "contains-duplicate-values-for-structure-unique-field") + " '%s'";
        String fullPattern = basePattern + ", " + LanguageUtil.get(user, "and-will-be-ignored");
        return String.format(fullPattern, velocityVarName);
    }

    /**
     * 
     * @return
     */
    private static String printSupportedDateFormats () {
        StringBuilder ret = new StringBuilder("[ ");
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
        text = text.replaceAll("\\+", "\\\\+").replaceAll(StringPool.COLON, "\\\\:");
        text = text.replaceAll("\\*","\\\\*").replaceAll("\\?","\\\\?");
        text = text.replaceAll("\\(","\\\\(").replaceAll("\\)","\\\\)");
        text = text.replaceAll("&&","\\\\&&").replaceAll("\\|\\|","\\\\||");
        text = text.replaceAll("!","\\\\!").replaceAll("\\^","\\\\^");
        text = text.replaceAll("-","\\\\-").replaceAll("~","\\\\~");
        text = text.replaceAll("\"", "\\\"");

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

        private int contentToCreate = 0;
        private int contentCreated = 0;
        private int contentToUpdate = 0;
        private int contentUpdated = 0;
        private int contentUpdatedDuplicated = 0;
        private int commits = 0;
        private int rollbacks = 0;

        private final Collection<Map<String, String>> keys = new ArrayList<>();

        private String lastInode = "";

        public int getContentToCreate() {
            return contentToCreate;
        }

        public void incContentToCreate(int contentToCreate) {
            this.contentToCreate += contentToCreate;
        }

        public int getContentToUpdate() {
            return contentToUpdate;
        }

        public void incContentToUpdate(int contentToUpdate) {
            this.contentToUpdate += contentToUpdate;
        }

        public int getContentCreated() {
            return contentCreated;
        }

        public void incContentCreated(int contentCreated) {
            this.contentCreated += contentCreated;
        }

        public int getContentUpdated() {
            return contentUpdated;
        }

        public void incContentUpdated(int contentUpdated) {
            this.contentUpdated += contentUpdated;
        }

        public int getContentUpdatedDuplicated() {
            return contentUpdatedDuplicated;
        }

        public void incContentUpdatedDuplicated(int contentUpdatedDuplicated) {
            this.contentUpdatedDuplicated += contentUpdatedDuplicated;
        }

        public String getLastInode() {
            return lastInode;
        }

        public void setLastInode(String lastInode) {
            this.lastInode = lastInode;
        }

        public void incCommits() {
            commits++;
        }

        public void incRollbacks() {
            rollbacks++;
        }

        int getCommits() {
            return commits;
        }

        int getRollbacks(){
            return rollbacks;
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
        return DateUtil.convertDate( date, false, ALL_DATE_FORMATS );
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
        private final List<Category> categories;
        private final List<String> updatedInodes;
        private final List<String> savedInodes;
        private int contentToCreateCount;
        private int createdContentCount;
        private int contentToUpdateCount;
        private int updatedContentCount;
        private int duplicateContentCount;
        private String lastInode;
        private boolean ignoreLine;
        private boolean isNewContent;

        public LineImportResultBuilder(int lineNumber) {
            this.builder = LineImportResult.builder();
            this.messages = new ArrayList<>();
            this.categories = new ArrayList<>();
            this.updatedInodes = new ArrayList<>();
            this.builder.lineNumber(lineNumber).ignoreLine(false);
            this.savedInodes = new ArrayList<>();
            this.contentToCreateCount = 0;
            this.createdContentCount = 0;
            this.contentToUpdateCount = 0;
            this.updatedContentCount = 0;
            this.duplicateContentCount = 0;
        }

        public void addValidationMessage(ValidationMessage message) {
            messages.add(message);
        }

        public void addCategory(Category category) {
            categories.add(category);
        }

        public void setUpdatedInodes(List<String> updatedInodes) {
            this.updatedInodes.addAll(updatedInodes);
        }

        public void setIgnoreLine(boolean ignoreLine) {
            this.ignoreLine = ignoreLine;
        }

        public void setNewContent(boolean isNewContent) {
            this.isNewContent = isNewContent;
        }

        public boolean isNewContent() {
            return this.isNewContent;
        }

        void addSavedInode(String inode) {
            savedInodes.add(inode);
        }

        void incrementContentToCreate() {
            contentToCreateCount++;
        }

        void incrementContentToCreate(final int count) {
            contentToCreateCount += count;
        }

        void incrementCreatedContent(final int count) {
            createdContentCount += count;
        }

        void incrementContentToUpdate(final int count) {
            contentToUpdateCount += count;
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
                    .categories(categories)
                    .updatedInodes(updatedInodes)
                    .savedInodes(savedInodes)
                    .isNewContent(isNewContent)
                    .ignoreLine(ignoreLine)
                    .contentToCreate(contentToCreateCount)
                    .createdContent(createdContentCount)
                    .contentToUpdate(contentToUpdateCount)
                    .updatedContent(updatedContentCount)
                    .duplicateContent(duplicateContentCount)
                    .lastInode(lastInode)
                    .build();
        }
    }

    /**
     * Builder class to help construct FieldProcessingResults during CSV field processing. Provides
     * methods to accumulate validation messages and set various result properties.
     */
    private static class FieldProcessingResultBuilder {

        private final FieldProcessingResult.Builder builder;
        private final int lineNumber;
        private final List<ValidationMessage> messages;
        private final List<Category> categories;
        UniqueFieldBean uniqueField;
        Object value;
        Pair<Host, Folder> siteAndFolder;
        Pair<Integer, String> urlValue;

        public FieldProcessingResultBuilder(int lineNumber) {
            this.builder = FieldProcessingResult.builder();
            this.messages = new ArrayList<>();
            this.categories = new ArrayList<>();
            this.builder.lineNumber(lineNumber);
            this.lineNumber = lineNumber;
        }

        void setValue(Object value) {
            this.value = value;
        }

        void setSiteAndFolder(Pair<Host, Folder> siteAndFolder) {
            this.siteAndFolder = siteAndFolder;
        }

        public void addValidationMessage(ValidationMessage message) {
            messages.add(message);
        }

        void setUniqueField(UniqueFieldBean uniqueField) {
            this.uniqueField = uniqueField;
        }

        void setUrlValue(Pair<Integer, String> urlValue) {
            this.urlValue = urlValue;
        }

        public void addWarning(final String message, final String code) {
            addWarning(message, code, "N/A");
        }

        public void addWarning(final String message, final String code, final String field) {
            addValidationMessage(ValidationMessage.builder()
                    .type(ValidationMessageType.WARNING)
                    .message(message)
                    .field(field)
                    .code(code)
                    .lineNumber(lineNumber)
                    .build());
        }

        public void addCategory(Category category) {
            categories.add(category);
        }

        public void addCategories(Collection<Category> categories) {
            this.categories.addAll(categories);
        }

        public FieldProcessingResult build() {
            return builder
                    .messages(messages)
                    .categories(categories)
                    .uniqueField(Optional.ofNullable(uniqueField))
                    .value(Optional.ofNullable(value))
                    .siteAndFolder(Optional.ofNullable(siteAndFolder))
                    .urlValue(Optional.ofNullable(urlValue))
                    .build();
        }
    }

    /**
     * Builder class to help construct FieldsProcessingResults during CSV field processing. Provides
     * methods to accumulate validation messages and set various result properties.
     */
    private static class FieldsProcessingResultBuilder {

        private final int lineNumber;
        private final FieldsProcessingResult.Builder builder;
        private final List<ValidationMessage> messages;
        private final List<Category> categories;
        List<UniqueFieldBean> uniqueFields;
        Map<Integer, Object> values;
        Pair<Host, Folder> siteAndFolder;
        Pair<Integer, String> urlValue;
        String urlValueAssetName;

        public FieldsProcessingResultBuilder(int lineNumber) {
            this.builder = FieldsProcessingResult.builder();
            this.messages = new ArrayList<>();
            this.categories = new ArrayList<>();
            this.uniqueFields = new ArrayList<>();
            this.values = new HashMap<>();
            this.builder.lineNumber(lineNumber);
            this.lineNumber = lineNumber;
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

        public void addError(final String message, final String field, final Object value) {
            addValidationMessage(ValidationMessage.builder()
                    .type(ValidationMessageType.ERROR)
                    .message(message)
                    .lineNumber(lineNumber)
                    .field(field != null ? Optional.of(field) : Optional.empty())
                    .invalidValue(value != null ? Optional.of(value.toString()) : Optional.empty())
                    .build());
        }

        public void addCategory(Category category) {
            categories.add(category);
        }

        public void setIgnoreLine(boolean ignoreLine) {
            builder.ignoreLine(ignoreLine);
        }

        public FieldsProcessingResult build() {
            return builder
                    .messages(messages)
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

        void addWarning(final String message, final String code) {
            messages.add(ValidationMessage.builder()
                    .type(ValidationMessageType.WARNING)
                    .message(message)
                    .code(code)
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
        private int contentToCreateCount;
        private int createdContentCount;
        private int contentToUpdateCount;
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

        void incrementContentToCreate() {
            contentToCreateCount++;
        }

        void incrementCreatedContent() {
            createdContentCount++;
        }

        void incrementContentToUpdate() {
            contentToUpdateCount++;
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
                    .contentToCreate(contentToCreateCount)
                    .createdContent(createdContentCount)
                    .contentToUpdate(contentToUpdateCount)
                    .updatedContent(updatedContentCount)
                    .duplicateContent(duplicateContentCount)
                    .lastInode(lastInode)
                    .messages(messages)
                    .build();
        }
    }

}
