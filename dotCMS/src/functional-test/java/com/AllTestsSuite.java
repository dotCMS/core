package com;

import org.junit.runner.RunWith;
import org.junit.runners.Suite;

import com.dotcms.csspreproc.CSSPreProcessServletTest;
import com.dotcms.csspreproc.SassCompilerTest;
import com.dotcms.publisher.ajax.RemotePublishAjaxActionTest;
import com.dotcms.rendering.velocity.viewtools.JSONToolFTest;
import com.dotcms.rest.ContentResourceTest;
import com.dotcms.rest.RoleResourceTest;
import com.dotcms.rest.WebResourceTest;
import com.dotcms.rest.api.v1.sites.rules.ActionResourceFTest;
import com.dotcms.rest.api.v1.sites.rules.ConditionGroupResourceFTest;
import com.dotcms.rest.api.v1.sites.rules.RuleResourceFTest;
import com.dotcms.rest.api.v1.system.i18n.I8NResourceFTest;
import com.dotcms.rest.api.v1.system.ruleengine.ActionletResourceFTest;
import com.dotmarketing.portlets.contentlet.business.FileAssetTest;
import com.dotmarketing.portlets.rules.RulesUnderPageAssetsFTest;
import com.dotmarketing.portlets.rules.actionlet.SetResponseHeaderActionletFTest;
import com.dotmarketing.portlets.rules.actionlet.SetSessionAttributeActionletFTest;
import com.dotmarketing.portlets.rules.business.RulesAPIFTest;
import com.dotmarketing.portlets.rules.conditionlet.CurrentSessionLanguageConditionletFTest;
import com.dotmarketing.portlets.rules.conditionlet.NumberOfTimesPreviouslyVisitedConditionletFTest;
import com.dotmarketing.portlets.rules.conditionlet.PagesViewedConditionletFTest;
import com.dotmarketing.portlets.rules.conditionlet.UsersBrowserLanguageConditionletFTest;
import com.dotmarketing.portlets.rules.conditionlet.UsersSiteVisitsConditionletFTest;
import com.dotmarketing.portlets.rules.conditionlet.VisitedUrlConditionletFTest;
import com.dotmarketing.portlets.rules.conditionlet.VisitorOperatingSystemConditionletFTest;
import com.dotmarketing.portlets.rules.conditionlet.VisitorsCurrentUrlConditionletFTest;
import com.dotmarketing.portlets.structure.business.URLMapTest;
import com.dotmarketing.sitesearch.ajax.SiteSearchAjaxActionTest;
import com.dotmarketing.webdav.WebDavTest;

/**
 * @author Jonathan Gamba.
 *         Date: 3/7/12
 */
@RunWith (Suite.class)
@Suite.SuiteClasses ({

    /**************************
     *      e2e tests         *
     **************************/
    CSSPreProcessServletTest.class, //Needs Enterprise License
    SassCompilerTest.class,
    RemotePublishAjaxActionTest.class, //Needs Enterprise License
    ActionResourceFTest.class,
    ConditionGroupResourceFTest.class,
    RuleResourceFTest.class,
    I8NResourceFTest.class,

    //Rules:Conditionlets.
    ActionletResourceFTest.class, //Needs Enterprise License.
    CurrentSessionLanguageConditionletFTest.class, //Needs Enterprise License.
    NumberOfTimesPreviouslyVisitedConditionletFTest.class, //Needs Enterprise License.
    PagesViewedConditionletFTest.class, //Needs Enterprise License.
    UsersBrowserLanguageConditionletFTest.class, //Needs Enterprise License.
    UsersSiteVisitsConditionletFTest.class, //Needs Enterprise License.
    VisitedUrlConditionletFTest.class, //Needs Enterprise License.
    VisitorOperatingSystemConditionletFTest.class, //Needs Enterprise License.
    VisitorsCurrentUrlConditionletFTest.class, //Needs Enterprise License.

    ContentResourceTest.class, //Needs Enterprise License
    RoleResourceTest.class,
    WebResourceTest.class,
    FileAssetTest.class,
    //Ignored PersonaActionletFTest, see: https://github.com/dotCMS/core/issues/10746
    //PersonaActionletFTest.class, //Needs Enterprise License.
    SetResponseHeaderActionletFTest.class, //Needs Enterprise License.
    SetSessionAttributeActionletFTest.class, //Needs Enterprise License.
    //Ignored VisitorsTagsActionletFTest, see: https://github.com/dotCMS/core/issues/10746
    //VisitorsTagsActionletFTest.class, //Needs Enterprise License.

    //Rules.
    RulesAPIFTest.class, //Needs Enterprise License.
    RulesUnderPageAssetsFTest.class, //Needs Enterprise License.

    URLMapTest.class,
    SiteSearchAjaxActionTest.class,
    WebDavTest.class,
    JSONToolFTest.class
})
public class AllTestsSuite {}
