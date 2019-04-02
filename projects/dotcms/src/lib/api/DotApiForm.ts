import { DotCMSFormConfig, DotCMSContentTypeField } from '../models';
import { DotApiContentType } from './DotApiContentType';
import { DotCMSHttpClient } from '../utils/DotCMSHttpClient';
import { FieldElementsTags } from '../utils/fieldsElementsTags';

/**
 * DotCMS Api Form Builder
 *
 */
export class DotApiForm {
    private dotApiContentType: DotApiContentType;
    private dotCMSHttpClient: DotCMSHttpClient;
    private formScript = '';

    constructor(httpClient: DotCMSHttpClient) {
        this.dotCMSHttpClient = httpClient;
        this.dotApiContentType = new DotApiContentType(this.dotCMSHttpClient);
    }

    /**
     * Returns a Form Builder instance based on configuration
     * @param {DotCMSFormConfig} config
     * @returns {Promise<DotApiForm>}
     * @memberof DotApiForm
     */
    get(config: DotCMSFormConfig): Promise<DotApiForm> {
        return this.dotApiContentType
            .getFields(config.identifier)
            .then((fields: DotCMSContentTypeField[]) => {
                if (config.fields && config.fields.length) {
                    this.createForm(fields, config);
                }
                return this;
            });
    }

    /**
     * Render form on provided html element
     * @param {HTMLElement} container
     * @memberof DotApiForm
     */
    render(container: HTMLElement): void {
        const importScript = document.createElement('script');
        const formTag = document.createElement('div');

        importScript.type = 'module';
        importScript.text = `
            import { defineCustomElements } from "https://unpkg.com/dotcms-field-elements@0.0.2/dist/loader";
            defineCustomElements(window);`;
        formTag.innerHTML = this.formScript;

        container.append(importScript, formTag);
    }

    private createForm(fields: DotCMSContentTypeField[], config: DotCMSFormConfig): void {
        let fieldTags = '';

        fields.map((field) => {
            fieldTags += config.fields.includes(field.variable) ? this.createField(field) : '';
        });

        this.formScript += `<form>${fieldTags}</form>`;
    }

    // tslint:disable-next-line:cyclomatic-complexity
    private createField(field: DotCMSContentTypeField): string {
        const fieldClazz = field.clazz.split('.');
        const fieldTag = FieldElementsTags[fieldClazz[fieldClazz.length - 1]];
        return fieldTag
            ? `
            <${fieldTag}
                ${field.name ? `label="${field.name}"` : ''}
                ${field.defaultValue ? `value="${field.defaultValue}"` : ''}
                ${field.values ? `options="${field.values.split('\r\n').join(',')}"` : ''}
                ${field.hint ? `hint="${field.hint}"` : ''}
                ${field.required ? 'required' : ''}
            ></${fieldTag}>`
            : '';
    }
}
