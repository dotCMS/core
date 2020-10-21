package com.dotcms.rest.api.v1.apps.view;

import static com.dotcms.rest.api.v1.apps.view.AppView.applyInterpolation;

import com.dotcms.repackage.com.google.common.collect.ImmutableMap;
import com.dotcms.rest.api.v1.apps.view.ViewStack.StackContext;
import com.dotmarketing.util.Logger;
import java.io.StringReader;
import java.io.StringWriter;
import org.apache.velocity.Template;
import org.apache.velocity.VelocityContext;
import org.apache.velocity.runtime.RuntimeServices;
import org.apache.velocity.runtime.RuntimeSingleton;
import org.apache.velocity.runtime.parser.ParseException;
import org.apache.velocity.runtime.parser.node.SimpleNode;
import org.elasticsearch.common.recycler.Recycler.V;
import org.junit.Test;

public class AppsInterpolationTest {


    final String inputJson = "{\n"
            + "   \"entity\": {\n"
            + "      \"allowExtraParams\": false,\n"
            + "      \"configurationsCount\": 0,\n"
            + "      \"description\": \"Amazon Rekognition provides automated classification and tagging of images. This App allows you to configure the Rekognition Workflow Actionlet.\",\n"
            + "      \"iconUrl\": \"https://static.dotcms.com/assets/icons/apps/amazon_rekognition.png\",\n"
            + "      \"key\": \"dotAmazonRekognition-config\",\n"
            + "      \"name\": \"Amazon Rekognition\",\n"
            + "      \"sites\": [\n"
            + "         {\n"
            + "            \"configured\": false,\n"
            + "            \"id\": \"48190c8c-42c4-46af-8d1a-0cd5db894797\",\n"
            + "            \"name\": \"demo.dotcms.com\",\n"
            + "            \"secrets\": [\n"
            + "               {\n"
            + "                  \"dynamic\": false,\n"
            + "                  \"hidden\": true,\n"
            + "                  \"hint\": \"The AWS access key, used to $sites.secrets.name the user interacting with AWS.\",\n"
            + "                  \"label\": \"AWS Access Key\",\n"
            + "                  \"name\": \"accessKey\",\n"
            + "                  \"required\": true,\n"
            + "                  \"type\": \"STRING\",\n"
            + "                  \"value\": \"\"\n"
            + "               },\n"
            + "               {\n"
            + "                  \"dynamic\": false,\n"
            + "                  \"hidden\": true,\n"
            + "                  \"hint\": \"The AWS secret access key, used to authenticate $name the user interacting with AWS.\",\n"
            + "                  \"label\": \"AWS Secret Access Key\",\n"
            + "                  \"name\": \"secretAccessKey\",\n"
            + "                  \"required\": true,\n"
            + "                  \"type\": \"STRING\",\n"
            + "                  \"value\": \"\"\n"
            + "               }\n"
            + "            ]\n"
            + "         }\n"
            + "      ]\n"
            + "   }\n"
            + "}";


    @Test
    public void Test(){
        ViewStack.createStack(ImmutableMap.of("allowExtraParams","true", "configurationsCount", "0", "description", "lol", "key",  "dotAmazonRekognition-config","name","Amazon Rekognition"));
        ViewStack.pushSite("48190c8c-42c4-46af-8d1a-0cd5db894797", ImmutableMap.of("id","", "name", "lol", "configured","false"));
        ViewStack.pushSecret(ImmutableMap.of("secret1","secret1"));
        final String interpolatedJson = applyInterpolation(inputJson,ViewStack.getCurrentStack());

    }



}
