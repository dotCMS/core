package com.dotcms.enterprise.cmis.query;

import com.dotcms.IntegrationTestBase;
import com.dotcms.enterprise.cmis.server.CMISManager;
import com.dotcms.repackage.org.apache.chemistry.opencmis.commons.data.ObjectList;
import com.dotcms.repackage.org.apache.chemistry.opencmis.commons.enums.BaseTypeId;
import com.dotcms.repackage.org.apache.chemistry.opencmis.commons.enums.IncludeRelationships;
import com.dotcms.util.IntegrationTestInitService;
import com.dotmarketing.beans.Host;
import com.dotmarketing.business.APILocator;
import com.liferay.portal.model.User;
import java.math.BigInteger;
import org.junit.Assert;
import org.junit.BeforeClass;
import org.junit.Test;

public class CMISQueryProcessorTest extends IntegrationTestBase {

    private static CMISManager cmisManager;
    private static Host host;
    private static User systemUser;

    @BeforeClass
    public static void prepare() throws Exception {
        //Setting web app environment
        IntegrationTestInitService.getInstance().init();

        cmisManager = new CMISManager();

        systemUser = APILocator.getUserAPI().getSystemUser();
        host = APILocator.getHostAPI().findDefaultHost(systemUser, false);

        cmisManager.createAndInitRepository();
    }

    @Test
    public void testDocumentQuery() {

        CMISQueryProcessor processor = new CMISQueryProcessor();

        ObjectList result = processor
                .query(cmisManager.getTypeManager(), systemUser.getEmailAddress(),
                        "select * from " + BaseTypeId.CMIS_DOCUMENT.value(),
                        Boolean.valueOf(false), Boolean.valueOf(false),
                        IncludeRelationships.NONE, "", BigInteger.valueOf(1000),
                        BigInteger.valueOf(0));

        Assert.assertNotNull(result);
        Assert.assertNotNull(result.getObjects());
        Assert.assertFalse(result.getObjects().isEmpty());
    }

    @Test
    public void testDocumentQueryWithCondition() {

        CMISQueryProcessor processor = new CMISQueryProcessor();

        ObjectList result = processor
                .query(cmisManager.getTypeManager(), systemUser.getEmailAddress(),
                        "select * from " + BaseTypeId.CMIS_DOCUMENT.value() + " WHERE IN_FOLDER( '" + host
                                .getInode() + "')",
                        Boolean.valueOf(false), Boolean.valueOf(false),
                        IncludeRelationships.NONE, "", BigInteger.valueOf(1000),
                        BigInteger.valueOf(0));

        Assert.assertNotNull(result);
        Assert.assertNotNull(result.getObjects());
        Assert.assertFalse(result.getObjects().isEmpty());
    }

    @Test
    public void testFolderQuery() {

        CMISQueryProcessor processor = new CMISQueryProcessor();

        ObjectList result = processor
                .query(cmisManager.getTypeManager(), systemUser.getEmailAddress(),
                        "select * from " + BaseTypeId.CMIS_FOLDER.value(),
                        Boolean.valueOf(false), Boolean.valueOf(false),
                        IncludeRelationships.NONE, "", BigInteger.valueOf(1000),
                        BigInteger.valueOf(0));

        Assert.assertNotNull(result);
        Assert.assertNotNull(result.getObjects());
        Assert.assertFalse(result.getObjects().isEmpty());
    }

    @Test
    public void testFolderQueryWithCondition() {

        CMISQueryProcessor processor = new CMISQueryProcessor();

        ObjectList result = processor
                .query(cmisManager.getTypeManager(), systemUser.getEmailAddress(),
                        "select * from " + BaseTypeId.CMIS_FOLDER.value() + " WHERE IN_FOLDER( '" + host
                                .getInode() + "')",
                        Boolean.valueOf(false), Boolean.valueOf(false),
                        IncludeRelationships.NONE, "", BigInteger.valueOf(1000),
                        BigInteger.valueOf(0));

        Assert.assertNotNull(result);
        Assert.assertNotNull(result.getObjects());
        Assert.assertFalse(result.getObjects().isEmpty());
    }


}
