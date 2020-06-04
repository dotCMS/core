package com.dotmarketing.portlets.rules;

import com.dotcms.LicenseTestUtil;
import com.dotcms.csspreproc.SassCompilerTest;
import com.dotcms.datagen.HTMLPageDataGen;
import com.dotcms.datagen.TemplateDataGen;
import com.dotcms.enterprise.rules.RulesAPI;
import com.dotmarketing.beans.Host;
import com.dotmarketing.business.APILocator;
import com.dotmarketing.db.HibernateUtil;
import com.dotmarketing.exception.DotDataException;
import com.dotmarketing.exception.DotSecurityException;
import com.dotmarketing.portlets.contentlet.model.Contentlet;
import com.dotmarketing.portlets.contentlet.model.IndexPolicy;
import com.dotmarketing.portlets.folders.model.Folder;
import com.dotmarketing.portlets.htmlpageasset.model.HTMLPageAsset;
import com.dotmarketing.portlets.rules.actionlet.CountRulesActionlet;
import com.dotmarketing.portlets.rules.model.ParameterModel;
import com.dotmarketing.portlets.rules.model.Rule;
import com.dotmarketing.portlets.rules.model.RuleAction;
import com.dotmarketing.portlets.templates.model.Template;
import com.dotmarketing.servlets.test.ServletTestRunner;
import com.dotmarketing.util.Logger;
import com.dotmarketing.util.UUIDGenerator;
import com.liferay.portal.model.User;
import org.junit.BeforeClass;
import org.junit.Test;

import javax.servlet.http.HttpServletRequest;
import java.io.FileNotFoundException;
import java.io.IOException;
import java.net.URL;
import java.net.URLConnection;
import java.util.ArrayList;
import java.util.List;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertTrue;

/**
 * Created by Oscar Arrieta on 2/24/16.
 */

public class RulesUnderPageAssetsFTest{

    private Host host;
    private User sysUser;
    private HttpServletRequest request;
    private final String indexUrl;

    @BeforeClass
    public static void prepare () throws Exception {
        LicenseTestUtil.getLicense();
    }

    public RulesUnderPageAssetsFTest() throws DotDataException, DotSecurityException {
        sysUser = APILocator.getUserAPI().getSystemUser();
        host = APILocator.getHostAPI().findDefaultHost(sysUser, false);

        if (!host.isLive()) {
            try {
                HibernateUtil.startTransaction();
                APILocator.getHostAPI().publish(host, sysUser, false);
                HibernateUtil.closeAndCommitTransaction();
            } catch (Exception e) {
                HibernateUtil.rollbackTransaction();
                Logger.error(SassCompilerTest.class, e.getMessage());
            } finally {
                HibernateUtil.closeSessionSilently();
            }

            APILocator.getContentletAPI().isInodeIndexed(host.getInode());
            APILocator.getContentletAPI().isInodeIndexed(host.getInode(), true);
        }

        request = ServletTestRunner.localRequest.get();
        String serverName = request.getServerName();
        int serverPort = request.getServerPort();
        indexUrl = String.format("http://%s:%s", serverName, serverPort);

        //Clean the Attribute.
        request.getServletContext().removeAttribute("count-" + Rule.FireOn.EVERY_PAGE.getCamelCaseName());
    }

