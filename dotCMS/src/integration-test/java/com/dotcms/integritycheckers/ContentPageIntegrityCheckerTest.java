package com.dotcms.integritycheckers;

import com.dotcms.IntegrationTestBase;
import com.dotcms.business.WrapInTransaction;
import com.dotcms.datagen.FolderDataGen;
import com.dotcms.datagen.HTMLPageDataGen;
import com.dotcms.datagen.SiteDataGen;
import com.dotcms.datagen.TemplateDataGen;
import com.dotmarketing.beans.Host;
import com.dotmarketing.common.db.DotConnect;
import com.dotmarketing.portlets.folders.model.Folder;
import com.dotmarketing.portlets.htmlpageasset.model.HTMLPageAsset;
import com.dotmarketing.portlets.templates.model.Template;
import com.dotmarketing.util.Logger;
import io.vavr.Tuple;
import io.vavr.Tuple2;
import java.util.UUID;
import org.junit.Assert;
import org.junit.Before;
import org.junit.Test;

/**
 * Simple test to validate the ContentPageIntegrityChecker
 */
public class ContentPageIntegrityCheckerTest extends IntegrationTestBase implements IntegrityCheckerTest  {

    @Before
    public void setup() throws Exception {
        //Setting web app environment
        setUpEnvironment();
    }

    /**
     * Directly introduces a conflict in the database
     * @param page
     * @param endpointId
     * @return
     * @throws Exception
     */
    @WrapInTransaction
    Tuple2<String, String> introduceConflict(final HTMLPageAsset page,  final String endpointId) throws Exception {

        final DotConnect dotConnect = new DotConnect();
        dotConnect.setSQL("INSERT INTO htmlpages_ir\n"
                + "        (html_page, local_working_inode, local_live_inode, remote_working_inode, remote_live_inode, local_identifier, remote_identifier, endpoint_id, language_id)\n"
                + "        VALUES( ?, ?, ?, ?, ?, ?, ?, ?, ? )");

        final String remoteIdentifier = UUID.randomUUID().toString();
        final String remoteWorkingInode = UUID.randomUUID().toString();
        final String remoteLiveInode = UUID.randomUUID().toString();

        dotConnect.addParam(page.getFriendlyName());
        dotConnect.addParam(page.getInode());
        dotConnect.addParam((String)null);

        dotConnect.addParam(remoteWorkingInode);
        dotConnect.addParam(remoteLiveInode);
        dotConnect.addParam(page.getIdentifier());
        dotConnect.addParam(remoteIdentifier);
        dotConnect.addParam(endpointId);
        dotConnect.addParam(page.getLanguageId());

        dotConnect.loadResult();
        return Tuple.of(remoteIdentifier, remoteWorkingInode);

    }

    /**
     * Given scenario: A page is conflicted
     * Expected result: The Fixer should solve the problem and a json should be generated
     * @throws Exception
     */
    @Test
    public void TestFixConflictGeneratesContentletAsJson() throws Exception {

        final Host host = new SiteDataGen().nextPersisted();
        final Folder folder = new FolderDataGen().site(host).nextPersisted();
        final Template template = new TemplateDataGen().host(host).nextPersisted();
        final HTMLPageAsset page = new HTMLPageDataGen(folder, template).languageId(1).title("conflicted page").nextPersisted();

        final ContentPageIntegrityChecker integrityChecker = new ContentPageIntegrityChecker();
        final Tuple2<String, String> remoteIdentifierAndInode = introduceConflict(page, endpointId.get());

        final String remoteIdentifier = remoteIdentifierAndInode._1();
        final String remoteWorkingInode = remoteIdentifierAndInode._2();

        Logger.debug(this, "remoteIdentifier: " + remoteIdentifier + " remoteWorkingInode: "
                + remoteWorkingInode);

        integrityChecker.executeFix(endpointId.get());
        Assert.assertTrue(validateFix(remoteIdentifier));
    }


}
