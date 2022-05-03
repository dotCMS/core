package com.dotcms.rendering.velocity.viewtools.content;

import com.dotcms.IntegrationTestBase;
import com.dotcms.repackage.org.codehaus.jettison.json.JSONException;
import com.dotcms.util.IntegrationTestInitService;
import org.junit.Assert;
import org.junit.BeforeClass;
import org.junit.Test;

public class StoryBlockMapTest extends IntegrationTestBase {

    private static final String JSON =
            "{\"type\":\"doc\",\"content\":[{\"type\":\"paragraph\",\"attrs\":{\"textAlign\":\"left\"}},{\"type\":\"dotContent\",\"attrs\":{\"data\":{\"hostName\":\"default\",\"modDate\":\"2021-12-06 17:14:41.847\",\"publishDate\":\"2021-12-06 17:14:41.847\",\"title\":\"test2\",\"body\":\"<p>test2</p>\",\"baseType\":\"CONTENT\",\"inode\":\"74329a5f-267b-470b-a403-8148dea85238\",\"archived\":false,\"host\":\"8a7d5e23-da1e-420a-b4f0-471e7da8ea2d\",\"working\":true,\"locked\":false,\"stInode\":\"2a3e91e4-fbbf-4876-8c5b-2233c1739b05\",\"contentType\":\"webPageContent\",\"live\":true,\"owner\":\"dotcms.org.1\",\"identifier\":\"2815d82dd8c37c4726479f5d37f47cf7\",\"languageId\":1,\"url\":\"/content.74329a5f-267b-470b-a403-8148dea85238\",\"titleImage\":\"TITLE_IMAGE_NOT_FOUND\",\"modUserName\":\"Admin User\",\"hasLiveVersion\":true,\"folder\":\"SYSTEM_FOLDER\",\"hasTitleImage\":false,\"sortOrder\":0,\"modUser\":\"dotcms.org.1\",\"__icon__\":\"contentIcon\",\"contentTypeIcon\":\"article\"}}},{\"type\":\"dotContent\",\"attrs\":{\"data\":{\"hostName\":\"default\",\"modDate\":\"2021-12-06 17:01:54.913\",\"publishDate\":\"2021-12-06 17:01:54.913\",\"title\":\"test1\",\"body\":\"<p>test1</p>\",\"baseType\":\"CONTENT\",\"inode\":\"0e31365a-30fb-46c9-b5b7-72e5943cb37b\",\"archived\":false,\"host\":\"8a7d5e23-da1e-420a-b4f0-471e7da8ea2d\",\"working\":true,\"locked\":false,\"stInode\":\"2a3e91e4-fbbf-4876-8c5b-2233c1739b05\",\"contentType\":\"webPageContent\",\"live\":true,\"owner\":\"dotcms.org.1\",\"identifier\":\"8be6bc4d1454e179379509c1fa0e9374\",\"languageId\":1,\"url\":\"/content.c3c2c819-730a-4114-8d27-cdc83b82929d\",\"titleImage\":\"TITLE_IMAGE_NOT_FOUND\",\"modUserName\":\"Admin User\",\"hasLiveVersion\":true,\"folder\":\"SYSTEM_FOLDER\",\"hasTitleImage\":false,\"sortOrder\":0,\"modUser\":\"dotcms.org.1\",\"__icon__\":\"contentIcon\",\"contentTypeIcon\":\"article\"}}},{\"type\":\"heading\",\"attrs\":{\"textAlign\":\"left\",\"level\":1},\"content\":[{\"type\":\"text\",\"marks\":[{\"type\":\"bold\"}],\"text\":\"heading 1\"}]},{\"type\":\"paragraph\",\"attrs\":{\"textAlign\":\"left\"}},{\"type\":\"heading\",\"attrs\":{\"textAlign\":\"left\",\"level\":2},\"content\":[{\"type\":\"text\",\"marks\":[{\"type\":\"bold\"},{\"type\":\"underline\"}],\"text\":\"heading 2\"}]},{\"type\":\"paragraph\",\"attrs\":{\"textAlign\":\"left\"}},{\"type\":\"heading\",\"attrs\":{\"textAlign\":\"left\",\"level\":3},\"content\":[{\"type\":\"text\",\"marks\":[{\"type\":\"italic\"},{\"type\":\"underline\"}],\"text\":\"heading 3\"}]},{\"type\":\"paragraph\",\"attrs\":{\"textAlign\":\"left\"}},{\"type\":\"paragraph\",\"attrs\":{\"textAlign\":\"left\"}},{\"type\":\"paragraph\",\"attrs\":{\"textAlign\":\"left\"}},{\"type\":\"orderedList\",\"attrs\":{\"start\":1},\"content\":[{\"type\":\"listItem\",\"content\":[{\"type\":\"paragraph\",\"attrs\":{\"textAlign\":\"left\"},\"content\":[{\"type\":\"text\",\"text\":\"one\"}]}]},{\"type\":\"listItem\",\"content\":[{\"type\":\"paragraph\",\"attrs\":{\"textAlign\":\"left\"},\"content\":[{\"type\":\"text\",\"marks\":[{\"type\":\"bold\"}],\"text\":\"two\"}]}]},{\"type\":\"listItem\",\"content\":[{\"type\":\"paragraph\",\"attrs\":{\"textAlign\":\"left\"},\"content\":[{\"type\":\"text\",\"text\":\"tree\"}]}]}]},{\"type\":\"bulletList\",\"content\":[{\"type\":\"listItem\",\"content\":[{\"type\":\"paragraph\",\"attrs\":{\"textAlign\":\"left\"},\"content\":[{\"type\":\"text\",\"text\":\"1\"}]}]},{\"type\":\"listItem\",\"content\":[{\"type\":\"paragraph\",\"attrs\":{\"textAlign\":\"left\"},\"content\":[{\"type\":\"text\",\"text\":\"2\"}]}]},{\"type\":\"listItem\",\"content\":[{\"type\":\"paragraph\",\"attrs\":{\"textAlign\":\"left\"},\"content\":[{\"type\":\"text\",\"text\":\"3\"}]}]}]},{\"type\":\"paragraph\",\"attrs\":{\"textAlign\":\"left\"}},{\"type\":\"paragraph\",\"attrs\":{\"textAlign\":\"left\"}}],\"render\":\"<p></p><dotcms-contentlet-block data=\\\"[object Object]\\\"></dotcms-contentlet-block><dotcms-contentlet-block data=\\\"[object Object]\\\"></dotcms-contentlet-block><h1><strong>heading 1</strong></h1><p></p><h2><strong><u>heading 2</u></strong></h2><p></p><h3><em><u>heading 3</u></em></h3><p></p><p></p><p></p><ol><li><p>one</p></li><li><p><strong>two</strong></p></li><li><p>tree</p></li></ol><ul><li><p>1</p></li><li><p>2</p></li><li><p>3</p></li></ul><p></p><p></p>\"}";

