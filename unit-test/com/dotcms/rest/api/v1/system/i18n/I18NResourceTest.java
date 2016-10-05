package com.dotcms.rest.api.v1.system.i18n;

import com.dotcms.repackage.com.google.common.collect.Maps;
import com.dotcms.repackage.org.json.JSONObject;
import com.dotcms.rest.exception.InternalServerException;
import com.dotcms.rest.exception.NotFoundException;

import org.hamcrest.Matchers;
import org.junit.Before;
import org.junit.Test;

import java.util.HashMap;
import java.util.Map;
import java.util.Optional;
import java.util.TreeMap;

import static org.hamcrest.MatcherAssert.assertThat;
import static org.hamcrest.Matchers.is;

public class I18NResourceTest {

    @Before
    public void setUp() throws Exception {
    }

    @Test(expected = NotFoundException.class)
    public void testCheckHasResultThrowsExceptionWhenNoResult() throws Exception {
        I18NResource rsrc = new I18NResource();
        rsrc.checkHasResult(new I18NResource.RestResourceLookup("en", "foo"), Optional.empty(), new HashMap<>());
    }

    @Test()
    public void testCheckHasResultDoesNotThrowForSingleResult() throws Exception {
        I18NResource rsrc = new I18NResource();
        rsrc.checkHasResult(new I18NResource.RestResourceLookup("en", "foo"), Optional.of("Fake Result"), new HashMap<>());
    }

    @Test()
    public void testCheckHasResultDoesNotThrowForMultipleResults() throws Exception {
        I18NResource rsrc = new I18NResource();
        Map<String, String> abcdMap = Maps.newHashMap();
        abcdMap.put("a", "b");
        abcdMap.put("c", "d");
        rsrc.checkHasResult(new I18NResource.RestResourceLookup("en", "foo"), Optional.<String>empty(), abcdMap);
    }

    @Test()
    public void testCheckHasResultDoesNotThrowForSingleAndMultipleResults() throws Exception {
        I18NResource rsrc = new I18NResource();
        Map<String, String> abcdMap = Maps.newHashMap();
        abcdMap.put("a", "b");
        abcdMap.put("c", "d");
        rsrc.checkHasResult(new I18NResource.RestResourceLookup("en", "foo"), Optional.of("Fake Result"), abcdMap);
    }

    @Test(expected = InternalServerException.class)
    public void testMessageToJsonDoesNotAllowNullRoot() throws Exception {
        I18NResource rsrc = new I18NResource();
        String[] pathKeys = {"foo"};
        String value = "Hello";
        rsrc.messageToJson(null, pathKeys, value);
    }

    @Test(expected = InternalServerException.class)
    public void testMessageToJsonDoesNotAllowEmptyPathKeys() throws Exception {
        JSONObject root = new JSONObject();
        I18NResource rsrc = new I18NResource();
        String[] pathKeys = {};
        String value = "Hello";
        rsrc.messageToJson(root, pathKeys, value);
    }

    @Test
    public void testMessageToJsonProvidesSimpleKeyValueAtRoot() throws Exception {
        JSONObject root = new JSONObject();
        I18NResource rsrc = new I18NResource();
        String[] pathKeys = {"foo"};
        String value = "Hello";
        rsrc.messageToJson(root, pathKeys, value);
        assertThat("", root.get("foo"), is(value));
    }

    @Test
    public void testMessageToJsonProvidesDottedResultAsNestedJsonObject() throws Exception {
        JSONObject root = new JSONObject();
        I18NResource rsrc = new I18NResource();
        String value = "Hello";
        rsrc.messageToJson(root, new String[]{"foo", "bar", "baz"}, value);

        assertThat("", root.get("foo"), Matchers.instanceOf(JSONObject.class));
        JSONObject foo = root.getJSONObject("foo");
        assertThat("", foo.get("bar"), Matchers.instanceOf(JSONObject.class));
        JSONObject bar = foo.getJSONObject("bar");
        assertThat("", bar.get("baz"), is(value));
    }

    @Test
    public void testMessageToJsonProvidesChildTreeEvenIfValueAlreadyPresentForKey() throws Exception {
        JSONObject root = new JSONObject();
        I18NResource rsrc = new I18NResource();
        String value = "Hello_One";
        String value2 = "Hello_Two";
        rsrc.messageToJson(root, new String[]{"foo", "bar"}, value);

        assertThat("", root.get("foo"), Matchers.instanceOf(JSONObject.class));
        JSONObject foo = root.getJSONObject("foo");
        assertThat("foo.bar is the string value.", foo.get("bar"), is(value));

        rsrc.messageToJson(root, new String[]{"foo", "bar", "baz"}, value2);
        foo = root.getJSONObject("foo");
        assertThat("foo.bar was replaced with a JSON value.", foo.get("bar"), Matchers.instanceOf(JSONObject.class));
        JSONObject bar = foo.getJSONObject("bar");
        assertThat("Result should be the string value.", bar.get("baz"), is(value2));
    }

