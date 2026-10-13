package com.dotcms.rest.api.v1.content.dotimport;

import com.dotmarketing.portlets.structure.model.Field;
import java.util.Set;
import java.util.List;

/**
 * Writes the sample CSV a content import offers for a content type (#37896): a header row of the
 * type's importable fields, and one row of placeholders showing each field's expected format.
 * <p>
 * This is the file Content Search's "download a CSV sample" has always produced, moved out of
 * {@code ImportContentletsAction} so Content Search and the REST endpoint share it. The output is
 * kept byte for byte, including its quirk: the separator after a field depends on the field's
 * position in the type, not on whether a later field is written, so a type whose last field is not
 * importable gets a trailing comma on both rows.
 */
public final class ContentImportTemplateGenerator {

    /** Field types that hold no content of their own, so an import has no column for them. */
    private static final Set<String> NOT_IMPORTABLE = Set.of(
            Field.FieldType.BUTTON.toString(),
            Field.FieldType.LINE_DIVIDER.toString(),
            Field.FieldType.TAB_DIVIDER.toString(),
            Field.FieldType.PERMISSIONS_TAB.toString(),
            Field.FieldType.COLUMN.toString(),
            Field.FieldType.ROW.toString());

    private ContentImportTemplateGenerator() {
    }

    /**
     * Whether an import has a column for this field: layout and tab fields hold no content.
     * {@code ImportUtil.isImportableField} delegates here, so the import and its sample agree.
     *
     * @param field a content type field
     * @return {@code true} when the field can be imported
     */
    public static boolean isImportableField(final Field field) {
        return !NOT_IMPORTABLE.contains(field.getFieldType());
    }

    /**
     * Builds the sample CSV for a content type's fields.
     *
     * @param fields the content type's fields, in their order on the type
     * @return the two-line sample, with no newline after the second line
     */
    public static String generate(final List<Field> fields) {
        final StringBuilder csv = new StringBuilder();

        for (int i = 0; i < fields.size(); i++) {
            final Field field = fields.get(i);
            if (isImportableField(field)) {
                final String variable = field.getVelocityVarName();
                csv.append(variable.contains(",") ? "\"" + variable + "\"" : variable);
                if (i < fields.size() - 1) {
                    csv.append(',');
                }
            }
        }
        csv.append('\n');

        for (int i = 0; i < fields.size(); i++) {
            final Field field = fields.get(i);
            if (isImportableField(field)) {
                csv.append(placeholderFor(field));
                if (i < fields.size() - 1) {
                    csv.append(',');
                }
            }
        }

        return csv.toString();
    }

    /**
     * The example value that tells a user what a field expects.
     *
     * @param field an importable field
     * @return the quoted placeholder
     */
    private static String placeholderFor(final Field field) {
        final String type = field.getFieldType();
        if (Field.FieldType.DATE.toString().equals(type)) {
            return "\"MM/dd/yyyy\"";
        }
        if (Field.FieldType.DATE_TIME.toString().equals(type)) {
            return "\"MM/dd/yyyy hh:mm aa\"";
        }
        if (Field.FieldType.TIME.toString().equals(type)) {
            return "\"hh:mm aa\"";
        }
        if (Field.FieldType.HOST_OR_FOLDER.toString().equals(type)) {
            return "\"Host/Folder Identifier\"";
        }
        if (Field.FieldType.CATEGORY.toString().equals(type)) {
            return "\"Category Unique Key\"";
        }
        if (Field.FieldType.KEY_VALUE.toString().equals(type)) {
            return "\"{'key1':'value','key2':'value2'}\"";
        }
        return "\"XXX\"";
    }
}
