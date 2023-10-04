import { Injectable, inject } from '@angular/core';

import { DotMessageService } from '@dotcms/data-access';

import { FieldTypeWithExtraFields, DotFieldContent } from '../dot-add-variable.models';

@Injectable()
export class DotFieldsService {
    private dotMessage = inject(DotMessageService);

    // You can add here a new variable and add the custom code that it has
    private readonly getCodeTemplate: Record<string, (variable: string) => string> = {
        image: (variable) =>
            `#if ($UtilMethods.isSet(\${${variable}ImageURI}))\n    <img src="$!{dotContentMap.${variable}ImageURI}" alt="$!{dotContentMap.${variable}ImageTitle}" />\n#end`,
        default: (variable) => `$!{dotContentMap.${variable}}`
    };

    readonly contentIdentifierField: DotFieldContent = {
        name: this.dotMessage.get('Content-Identifier-value'),
        variable: 'ContentIdentifier',
        fieldTypeLabel: this.dotMessage.get('Content-Identifier'),
        codeTemplate: this.getCodeTemplate.default('ContentIdentifier')
    };

    // You can add here a new fieldType and add the meta data fields that it has
    readonly fields: Record<
        FieldTypeWithExtraFields, // Remember to add the new fieldType here
        (variableContent: DotFieldContent) => DotFieldContent[]
    > = {
        Image: ({ variable, name, fieldTypeLabel }) => [
            {
                name: `${name}: ${this.dotMessage.get('Image-Identifier')}`,
                variable: `${variable}ImageIdentifier`,
                codeTemplate: this.getCodeTemplate.default(`${variable}ImageIdentifier`),
                fieldTypeLabel
            },
            {
                name: `${name}: ${this.dotMessage.get('Image')}`,
                variable: `${variable}Image`,
                codeTemplate: this.getCodeTemplate.image(variable),
                fieldTypeLabel
            },
            {
                name: `${name}: ${this.dotMessage.get('Image-Title')}`,
                variable: `${variable}ImageTitle`,
                codeTemplate: this.getCodeTemplate.default(`${variable}ImageTitle`),
                fieldTypeLabel
            },
            {
                name: `${name}: ${this.dotMessage.get('Image-Extension')}`,
                variable: `${variable}ImageExtension`,
                codeTemplate: this.getCodeTemplate.default(`${variable}ImageExtension`),
                fieldTypeLabel
            },
            {
                name: `${name}: ${this.dotMessage.get('Image-Width')}`,
                variable: `${variable}ImageWidth`,
                codeTemplate: this.getCodeTemplate.default(`${variable}ImageWidth`),
                fieldTypeLabel
            },
            {
                name: `${name}: ${this.dotMessage.get('Image-Height')}`,
                variable: `${variable}ImageHeight`,
                codeTemplate: this.getCodeTemplate.default(`${variable}ImageHeight`),
                fieldTypeLabel
            }
        ],
        default: ({
            variable,
            name,
            fieldTypeLabel,
            codeTemplate = this.getCodeTemplate.default(variable)
        }) => [
            {
                name,
                variable,
                fieldTypeLabel,
                codeTemplate
            }
        ]
    };
}