    //Test firing rules only on live pages.
    @Test
    public void testFireRuleUnderLivePage() throws Exception {
        final String folderPath = "/RuleUnderPageFolder/";
        final String pageName = "testFireRuleUnderLivePagePage" + System.currentTimeMillis();
        //HibernateUtil.setAsyncCommitListenersFinalization(false);

        //Create Folder.
        APILocator.getFolderAPI().createFolders(folderPath, host, sysUser, false);
        Folder folder = APILocator.getFolderAPI().findFolderByPath(folderPath, host, sysUser, false);

        //Create Template.
        Template template = new TemplateDataGen().nextPersisted();

        APILocator.getVersionableAPI().setLive(template);
        //Create Working Page.
        HTMLPageAsset dummyPage = new HTMLPageDataGen(folder, template).friendlyName(pageName).pageURL(pageName)
            .title(pageName).nextPersisted();


        boolean isDummyPageIndexed = APILocator.getContentletAPI().isInodeIndexed(dummyPage.getInode());
        Logger.info(this, "isDummyPageIndexed: " + isDummyPageIndexed);

        //Create Rule with page as Parent.
        createRuleUnderPage(dummyPage);

        //Hit working page and test rule didn't fire.
        try{
            makeRequest(indexUrl + folderPath + pageName);
        } catch (FileNotFoundException fnfexc){
            //Do nothing. This is expected cause 404 returned.
        }
        assertEquals(null, request.getServletContext().getAttribute("count-" + Rule.FireOn.EVERY_PAGE.getCamelCaseName()));

        //Publish Page.
        dummyPage.setIndexPolicy(IndexPolicy.FORCE);
        dummyPage.setIndexPolicyDependencies(IndexPolicy.FORCE);
        dummyPage.setBoolProperty(Contentlet.IS_TEST_MODE, true);
        APILocator.getContentletAPI().publish(dummyPage, sysUser, false);

        //Hit live page and test rule did fire.
        makeRequest(indexUrl + folderPath + pageName);
        Integer count = (Integer) request.getServletContext().getAttribute("count-" + Rule.FireOn.EVERY_PAGE.getCamelCaseName());
        assertTrue(count > 0);

        //Remove Page with rules.
        dummyPage.setIndexPolicy(IndexPolicy.FORCE);
        dummyPage.setIndexPolicyDependencies(IndexPolicy.FORCE);
        dummyPage.setBoolProperty(Contentlet.IS_TEST_MODE, true);
        APILocator.getContentletAPI().unpublish(dummyPage, sysUser, false);
        dummyPage.setIndexPolicy(IndexPolicy.FORCE);
        dummyPage.setIndexPolicyDependencies(IndexPolicy.FORCE);
        dummyPage.setBoolProperty(Contentlet.IS_TEST_MODE, true);
        APILocator.getContentletAPI().archive(dummyPage, sysUser, false);
        APILocator.getContentletAPI().isInodeIndexedArchived(dummyPage.getInode());
        assertTrue(dummyPage.isArchived());
        dummyPage.setIndexPolicy(IndexPolicy.FORCE);
        dummyPage.setIndexPolicyDependencies(IndexPolicy.FORCE);
        dummyPage.setBoolProperty(Contentlet.IS_TEST_MODE, true);
        APILocator.getContentletAPI().delete(dummyPage, sysUser, false);

        //Remove Template.
        APILocator.getTemplateAPI().delete(template, sysUser, false);

        //Remove Folder.
        APILocator.getFolderAPI().delete(folder, sysUser, false);
    }