    @Test
    public void testGetSubTreeReturnsEndOfTreeWhenEndKeyTokenIsAfterLastElement() throws Exception {
        I18NResource rsrc = new I18NResource();
        Map<String, String> subTree = rsrc.getSubTree("foo.kiwi", "foo.zebra", getTreeMapTestData());
        assertThat("Subtree does not contain apple.", subTree.containsKey("foo.apple.name"), is(false));
        assertThat("Subtree does not contain banana.", subTree.containsKey("foo.banana.name"), is(false));
        assertThat("Subtree contains kiwi.", subTree.containsKey("foo.kiwi.name"), is(true));
        assertThat("Subtree contains kiwi.", subTree.containsKey("foo.kiwi.description"), is(true));
        assertThat("Subtree contains orange.", subTree.containsKey("foo.orange.name"), is(true));
        assertThat("Subtree contains orange.", subTree.containsKey("foo.orange.description"), is(true));
        assertThat("Subtree contains pear.", subTree.containsKey("foo.pear.name"), is(true));
        assertThat("Subtree contains pear.", subTree.containsKey("foo.pear.description"), is(true));
    }

    @Test
    public void testGetSubTreeReturnsStartOfTreeWhenStartKeyTokenIsBeforeFirstElement() throws Exception {
        I18NResource rsrc = new I18NResource();
        Map<String, String> subTree = rsrc.getSubTree("foo.Açaí", "foo.kiwi", getTreeMapTestData());
        assertThat("Subtree does not contain apple.", subTree.containsKey("foo.apple.name"), is(true));
        assertThat("Subtree does not contain banana.", subTree.containsKey("foo.banana.name"), is(true));
        assertThat("Subtree contains kiwi.", subTree.containsKey("foo.kiwi.name"), is(true));
        assertThat("Subtree contains kiwi description.", subTree.containsKey("foo.kiwi.description"), is(true));

        assertThat("Subtree contains orange.", subTree.containsKey("foo.orange.name"), is(false));
        assertThat("Subtree contains orange.", subTree.containsKey("foo.orange.description"), is(false));
        assertThat("Subtree contains pear.", subTree.containsKey("foo.pear.name"), is(false));
        assertThat("Subtree contains pear.", subTree.containsKey("foo.pear.description"), is(false));
    }

    @Test
    public void testGetSubTreeReturnsMiddleOfTreeWhenKeysLieWithinBoundsOfMap() throws Exception {
        I18NResource rsrc = new I18NResource();
        Map<String, String> subTree = rsrc.getSubTree("foo.kiwi", "foo.orange", getTreeMapTestData());
        assertThat("Subtree does not contain apple.", subTree.containsKey("foo.apple.name"), is(false));
        assertThat("Subtree does not contain banana.", subTree.containsKey("foo.banana.name"), is(false));

        assertThat("Subtree contains kiwi.", subTree.containsKey("foo.kiwi.name"), is(true));
        assertThat("Subtree contains kiwi.", subTree.containsKey("foo.kiwi.description"), is(true));
        assertThat("Subtree contains orange.", subTree.containsKey("foo.orange.name"), is(true));
        assertThat("Subtree contains orange.", subTree.containsKey("foo.orange.description"), is(true));

        assertThat("Subtree contains pear.", subTree.containsKey("foo.pear.name"), is(false));
        assertThat("Subtree contains pear.", subTree.containsKey("foo.pear.description"), is(false));
    }

    @Test
    public void testGetSubTreeReturnsAllChildrenWhenParentRequestedUsingEqualStartAndEndKeys() throws Exception {
        I18NResource rsrc = new I18NResource();
        Map<String, String> subTree = rsrc.getSubTree("foo.kiwi", "foo.kiwi", getTreeMapTestData());
        assertThat("Subtree does not contain apple.", subTree.containsKey("foo.apple.name"), is(false));
        assertThat("Subtree does not contain banana.", subTree.containsKey("foo.banana.name"), is(false));

        assertThat("Subtree contains kiwi.", subTree.containsKey("foo.kiwi.name"), is(true));
        assertThat("Subtree contains kiwi.", subTree.containsKey("foo.kiwi.description"), is(true));

        assertThat("Subtree contains orange.", subTree.containsKey("foo.orange.name"), is(false));
        assertThat("Subtree contains orange.", subTree.containsKey("foo.orange.description"), is(false));
        assertThat("Subtree contains pear.", subTree.containsKey("foo.pear.name"), is(false));
        assertThat("Subtree contains pear.", subTree.containsKey("foo.pear.description"), is(false));
    }

    private TreeMap<String, String> getTreeMapTestData() {
        TreeMap<String, String> map = new TreeMap<>();
        map.put("foo.apple.name", "Apple");
        map.put("foo.apple.description", "A tasty, crisp fruit. ");
        map.put("foo.banana.name", "Banana");
        map.put("foo.banana.description", "Yellow. Eaten by simians.");
        map.put("foo.kiwi.name", "Kiwi");
        map.put("foo.kiwi.description", "A fuzzy green fruit.");
        map.put("foo.orange.name", "Orange");
        map.put("foo.orange.description", "A juicy fruit that is orange when ripe.");
        map.put("foo.pear.name", "Pear");
        map.put("foo.pear.description", "Pear shaped.");
        return map;
    }

    @Test
    public void testName() throws Exception {

        assertThat("Because I want it to", "pass", Matchers.not(is("fail")));
    }
}