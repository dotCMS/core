import {
    DotCMSFormConfig,
    DotCMSContentTypeField,
    DotCMSContentType,
    DotCMSError
} from '../models';
import { DotApiContentType } from './DotApiContentType';
import { DotApiContent } from './DotApiContent';

// This import allow us to use the type for the form: HTMLDotFormElement
import { Components as _Components } from 'projects/dotcms-field-elements/dist/types/components';

/**
 * Creates and provide methods to render a DotCMS Form
 *
 */
export class DotApiForm {
    private contentType: DotCMSContentType;
    private fields: DotCMSContentTypeField[];

    constructor(
        private dotApiContentType: DotApiContentType,
        private formConfig: DotCMSFormConfig,
        private content: DotApiContent,
        defineCustomElements: (win: Window, opt?: any) => Promise<void>
    ) {
        defineCustomElements(formConfig.win || window);
    }

    /**
     * Render form on provided html element
     * @param {HTMLElement} container
     * @memberof DotApiForm
     */
    async render(container: HTMLElement) {
        this.contentType =
            this.contentType || (await this.dotApiContentType.get(this.formConfig.identifier));
        this.fields = this.contentType.fields;

        const formTag = this.createForm(this.fields);
        container.append(formTag);
    }

    private shouldSetFormLabel(
        label: string,
        labelConfig: { submit?: string; reset?: string }
    ): boolean {
        return !!(labelConfig && labelConfig[label]);
    }

    private createForm(fields: DotCMSContentTypeField[]): HTMLElement {
        const dotFormEl: HTMLDotFormElement = document.createElement('dot-form');

        ['submit', 'reset'].forEach((label: string) => {
            if (this.shouldSetFormLabel(label, this.formConfig.labels)) {
                dotFormEl.setAttribute(`${label}-label`, this.formConfig.labels[label]);
            }
        });

        dotFormEl.fields = fields;
        dotFormEl.fieldsToShow = this.formConfig.fields;

        dotFormEl.addEventListener('onSubmit', (e: CustomEvent) => {
            e.preventDefault();
            this.content
                .save({
                    contentHost: this.formConfig.contentHost,
                    stName: this.contentType.variable,
                    ...e.detail
                })
                .then((data: Response) => {
                    if (this.formConfig.onSuccess) {
                        this.formConfig.onSuccess(data);
                    }
                })
                .catch((error: DotCMSError) => {
                    if (this.formConfig.onError) {
                        this.formConfig.onError(error);
                    }
                });
        });

        return dotFormEl;
    }
}