    //todo: Test copy page with rules.
    @Test
    public void copyPageWithRules() throws Exception {
        final String folderPath = "/RuleUnderPageFolder/";
        final String targetFolderPath = "/TargetPageFolder/";
        final String pageName = "copyPageWithRulesPage" + System.currentTimeMillis();
        //HibernateUtil.setAsyncCommitListenersFinalization(false);

        //Create Folder.
        APILocator.getFolderAPI().createFolders(folderPath, host, sysUser, false);
        Folder folder = APILocator.getFolderAPI().findFolderByPath(folderPath, host, sysUser, false);

        //Create Template.
        Template template = new TemplateDataGen().nextPersisted();

        APILocator.getVersionableAPI().setLive(template);
        //Create Working Page.
        HTMLPageAsset dummyPage = new HTMLPageDataGen(folder, template).friendlyName(pageName).pageURL(pageName)
            .title(pageName).nextPersisted();

        boolean isDummyPageIndexed = APILocator.getContentletAPI().isInodeIndexed(dummyPage.getInode());
        Logger.info(this, "isDummyPageIndexed: " + isDummyPageIndexed);

        //Create Rule with page as Parent.
        createRuleUnderPage(dummyPage);

        //Publish Page.
        dummyPage.setIndexPolicy(IndexPolicy.FORCE);
        dummyPage.setIndexPolicyDependencies(IndexPolicy.FORCE);
        dummyPage.setBoolProperty(Contentlet.IS_TEST_MODE, true);
        APILocator.getContentletAPI().publish(dummyPage, sysUser, false);

        //Hit live page and test rule did fire.
        makeRequest(indexUrl + folderPath + pageName);
        Integer count = (Integer) request.getServletContext().getAttribute("count-" + Rule.FireOn.EVERY_PAGE.getCamelCaseName());
        assertTrue(count > 0);

        //Create target folder.
        APILocator.getFolderAPI().createFolders(targetFolderPath, host, sysUser, false);
        Folder targetfolder = APILocator.getFolderAPI().findFolderByPath(targetFolderPath, host, sysUser, false);

        //Copy page to target folder.
        dummyPage.setIndexPolicy(IndexPolicy.FORCE);
        dummyPage.setIndexPolicyDependencies(IndexPolicy.FORCE);
        dummyPage.setBoolProperty(Contentlet.IS_TEST_MODE, true);
        Contentlet targetHTMLPageAsset = APILocator.getContentletAPI().copyContentlet(dummyPage, targetfolder, sysUser, false);

        //Hit page under target folder and test it fired.
        makeRequest(indexUrl + targetFolderPath + pageName);
        Integer targetcount = (Integer) request.getServletContext().getAttribute("count-" + Rule.FireOn.EVERY_PAGE.getCamelCaseName());
        assertTrue(targetcount > count);

        //Remove Page with rules.
        dummyPage.setIndexPolicy(IndexPolicy.FORCE);
        dummyPage.setIndexPolicyDependencies(IndexPolicy.FORCE);
        dummyPage.setBoolProperty(Contentlet.IS_TEST_MODE, true);
        APILocator.getContentletAPI().unpublish(dummyPage, sysUser, false);
        dummyPage.setIndexPolicy(IndexPolicy.FORCE);
        dummyPage.setIndexPolicyDependencies(IndexPolicy.FORCE);
        dummyPage.setBoolProperty(Contentlet.IS_TEST_MODE, true);
        APILocator.getContentletAPI().archive(dummyPage, sysUser, false);

        APILocator.getContentletAPI().isInodeIndexedArchived(dummyPage.getInode());
        assertTrue(dummyPage.isArchived());
        dummyPage.setIndexPolicy(IndexPolicy.FORCE);
        dummyPage.setIndexPolicyDependencies(IndexPolicy.FORCE);
        dummyPage.setBoolProperty(Contentlet.IS_TEST_MODE, true);
        APILocator.getContentletAPI().delete(dummyPage, sysUser, false);

        targetHTMLPageAsset.setIndexPolicy(IndexPolicy.FORCE);
        targetHTMLPageAsset.setIndexPolicyDependencies(IndexPolicy.FORCE);
        targetHTMLPageAsset.setBoolProperty(Contentlet.IS_TEST_MODE, true);
        APILocator.getContentletAPI().unpublish(targetHTMLPageAsset, sysUser, false);
        targetHTMLPageAsset.setIndexPolicy(IndexPolicy.FORCE);
        targetHTMLPageAsset.setIndexPolicyDependencies(IndexPolicy.FORCE);
        targetHTMLPageAsset.setBoolProperty(Contentlet.IS_TEST_MODE, true);
        APILocator.getContentletAPI().archive(targetHTMLPageAsset, sysUser, false);
        APILocator.getContentletAPI().isInodeIndexedArchived(targetHTMLPageAsset.getInode());
        assertTrue(targetHTMLPageAsset.isArchived());
        targetHTMLPageAsset.setIndexPolicy(IndexPolicy.FORCE);
        targetHTMLPageAsset.setIndexPolicyDependencies(IndexPolicy.FORCE);
        targetHTMLPageAsset.setBoolProperty(Contentlet.IS_TEST_MODE, true);
        APILocator.getContentletAPI().delete(targetHTMLPageAsset, sysUser, false);
        //Remove Template.
        APILocator.getTemplateAPI().delete(template, sysUser, false);
        //Remove Folder.
        APILocator.getFolderAPI().delete(folder, sysUser, false);
        APILocator.getFolderAPI().delete(targetfolder, sysUser, false);
    }

