package com.dotcms.publishing;

import com.dotcms.publisher.util.PusheableAsset;
import com.dotcms.util.IntegrationTestInitService;
import com.dotmarketing.util.UUIDGenerator;
import java.util.ArrayList;
import java.util.Arrays;
import org.junit.Assert;
import org.junit.BeforeClass;
import org.junit.Test;

public class PublisherFilterImplTest {

    @BeforeClass
    public static void prepare() throws Exception {
        //Setting web app environment
        IntegrationTestInitService.getInstance().init();
    }

    /**
     * Method to test: {@link PublisherFilter#doesExcludeDependencyClassesContainsType(String)}
     * Given Scenario: Given a list of types that are gonna be excluded to PP as a Dependency
     * ExpectedResult: Types in the list are excluded and types not in the list are accepted
     *
     */
    @Test
    public void Test_doesExcludeDependencyClassesContainsType(){
        // Using diff case because user can use diff case
        final ArrayList<String> listOfTypes = new ArrayList<String>(Arrays.asList("Containers", "TEMPLATE","host","ContentType"));

        final PublisherFilterImpl publisherFilter = new PublisherFilterImpl(true,true);

        // Should return false since the types have not been add to the Set, using the PusheableAsset since this is the value passed in core
        Assert.assertFalse(publisherFilter.doesExcludeDependencyClassesContainsType(PusheableAsset.CONTAINER.getType()));
        Assert.assertFalse(publisherFilter.doesExcludeDependencyClassesContainsType(PusheableAsset.TEMPLATE.getType()));
        Assert.assertFalse(publisherFilter.doesExcludeDependencyClassesContainsType(PusheableAsset.SITE.getType()));
        Assert.assertFalse(publisherFilter.doesExcludeDependencyClassesContainsType(PusheableAsset.CONTENTLET.getType()));
        Assert.assertFalse(publisherFilter.doesExcludeDependencyClassesContainsType(PusheableAsset.CONTENT_TYPE.getType()));
        // Adding the types to the Set
        listOfTypes.stream().forEach(type -> publisherFilter.addTypeToExcludeDependencyClassesSet(type));
        // Should return true since types are in the Set
        Assert.assertTrue(publisherFilter.doesExcludeDependencyClassesContainsType(PusheableAsset.CONTAINER.getType()));
        Assert.assertTrue(publisherFilter.doesExcludeDependencyClassesContainsType(PusheableAsset.TEMPLATE.getType()));
        Assert.assertTrue(publisherFilter.doesExcludeDependencyClassesContainsType(PusheableAsset.SITE.getType()));
        Assert.assertTrue(publisherFilter.doesExcludeDependencyClassesContainsType(PusheableAsset.CONTENT_TYPE.getType()));
        // Should return false since the type is not in the Set
        Assert.assertFalse(publisherFilter.doesExcludeDependencyClassesContainsType(PusheableAsset.CONTENTLET.getType()));

    }

    /**
     * Method to test: {@link PublisherFilter#doesExcludeClassesContainsType(String)}
     * Given Scenario: Given a list of types that are gonna be excluded to PP
     * ExpectedResult: Types in the list are excluded and types not in the list are accepted
     *
     */
    @Test
    public void Test_doesExcludeClassesContainsType(){
        // Using diff case because user can use diff case
        final ArrayList<String> listOfTypes = new ArrayList<String>(Arrays.asList("Containers", "TEMPLATE","host"));

        final PublisherFilterImpl publisherFilter = new PublisherFilterImpl(true,true);

        // Should return false since the types have not been add to the Set, using the PusheableAsset since this is the value passed in core
        Assert.assertFalse(publisherFilter.doesExcludeClassesContainsType(PusheableAsset.CONTAINER.getType()));
        Assert.assertFalse(publisherFilter.doesExcludeClassesContainsType(PusheableAsset.TEMPLATE.getType()));
        Assert.assertFalse(publisherFilter.doesExcludeClassesContainsType(PusheableAsset.SITE.getType()));
        Assert.assertFalse(publisherFilter.doesExcludeClassesContainsType(PusheableAsset.CONTENTLET.getType()));
        // Adding the types to the Set
        listOfTypes.stream().forEach(type -> publisherFilter.addTypeToExcludeClassesSet(type));
        // Should return true since types are in the Set
        Assert.assertTrue(publisherFilter.doesExcludeClassesContainsType(PusheableAsset.CONTAINER.getType()));
        Assert.assertTrue(publisherFilter.doesExcludeClassesContainsType(PusheableAsset.TEMPLATE.getType()));
        Assert.assertTrue(publisherFilter.doesExcludeClassesContainsType(PusheableAsset.SITE.getType()));
        // Should return false since the type is not in the Set
        Assert.assertFalse(publisherFilter.doesExcludeClassesContainsType(PusheableAsset.CONTENTLET.getType()));

    }

