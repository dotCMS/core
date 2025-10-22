package com.dotcms.content.business;

import com.dotcms.rest.api.v1.DotObjectMapperProvider;
import com.dotcms.util.IntegrationTestInitService;
import com.fasterxml.jackson.annotation.JsonView;
import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.google.common.collect.ImmutableMap;
import java.util.Map;
import org.junit.Assert;
import org.junit.BeforeClass;
import org.junit.Test;

/**
 * Here a small test created for the purpose of experimenting wight jackson mapper and it's capabilities
 */
public class ObjectMapperTest {

    @BeforeClass
    public static void prepare() throws Exception {
        //Setting web app environment
        IntegrationTestInitService.getInstance().init();
    }

    @Test
    public void TestJsonHtmlEscapingDisabledByDefault() throws JsonProcessingException {
        final ObjectMapper objectMapper = DotObjectMapperProvider.getInstance().getDefaultObjectMapper();
        final String inputHtml = "<p>Any Random text will do</p>";
        final Map<String,Object> in = Map.of("html", inputHtml);
        final String out = objectMapper.writeValueAsString(in);
        //If the text would have come out escaped this could never be true
        Assert.assertTrue(out.contains(inputHtml));
    }

     static class Views {

        static class Public {}

        static class Internal extends Public {}

        static class Selective {}
    }

    static class ItemContainer {

        public final String name;
        public final Item item;

        public ItemContainer(String name, Item item) {
            this.name = name;
            this.item = item;
        }
    }

    static class Item {
        //View annotation can accept multiple views.. "Selective" View is here for the purpose of exemplifying it.
        @JsonView({Views.Public.class,Views.Selective.class})
        public final String id;

        @JsonView(Views.Public.class)
        public final String itemName;

        @JsonView(Views.Internal.class)
        public final String ownerName;

        @JsonView(Views.Selective.class)
        public final String parentItemId;

        public final String nonAnnotatedValue;

        public Item(final String id, final String itemName, final String ownerName, final String parentItemId, final String nonAnnotatedValue) {
            this.id = id;
            this.itemName = itemName;
            this.ownerName = ownerName;
            this.parentItemId = parentItemId;
            this.nonAnnotatedValue = nonAnnotatedValue;
        }
    }

    static class ItemDescendant extends Item {

        public ItemDescendant(String id, String itemName, String ownerName,
                String parentItemId, String nonAnnotatedValue) {
            super(id, itemName, ownerName, parentItemId, nonAnnotatedValue);
        }
    }

    /**
     * A small example that help understanding how json views can be used.
     * @throws JsonProcessingException
     */
    @Test
    public void TestJsonViews() throws JsonProcessingException {

        final Item item = new ItemDescendant("1","item-1","dude-1","2", null);

        final ItemContainer container = new ItemContainer("container", item);

        final ObjectMapper objectMapper = DotObjectMapperProvider.getInstance().getDefaultObjectMapper();

        final String withNoViews = objectMapper
                .writeValueAsString(container);

        Assert.assertTrue(withNoViews.contains("id"));
        Assert.assertTrue(withNoViews.contains("itemName"));
        Assert.assertTrue(withNoViews.contains("ownerName"));
        Assert.assertTrue(withNoViews.contains("parentItemId"));
        Assert.assertTrue(withNoViews.contains("nonAnnotatedValue"));

        final String withPublicView = objectMapper
                .writerWithView(Views.Public.class)
                .writeValueAsString(container);

        Assert.assertTrue(withPublicView.contains("id"));
        Assert.assertTrue(withPublicView.contains("itemName"));
        //Still the none annotated field gets rendered
        Assert.assertTrue(withPublicView.contains("nonAnnotatedValue"));
        //Fields annotated with other view won't render
        Assert.assertFalse(withPublicView.contains("parentItemId"));
        Assert.assertFalse(withPublicView.contains("ownerName"));

        final String withInternalView = objectMapper
                .writerWithView(Views.Internal.class)
                .writeValueAsString(container);
        //Still the none annotated field gets rendered
        Assert.assertTrue(withInternalView.contains("nonAnnotatedValue"));
        //This field should make ti since it is directly annotated as Internal
        Assert.assertTrue(withInternalView.contains("ownerName"));
        //Public Fields should make it since Internal extends public
        Assert.assertTrue(withInternalView.contains("id"));
        Assert.assertTrue(withInternalView.contains("itemName"));
        //Shouldn't make it since it's a different view
        Assert.assertFalse(withInternalView.contains("parentItemId"));

        //Now Lets see if after having provided a view and calling `writeValueAsString` again without an implicit View the mapper remember or not the settings
        final String withNoViewsAgain = objectMapper
                .writeValueAsString(container);
        Assert.assertTrue(withNoViewsAgain.contains("id"));
        Assert.assertTrue(withNoViewsAgain.contains("itemName"));
        Assert.assertTrue(withNoViewsAgain.contains("ownerName"));
        Assert.assertTrue(withNoViewsAgain.contains("parentItemId"));
        Assert.assertTrue(withNoViewsAgain.contains("nonAnnotatedValue"));
        //Writer is cleared internally!

        //I experimented using json views on the contentlet class and there are problems
        // Out of the box the mapper won't work (as it does here) even after I feed it with the proper view.
        // The outcome does not match what is expected and delimited  by the json view
        // That unless we do objectMapper.disable(MapperFeature.DEFAULT_VIEW_INCLUSION);
        // which shouldn't be necessary and has side effects.
        //So for now I am dropping json views
    }


}