    //Test delete page with rules.
    @Test
    public void deletePageWithRules() throws Exception {
        final String folderPath = "/DeletePageFolder/";
        final String pageName = "deletePageWithRulesPage" + System.currentTimeMillis();
        //HibernateUtil.setAsyncCommitListenersFinalization(false);

        //Create Folder.
        APILocator.getFolderAPI().createFolders(folderPath, host, sysUser, false);
        Folder folder = APILocator.getFolderAPI().findFolderByPath(folderPath, host, sysUser, false);

        //Create Template.
        Template template = new TemplateDataGen().nextPersisted();

        APILocator.getVersionableAPI().setLive(template);
        //Create Working Page.
        HTMLPageAsset dummyPage = new HTMLPageDataGen(folder, template).friendlyName(pageName).pageURL(pageName)
            .title(pageName).nextPersisted();

        boolean isDummyPageIndexed = APILocator.getContentletAPI().isInodeIndexed(dummyPage.getInode());
        Logger.info(this, "isDummyPageIndexed: " + isDummyPageIndexed);

        //Create Rule with page as Parent.
        createRuleUnderPage(dummyPage);

        //Publish Page.
        dummyPage.setIndexPolicy(IndexPolicy.FORCE);
        dummyPage.setIndexPolicyDependencies(IndexPolicy.FORCE);
        dummyPage.setBoolProperty(Contentlet.IS_TEST_MODE, true);
        APILocator.getContentletAPI().publish(dummyPage, sysUser, false);

        //Get all the rules from page.
        List<Rule> rulesByParent = APILocator.getRulesAPI().getAllRulesByParent(dummyPage, sysUser, false);
        assertEquals(1, rulesByParent.size());

        //Remove Page with rules.
        dummyPage.setIndexPolicy(IndexPolicy.FORCE);
        dummyPage.setIndexPolicyDependencies(IndexPolicy.FORCE);
        dummyPage.setBoolProperty(Contentlet.IS_TEST_MODE, true);
        APILocator.getContentletAPI().unpublish(dummyPage, sysUser, false);

        dummyPage.setIndexPolicy(IndexPolicy.FORCE);
        dummyPage.setIndexPolicyDependencies(IndexPolicy.FORCE);
        dummyPage.setBoolProperty(Contentlet.IS_TEST_MODE, true);
        APILocator.getContentletAPI().archive(dummyPage, sysUser, false);
        APILocator.getContentletAPI().isInodeIndexedArchived(dummyPage.getInode());
        assertTrue(dummyPage.isArchived());

        dummyPage.setIndexPolicy(IndexPolicy.FORCE);
        dummyPage.setIndexPolicyDependencies(IndexPolicy.FORCE);
        dummyPage.setBoolProperty(Contentlet.IS_TEST_MODE, true);
        APILocator.getContentletAPI().delete(dummyPage, sysUser, false);

        //Get all rules from the deleted page, shouldn't be any.
        rulesByParent = APILocator.getRulesAPI().getAllRulesByParent(dummyPage, sysUser, false);
        assertEquals(0, rulesByParent.size());

        //Remove Template.
        APILocator.getTemplateAPI().delete(template, sysUser, false);

        //Remove Folder.
        APILocator.getFolderAPI().delete(folder, sysUser, false);
    }

    //Test hit another page, one without rules.
    @Test
    public void testFireRuleAnotherPage() throws Exception {
        final String folderPath = "/RuleUnderPageFolder/";
        final String pageName = "DummyPage"  + System.currentTimeMillis();;
        final String secondPageName = "SecondDummyPage"  + System.currentTimeMillis();

        //HibernateUtil.setAsyncCommitListenersFinalization(false);

        //Create Folder.
        APILocator.getFolderAPI().createFolders(folderPath, host, sysUser, false);
        Folder folder = APILocator.getFolderAPI().findFolderByPath(folderPath, host, sysUser, false);

        //Create Template.
        Template template = new TemplateDataGen().nextPersisted();

        APILocator.getVersionableAPI().setLive(template);
        //Create Working Page.
        HTMLPageAsset dummyPage = new HTMLPageDataGen(folder, template).friendlyName(pageName).pageURL(pageName)
            .title(pageName).nextPersisted();

        HTMLPageAsset secondDummyPage = new HTMLPageDataGen(folder, template).friendlyName(secondPageName)
            .pageURL(secondPageName).title(secondPageName).nextPersisted();

        boolean isDummyPageIndexed = APILocator.getContentletAPI().isInodeIndexed(dummyPage.getInode());
        Logger.info(this, "isDummyPageIndexed: " + isDummyPageIndexed);

        boolean isSecondDummyPageIndexed = APILocator.getContentletAPI().isInodeIndexed(secondDummyPage.getInode());
        Logger.info(this, "isSecondDummyPageIndexed: " + isSecondDummyPageIndexed);

        //Create Rule with page as Parent.
        createRuleUnderPage(dummyPage);

        //Publish Page.
        dummyPage.setIndexPolicy(IndexPolicy.FORCE);
        dummyPage.setIndexPolicyDependencies(IndexPolicy.FORCE);
        APILocator.getContentletAPI().publish(dummyPage, sysUser, false);
        secondDummyPage.setIndexPolicy(IndexPolicy.FORCE);
        secondDummyPage.setIndexPolicyDependencies(IndexPolicy.FORCE);
        APILocator.getContentletAPI().publish(secondDummyPage, sysUser, false);

        //Hit live page and test rule did fire.
        makeRequest(indexUrl + folderPath + pageName);
        Integer count = (Integer) request.getServletContext().getAttribute("count-" + Rule.FireOn.EVERY_PAGE.getCamelCaseName());
        assertTrue(count > 0);

        //Hit live second page and test rule didn't fire.
        makeRequest(indexUrl + folderPath + secondPageName);
        Integer secondCount = (Integer) request.getServletContext().getAttribute("count-" + Rule.FireOn.EVERY_PAGE.getCamelCaseName());
        assertEquals(count, secondCount);

        //Remove Page with rules.
        dummyPage.setIndexPolicy(IndexPolicy.FORCE);
        dummyPage.setIndexPolicyDependencies(IndexPolicy.FORCE);
        dummyPage.setBoolProperty(Contentlet.IS_TEST_MODE, true);
        APILocator.getContentletAPI().unpublish(dummyPage, sysUser, false);

        dummyPage.setIndexPolicy(IndexPolicy.FORCE);
        dummyPage.setIndexPolicyDependencies(IndexPolicy.FORCE);
        dummyPage.setBoolProperty(Contentlet.IS_TEST_MODE, true);

        APILocator.getContentletAPI().archive(dummyPage, sysUser, false);
        APILocator.getContentletAPI().isInodeIndexedArchived(dummyPage.getInode());
        assertTrue(dummyPage.isArchived());
        dummyPage.setIndexPolicy(IndexPolicy.FORCE);
        dummyPage.setIndexPolicyDependencies(IndexPolicy.FORCE);
        dummyPage.setBoolProperty(Contentlet.IS_TEST_MODE, true);

        APILocator.getContentletAPI().delete(dummyPage, sysUser, false);

        secondDummyPage.setIndexPolicy(IndexPolicy.FORCE);
        secondDummyPage.setIndexPolicyDependencies(IndexPolicy.FORCE);
        secondDummyPage.setBoolProperty(Contentlet.IS_TEST_MODE, true);
        APILocator.getContentletAPI().unpublish(secondDummyPage, sysUser, false);

        secondDummyPage.setIndexPolicy(IndexPolicy.FORCE);
        secondDummyPage.setIndexPolicyDependencies(IndexPolicy.FORCE);
        secondDummyPage.setBoolProperty(Contentlet.IS_TEST_MODE, true);
        APILocator.getContentletAPI().archive(secondDummyPage, sysUser, false);
        APILocator.getContentletAPI().isInodeIndexedArchived(secondDummyPage.getInode());

        assertTrue(secondDummyPage.isArchived());
        secondDummyPage.setIndexPolicy(IndexPolicy.FORCE);
        secondDummyPage.setIndexPolicyDependencies(IndexPolicy.FORCE);
        secondDummyPage.setBoolProperty(Contentlet.IS_TEST_MODE, true);

        APILocator.getContentletAPI().delete(secondDummyPage, sysUser, false);

        //Remove Template.
        APILocator.getTemplateAPI().delete(template, sysUser, false);

        //Remove Folder.
        APILocator.getFolderAPI().delete(folder, sysUser, false);
    }