    /**
     * Method to test: {@link PublisherFilter#doesExcludeQueryContainsContentletId(String)}
     * Given Scenario: Given a list of ids that are gonna be excluded to PP
     * ExpectedResult: Ids in the list are excluded and ids not in the list are accepted
     *
     */
    @Test
    public void Test_doesExcludeQueryContainContentletId(){
        // Generate a couple of uuid
        final String id1 = UUIDGenerator.generateUuid();
        final String id2 = UUIDGenerator.generateUuid();
        final String id3 = UUIDGenerator.generateUuid();
        final ArrayList<String> listOfIds = new ArrayList<String>(Arrays.asList(id1,id2,id3));

        final PublisherFilterImpl publisherFilter = new PublisherFilterImpl(true,true);

        // Should return false since the ids have not been add to the Set
        Assert.assertFalse(publisherFilter.doesExcludeQueryContainsContentletId(id1));
        Assert.assertFalse(publisherFilter.doesExcludeQueryContainsContentletId(id2));
        Assert.assertFalse(publisherFilter.doesExcludeQueryContainsContentletId(id3));
        Assert.assertFalse(publisherFilter.doesExcludeQueryContainsContentletId(UUIDGenerator.generateUuid()));
        // Adding the types to the Set
        listOfIds.stream().forEach(id -> publisherFilter.addContentletIdToExcludeQueryAssetIdSet(id));
        // Should return true since ids are in the Set
        Assert.assertTrue(publisherFilter.doesExcludeQueryContainsContentletId(id1));
        Assert.assertTrue(publisherFilter.doesExcludeQueryContainsContentletId(id2));
        Assert.assertTrue(publisherFilter.doesExcludeQueryContainsContentletId(id3));
        // Should return false since the id is not in the Set
        Assert.assertFalse(publisherFilter.doesExcludeQueryContainsContentletId(UUIDGenerator.generateUuid()));

    }

    /**
     * Method to test: {@link PublisherFilter#doesExcludeDependencyQueryContainsContentletId(String)}
     * Given Scenario: Given a list of ids that are gonna be excluded to PP as a Dependency
     * ExpectedResult: Ids in the list are excluded and ids not in the list are accepted
     *
     */
    @Test
    public void Test_doesExcludeDependencyQueryContainsContentletId(){
        // Generate a couple of uuid
        final String id1 = UUIDGenerator.generateUuid();
        final String id2 = UUIDGenerator.generateUuid();
        final String id3 = UUIDGenerator.generateUuid();
        final ArrayList<String> listOfIds = new ArrayList<String>(Arrays.asList(id1,id2,id3));

        final PublisherFilterImpl publisherFilter = new PublisherFilterImpl(true,true);

        // Should return false since the ids have not been add to the Set
        Assert.assertFalse(publisherFilter.doesExcludeDependencyQueryContainsContentletId(id1));
        Assert.assertFalse(publisherFilter.doesExcludeDependencyQueryContainsContentletId(id2));
        Assert.assertFalse(publisherFilter.doesExcludeDependencyQueryContainsContentletId(id3));
        Assert.assertFalse(publisherFilter.doesExcludeDependencyQueryContainsContentletId(UUIDGenerator.generateUuid()));
        // Adding the types to the Set
        listOfIds.stream().forEach(id -> publisherFilter.addContentletIdToExcludeDependencyQueryAssetIdSet(id));
        // Should return true since ids are in the Set
        Assert.assertTrue(publisherFilter.doesExcludeDependencyQueryContainsContentletId(id1));
        Assert.assertTrue(publisherFilter.doesExcludeDependencyQueryContainsContentletId(id2));
        Assert.assertTrue(publisherFilter.doesExcludeDependencyQueryContainsContentletId(id3));
        // Should return false since the id is not in the Set
        Assert.assertFalse(publisherFilter.doesExcludeDependencyQueryContainsContentletId(UUIDGenerator.generateUuid()));

    }



}
