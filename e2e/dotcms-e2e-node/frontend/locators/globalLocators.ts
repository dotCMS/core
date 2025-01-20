/**
 * Locators for the iframes in the main page.
 */
export const iFramesLocators = {
  main_iframe: 'iframe[name="detailFrame"]',
  dot_iframe: 'dot-iframe-dialog iframe[name="detailFrame"]',
  wysiwygFrame:
    'iframe[title="Rich Text Area\\. Press ALT-F9 for menu\\. Press ALT-F10 for toolbar\\. Press ALT-0 for help"]',
  dataTestId: '[data-testid="iframe"]',
  dot_edit_iframe: 'dot-edit-contentlet iframe[name="detailFrame"]',
};

/**
 * Locators for the login functionality.
 */
export const loginLocators = {
  userNameInput: 'input[id="inputtext"]',
  passwordInput: 'input[id="password"]',
  loginBtn: "submitButton",
};

/**
 * Locators for the Add Content functionality.
 */
export const addContent = {
  addBtn: "#dijit_form_DropDownButton_0",
  addNewContentSubMenu: "Add New Content",
  addNewMenuLabel: "▼",
};

/**
 * Locators for the Rich Text functionality.
 */
export const contentGeneric = {
  locator: "articleContent (Generic)",
  label: "Content (Generic)",
};

export const fileAsset = {
  locator: "attach_fileFile Asset",
  label: "File Asset",
};

export const pageAsset = {
  locator: "descriptionPage",
  label: "Page",
};

export {} from "./navigation/menuLocators";
