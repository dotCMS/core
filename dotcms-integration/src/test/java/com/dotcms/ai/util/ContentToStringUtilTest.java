package com.dotcms.ai.util;

import com.dotcms.contenttype.model.field.Field;
import com.dotcms.contenttype.model.field.StoryBlockField;
import com.dotcms.contenttype.model.type.ContentType;
import com.dotcms.datagen.ContentTypeDataGen;
import com.dotcms.datagen.FieldDataGen;
import com.dotcms.util.IntegrationTestInitService;
import com.dotmarketing.portlets.contentlet.model.Contentlet;
import com.dotmarketing.util.UUIDGenerator;
import org.junit.Assert;
import org.junit.BeforeClass;
import org.junit.Test;

import java.util.ArrayList;
import java.util.List;
import java.util.Optional;

/**
 * Integration tests for {@link ContentToStringUtil} focused on how Story Block (Block Editor)
 * fields are turned into the text that dotAI embeds.
 * <p>
 * #36003: a Story Block field is now extracted as Markdown (via {@code StoryBlockMap.toMarkdown()})
 * instead of being rendered to HTML and stripped by Tika. Markdown preserves the structure
 * (tables, code blocks, lists, headings) that the old Tika path flattened away. These tests assert
 * that the structure survives into the extracted text.
 *
 * @author hassandotcms
 */
public class ContentToStringUtilTest {

    /**
     * A Story Block (Tiptap/ProseMirror) document holding a heading, a table and a fenced code
     * block — exactly the structure that the old HTML + Tika path flattened to plain text.
     */
    private static final String STORY_BLOCK_WITH_TABLE_AND_CODE =
            "{" +
                "\"type\":\"doc\"," +
                "\"content\":[" +
                    "{" +
                        "\"type\":\"heading\",\"attrs\":{\"level\":2}," +
                        "\"content\":[{\"type\":\"text\",\"text\":\"Supported Languages\"}]" +
                    "}," +
                    "{" +
                        "\"type\":\"table\",\"content\":[" +
                            "{" +
                                "\"type\":\"tableRow\",\"content\":[" +
                                    "{\"type\":\"tableHeader\",\"attrs\":{\"colspan\":1,\"rowspan\":1}," +
                                        "\"content\":[{\"type\":\"paragraph\",\"content\":[{\"type\":\"text\",\"text\":\"Language\"}]}]}," +
                                    "{\"type\":\"tableHeader\",\"attrs\":{\"colspan\":1,\"rowspan\":1}," +
                                        "\"content\":[{\"type\":\"paragraph\",\"content\":[{\"type\":\"text\",\"text\":\"Use Case\"}]}]}" +
                                "]" +
                            "}," +
                            "{" +
                                "\"type\":\"tableRow\",\"content\":[" +
                                    "{\"type\":\"tableCell\",\"attrs\":{\"colspan\":1,\"rowspan\":1}," +
                                        "\"content\":[{\"type\":\"paragraph\",\"content\":[{\"type\":\"text\",\"text\":\"Java\"}]}]}," +
                                    "{\"type\":\"tableCell\",\"attrs\":{\"colspan\":1,\"rowspan\":1}," +
                                        "\"content\":[{\"type\":\"paragraph\",\"content\":[{\"type\":\"text\",\"text\":\"Enterprise applications\"}]}]}" +
                                "]" +
                            "}" +
                        "]" +
                    "}," +
                    "{" +
                        "\"type\":\"codeBlock\",\"attrs\":{\"language\":\"java\"}," +
                        "\"content\":[{\"type\":\"text\",\"text\":\"System.out.println(\\\"Hello, World!\\\");\"}]" +
                    "}" +
                "]" +
            "}";

    @BeforeClass
    public static void beforeClass() throws Exception {
        IntegrationTestInitService.getInstance().init();
    }

    /**
     * Method to test: {@link ContentToStringUtil#turnContentletIntoString(Contentlet)}
     * Given Scenario: A contentlet with a Story Block field whose value is a Tiptap JSON document
     * containing a heading, a table and a fenced code block.
     * Expected Result: The extracted text is Markdown with the structure preserved — table pipes
     * and separator row, a fenced code block with its language, and the heading marker — none of
     * which survive the old HTML-to-Tika flattening.
     */
    @Test
    public void test_story_block_embedded_as_markdown_preserves_table_and_code() {
        final ContentTypeDataGen dataGen = new ContentTypeDataGen();
        final List<Field> inputFields = new ArrayList<>();
        inputFields.add(new FieldDataGen().velocityVarName("title").next());
        inputFields.add(new FieldDataGen().type(StoryBlockField.class).velocityVarName("body").next());
        dataGen.fields(inputFields);
        final ContentType contentType = dataGen.nextPersisted();

        final Contentlet contentlet = new Contentlet();
        contentlet.setContentType(contentType);
        contentlet.setIdentifier(UUIDGenerator.generateUuid());
        contentlet.setProperty("title", "Supported Languages");
        contentlet.setProperty("body", STORY_BLOCK_WITH_TABLE_AND_CODE);

        final Optional<String> extracted = ContentToStringUtil.impl.get().turnContentletIntoString(contentlet);

        Assert.assertTrue("Story Block field should produce extractable text", extracted.isPresent());
        final String text = extracted.get();

        // Table structure survives as Markdown: pipe-delimited cells + the separator row.
        Assert.assertTrue("Table cells should be pipe-delimited Markdown, got: " + text, text.contains("|"));
        Assert.assertTrue("Table separator row should be present, got: " + text, text.contains("---"));
        Assert.assertTrue("Table header text should survive", text.contains("Language") && text.contains("Use Case"));

        // Code block survives as a fenced block carrying its language.
        Assert.assertTrue("Fenced code block with language should be present, got: " + text, text.contains("```java"));
        Assert.assertTrue("Code body should survive", text.contains("System.out.println(\"Hello, World!\");"));

        // Heading survives as a Markdown heading marker.
        Assert.assertTrue("Heading marker should be present, got: " + text, text.contains("## Supported Languages"));

        // Sanity: this is Markdown, not HTML that Tika stripped — no markup tags leaked through.
        Assert.assertFalse("Extracted text must not contain HTML tags", text.contains("<table") || text.contains("<code"));
    }

}
