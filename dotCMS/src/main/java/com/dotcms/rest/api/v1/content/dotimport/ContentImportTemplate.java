package com.dotcms.rest.api.v1.content.dotimport;

import com.dotcms.contenttype.model.type.ContentType;
import com.dotcms.contenttype.transform.field.LegacyFieldTransformer;
import com.dotmarketing.business.APILocator;
import com.dotmarketing.business.PermissionAPI;
import com.dotmarketing.exception.DotDataException;
import com.dotmarketing.exception.DotSecurityException;
import com.dotmarketing.portlets.structure.model.Field;
import com.dotmarketing.util.Constants;
import com.dotmarketing.util.ImportUtil;
import com.dotmarketing.util.SecurityLogger;
import com.dotmarketing.util.UtilMethods;
import com.liferay.portal.model.User;
import java.util.List;
import java.util.stream.Collectors;

/**
 * Builds the CSV template that the content import screen hands the user as a starting point: a
 * header line with the variable name of every field a CSV row can set, and a sample row showing
 * the format each of those fields expects.
 *
 * <p>The template describes a Content Type's schema, so it is built only for a caller who may read
 * that type. The bar is READ on the type, which is what the Content Type API already applies to
 * {@code GET /api/v1/contenttype/id/{id}}: a caller who holds it can list the same field names
 * there, and a caller who does not gets nothing from either. Front-end roles do not count towards
 * it — this is a back-end screen, and a type that inherits a grant held by CMS Anonymous is not
 * thereby part of every back-end account's reach.</p>
 *
 * <p>Which fields reach the template is decided by {@link ImportUtil#isImportableField(Field)},
 * the same predicate the importer applies to the header line of an uploaded file, so the template
 * and the parser cannot drift apart.</p>
 */
public final class ContentImportTemplate {

    private static final String DATE_SAMPLE = "MM/dd/yyyy";
    private static final String DATE_TIME_SAMPLE = "MM/dd/yyyy hh:mm aa";
    private static final String TIME_SAMPLE = "hh:mm aa";
    private static final String SITE_OR_FOLDER_SAMPLE = "Host/Folder Identifier";
    private static final String CATEGORY_SAMPLE = "Category Unique Key";
    private static final String KEY_VALUE_SAMPLE = "{'key1':'value','key2':'value2'}";
    private static final String TEXT_SAMPLE = "XXX";
    private static final String LINE_BREAK = "\n";

    private ContentImportTemplate() {
        throw new AssertionError("ContentImportTemplate is a utility class");
    }

    /**
     * Builds the CSV template for a Content Type, as long as the user may read it.
     *
     * @param contentTypeIdOrVar the id or the variable name of the {@link ContentType} the
     *                           template is for
     * @param user               the {@link User} asking for it
     * @return the template: the header line, a line break, and one sample row
     * @throws DotDataException     when the Content Type cannot be read from the database; a
     *                              Content Type that does not exist surfaces as a
     *                              {@code NotFoundInDbException}
     * @throws DotSecurityException when the user does not hold READ on the Content Type
     */
    public static String generate(final String contentTypeIdOrVar, final User user)
            throws DotDataException, DotSecurityException {
        if (!UtilMethods.isSet(contentTypeIdOrVar)) {
            throw new IllegalArgumentException("A Content Type id or variable name is required");
        }
        final ContentType contentType = resolve(contentTypeIdOrVar, user);
        final List<Field> fields = new LegacyFieldTransformer(contentType.fields())
                .asOldFieldList().stream()
                .filter(ImportUtil::isImportableField)
                .collect(Collectors.toList());
        return headerLine(fields) + LINE_BREAK + sampleLine(fields);
    }

    /**
     * Finds the Content Type and refuses the caller who cannot read it.
     *
     * @param contentTypeIdOrVar the id or the variable name of the {@link ContentType}
     * @param user               the {@link User} asking for it; a missing user is refused
     * @return the matching {@link ContentType}
     * @throws DotDataException     when the lookup fails
     * @throws DotSecurityException when the user does not hold READ on it
     */
    private static ContentType resolve(final String contentTypeIdOrVar, final User user)
            throws DotDataException, DotSecurityException {
        // The type is looked up as the System User so that a type the caller cannot read is still
        // told apart from one that does not exist; the permission is then checked explicitly.
        final ContentType contentType = APILocator.getContentTypeAPI(APILocator.systemUser())
                .find(contentTypeIdOrVar);
        if (null == user || !APILocator.getPermissionAPI().doesUserHavePermission(contentType,
                PermissionAPI.PERMISSION_READ, user, Constants.DONT_RESPECT_FRONT_END_ROLES)) {
            final String userId = null == user ? "anonymous" : user.getUserId();
            SecurityLogger.logInfo(ContentImportTemplate.class, String.format(
                    "User '%s' is not allowed to read Content Type '%s'", userId,
                    contentType.variable()));
            throw new DotSecurityException(String.format(
                    "User %s does not have READ permission on content type %s", userId,
                    contentType.id()));
        }
        return contentType;
    }

    /**
     * Writes the header line: the variable name of each field, comma separated.
     *
     * @param fields the fields a CSV row can set
     * @return the header line
     */
    private static String headerLine(final List<Field> fields) {
        return fields.stream()
                .map(field -> field.getVelocityVarName().contains(",")
                        ? quote(field.getVelocityVarName())
                        : field.getVelocityVarName())
                .collect(Collectors.joining(","));
    }

    /**
     * Writes the sample row: for each field, an example of the format its column expects.
     *
     * @param fields the fields a CSV row can set
     * @return the sample row
     */
    private static String sampleLine(final List<Field> fields) {
        return fields.stream()
                .map(field -> quote(sampleValue(field)))
                .collect(Collectors.joining(","));
    }

    /**
     * Tells what a value of this field looks like in the CSV, so the user can see the expected
     * format rather than guess it. Fields whose value is free text get a placeholder.
     *
     * @param field the field the column stands for
     * @return the sample value, unquoted
     */
    private static String sampleValue(final Field field) {
        final String fieldType = field.getFieldType();
        if (Field.FieldType.DATE.toString().equals(fieldType)) {
            return DATE_SAMPLE;
        }
        if (Field.FieldType.DATE_TIME.toString().equals(fieldType)) {
            return DATE_TIME_SAMPLE;
        }
        if (Field.FieldType.TIME.toString().equals(fieldType)) {
            return TIME_SAMPLE;
        }
        if (Field.FieldType.HOST_OR_FOLDER.toString().equals(fieldType)) {
            return SITE_OR_FOLDER_SAMPLE;
        }
        if (Field.FieldType.CATEGORY.toString().equals(fieldType)) {
            return CATEGORY_SAMPLE;
        }
        if (Field.FieldType.KEY_VALUE.toString().equals(fieldType)) {
            return KEY_VALUE_SAMPLE;
        }
        return TEXT_SAMPLE;
    }

    /**
     * Wraps a value in the double quotes a CSV field uses.
     *
     * @param value the value to wrap
     * @return the quoted value
     */
    private static String quote(final String value) {
        return "\"" + value + "\"";
    }
}
