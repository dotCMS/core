import { type Page } from '@playwright/test';

import { FileField } from '../../file-field/helpers/file-field';

/**
 * Locator wrapper for the Image field (same `dot-edit-content-file-field` component as File).
 */
export class ImageField extends FileField {
    constructor(page: Page, fieldVariable = 'imageField') {
        super(page, fieldVariable);
    }
}
