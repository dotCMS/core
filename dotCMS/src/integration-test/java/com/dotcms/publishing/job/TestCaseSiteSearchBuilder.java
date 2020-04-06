package com.dotcms.publishing.job;

import com.dotcms.publishing.job.SiteSearchJobImplTest.ExpectedResults;
import com.dotcms.publishing.job.SiteSearchJobImplTest.TestCaseSiteSearch;

public class TestCaseSiteSearchBuilder {

    private boolean defaultPageToDefaultLanguage;
    private boolean defaultContentToDefaultLanguage;
    private boolean siteSearchDefaultLanguage;
    private boolean siteSearchSecondLanguage;
    private boolean createContentInDefaultLanguage;
    private boolean createContentInSecondLanguage;
    private boolean createPageInDefaultLanguage;
    private boolean createPageInSecondLanguage;
    private ExpectedResults expectedResultsWhenSearchingContentInDefaultLanguage;
    private ExpectedResults expectedResultsWhenSearchingContentInSecondLanguage;

    public TestCaseSiteSearchBuilder defaultPageToDefaultLanguage(
            boolean defaultPageToDefaultLanguage) {
        this.defaultPageToDefaultLanguage = defaultPageToDefaultLanguage;
        return this;
    }

    public TestCaseSiteSearchBuilder defaultContentToDefaultLanguage(
            boolean defaultContentToDefaultLanguage) {
        this.defaultContentToDefaultLanguage = defaultContentToDefaultLanguage;
        return this;
    }

    public TestCaseSiteSearchBuilder siteSearchDefaultLanguage(
            boolean siteSearchDefaultLanguage) {
        this.siteSearchDefaultLanguage = siteSearchDefaultLanguage;
        return this;
    }

    public TestCaseSiteSearchBuilder siteSearchSecondLanguage(boolean siteSearchSecondLanguage) {
        this.siteSearchSecondLanguage = siteSearchSecondLanguage;
        return this;
    }

    public TestCaseSiteSearchBuilder createContentInDefaultLanguage(
            boolean createContentInDefaultLanguage) {
        this.createContentInDefaultLanguage = createContentInDefaultLanguage;
        return this;
    }

    public TestCaseSiteSearchBuilder createContentInSecondLanguage(
            boolean createContentInSecondLanguage) {
        this.createContentInSecondLanguage = createContentInSecondLanguage;
        return this;
    }

    public TestCaseSiteSearchBuilder createPageInDefaultLanguage(
            boolean createPageInDefaultLanguage) {
        this.createPageInDefaultLanguage = createPageInDefaultLanguage;
        return this;
    }

    public TestCaseSiteSearchBuilder createPageInSecondLanguage(
            boolean createPageInSecondLanguage) {
        this.createPageInSecondLanguage = createPageInSecondLanguage;
        return this;
    }

    public TestCaseSiteSearchBuilder expectedResultsWhenSearchingContentInDefaultLanguage(
            ExpectedResults expectedResultsWhenSearchingContentInDefaultLanguage) {
        this.expectedResultsWhenSearchingContentInDefaultLanguage = expectedResultsWhenSearchingContentInDefaultLanguage;
        return this;
    }

    public TestCaseSiteSearchBuilder expectedResultsWhenSearchingContentInSecondLanguage(
            ExpectedResults expectedResultsWhenSearchingContentInSecondLanguage) {
        this.expectedResultsWhenSearchingContentInSecondLanguage = expectedResultsWhenSearchingContentInSecondLanguage;
        return this;
    }

    public TestCaseSiteSearch createTestCaseSiteSearch() {
        return new TestCaseSiteSearch(defaultPageToDefaultLanguage, defaultContentToDefaultLanguage,
                siteSearchDefaultLanguage, siteSearchSecondLanguage, createContentInDefaultLanguage,
                createContentInSecondLanguage, createPageInDefaultLanguage,
                createPageInSecondLanguage, expectedResultsWhenSearchingContentInDefaultLanguage,
                expectedResultsWhenSearchingContentInSecondLanguage);
    }
}