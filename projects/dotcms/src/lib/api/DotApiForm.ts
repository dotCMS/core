import { DotCMSFormConfig, DotCMSContentTypeField } from '../models';
import { DotApiContentType } from './DotApiContentType';
import { DotCMSHttpClient } from '../utils/DotCMSHttpClient';
import { FieldElementsTags } from '../utils/fieldsElementsTags';

export class DotApiForm {
    private dotCMSHttpClient: DotCMSHttpClient;
    private dotApiContentType: DotApiContentType;
    private formScript: string;

    constructor(httpClient: DotCMSHttpClient) {
        this.dotCMSHttpClient = httpClient;
        this.dotApiContentType = new DotApiContentType(this.dotCMSHttpClient);
    }

    get(config: DotCMSFormConfig): void {
        this.dotApiContentType
            .getFields(config.identifier)
            .then((fields: DotCMSContentTypeField[]) => {
                this.createForm(fields);
            });
    }

    /**
     *
     */
    render(container: HTMLElement): void {
        console.log('---container', container);
        const escript = document.createElement('script');
        const tag = document.createElement('div');

        escript.type = 'module';
        escript.text = `
            import { defineCustomElements } from 'https://unpkg.com/dotcms-field-elements@latest/dist/loader';
            defineCustomElements(window);`;
        tag.innerHTML = this.formScript;

        container.append(escript);
        container.appendChild(tag);
    }

    private createForm(fields: DotCMSContentTypeField[]): void {
console.log('--fields', fields)

        this.formScript = '<form>';
        let fieldTags = '';
        fields.map((field) => {
            fieldTags += this.createField(field);
        });
        this.formScript += `${fieldTags}</form>`;
        console.log('--formScript', this.formScript);
    }

    // tslint:disable-next-line:cyclomatic-complexity
    private createField(field: DotCMSContentTypeField): string {
        let fieldScript = '';
        const fieldClazz = field.clazz.split('.');
        const fieldTag = FieldElementsTags[fieldClazz[fieldClazz.length - 1]];
        if (fieldTag) {
                fieldScript = `
                <${fieldTag}
                    ${field.name ? `label="${field.name}"` : ''}
                    ${field.defaultValue ? `defaultValue="${field.defaultValue}"` : ''}
                    ${field.values ? `options="${field.values.split('\r\n').join(',')}"` : ''}
                    ${field.hint ? `hint="${field.hint}"` : ''}
                    ${field.required ? 'required' : ''}
                ></${fieldTag}>`;
        }
        return fieldScript;
    }
}