    private static final String JSON_PARAGRAPH =
            "{\"type\":\"doc\",\"content\":[{\"type\":\"paragraph\",\"attrs\":{\"textAlign\":\"left\"},\"content\":[{\"type\":\"text\",\"text\":\"this is paragraph\"}]}]}";

    private static final String JSON_CONTENTLET =
            "{\"type\":\"doc\",\"content\":[{\"type\":\"dotContent\",\"attrs\":{\"data\":{\"hostName\":\"default\",\"modDate\":\"2021-12-13 21:52:00.474\",\"publishDate\":\"2021-12-13 21:52:00.474\",\"title\":\"test2\",\"baseType\":\"CONTENT\",\"inode\":\"34b4f7d5-6fa5-4bb2-a38e-0409888f7660\",\"archived\":false,\"host\":\"8a7d5e23-da1e-420a-b4f0-471e7da8ea2d\",\"working\":true,\"locked\":false,\"stInode\":\"4b0fc95d691094f27d68667cdad439ac\",\"contentType\":\"StoryBlock\",\"live\":true,\"owner\":\"dotcms.org.1\",\"identifier\":\"3bd8d3da891d41c680edbf83126a02ce\",\"languageId\":1,\"url\":\"/content.7d947920-85f4-451d-8593-99afcaa83db0\",\"titleImage\":\"TITLE_IMAGE_NOT_FOUND\",\"modUserName\":\"Admin User\",\"hasLiveVersion\":true,\"folder\":\"SYSTEM_FOLDER\",\"hasTitleImage\":false,\"sortOrder\":0,\"modUser\":\"dotcms.org.1\",\"story\":\"{\\\"type\\\":\\\"doc\\\",\\\"content\\\":[{\\\"type\\\":\\\"paragraph\\\",\\\"attrs\\\":{\\\"textAlign\\\":\\\"left\\\"}}]}\",\"__icon__\":\"contentIcon\",\"contentTypeIcon\":\"event_note\"}}}]}";

