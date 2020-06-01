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
     * Method to test: {@link PublisherFilter#acceptExcludeDependencyClasses(String)}
     * Given Scenario: Given a list of types that are gonna be excluded to PP as a Dependency
     * ExpectedResult: Types in the list are excluded and types not in the list are accepted
     *
     */
    @Test
    public void Test_acceptExcludeDependencyClasses(){
        // Using diff case because user can use diff case
        final ArrayList<String> listOfTypes = new ArrayList<String>(Arrays.asList("Containers", "TEMPLATE","host","ContentType"));

        final PublisherFilterImpl publisherFilter = new PublisherFilterImpl(true,true);

        // Should return true since the types have not been add to the Set, using the PusheableAsset since this is the value passed in core
        Assert.assertTrue(publisherFilter.acceptExcludeDependencyClasses(PusheableAsset.CONTAINER.getType()));
        Assert.assertTrue(publisherFilter.acceptExcludeDependencyClasses(PusheableAsset.TEMPLATE.getType()));
        Assert.assertTrue(publisherFilter.acceptExcludeDependencyClasses(PusheableAsset.SITE.getType()));
        Assert.assertTrue(publisherFilter.acceptExcludeDependencyClasses(PusheableAsset.CONTENTLET.getType()));
        Assert.assertTrue(publisherFilter.acceptExcludeDependencyClasses(PusheableAsset.CONTENT_TYPE.getType()));
        // Adding the types to the Set
        listOfTypes.stream().forEach(type -> publisherFilter.addTypeToExcludeDependencyClassesSet(type));
        // Should return false since types are in the Set
        Assert.assertFalse(publisherFilter.acceptExcludeDependencyClasses(PusheableAsset.CONTAINER.getType()));
        Assert.assertFalse(publisherFilter.acceptExcludeDependencyClasses(PusheableAsset.TEMPLATE.getType()));
        Assert.assertFalse(publisherFilter.acceptExcludeDependencyClasses(PusheableAsset.SITE.getType()));
        Assert.assertFalse(publisherFilter.acceptExcludeDependencyClasses(PusheableAsset.CONTENT_TYPE.getType()));
        // Should return true since the type is not in the Set
        Assert.assertTrue(publisherFilter.acceptExcludeDependencyClasses(PusheableAsset.CONTENTLET.getType()));

    }

    /**
     * Method to test: {@link PublisherFilter#acceptExcludeClasses(String)}
     * Given Scenario: Given a list of types that are gonna be excluded to PP
     * ExpectedResult: Types in the list are excluded and types not in the list are accepted
     *
     */
    @Test
    public void Test_acceptExcludeClasses(){
        // Using diff case because user can use diff case
        final ArrayList<String> listOfTypes = new ArrayList<String>(Arrays.asList("Containers", "TEMPLATE","host"));

        final PublisherFilterImpl publisherFilter = new PublisherFilterImpl(true,true);

        // Should return true since the types have not been add to the Set, using the PusheableAsset since this is the value passed in core
        Assert.assertTrue(publisherFilter.acceptExcludeClasses(PusheableAsset.CONTAINER.getType()));
        Assert.assertTrue(publisherFilter.acceptExcludeClasses(PusheableAsset.TEMPLATE.getType()));
        Assert.assertTrue(publisherFilter.acceptExcludeClasses(PusheableAsset.SITE.getType()));
        Assert.assertTrue(publisherFilter.acceptExcludeClasses(PusheableAsset.CONTENTLET.getType()));
        // Adding the types to the Set
        listOfTypes.stream().forEach(type -> publisherFilter.addTypeToExcludeClassesSet(type));
        // Should return false since types are in the Set
        Assert.assertFalse(publisherFilter.acceptExcludeClasses(PusheableAsset.CONTAINER.getType()));
        Assert.assertFalse(publisherFilter.acceptExcludeClasses(PusheableAsset.TEMPLATE.getType()));
        Assert.assertFalse(publisherFilter.acceptExcludeClasses(PusheableAsset.SITE.getType()));
        // Should return true since the type is not in the Set
        Assert.assertTrue(publisherFilter.acceptExcludeClasses(PusheableAsset.CONTENTLET.getType()));

    }

    /**
     * Method to test: {@link PublisherFilter#acceptExcludeQuery(String)}
     * Given Scenario: Given a list of ids that are gonna be excluded to PP
     * ExpectedResult: Ids in the list are excluded and ids not in the list are accepted
     *
     */
    @Test
    public void Test_acceptExcludeQuery(){
        // Generate a couple of uuid
        final String id1 = UUIDGenerator.generateUuid();
        final String id2 = UUIDGenerator.generateUuid();
        final String id3 = UUIDGenerator.generateUuid();
        final ArrayList<String> listOfIds = new ArrayList<String>(Arrays.asList(id1,id2,id3));

        final PublisherFilterImpl publisherFilter = new PublisherFilterImpl(true,true);

        // Should return true since the ids have not been add to the Set
        Assert.assertTrue(publisherFilter.acceptExcludeQuery(id1));
        Assert.assertTrue(publisherFilter.acceptExcludeQuery(id2));
        Assert.assertTrue(publisherFilter.acceptExcludeQuery(id3));
        Assert.assertTrue(publisherFilter.acceptExcludeQuery(UUIDGenerator.generateUuid()));
        // Adding the types to the Set
        listOfIds.stream().forEach(id -> publisherFilter.addContentletIdToExcludeQueryAssetIdSet(id));
        // Should return false since ids are in the Set
        Assert.assertFalse(publisherFilter.acceptExcludeQuery(id1));
        Assert.assertFalse(publisherFilter.acceptExcludeQuery(id2));
        Assert.assertFalse(publisherFilter.acceptExcludeQuery(id3));
        // Should return true since the id is not in the Set
        Assert.assertTrue(publisherFilter.acceptExcludeQuery(UUIDGenerator.generateUuid()));

    }

    /**
     * Method to test: {@link PublisherFilter#acceptExcludeDependencyQuery(String)}
     * Given Scenario: Given a list of ids that are gonna be excluded to PP as a Dependency
     * ExpectedResult: Ids in the list are excluded and ids not in the list are accepted
     *
     */
    @Test
    public void Test_acceptExcludeDependencyQuery(){
        // Generate a couple of uuid
        final String id1 = UUIDGenerator.generateUuid();
        final String id2 = UUIDGenerator.generateUuid();
        final String id3 = UUIDGenerator.generateUuid();
        final ArrayList<String> listOfIds = new ArrayList<String>(Arrays.asList(id1,id2,id3));

        final PublisherFilterImpl publisherFilter = new PublisherFilterImpl(true,true);

        // Should return true since the ids have not been add to the Set
        Assert.assertTrue(publisherFilter.acceptExcludeDependencyQuery(id1));
        Assert.assertTrue(publisherFilter.acceptExcludeDependencyQuery(id2));
        Assert.assertTrue(publisherFilter.acceptExcludeDependencyQuery(id3));
        Assert.assertTrue(publisherFilter.acceptExcludeDependencyQuery(UUIDGenerator.generateUuid()));
        // Adding the types to the Set
        listOfIds.stream().forEach(id -> publisherFilter.addContentletIdToExcludeDependencyQueryAssetIdSet(id));
        // Should return false since ids are in the Set
        Assert.assertFalse(publisherFilter.acceptExcludeDependencyQuery(id1));
        Assert.assertFalse(publisherFilter.acceptExcludeDependencyQuery(id2));
        Assert.assertFalse(publisherFilter.acceptExcludeDependencyQuery(id3));
        // Should return true since the id is not in the Set
        Assert.assertTrue(publisherFilter.acceptExcludeDependencyQuery(UUIDGenerator.generateUuid()));

    }



}
