package com.dotmarketing.business;

import com.dotmarketing.business.web.UserWebAPI;
import com.dotmarketing.business.web.WebAPILocator;
import com.dotmarketing.portlets.categories.business.CategoryAPI;
import com.dotmarketing.portlets.contentlet.business.ContentletAPI;
import com.dotmarketing.portlets.contentlet.business.HostAPI;
import com.dotcms.enterprise.rules.RulesAPI;
import com.dotmarketing.portlets.languagesmanager.business.LanguageAPI;

/**
 * Proxy the API Locator behind non-static method calls. This enables the decoupling that is required for proper mocking.
 *
 * To enable unit testing in your class, create a package-private constructor that takes an instance of this class (and any other types that you'll
 * be mocking). See RuleResource and RuleResourceTest for an example.
 *
 * This is, at best, a partial implementation, suitable only for true, single-method unit testing. Because this class does not provide a way to set the instance
 * of APILocator that is ultimately called, your classes under test will have to pass the same mock-instance of ApiProvider downward into any
 * dependent calls. Just consider it motivation to keep unit tests as local as possible.
 *
 * @author Geoff M. Grnaum
 * @version 1.0.0
 * @since 3.2.0
 */
public class ApiProvider {

    public HostAPI hostAPI() {
        return APILocator.getHostAPI();
    }

    public RulesAPI rulesAPI() {
        return APILocator.getRulesAPI();
    }

    public UserAPI userAPI() {
        return APILocator.getUserAPI();
    }

    public LayoutAPI layoutAPI() { return APILocator.getLayoutAPI(); }

    public LanguageAPI languageAPI() { return APILocator.getLanguageAPI(); }

    public ContentletAPI contentletAPI() { return APILocator.getContentletAPI(); }

    public PermissionAPI permissionAPI() { return APILocator.getPermissionAPI(); }

    public CategoryAPI categoryAPI() { return APILocator.getCategoryAPI(); }

    /*******  Web API Accessors ******/
    public UserWebAPI userWebAPI() {
        return WebAPILocator.getUserWebAPI();
    }
}