    private static final String JSON_OLIST =
        "{\"type\":\"doc\",\"content\":[{\"type\":\"orderedList\",\"attrs\":{\"start\":1},\"content\":[{\"type\":\"listItem\",\"attrs\":{\"textAlign\":\"left\"},\"content\":[{\"type\":\"paragraph\",\"attrs\":{\"textAlign\":\"left\"},\"content\":[{\"type\":\"text\",\"text\":\"one\"}]}]},{\"type\":\"listItem\",\"attrs\":{\"textAlign\":\"left\"},\"content\":[{\"type\":\"paragraph\",\"attrs\":{\"textAlign\":\"left\"},\"content\":[{\"type\":\"text\",\"text\":\"two\"}]}]},{\"type\":\"listItem\",\"attrs\":{\"textAlign\":\"left\"},\"content\":[{\"type\":\"paragraph\",\"attrs\":{\"textAlign\":\"left\"},\"content\":[{\"type\":\"text\",\"text\":\"tree\"}]}]}]}]}";

    private static final String JSON_ULIST =
            "{\"type\":\"doc\",\"content\":[{\"type\":\"bulletList\",\"content\":[{\"type\":\"listItem\",\"attrs\":{\"textAlign\":\"left\"},\"content\":[{\"type\":\"paragraph\",\"attrs\":{\"textAlign\":\"left\"},\"content\":[{\"type\":\"text\",\"text\":\"1\"}]}]},{\"type\":\"listItem\",\"attrs\":{\"textAlign\":\"left\"},\"content\":[{\"type\":\"paragraph\",\"attrs\":{\"textAlign\":\"left\"},\"content\":[{\"type\":\"text\",\"text\":\"2\"}]}]},{\"type\":\"listItem\",\"attrs\":{\"textAlign\":\"left\"},\"content\":[{\"type\":\"paragraph\",\"attrs\":{\"textAlign\":\"left\"},\"content\":[{\"type\":\"text\",\"text\":\"3\"}]}]}]}]}";
    @BeforeClass
    public static void prepare() throws Exception {
        //Setting web app environment
        IntegrationTestInitService.getInstance().init();
    }

    /**
     * Method to test: {@link StoryBlockMap#toHtml()}
     * Given Scenario: Sends an invalid json
     * ExpectedResult: Expected a JSONException
     *
     */
    @Test(expected = JSONException.class)
    public void test_empty_json_to_html() throws  JSONException {

        new StoryBlockMap("");
    }

    /**
     * Method to test: {@link StoryBlockMap#toHtml()}
     * Given Scenario: Will parse the json and render the default olist html
     * ExpectedResult: The olist should be rendered
     *
     */
    @Test
    public void test_default_olist_render_to_html() throws  JSONException {

        final StoryBlockMap storyBlockMap = new StoryBlockMap(JSON_OLIST);
        final String html = storyBlockMap.toHtml();
        Assert.assertTrue(html.contains("<ol style=\"text-align: \">\n" +
                "\n" +
                "                            <li style=\"text-align: left\">\n" +
                "\n" +
                "                                                                    \n" +
                "    \n" +
                "    one\n" +
                "\n" +
                "    \n" +
                "                                        </li>\n" +
                "        \n" +
                "                        <li style=\"text-align: left\">\n" +
                "\n" +
                "                                                                    \n" +
                "    \n" +
                "    two\n" +
                "\n" +
                "    \n" +
                "                                        </li>\n" +
                "        \n" +
                "                        <li style=\"text-align: left\">\n" +
                "\n" +
                "                                                                    \n" +
                "    \n" +
                "    tree\n" +
                "\n" +
                "    \n" +
                "                                        </li>\n" +
                "        \n" +
                "    </ol>"));
    }

    /**
     * Method to test: {@link StoryBlockMap#toHtml()}
     * Given Scenario: Will parse the json and render the default ulist html
     * ExpectedResult: The ulist should be rendered
     *
     */
    @Test
    public void test_default_ulist_render_to_html() throws  JSONException {

        final StoryBlockMap storyBlockMap = new StoryBlockMap(JSON_ULIST);
        final String html = storyBlockMap.toHtml();
        Assert.assertTrue(html.contains("<ul>\n" +
                "\n" +
                "                            <li style=\"text-align: left\">\n" +
                "\n" +
                "                                                                            \n" +
                "    \n" +
                "    1\n" +
                "\n" +
                "    \n" +
                "                                                 </li>\n" +
                "        \n" +
                "                        <li style=\"text-align: left\">\n" +
                "\n" +
                "                                                                            \n" +
                "    \n" +
                "    2\n" +
                "\n" +
                "    \n" +
                "                                                 </li>\n" +
                "        \n" +
                "                        <li style=\"text-align: left\">\n" +
                "\n" +
                "                                                                            \n" +
                "    \n" +
                "    3\n" +
                "\n" +
                "    \n" +
                "                                                 </li>\n" +
                "        \n" +
                "    </ul>"));
    }

