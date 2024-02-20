import { Injectable, inject } from '@angular/core';

import { DotMessageService } from '@dotcms/data-access';

import { FieldTypes, DotFieldContent, GetFieldsFunction } from '../dot-add-variable.models';

export const DOT_CONTENT_MAP = 'dotContentMap';

@Injectable()
export class DotFieldsService {
    private dotMessage = inject(DotMessageService);

    // You can add here a new variable and add the custom code that it has
    private readonly getCodeTemplate: Record<string, (variable: string) => string> = {
        image: (variable) =>
            `#if ($!{${DOT_CONTENT_MAP}.${variable}.rawUri})\n    <img src="$!{${DOT_CONTENT_MAP}.${variable}.rawUri}" alt="$!{${DOT_CONTENT_MAP}.${variable}.title}" />\n#elseif($!{${DOT_CONTENT_MAP}.${variable}.identifier})\n    <img src="/dA/\${${DOT_CONTENT_MAP}.${variable}.identifier}" alt="$!{${DOT_CONTENT_MAP}.${variable}.title}"/>\n#end`,
        file: (variable) =>
            `#if ($!{${DOT_CONTENT_MAP}.${variable}.rawUri})\n    <a href="$!{${DOT_CONTENT_MAP}.${variable}.rawUri}" target="_blank" >$!{${DOT_CONTENT_MAP}.${variable}.title}</a>\n#elseif($!{${DOT_CONTENT_MAP}.${variable}.identifier})\n    <a href="/dA/\${${DOT_CONTENT_MAP}.${variable}.identifier}" target="_blank" >$!{${DOT_CONTENT_MAP}.${variable}.title}</a>\n#end`,
        binaryFile: (variable) =>
            `#if ($!{${DOT_CONTENT_MAP}.${variable}.rawUri})\n    <a href="$!{${DOT_CONTENT_MAP}.${variable}.rawUri}?force_download=1&filename=$!{${DOT_CONTENT_MAP}.${variable}.title}" target="_blank" >$!{${DOT_CONTENT_MAP}.${variable}.title}</a>\n#end`,
        binaryResized: (variable) =>
            `#if ($!{${DOT_CONTENT_MAP}.${variable}.rawUri})\n    <img src="/contentAsset/resize-image/\${ContentIdentifier}/${variable}?w=150&h=100&language_id=\${language}" />\n#end`,
        binaryThumbnailed: (variable) =>
            `#if ($!{${DOT_CONTENT_MAP}.${variable}.rawUri})\n    <img src="/contentAsset/image-thumbnail/\${ContentIdentifier}/${variable}?w=150&h=150&language_id=\${language}" />\n#end`,
        blockEditor: (variable) => `$${DOT_CONTENT_MAP}.get('${variable}').toHtml()`,
        dateDatabaseFormat: (variable) =>
            `$date.format("yyyy-M-dd", $${DOT_CONTENT_MAP}.${variable})`,
        dateShortFormat: (variable) => `$date.format("M-dd-yyyy", $${DOT_CONTENT_MAP}.${variable})`,
        dateLongFormat: (variable) =>
            `$date.format("M-dd-yyyy H:m:s", $${DOT_CONTENT_MAP}.${variable})`,
        time: (variable) => `$date.format("H:m:s", $${DOT_CONTENT_MAP}.${variable})`,
        default: (variable) => `$!{${DOT_CONTENT_MAP}.${variable}}`
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
        'Multi-Select': (dotFieldContent) => this.getMultiSelectFields(dotFieldContent),
        Radio: (dotFieldContent) => this.getSelectFields(dotFieldContent), // Is the same as Select
        Checkbox: (dotFieldContent) => this.getMultiSelectFields(dotFieldContent), // Is the same as MultiSelect
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
     * Get Image Fields
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
                variable: `${variable}.identifier`,
                codeTemplate: this.getCodeTemplate.default(`${variable}.identifier`),
                fieldTypeLabel
            },
            {
                name: `${name}: ${this.dotMessage.get('Image')}`,
                variable: `${variable}.image`,
                codeTemplate: this.getCodeTemplate.image(variable),
                fieldTypeLabel
            },
            {
                name: `${name}: ${this.dotMessage.get('Image-Title')}`,
                variable: `${variable}.title`,
                codeTemplate: this.getCodeTemplate.default(`${variable}.title`),
                fieldTypeLabel
            },
            {
                name: `${name}: ${this.dotMessage.get('Image-Extension')}`,
                variable: `${variable}.extension`,
                codeTemplate: this.getCodeTemplate.default(`${variable}.extension`),
                fieldTypeLabel
            },
            {
                name: `${name}: ${this.dotMessage.get('Image-Width')}`,
                variable: `${variable}.width`,
                codeTemplate: this.getCodeTemplate.default(`${variable}.width`),
                fieldTypeLabel
            },
            {
                name: `${name}: ${this.dotMessage.get('Image-Height')}`,
                variable: `${variable}.height`,
                codeTemplate: this.getCodeTemplate.default(`${variable}.height`),
                fieldTypeLabel
            }
        ];
    }

    /**
     * Get File Fields
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
                variable: `${variable}.file`,
                codeTemplate: this.getCodeTemplate.file(variable),
                fieldTypeLabel
            },
            {
                name: `${name}: ${this.dotMessage.get('File-Identifier')}`,
                variable: `${variable}.identifier`,
                codeTemplate: this.getCodeTemplate.default(`${variable}.identifier`),
                fieldTypeLabel
            },
            {
                name: `${name}: ${this.dotMessage.get('File-Extension')}`,
                variable: `${variable}.extension`,
                codeTemplate: this.getCodeTemplate.default(`${variable}.extension`),
                fieldTypeLabel
            }
        ];
    }

    /**
     * Get BlockEditor Fields
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
     * Get Host Fields
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
     * Get Binary Fields
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
                variable: `${variable}.file`,
                codeTemplate: this.getCodeTemplate.binaryFile(variable),
                fieldTypeLabel
            },
            {
                name: `${name}: ${this.dotMessage.get('Binary-File-Resized')}`,
                variable: `${variable}.fileResized`,
                codeTemplate: this.getCodeTemplate.binaryResized(variable),
                fieldTypeLabel
            },
            {
                name: `${name}: ${this.dotMessage.get('Binary-File-Thumbnail')}`,
                variable: `${variable}.fileThumbnail`,
                codeTemplate: this.getCodeTemplate.binaryThumbnailed(variable),
                fieldTypeLabel
            },
            {
                name: `${name}: ${this.dotMessage.get('Binary-File-Size')}`,
                variable: `${variable}.size`,
                codeTemplate: this.getCodeTemplate.default(`${variable}.size`),
                fieldTypeLabel
            }
        ];
    }

    /**
     * Get Select Fields
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
                codeTemplate: this.getCodeTemplate.default(`${variable}.selectValue`),
                variable: `${variable}.selectValue`,
                fieldTypeLabel
            },
            {
                name: `${name}: ${this.dotMessage.get('Options')}`,
                codeTemplate: this.getCodeTemplate.default(`${variable}.options`),
                variable: `${variable}.options`,
                fieldTypeLabel
            },
            {
                name: `${name}: ${this.dotMessage.get('values')}`,
                codeTemplate: this.getCodeTemplate.default(`${variable}.values`),
                variable: `${variable}.values`,
                fieldTypeLabel
            }
        ];
    }

    /**
     * Get MultiSelect Fields
     *
     * @private
     * @param {DotFieldContent} { variable, name, fieldTypeLabel }
     * @return {*}  {DotFieldContent[]}
     * @memberof DotFieldsService
     */
    private getMultiSelectFields({
        variable,
        name,
        fieldTypeLabel
    }: DotFieldContent): DotFieldContent[] {
        return [
            {
                name: `${name}: ${this.dotMessage.get('Selected-Values')}`,
                codeTemplate: this.getCodeTemplate.default(`${variable}.selectedValues`),
                variable: `${variable}.selectedValues`,
                fieldTypeLabel
            },
            {
                name: `${name}: ${this.dotMessage.get('Options')}`,
                codeTemplate: this.getCodeTemplate.default(`${variable}.options`),
                variable: `${variable}.options`,
                fieldTypeLabel
            },
            {
                name: `${name}: ${this.dotMessage.get('values')}`,
                codeTemplate: this.getCodeTemplate.default(`${variable}.values`),
                variable: `${variable}.values`,
                fieldTypeLabel
            }
        ];
    }

    /**
     * Get Date Fields
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
                codeTemplate: this.getCodeTemplate.dateShortFormat(variable),
                variable,
                fieldTypeLabel
            },
            {
                name: `${name}: ${this.dotMessage.get(
                    'Date-Database-Format'
                )} ${this.dotMessage.get('yyyy-mm-dd')}`,
                codeTemplate: this.getCodeTemplate.dateDatabaseFormat(`${variable}`),
                variable: `${variable}.DBFormat`,
                fieldTypeLabel
            }
        ];
    }

    /**
     * Get Time Fields
     *
     * @private
     * @param {DotFieldContent} { variable, name, fieldTypeLabel }
     * @return {*}  {DotFieldContent[]}
     * @memberof DotFieldsService
     */
    private getTimeFields({ variable, name, fieldTypeLabel }: DotFieldContent): DotFieldContent[] {
        // This is retriving the full date instead of the Time
        return [
            {
                name: `${name}: ${this.dotMessage.get('Time')} ${this.dotMessage.get('hh-mm-ss')}`,
                codeTemplate: this.getCodeTemplate.time(variable),
                variable,
                fieldTypeLabel
            }
        ];
    }

    /**
     * Get Date and Time Fields
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
                codeTemplate: this.getCodeTemplate.dateShortFormat(`${variable}`),
                variable: `${variable}.shortFormat`,
                fieldTypeLabel
            },
            {
                name: `${name}: ${this.dotMessage.get('Date-Long-String')}`,
                codeTemplate: this.getCodeTemplate.dateLongFormat(`${variable}`),
                variable: `${variable}.longFormat`,
                fieldTypeLabel
            },
            {
                name: `${name}: ${this.dotMessage.get(
                    'Date-Database-Format'
                )} ${this.dotMessage.get('yyyy-mm-dd')}`,
                codeTemplate: this.getCodeTemplate.dateDatabaseFormat(`${variable}`),
                variable: `${variable}.DBFormat`,
                fieldTypeLabel
            }
        ];
    }
}
