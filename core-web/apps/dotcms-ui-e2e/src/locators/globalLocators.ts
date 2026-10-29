/**
 * Locators for the iframes in the main page.
 */
export const iFramesLocators = {
    main_iframe: 'iframe[name="detailFrame"]',
    /**
     * The legacy editor's iframe inside the side panel, the only place content is edited outside
     * the full-screen route since #38303. The panel renders it through the generic legacy-screen
     * panel, hence the test id. Pass it to `frameLocator()` scoped to one panel when several are
     * open.
     */
    legacy_editor_iframe: '[data-testid="legacy-screen-iframe"]',
    wysiwygFrame:
        'iframe[title="Rich Text Area\\. Press ALT-F9 for menu\\. Press ALT-F10 for toolbar\\. Press ALT-0 for help"]',
    dataTestId: '[data-testid="iframe"]'
};

/**
 * Locators for the login functionality.
 */
export const loginLocators = {
    userNameInput: 'input[id="inputtext"]',
    passwordInput: 'input[id="password"]',
    loginBtn: 'submitButton'
};

/**
 * Locators for the Add Content functionality.
 */
export const addContent = {
    addBtn: '#dijit_form_DropDownButton_0',
    addNewContentSubMenu: 'Add New Content',
    addNewMenuLabel: '▼'
};

/**
 * Locators for the Rich Text functionality.
 */
export const contentGeneric = {
    locator: 'articleContent (Generic)',
    label: 'Content (Generic)'
};

export const fileAsset = {
    locator: 'attach_fileFile Asset',
    label: 'File Asset'
};

export const pageAsset = {
    locator: 'descriptionPage',
    label: 'Page'
};

export {} from './navigation/menuLocators';