    /**
     * Method to test: {@link StoryBlockMap#toHtml()}
     * Given Scenario: Will parse the json and render the default paragraph html
     * ExpectedResult: The paragraph should be rendered
     *
     */
    @Test
    public void test_default_paragraph_render_to_html() throws  JSONException {

        final StoryBlockMap storyBlockMap = new StoryBlockMap(JSON_PARAGRAPH);
        final String html = storyBlockMap.toHtml();
        Assert.assertTrue(html.contains("<p style=\"text-align: left\">"));
        Assert.assertTrue(html.contains("this is paragraph"));
        Assert.assertTrue(html.contains("</p>"));
    }

    /**
     * Method to test: {@link StoryBlockMap#toHtml()}
     * Given Scenario: Will parse the json and render the default contentlet html
     * ExpectedResult: The contentlet should be rendered
     *
     */
    @Test
    public void test_default_contentlet_render_to_html() throws  JSONException {

        final StoryBlockMap storyBlockMap = new StoryBlockMap(JSON_CONTENTLET);
        final String html = storyBlockMap.toHtml();
        Assert.assertTrue(html.contains("test2</h2>"));
    }

    /**
     * Method to test: {@link StoryBlockMap#toHtml()}
     * Given Scenario: Will parse the json and render the default html
     * ExpectedResult: Expected result that the assertion is checking against
     *
     */
    @Test
    public void test_default_render_to_html() throws  JSONException {

        final StoryBlockMap storyBlockMap = new StoryBlockMap(JSON);
        final String html = storyBlockMap.toHtml();
        Assert.assertTrue(html.contains("test2</h2>"));
        Assert.assertTrue(html.contains("test2</h2>"));
        Assert.assertTrue(html.contains("test1</h2>"));
        Assert.assertTrue(html.contains("<strong>\n" +
                "                                                \n" +
                "    heading 1\n" +
                "\n" +
                "                                    </strong>"));
        Assert.assertTrue(html.contains("<h2 style=\"text-align: left\">\n" +
                "\n" +
                "                \n" +
                "    \n" +
                "                                <strong>\n" +
                "                                                                                            <u>\n" +
                "                        \n" +
                "    heading 2\n" +
                "\n" +
                "                                                            </u>\n" +
                "                                            </strong>\n" +
                "                                                \n" +
                "    </h2>"));
        Assert.assertTrue(html.contains("<ol style=\"text-align: \">\n" +
                "\n" +
                "                            <li style=\"text-align: $listItem.attrs.textAlign\">\n" +
                "\n" +
                "                                                                    \n" +
                "    \n" +
                "    one\n" +
                "\n" +
                "    \n" +
                "                                        </li>\n" +
                "        \n" +
                "                        <li style=\"text-align: $listItem.attrs.textAlign\">\n" +
                "\n" +
                "                                                                    \n" +
                "    \n" +
                "                                <strong>\n" +
                "                                                \n" +
                "    two\n" +
                "\n" +
                "                                    </strong>\n" +
                "                                                \n" +
                "                                        </li>\n" +
                "        \n" +
                "                        <li style=\"text-align: $listItem.attrs.textAlign\">\n" +
                "\n" +
                "                                                                    \n" +
                "    \n" +
                "    tree\n" +
                "\n" +
                "    \n" +
                "                                        </li>\n" +
                "        \n" +
                "    </ol>"));
        Assert.assertTrue(html.contains("<ul>\n" +
                "\n" +
                "                            <li style=\"text-align: $listItem.attrs.textAlign\">\n" +
                "\n" +
                "                                                                            \n" +
                "    \n" +
                "    1\n" +
                "\n" +
                "    \n" +
                "                                                 </li>\n" +
                "        \n" +
                "                        <li style=\"text-align: $listItem.attrs.textAlign\">\n" +
                "\n" +
                "                                                                            \n" +
                "    \n" +
                "    2\n" +
                "\n" +
                "    \n" +
                "                                                 </li>\n" +
                "        \n" +
                "                        <li style=\"text-align: $listItem.attrs.textAlign\">\n" +
                "\n" +
                "                                                                            \n" +
                "    \n" +
                "    3\n" +
                "\n" +
                "    \n" +
                "                                                 </li>\n" +
                "        \n" +
                "    </ul>"));
    }


}
