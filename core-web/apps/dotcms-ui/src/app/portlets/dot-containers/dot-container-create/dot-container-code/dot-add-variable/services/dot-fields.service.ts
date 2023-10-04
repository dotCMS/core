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
        file: (variable) =>
            `#if (\${${variable}FileURI})\n    <a href="$!{dotContentMap.${variable}FileURI}">$!{dotContentMap.${variable}FileTitle}</a>\n#end`,
        binaryFile: (variable) =>
            `#if ($UtilMethods.isSet(\${${variable}BinaryFileURI}))\n    <a href="$!{dotContentMap.${variable}BinaryFileURI}?force_download=1&filename=$!{dotContentMap.${variable}BinaryFileTitle}">$!{dotContentMap.${variable}BinaryFileTitle}</a>\n#end`,
        binaryResized: (variable) =>
            `#if ($UtilMethods.isSet(\${${variable}BinaryFileURI}))\n    <img src="/contentAsset/resize-image/\${ContentIdentifier}/${variable}?w=150&h=100&language_id=\${language}" />\n#end`,
        binaryThumbnailed: (variable) =>
            `#if ($UtilMethods.isSet(\${${variable}BinaryFileURI}))\n    <img src="/contentAsset/image-thumbnail/\${ContentIdentifier}/${variable}?w=150&h=150&language_id=\${language}" />\n#end`,
        blockEditor: (variable) => `$dotContentMap.get('${variable}').toHtml()`,
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
        'Host-Folder': ({
            variable,
            name,
            fieldTypeLabel,
            codeTemplate = this.getCodeTemplate.default('ConHostFolder')
        }) => [
            {
                name,
                variable,
                fieldTypeLabel,
                codeTemplate
            }
        ],
        File: ({ variable, name, fieldTypeLabel }) => [
            {
                name: `${name}: ${this.dotMessage.get('File')}`,
                variable: `${variable}File`,
                codeTemplate: this.getCodeTemplate.file(variable),
                fieldTypeLabel
            },
            {
                name: `${name}: ${this.dotMessage.get('File-Identifier')}`,
                variable: `${variable}FileIdentifier`,
                codeTemplate: this.getCodeTemplate.default(`${variable}FileIdentifier`),
                fieldTypeLabel
            },
            {
                name: `${name}: ${this.dotMessage.get('File-Extension')}`,
                variable: `${variable}FileExtension`,
                codeTemplate: this.getCodeTemplate.default(`${variable}FileExtension`),
                fieldTypeLabel
            }
        ],
        'Story-Block': ({ variable, name, fieldTypeLabel }) => [
            {
                name: `${name}: ${this.dotMessage.get('html-render')}`,
                codeTemplate: this.getCodeTemplate.blockEditor(variable),
                variable,
                fieldTypeLabel
            }
        ],
        Binary: ({ variable, name, fieldTypeLabel }) => [
            {
                name: `${name}: ${this.dotMessage.get('Binary-File')}`,
                variable: `${variable}BinaryFile`,
                codeTemplate: this.getCodeTemplate.binaryFile(variable),
                fieldTypeLabel
            },
            {
                name: `${name}: ${this.dotMessage.get('Binary-File-Resized')}`,
                variable: `${variable}BinaryFileResized`,
                codeTemplate: this.getCodeTemplate.binaryResized(variable),
                fieldTypeLabel
            },
            {
                name: `${name}: ${this.dotMessage.get('Binary-File-Thumbnail')}`,
                variable: `${variable}BinaryFileThumbnail`,
                codeTemplate: this.getCodeTemplate.binaryThumbnailed(variable),
                fieldTypeLabel
            },
            {
                name: `${name}: ${this.dotMessage.get('Binary-File-Size')}`,
                variable: `${variable}FileSize`,
                codeTemplate: this.getCodeTemplate.default(`${variable}FileSize`),
                fieldTypeLabel
            }
        ],
        Select: ({ variable, name, fieldTypeLabel }) => [
            {
                name: `${name}: ${this.dotMessage.get('Selected-Value')}`,
                codeTemplate: this.getCodeTemplate.default(variable),
                variable,
                fieldTypeLabel
            },
            {
                name: `${name}: ${this.dotMessage.get('Labels-Values')}`,
                codeTemplate: this.getCodeTemplate.default(`${variable}SelectLabelsValues`),
                variable: `${variable}SelectLabelsValues`,
                fieldTypeLabel
            }
        ],
        'Multi-Select': ({ variable, name, fieldTypeLabel }) => [
            {
                name: `${name}: ${this.dotMessage.get('Selected-Value')}`,
                codeTemplate: this.getCodeTemplate.default(variable),
                variable,
                fieldTypeLabel
            },
            {
                name: `${name}: ${this.dotMessage.get('Labels-Values')}`,
                codeTemplate: this.getCodeTemplate.default(`${variable}SelectLabelsValues`),
                variable: `${variable}SelectLabelsValues`,
                fieldTypeLabel
            }
        ],
        Radio: ({ variable, name, fieldTypeLabel }) => [
            {
                name: `${name}: ${this.dotMessage.get('Selected-Value')}`,
                codeTemplate: this.getCodeTemplate.default(variable),
                variable,
                fieldTypeLabel
            },
            {
                name: `${name}: ${this.dotMessage.get('Labels-Values')}`,
                codeTemplate: this.getCodeTemplate.default(`${variable}RadioLabelsValues`),
                variable: `${variable}RadioLabelsValues`,
                fieldTypeLabel
            }
        ],
        Checkbox: ({ variable, name, fieldTypeLabel }) => [
            {
                name: `${name}: ${this.dotMessage.get('Selected-Value')}`,
                codeTemplate: this.getCodeTemplate.default(variable),
                variable,
                fieldTypeLabel
            },
            {
                name: `${name}: ${this.dotMessage.get('Labels-Values')}`,
                codeTemplate: this.getCodeTemplate.default(`${variable}CheckboxLabelsValues`),
                variable: `${variable}CheckboxLabelsValues`,
                fieldTypeLabel
            }
        ],
        Date: ({ variable, name, fieldTypeLabel }) => [
            {
                name: `${name}: ${this.dotMessage.get(
                    'contenttypes.field.properties.data_type.values.date'
                )} ${this.dotMessage.get('mm-dd-yyyy')}`,
                codeTemplate: this.getCodeTemplate.default(variable),
                variable,
                fieldTypeLabel
            },
            {
                name: `${name}: ${this.dotMessage.get(
                    'Date-Database-Format'
                )} ${this.dotMessage.get('yyyy-mm-dd')}`,
                codeTemplate: this.getCodeTemplate.default(`${variable}DBFormat`),
                variable: `${variable}DBFormat`,
                fieldTypeLabel
            }
        ],
        Time: ({ variable, name, fieldTypeLabel }) => [
            {
                name: `${name}: ${this.dotMessage.get('Time')} ${this.dotMessage.get('hh-mm-ss')}`,
                codeTemplate: this.getCodeTemplate.default(variable),
                variable,
                fieldTypeLabel
            }
        ],
        'Date-and-Time': ({ variable, name, fieldTypeLabel }) => [
            {
                name: `${name}: ${this.dotMessage.get('Date')}`,
                codeTemplate: this.getCodeTemplate.default(variable),
                variable,
                fieldTypeLabel
            },
            {
                name: `${name}: ${this.dotMessage.get('Date-Short-String')} ${this.dotMessage.get(
                    'mm-dd-yyyy'
                )}`,
                codeTemplate: this.getCodeTemplate.default(`${variable}ShortFormat`),
                variable: `${variable}ShortFormat`,
                fieldTypeLabel
            },
            {
                name: `${name}: ${this.dotMessage.get('Date-Long-String')}`,
                codeTemplate: this.getCodeTemplate.default(`${variable}LongFormat`),
                variable: `${variable}LongFormat`,
                fieldTypeLabel
            },
            {
                name: `${name}: ${this.dotMessage.get(
                    'Date-Database-Format'
                )} ${this.dotMessage.get('yyyy-mm-dd')}`,
                codeTemplate: this.getCodeTemplate.default(`${variable}DBFormat`),
                variable: `${variable}DBFormat`,
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
