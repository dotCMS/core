import { DotCMSContentType } from 'dotcms-models';

import { DotApiContentType } from './DotApiContentType';

import { DotCMSFormConfig } from '../models';

// This import allow us to use the type for the form: HTMLDotFormElement

/**
 * Creates and provide methods to render a DotCMS Form
 *
 */
export class DotApiForm {
    constructor(
        private dotApiContentType: DotApiContentType,
        private formConfig: DotCMSFormConfig
    ) {}

    /**
     * Render form on provided html element
     * @memberof DotApiForm
     */
    async get(): Promise<DotCMSContentType> {
        return this.dotApiContentType.get(this.formConfig.identifier);
    }
}
