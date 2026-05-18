import { expect, test } from '@playwright/test';
import { waitForVisibleAndCallback } from '@utils/utils';

const languages = [
    { language: 'español (España)', translation: '¡Bienvenido!' },
    { language: 'italiano (Italia)', translation: 'Benvenuto!' },
    { language: 'français (France)', translation: 'Bienvenue !' },
    { language: 'Deutsch (Deutschland)', translation: 'Willkommen!' },
    { language: '中文 (中国)', translation: '欢迎' },
    { language: 'Nederlands (Nederland)', translation: 'Welkom!' },
    { language: 'русский (Россия)', translation: 'Добро пожаловать!' },
    { language: 'suomi (Suomi)', translation: 'Tervetuloa!' }
];

/**
 * Test to validate the translations of the login page
 */
languages.forEach((list) => {
    test(`Validate Translation: ${list.language}`, async ({ page }) => {
        const { language, translation } = list;

        await page.goto('/dotAdmin');
        const dropdownTriggerLocator = page.getByLabel('dropdown trigger');
        await waitForVisibleAndCallback(dropdownTriggerLocator, () =>
            dropdownTriggerLocator.click()
        );

        const pageContentLocator = dropdownTriggerLocator.locator('xpath=ancestor::body');
        const languageOptionLocator = pageContentLocator.getByText(language);
        await languageOptionLocator.waitFor({ state: 'visible' });
        await languageOptionLocator.click();

        await expect(pageContentLocator.locator('[data-testid="header"]')).toContainText(
            translation
        );
    });
});
