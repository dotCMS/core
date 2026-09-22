import { NewEditContentFormPage } from '@pages';
import { expect, test } from '@playwright/test';
import { ContentType, createFakeContentType, deleteContentType } from '@requests/contentType';
import {
    createFakePayloadDateField,
    createFakePayloadDateTimeField,
    createFakePayloadTextField,
    createFakePayloadTimeField
} from '@utils/dot-content-types.mock';

/**
 * Calendar field round-trip (issue #37670) — the highest-value of the three specs added here.
 *
 * All three calendar types resolve through one function, and the calendar component lost five
 * type assertions when its input was narrowed to `ContentTypeCalendarField`. Date handling is
 * also where a silent transformation bug is least likely to be noticed by eye: a value that
 * comes back shifted by a timezone still looks like a plausible date.
 */
let contentType: ContentType | null = null;
let contentTypeVariable: string;

test.beforeEach(async ({ request }) => {
    contentType = await createFakeContentType(request, {
        name: `E2ECalendarField${Date.now()}`,
        fields: [
            createFakePayloadTextField({ name: 'Title', variable: 'title', sortOrder: 1 }),
            createFakePayloadDateField({ name: 'Date', variable: 'dateField', sortOrder: 2 }),
            createFakePayloadDateTimeField({
                name: 'Date and time',
                variable: 'dateTimeField',
                sortOrder: 3
            }),
            createFakePayloadTimeField({ name: 'Time', variable: 'timeField', sortOrder: 4 })
        ]
    });
    contentTypeVariable = contentType.variable;
});

test.afterEach(async ({ request }) => {
    if (contentType) {
        await deleteContentType(request, contentType.id);
        contentType = null;
    }
});

test('date, date-and-time and time each survive save and reopen @critical', async ({ page }) => {
    const formPage = new NewEditContentFormPage(page);
    await formPage.goToNew(contentTypeVariable);

    await page.getByTestId('title').fill('Calendar round trip');

    // `calendar-input-<variable>` is what the calendar field renders; the bare variable is only
    // an id on PrimeNG's inner input, and the dispatcher's own testid is `field-<variable>`.
    // Waiting on `dateField` matched nothing and simply timed out.
    const dateInput = page.getByTestId('calendar-input-dateField').locator('input').first();
    const dateTimeInput = page.getByTestId('calendar-input-dateTimeField').locator('input').first();
    const timeInput = page.getByTestId('calendar-input-timeField').locator('input').first();

    await expect(dateInput).toBeVisible({ timeout: 20000 });

    await dateInput.fill('09/22/2026');
    await dateTimeInput.fill('09/22/2026 10:30');
    await timeInput.fill('10:30');

    const before = {
        date: await dateInput.inputValue(),
        dateTime: await dateTimeInput.inputValue(),
        time: await timeInput.inputValue()
    };

    await formPage.save();

    // `save()` only waits for the workflow API response, not for the editor to navigate from
    // /content/new/<type> to the saved contentlet. Reloading before that lands re-opens the *new*
    // content form — an empty one — which is why this read back nothing. Same wait the image,
    // file and binary specs already use.
    await page.waitForURL(/\/content\/([a-f0-9-]+)/);
    const [, savedContentIdentifier] = page
        .url()
        .match(/\/content\/([a-f0-9-]+)/) as RegExpMatchArray;
    expect(savedContentIdentifier).toBeTruthy();

    await formPage.goToContent(savedContentIdentifier);

    await expect(dateInput).toBeVisible({ timeout: 20000 });

    // Identical, not merely present: a timezone regression produces a value that is still a
    // valid date, so only comparing against what was entered catches it.
    await expect(dateInput).toHaveValue(before.date);
    await expect(dateTimeInput).toHaveValue(before.dateTime);
    await expect(timeInput).toHaveValue(before.time);
});
