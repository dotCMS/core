package com.dotmarketing.util.importer;

import static org.junit.Assert.assertEquals;

import com.dotmarketing.portlets.structure.model.Field;
import com.dotmarketing.portlets.structure.model.Field.FieldType;
import java.util.List;
import org.junit.Test;

/**
 * Unit tests for {@link ContentImportTemplateGenerator} (#37896).
 * <p>
 * The generator is the sample CSV Content Search has always offered, moved out of
 * {@code ImportContentletsAction} so Content Drive's REST endpoint returns the same file. The
 * expected strings below are what the legacy action writes, byte for byte — quirks included.
 */
public class ContentImportTemplateGeneratorTest {

    private static Field field(final String variable, final FieldType type) {
        final Field field = new Field();
        field.setVelocityVarName(variable);
        field.setFieldType(type.toString());
        return field;
    }

    /**
     * Given scenario: one field of every type with its own placeholder, plus a plain text field.
     * Expected result: a header row of the variables, then one row with each type's placeholder.
     */
    @Test
    public void test_headerAndPlaceholderPerFieldType() {
        final String csv = ContentImportTemplateGenerator.generate(List.of(
                field("title", FieldType.TEXT),
                field("publishDate", FieldType.DATE),
                field("eventDate", FieldType.DATE_TIME),
                field("startTime", FieldType.TIME),
                field("location", FieldType.HOST_OR_FOLDER),
                field("topics", FieldType.CATEGORY),
                field("metadata", FieldType.KEY_VALUE)));

        assertEquals("title,publishDate,eventDate,startTime,location,topics,metadata\n"
                + "\"XXX\",\"MM/dd/yyyy\",\"MM/dd/yyyy hh:mm aa\",\"hh:mm aa\","
                + "\"Host/Folder Identifier\",\"Category Unique Key\","
                + "\"{'key1':'value','key2':'value2'}\"", csv);
    }

    /**
     * Given scenario: layout and tab fields between two importable fields.
     * Expected result: they are left out of both rows.
     */
    @Test
    public void test_layoutFieldsAreLeftOut() {
        final String csv = ContentImportTemplateGenerator.generate(List.of(
                field("title", FieldType.TEXT),
                field("row", FieldType.ROW),
                field("column", FieldType.COLUMN),
                field("divider", FieldType.LINE_DIVIDER),
                field("tab", FieldType.TAB_DIVIDER),
                field("permissions", FieldType.PERMISSIONS_TAB),
                field("button", FieldType.BUTTON),
                field("body", FieldType.TEXT_AREA)));

        assertEquals("title,body\n\"XXX\",\"XXX\"", csv);
    }

    /**
     * Given scenario: a variable that contains a comma.
     * Expected result: it is quoted in the header row.
     */
    @Test
    public void test_variableWithCommaIsQuoted() {
        final String csv = ContentImportTemplateGenerator.generate(List.of(
                field("a,b", FieldType.TEXT),
                field("c", FieldType.TEXT)));

        assertEquals("\"a,b\",c\n\"XXX\",\"XXX\"", csv);
    }

    /**
     * Given scenario: the last field of the type is not importable.
     * Expected result: both rows end with a comma, as the legacy action writes them — the comma
     * depends on the field's position, not on whether a later field is written.
     */
    @Test
    public void test_trailingCommaWhenTheLastFieldIsNotImportable() {
        final String csv = ContentImportTemplateGenerator.generate(List.of(
                field("title", FieldType.TEXT),
                field("divider", FieldType.LINE_DIVIDER)));

        assertEquals("title,\n\"XXX\",", csv);
    }
}
