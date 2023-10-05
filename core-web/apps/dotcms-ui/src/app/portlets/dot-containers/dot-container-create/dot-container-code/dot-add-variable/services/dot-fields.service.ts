import { Injectable, inject } from '@angular/core';

import { DotMessageService } from '@dotcms/data-access';

import { FieldTypes, DotFieldContent, GetFieldsFunction } from '../dot-add-variable.models';

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

    // You can add here a new fieldType and add the custom fields that it has, if it doesn't have any custom fields, it will return the default fields
    readonly fields: Record<FieldTypes, GetFieldsFunction> = {
        Image: (dotFieldContent) => this.getImageFields(dotFieldContent),
        File: (dotFieldContent) => this.getFileFields(dotFieldContent),
        'Host-Folder': (dotFieldContent) => this.getHostFields(dotFieldContent),
        'Story-Block': (dotFieldContent) => this.getBlockEditorFields(dotFieldContent),
        Binary: (dotFieldContent) => this.getBinaryFields(dotFieldContent),
        Select: (dotFieldContent) => this.getSelectFields(dotFieldContent),
        'Multi-Select': (dotFieldContent) => this.getSelectFields(dotFieldContent), // Select and Multiselect has the same custom fields
        Radio: (dotFieldContent) => this.getRadioFields(dotFieldContent),
        Checkbox: (dotFieldContent) => this.getCheckboxFields(dotFieldContent),
        Date: (dotFieldContent) => this.getDateFields(dotFieldContent),
        Time: (dotFieldContent) => this.getTimeFields(dotFieldContent),
        'Date-and-Time': (dotFieldContent) => this.getDateAndTimeFields(dotFieldContent),
        default: (dotFieldContent) => this.getDefaultFields(dotFieldContent)
    };

    /**
     * Get the default fields for a given fieldType
     *
     * @param {DotFieldContent} dotFieldContent
     * @return {*}  {DotFieldContent[]}
     * @memberof DotFieldsService
     */
    private getDefaultFields({
        variable,
        name,
        fieldTypeLabel,
        codeTemplate = this.getCodeTemplate.default(variable)
    }: DotFieldContent): DotFieldContent[] {
        return [
            {
                name,
                variable,
                fieldTypeLabel,
                codeTemplate
            }
        ];
    }

    /**
     * Get Image  Fields
     *
     * @private
     * @param {DotFieldContent} { variable, name, fieldTypeLabel }
     * @return {*}  {DotFieldContent[]}
     * @memberof DotFieldsService
     */
    private getImageFields({ variable, name, fieldTypeLabel }: DotFieldContent): DotFieldContent[] {
        return [
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
        ];
    }

    /**
     * Get File  Fields
     *
     * @private
     * @param {DotFieldContent} { variable, name, fieldTypeLabel }
     * @return {*}  {DotFieldContent[]}
     * @memberof DotFieldsService
     */
    private getFileFields({ variable, name, fieldTypeLabel }: DotFieldContent): DotFieldContent[] {
        return [
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
        ];
    }

    /**
     * Get BlockEditor  Fields
     *
     * @private
     * @param {DotFieldContent} { variable, name, fieldTypeLabel }
     * @return {*}  {DotFieldContent[]}
     * @memberof DotFieldsService
     */
    private getBlockEditorFields({
        variable,
        name,
        fieldTypeLabel
    }: DotFieldContent): DotFieldContent[] {
        return [
            {
                name: `${name}: ${this.dotMessage.get('html-render')}`,
                codeTemplate: this.getCodeTemplate.blockEditor(variable),
                variable,
                fieldTypeLabel
            }
        ];
    }

    /**
     * Get Host  Fields
     *
     * @private
     * @param {DotFieldContent} { variable, name, fieldTypeLabel }
     * @return {*}  {DotFieldContent[]}
     * @memberof DotFieldsService
     */
    private getHostFields({
        variable,
        name,
        fieldTypeLabel,
        codeTemplate = this.getCodeTemplate.default('ConHostFolder')
    }: DotFieldContent): DotFieldContent[] {
        return [
            {
                name,
                variable,
                fieldTypeLabel,
                codeTemplate
            }
        ];
    }

    /**
     * Get Binary  Fields
     *
     * @private
     * @param {DotFieldContent} { variable, name, fieldTypeLabel }
     * @return {*}  {DotFieldContent[]}
     * @memberof DotFieldsService
     */
    private getBinaryFields({
        variable,
        name,
        fieldTypeLabel
    }: DotFieldContent): DotFieldContent[] {
        return [
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
        ];
    }

    /**
     * Get Select  Fields
     *
     * @private
     * @param {DotFieldContent} { variable, name, fieldTypeLabel }
     * @return {*}  {DotFieldContent[]}
     * @memberof DotFieldsService
     */
    private getSelectFields({
        variable,
        name,
        fieldTypeLabel
    }: DotFieldContent): DotFieldContent[] {
        return [
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
        ];
    }

    /**
     * Get Radio  Fields
     *
     * @private
     * @param {DotFieldContent} { variable, name, fieldTypeLabel }
     * @return {*}  {DotFieldContent[]}
     * @memberof DotFieldsService
     */
    private getRadioFields({ variable, name, fieldTypeLabel }: DotFieldContent): DotFieldContent[] {
        return [
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
        ];
    }

    /**
     * Get Checkbox  Fields
     *
     * @private
     * @param {DotFieldContent} { variable, name, fieldTypeLabel }
     * @return {*}  {DotFieldContent[]}
     * @memberof DotFieldsService
     */
    private getCheckboxFields({
        variable,
        name,
        fieldTypeLabel
    }: DotFieldContent): DotFieldContent[] {
        return [
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
        ];
    }

    /**
     * Get Date  Fields
     *
     * @private
     * @param {DotFieldContent} { variable, name, fieldTypeLabel }
     * @return {*}  {DotFieldContent[]}
     * @memberof DotFieldsService
     */
    private getDateFields({ variable, name, fieldTypeLabel }: DotFieldContent): DotFieldContent[] {
        return [
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
        ];
    }

    /**
     * Get Time  Fields
     *
     * @private
     * @param {DotFieldContent} { variable, name, fieldTypeLabel }
     * @return {*}  {DotFieldContent[]}
     * @memberof DotFieldsService
     */
    private getTimeFields({ variable, name, fieldTypeLabel }: DotFieldContent): DotFieldContent[] {
        return [
            {
                name: `${name}: ${this.dotMessage.get('Time')} ${this.dotMessage.get('hh-mm-ss')}`,
                codeTemplate: this.getCodeTemplate.default(variable),
                variable,
                fieldTypeLabel
            }
        ];
    }

    /**
     * Get Date and Time  Fields
     *
     * @private
     * @param {DotFieldContent} { variable, name, fieldTypeLabel }
     * @return {*}  {DotFieldContent[]}
     * @memberof DotFieldsService
     */
    private getDateAndTimeFields({
        variable,
        name,
        fieldTypeLabel
    }: DotFieldContent): DotFieldContent[] {
        return [
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
        ];
    }
}
