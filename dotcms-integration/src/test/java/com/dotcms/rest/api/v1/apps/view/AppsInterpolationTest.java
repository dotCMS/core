package com.dotcms.rest.api.v1.apps.view;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertTrue;

import com.dotcms.datagen.AppDescriptorDataGen;
import com.dotcms.rest.api.v1.DotObjectMapperProvider;
import com.dotcms.security.apps.AppDescriptor;
import com.dotcms.security.apps.ParamDescriptor;
import com.dotcms.security.apps.Secret;
import com.dotcms.security.apps.Type;
import com.dotcms.util.IntegrationTestInitService;
import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.core.type.TypeReference;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.google.common.collect.ImmutableList;
import com.google.common.collect.ImmutableMap;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import org.junit.BeforeClass;
import org.junit.Test;

public class AppsInterpolationTest {


    @BeforeClass
    public static void prepare() throws Exception {
        //Setting web app environment
        IntegrationTestInitService.getInstance().init();
    }

    static final ObjectMapper mapper = DotObjectMapperProvider.getInstance()
            .getDefaultObjectMapper();

    /**
     * Given scenario: we build an AppView json based on an AppDescriptor. This fires the serializers so that any embedded velocity code will br processed
     * Expected Results: No velocity variable declared in the data model used to create the initial json such as `$myva` should make it in the result.
     * Also we validate the replacements individually.
     * @throws JsonProcessingException
     */
    @Test
    public void Test_Interpolation() throws JsonProcessingException {

        final List<Map<String, String>> list = ImmutableList.of(
                ImmutableMap.of("label", "-", "value", ""),
                ImmutableMap.of("label", "$uno", "value", "1"),
                //This entry is required to be mutable since the selected attribute gets removed internally.
                new HashMap<>(ImmutableMap.of("label", "dos", "value", "2", "selected", "true")),
                ImmutableMap.of("label", "tres", "value", "3")
        );

        final AppDescriptorDataGen dataGen = new AppDescriptorDataGen()
                .stringParam("param1", false, true, "name is `$param1.name`.", "This is `$param2.value`'s hint.", "lol")
                .stringParam("param2", false, true, "name is `$param2.name`.", "This is `$param1.value`'s hint.", "none")
                .selectParam("selectParam", true, list)
                .buttonParam("buttonParam","https://www.google.com/search?q=$param1.name","Button","button's hint.")
                .withName("any")
                .withDescription(" #set( $uno = 'uno' ) "
                  + "App's name is $app.name ")
                .withExtraParameters(true);

        final AppDescriptor appDescriptor = dataGen.next();
        final Map<String, ParamDescriptor> descriptorParams = appDescriptor.getParams();

        final AppView appView = new AppView(appDescriptor, 1, ImmutableList.of(
                new SiteView("SYSTEM_HOST", "System host",  ImmutableList.of(
                        new SecretView("param1", null, descriptorParams.get("param1"),
                                ImmutableList.of()),
                        new SecretView("param2", null, descriptorParams.get("param2"),
                                ImmutableList.of()),
                        new SecretView("selectParam", null, descriptorParams.get("selectParam"),
                                ImmutableList.of()),
                        new SecretView("button", null, descriptorParams.get("buttonParam"),
                                ImmutableList.of())
                        )
                ),
                new SiteView("48190c8c-42c4-46af-8d1a-0cd5db894797", "demo.dotcms.com",  ImmutableList.of(
                        new SecretView("param1", null, descriptorParams.get("param1"),
                                ImmutableList.of()),
                        new SecretView(
                                "param2",
                                 Secret.builder()
                                         .withValue("This is me Param2's Value")
                                         .withHidden(false)
                                         .withType(Type.STRING)
                                         .build(),
                                descriptorParams.get("param2"),
                                ImmutableList.of()),
                        new SecretView("selectParam", null, descriptorParams.get("selectParam"),
                                ImmutableList.of()),
                        new SecretView("button", null, descriptorParams.get("buttonParam"),
                                ImmutableList.of())
                        )
                )
        ));

        final String json = mapper.writeValueAsString(appView);
        assertFalse(json.contains("$"));
        final Map<String, Object> map = mapper
              .readValue(json, new TypeReference<>() {});
        final String name = (String)map.get("name");
        assertTrue(map.get("description").toString().contains(name));
        List<Map<String,Object>> sites = (List<Map<String, Object>>) map.get("sites");
        List<Map<String,Object>> secrets = (List<Map<String, Object>>) sites.get(0).get("secrets");
        final Map<String,Object> secrets1 = secrets.get(0);
        assertEquals(secrets1.get("hint").toString(), "This is `none`'s hint.");
        assertEquals(secrets1.get("label").toString(), "name is `param1`.");

        final Map<String,Object> secrets2 = secrets.get(1);
        assertEquals(secrets2.get("hint").toString(), "This is `lol`'s hint.");
        assertEquals(secrets2.get("label").toString(), "name is `param2`.");
    }

}
