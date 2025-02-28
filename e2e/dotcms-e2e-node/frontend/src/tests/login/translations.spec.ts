import { expect, test } from "@playwright/test";
import { assert } from "console";
import { waitForVisibleAndCallback } from "@utils/utils";

const languages = [
  { language: "español (España)", translation: "¡Bienvenido!" },
  { language: "italiano (Italia)", translation: "Benvenuto!" },
  { language: "français (France)", translation: "Bienvenue !" },
  { language: "Deutsch (Deutschland)", translation: "Willkommen!" },
  { language: "中文 (中国)", translation: "欢迎" },
  { language: "Nederlands (Nederland)", translation: "Welkom!" },
  { language: "русский (Россия)", translation: "Добро пожаловать!" },
  { language: "suomi (Suomi)", translation: "Tervetuloa!" },
];

/**
 * Test to validate the translations of the login page
 */
languages.forEach((list) => {
  test(`Validate Translation: ${list.language}`, async ({ page }) => {
    const { language, translation } = list;

    await page.goto("/dotAdmin");
    const dropdownTriggerLocator = page.getByLabel("dropdown trigger");
    await waitForVisibleAndCallback(dropdownTriggerLocator, () =>
      dropdownTriggerLocator.click(),
    );

    const pageByTextLocator = page.getByText(language);
    await waitForVisibleAndCallback(pageByTextLocator, () =>
      pageByTextLocator.click(),
    );

    // Assertion of the translation
    assert(await expect(page.getByTestId("header")).toContainText(translation));
  });
});
