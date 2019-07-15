import { DotCMSFormConfig, DotCMSError } from '../models';
import { DotApiContentType } from './DotApiContentType';
import { DotApiContent } from './DotApiContent';

// This import allow us to use the type for the form: HTMLDotFormElement
import { Components as _Components } from 'projects/dotcms-field-elements/dist/types/components';
import { DotCMSContentTypeLayoutRow, DotCMSContentType } from 'dotcms-models';

/**
 * Creates and provide methods to render a DotCMS Form
 *
 */
export class DotApiForm {
    private contentType: DotCMSContentType;
    private layout: DotCMSContentTypeLayoutRow[];

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
     * @memberof DotApiForm
     */
    async render(container: HTMLElement) {
        this.contentType =
            this.contentType || (await this.dotApiContentType.get(this.formConfig.identifier));
        this.layout = this.contentType.layout;

        const formTag = this.createForm(this.layout);
        container.append(formTag);
    }

    private shouldSetFormLabel(
        label: string,
        labelConfig: { submit?: string; reset?: string }
    ): boolean {
        return !!(labelConfig && labelConfig[label]);
    }

    private createForm(layout: DotCMSContentTypeLayoutRow[]): HTMLElement {
        const dotFormEl: HTMLDotFormElement = document.createElement('dot-form');

        ['submit', 'reset'].forEach((label: string) => {
            if (this.shouldSetFormLabel(label, this.formConfig.labels)) {
                dotFormEl.setAttribute(`${label}-label`, this.formConfig.labels[label]);
            }
        });

        dotFormEl.layout = layout;
        dotFormEl.fieldsToShow = this.formConfig.fieldsToShow;

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
