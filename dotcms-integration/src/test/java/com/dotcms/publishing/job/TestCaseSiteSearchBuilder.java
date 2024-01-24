package com.dotcms.publishing.job;

import com.dotcms.publishing.job.SiteSearchJobImplTest.TestCaseSiteSearch;

public class TestCaseSiteSearchBuilder {

    private boolean defaultPageToDefaultLanguage;
    private boolean defaultContentToDefaultLanguage;
    private boolean siteSearchDefaultLanguage;
    private boolean siteSearchSecondLanguage;
    private boolean siteSearchThirdLanguage;
    private boolean createContentInDefaultLanguage;
    private boolean createContentInSecondLanguage;
    private boolean createContentInThirdLanguage;
    private boolean createPageInDefaultLanguage;
    private boolean createPageInSecondLanguage;
    private boolean expectedResultsWhenSearchingContentInDefaultLanguage;
    private boolean expectedResultsWhenSearchingContentInSecondLanguage;
    private boolean expectedResultsWhenSearchingContentInThirdLanguage;

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

    public TestCaseSiteSearchBuilder siteSearchThirdLanguage(boolean siteSearchThirdLanguage) {
        this.siteSearchThirdLanguage = siteSearchThirdLanguage;
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

    public TestCaseSiteSearchBuilder createContentInThirdLanguage(
            boolean createContentInThirdLanguage) {
        this.createContentInThirdLanguage = createContentInThirdLanguage;
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
            boolean expectedResultsWhenSearchingContentInDefaultLanguage) {
        this.expectedResultsWhenSearchingContentInDefaultLanguage = expectedResultsWhenSearchingContentInDefaultLanguage;
        return this;
    }

    public TestCaseSiteSearchBuilder expectedResultsWhenSearchingContentInSecondLanguage(
            boolean expectedResultsWhenSearchingContentInSecondLanguage) {
        this.expectedResultsWhenSearchingContentInSecondLanguage = expectedResultsWhenSearchingContentInSecondLanguage;
        return this;
    }

    public TestCaseSiteSearchBuilder expectedResultsWhenSearchingContentInThirdLanguage(
            boolean expectedResultsWhenSearchingContentInThirdLanguage) {
        this.expectedResultsWhenSearchingContentInThirdLanguage = expectedResultsWhenSearchingContentInThirdLanguage;
        return this;
    }

    public TestCaseSiteSearch createTestCaseSiteSearch() {
        return new TestCaseSiteSearch(defaultPageToDefaultLanguage, defaultContentToDefaultLanguage,
                siteSearchDefaultLanguage, siteSearchSecondLanguage, siteSearchThirdLanguage,
                createContentInDefaultLanguage,
                createContentInSecondLanguage, createContentInThirdLanguage,
                createPageInDefaultLanguage,
                createPageInSecondLanguage, expectedResultsWhenSearchingContentInDefaultLanguage,
                expectedResultsWhenSearchingContentInSecondLanguage,
                expectedResultsWhenSearchingContentInThirdLanguage);
    }
}