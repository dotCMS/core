package com.dotcms.contenttype.business;

import com.dotcms.contenttype.exception.NotFoundInDbException;
import com.dotcms.contenttype.model.field.Field;
import com.dotcms.contenttype.model.field.FieldBuilder;
import com.dotcms.contenttype.model.field.RelationshipField;
import com.dotcms.contenttype.model.field.RelationshipFieldBuilder;
import com.dotcms.contenttype.model.type.ContentType;
import com.dotmarketing.business.APILocator;
import com.dotmarketing.business.PermissionLevel;
import com.dotmarketing.exception.DotDataException;
import com.dotmarketing.exception.DotSecurityException;
import com.dotmarketing.util.SecurityLogger;
import com.liferay.portal.model.User;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.function.Function;
import java.util.stream.Collectors;

/**
 * Saves a new sort order for the fields of one content type on behalf of the user who asked for it.
 * Both legacy entry points that reorder fields (the DWR {@code StructureAjax.reorderfields} call
 * and the Struts {@code edit_field} action with {@code cmd=reorder}) go through this class, so the
 * rules live in one place. A request is handled in this order:
 * <ol>
 *     <li>the content type is resolved and the user must have
 *     {@link PermissionLevel#EDIT_PERMISSIONS} on it, checked with
 *     {@code PermissionAPI.checkPermission(type, EDIT_PERMISSIONS, user)}, the same call
 *     {@link FieldAPI#save(Field, User, boolean)} makes;</li>
 *     <li>every field id in the request must be one of that content type's fields;</li>
 *     <li>every sort order must be a whole number;</li>
 *     <li>the fields are saved through {@link FieldAPI#saveFields(List, User)} as the calling user,
 *     which also updates the content type's modification date when a sort order changes. A
 *     relationship field is saved without creating or updating its relationship, so only its sort
 *     order changes.</li>
 * </ol>
 * Nothing is saved until every entry has passed these checks. A missing content type or field, a
 * content type the user cannot read or edit, and a field of another content type are rejected with
 * a {@link DotSecurityException} and one line in the security log. A sort order that is not a whole
 * number is rejected with a {@link NumberFormatException}, and a failure to load the content type
 * or its fields is reported as a {@link DotDataException}; neither is logged as a rejection.
 */
public final class FieldReorderHelper {

    private static final String NO_USER = "a logged-in user is required to reorder fields";
    private static final String TYPE_NOT_EDITABLE =
            "reordering fields requires EDIT_PERMISSIONS on an existing content type";
    private static final String FIELDS_NOT_OF_TYPE =
            "every field to reorder must be an existing field of the same content type";

    private FieldReorderHelper() {
        throw new IllegalStateException("Utility class");
    }

    /**
     * Saves new sort orders for fields of the given content type.
     *
     * <p>The user is checked against the content type first, so a user without EDIT_PERMISSIONS
     * is rejected whatever the request holds, even when no field is sent. An empty or {@code null}
     * map saves nothing.</p>
     *
     * @param user               the user asking for the new order
     * @param contentTypeId      id (inode) of the content type whose fields are reordered
     * @param sortOrderByFieldId new sort order per field id, as sent by the client
     * @return the content type whose fields were reordered
     * @throws DotSecurityException  when there is no user, the content type does not exist, the
     *                               user cannot edit its permissions, or a field does not belong to
     *                               it
     * @throws NumberFormatException when the user may reorder the fields but a sort order is not a
     *                               whole number; nothing is saved
     * @throws DotDataException      when the content type or its fields cannot be loaded or saved
     */
    public static ContentType reorder(final User user, final String contentTypeId,
            final Map<String, String> sortOrderByFieldId)
            throws DotSecurityException, DotDataException {

        requireUser(user);
        final ContentType type = findEditableType(user, contentTypeId);
        if (null == sortOrderByFieldId || sortOrderByFieldId.isEmpty()) {
            return type;
        }

        saveSortOrders(user, type, sortOrderByFieldId);
        return type;
    }

