package com.dotmarketing.portlets.personas.business;

import com.dotcms.IntegrationTestBase;
import com.dotcms.util.IntegrationTestInitService;
import com.dotmarketing.beans.MultiTree;
import com.dotmarketing.business.APILocator;
import com.dotmarketing.factories.MultiTreeAPI;
import com.dotmarketing.portlets.personas.model.Persona;
import com.dotmarketing.util.UUIDGenerator;
import com.dotmarketing.util.UtilMethods;
import org.junit.BeforeClass;
import org.junit.Test;

import java.util.List;

public class DeleteMultiTreeUsedPersonaTagJobTest extends IntegrationTestBase {

    @BeforeClass
    public static void initData() throws Exception {
        IntegrationTestInitService.getInstance().init();
    }

    @Test
    public  void testExecute() throws Exception {

        final DeleteMultiTreeUsedPersonaTagJob personaTagJob =
                new DeleteMultiTreeUsedPersonaTagJob();

        final MultiTreeAPI multiTreeAPI = APILocator.getMultiTreeAPI();
        final String htmlPage           = UUIDGenerator.generateUuid();
        final String container          = UUIDGenerator.generateUuid();
        final String content1           = UUIDGenerator.generateUuid();
        final String content2           = UUIDGenerator.generateUuid();
        final String personalization    = Persona .DOT_PERSONA_PREFIX_SCHEME + ":somepersona";
        final String newPersonalization = Persona .DOT_PERSONA_PREFIX_SCHEME + ":newpersona";

        multiTreeAPI.saveMultiTree(new MultiTree(htmlPage, container, content1, UUIDGenerator.generateUuid(), 1)); // dot:default
        multiTreeAPI.saveMultiTree(new MultiTree(htmlPage, container, content2, UUIDGenerator.generateUuid(), 1)); // dot:default
        multiTreeAPI.saveMultiTree(new MultiTree(htmlPage, container, content1, UUIDGenerator.generateUuid(), 2, personalization)); // dot:somepersona
        multiTreeAPI.saveMultiTree(new MultiTree(htmlPage, container, content2, UUIDGenerator.generateUuid(), 2, personalization)); // dot:somepersona
        multiTreeAPI.saveMultiTree(new MultiTree(htmlPage, container, content1, UUIDGenerator.generateUuid(), 3, newPersonalization)); // dot:newpersona
        multiTreeAPI.saveMultiTree(new MultiTree(htmlPage, container, content2, UUIDGenerator.generateUuid(), 3, newPersonalization)); // dot:newpersona

        List<MultiTree> multiTrees = multiTreeAPI.getMultiTreesByPersonalizedPage(htmlPage, MultiTree.DOT_PERSONALIZATION_DEFAULT);
        org.junit.Assert.assertNotNull(multiTrees);
        org.junit.Assert.assertEquals(2, multiTrees.size());

        multiTrees = multiTreeAPI.getMultiTreesByPersonalizedPage(htmlPage, personalization);
        org.junit.Assert.assertNotNull(multiTrees);
        org.junit.Assert.assertEquals(2, multiTrees.size());

        multiTrees = multiTreeAPI.getMultiTreesByPersonalizedPage(htmlPage, newPersonalization);
        org.junit.Assert.assertNotNull(multiTrees);
        org.junit.Assert.assertEquals(2, multiTrees.size());

        //////////
        personaTagJob.execute(APILocator.systemUser(), false);

        multiTrees = multiTreeAPI.getMultiTreesByPersonalizedPage(htmlPage, MultiTree.DOT_PERSONALIZATION_DEFAULT);
        org.junit.Assert.assertNotNull(multiTrees);
        org.junit.Assert.assertEquals(2, multiTrees.size());

        multiTrees = multiTreeAPI.getMultiTreesByPersonalizedPage(htmlPage, personalization);
        org.junit.Assert.assertFalse(UtilMethods.isSet(multiTrees));

        multiTrees = multiTreeAPI.getMultiTreesByPersonalizedPage(htmlPage, newPersonalization);
        org.junit.Assert.assertFalse(UtilMethods.isSet(multiTrees));
    }

}
