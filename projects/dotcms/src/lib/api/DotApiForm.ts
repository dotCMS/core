import { DotCMSFormConfig, DotCMSContentTypeField } from '../models';
import { DotApiContentType } from './DotApiContentType';

enum FieldElementsTags {
    Text = 'dot-textfield',
    Checkbox = 'dot-checkbox',
    Select = 'dot-dropdown'
}

/**
 * Creates and provide methods to render a DotCMS Form
 *
 */
export class DotApiForm {
    private formConfig: DotCMSFormConfig;
    private fields: DotCMSContentTypeField[];
    private dotApiContentType: DotApiContentType;

    constructor(dotApiContentType: DotApiContentType, config: DotCMSFormConfig) {
        this.dotApiContentType = dotApiContentType;
        this.formConfig = config;
    }

    /**
     * Render form on provided html element
     * @param {HTMLElement} container
     * @memberof DotApiForm
     */
    async render(container: HTMLElement) {
        this.fields =
            this.fields || (await this.dotApiContentType.getFields(this.formConfig.identifier));

        const fieldScript = this.createFieldTags(this.fields);
        const formScript = this.createForm(fieldScript);
        const importScript = document.createElement('script');
        const formTag = document.createElement('div');

        importScript.type = 'module';
        importScript.text = `
            import { defineCustomElements } from "https://unpkg.com/dotcms-field-elements@0.0.2/dist/loader";
            defineCustomElements(window);`;
        formTag.innerHTML = formScript;

        container.append(importScript, formTag);
    }

    private createForm(fieldTags: string): string {
        return `<form>${fieldTags}</form>`;
    }

    private createFieldTags(fields: DotCMSContentTypeField[]): string {
        const fieldTags = fields
            .map((field: DotCMSContentTypeField) =>
                this.formConfig.fields.includes(field.variable) ? this.createField(field) : ''
            )
            .join('');
        return fieldTags;
    }

    private getFieldTag(field: DotCMSContentTypeField): string {
        return FieldElementsTags[field.fieldType];
    }

    private formatValuesAttribute(values: string, fieldTag: string): string {
        const breakLineTags = ['dot-checkbox', 'dot-dropdown', 'dot-radio-button'];
        let formattedValue = values;

        // Todo: complete with other DOT-FIELDS as they get created
        if (breakLineTags.includes(fieldTag)) {
            formattedValue = values.split('\r\n').join(',');
        }
        return formattedValue;
    }

    // tslint:disable-next-line:cyclomatic-complexity
    private createField(field: DotCMSContentTypeField): string {
        const fieldTag = this.getFieldTag(field);
        return fieldTag
            ? `
            <${fieldTag}
                ${field.name ? `label="${field.name}"` : ''}
                ${field.defaultValue ? `value="${field.defaultValue}"` : ''}
                ${
                    field.values
                        ? `options="${this.formatValuesAttribute(field.values, fieldTag)}"`
                        : ''
                }
                ${field.hint ? `hint="${field.hint}"` : ''}
                ${field.required ? 'required' : ''}
            ></${fieldTag}>`
            : '';
    }
}