    /**
     * Saves new sort orders for fields when the request does not name the content type. The type
     * is the one the first field belongs to; the user must have EDIT_PERMISSIONS on it before the
     * other fields or the sort orders are looked at, and every other field must belong to it.
     *
     * @param user               the user asking for the new order
     * @param sortOrderByFieldId new sort order per field id, as sent by the client; must not be
     *                           empty
     * @return the content type whose fields were reordered
     * @throws DotSecurityException  when there is no user, no field is sent, the first field does
     *                               not exist, the user cannot edit the permissions of its content
     *                               type, or another field does not belong to that type
     * @throws NumberFormatException when the user may reorder the fields but a sort order is not a
     *                               whole number; nothing is saved
     * @throws DotDataException      when the fields or their content type cannot be loaded or saved
     */
    public static ContentType reorderFieldsOfOneType(final User user,
            final Map<String, String> sortOrderByFieldId)
            throws DotSecurityException, DotDataException {

        requireUser(user);
        if (null == sortOrderByFieldId || sortOrderByFieldId.isEmpty()) {
            throw deny(user, FIELDS_NOT_OF_TYPE);
        }

        final String firstFieldId = sortOrderByFieldId.keySet().iterator().next();
        final ContentType type = findEditableType(user, contentTypeIdOf(user, firstFieldId));
        saveSortOrders(user, type, sortOrderByFieldId);
        return type;
    }

    private static void requireUser(final User user) throws DotSecurityException {
        if (null == user) {
            throw deny(null, NO_USER);
        }
    }

    /**
     * Returns the id of the content type a field belongs to. A field that does not exist is
     * rejected like a content type the user cannot edit, so the answer does not tell whether the
     * field exists; any other failure to load it is a data error.
     */
    private static String contentTypeIdOf(final User user, final String fieldId)
            throws DotSecurityException, DotDataException {

        final Field field;
        try {
            field = APILocator.getContentTypeFieldAPI().find(fieldId);
        } catch (final NotFoundInDbException e) {
            throw deny(user, TYPE_NOT_EDITABLE);
        }
        if (null == field) {
            throw deny(user, TYPE_NOT_EDITABLE);
        }
        return field.contentTypeId();
    }

    /**
     * Loads the content type with the user's own permissions (which requires READ) and then
     * requires EDIT_PERMISSIONS on it. A missing type and a type the user cannot read or edit are
     * rejected the same way, with a fixed message. Any other failure to load the type is a data
     * error and is not logged as a rejection.
     */
    private static ContentType findEditableType(final User user, final String contentTypeId)
            throws DotSecurityException, DotDataException {

        final ContentType type;
        try {
            type = APILocator.getContentTypeAPI(user).find(contentTypeId);
        } catch (final NotFoundInDbException | DotSecurityException e) {
            throw deny(user, TYPE_NOT_EDITABLE);
        }
        if (null == type) {
            throw deny(user, TYPE_NOT_EDITABLE);
        }

        try {
            APILocator.getPermissionAPI().checkPermission(type, PermissionLevel.EDIT_PERMISSIONS, user);
        } catch (final DotSecurityException e) {
            throw deny(user, TYPE_NOT_EDITABLE);
        }
        return type;
    }

    /**
     * Matches every requested field id against the content type's own fields, reads every sort
     * order and only then saves the fields with their new sort order as the user. An id that is
     * not one of the type's fields rejects the whole request, and a sort order that is not a whole
     * number fails it, before anything is saved.
     */
    private static void saveSortOrders(final User user, final ContentType type,
            final Map<String, String> sortOrderByFieldId)
            throws DotSecurityException, DotDataException {

        final Map<String, Field> fieldsOfType = type.fields().stream()
                .collect(Collectors.toMap(Field::id, Function.identity()));
        if (!fieldsOfType.keySet().containsAll(sortOrderByFieldId.keySet())) {
            throw deny(user, FIELDS_NOT_OF_TYPE);
        }

        final List<Field> reordered = new ArrayList<>(sortOrderByFieldId.size());
        for (final Map.Entry<String, String> fieldOrder : sortOrderByFieldId.entrySet()) {
            final Field field = fieldsOfType.get(fieldOrder.getKey());
            reordered.add(keepRelationship(FieldBuilder.builder(field)
                    .sortOrder(Integer.parseInt(fieldOrder.getValue()))
                    .build()));
        }
        APILocator.getContentTypeFieldAPI().saveFields(reordered, user);
    }

    /**
     * Marks an existing relationship field so that saving its new sort order neither creates nor
     * updates its relationship, as the content type field layout does when it moves fields. Any
     * other field is returned as it is.
     */
    private static Field keepRelationship(final Field field) {
        return field instanceof RelationshipField
                ? RelationshipFieldBuilder.builder(field).skipRelationshipCreation(true).build()
                : field;
    }

    /**
     * Writes the single security log line for a rejected reorder and builds the exception to
     * throw. Logs the user id and a fixed reason.
     */
    private static DotSecurityException deny(final User user, final String reason) {
        final String userId = null == user ? "[not logged in]" : user.getUserId();
        SecurityLogger.logInfo(FieldReorderHelper.class,
                String.format("User %s could not reorder fields: %s", userId, reason));
        return new DotSecurityException("The fields could not be reordered: " + reason);
    }
}