    //Util Methods.
    private Template createDummyTemplate() throws DotSecurityException, DotDataException {
        Template template=new Template();
        template.setTitle("Test template " + UUIDGenerator.generateUuid());
        template.setBody("<html><body>Test Rule Under Page</body></html>");
        return APILocator.getTemplateAPI().saveTemplate(template, host, sysUser, false);
    }

    private void createRuleUnderPage(HTMLPageAsset htmlPageAsset) throws Exception {
        RulesAPI rulesAPI = APILocator.getRulesAPI();

        // Create Rule
        Rule rule = new Rule();
        rule.setName(UUIDGenerator.generateUuid() + "-Rule");
        rule.setParent(htmlPageAsset.getIdentifier());
        rule.setEnabled(true);
        rule.setFireOn(Rule.FireOn.EVERY_PAGE);

        rulesAPI.saveRule(rule, sysUser, false);

        RuleAction action = new RuleAction();
        action.setActionlet(CountRulesActionlet.class.getSimpleName());
        action.setRuleId(rule.getId());

        ParameterModel fireOnParam = new ParameterModel();
        fireOnParam.setOwnerId(action.getId());
        fireOnParam.setKey(CountRulesActionlet.PARAMETER_NAME);
        fireOnParam.setValue("count-" + Rule.FireOn.EVERY_PAGE.getCamelCaseName());

        List<ParameterModel> params = new ArrayList<>();
        params.add(fireOnParam);

        action.setParameters(params);

        rulesAPI.saveRuleAction(action, sysUser, false);
    }

    private URLConnection makeRequest(String urlStr) throws IOException {
        return makeRequest(urlStr, null);
    }

    private URLConnection makeRequest(String urlStr, String cookie) throws IOException {
        URL url = new URL(urlStr);
        URLConnection con = url.openConnection();

        if (cookie != null) {
            con.setRequestProperty("Cookie", cookie);
        }

        con.connect();
        con.getInputStream();
        return con;
    }
}
