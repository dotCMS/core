import { h } from '@stencil/core';

import { DotCMSContentTypeField } from '@dotcms/dotcms-models';

import { getFieldVariableValue, setAttributesToTag } from '.';

export const DotFormFields = {
    Text: (field: DotCMSContentTypeField) => (
        <dot-textfield
            hint={field.hint}
            label={field.name}
            name={field.variable}
            ref={(el: HTMLElement) => {
                setAttributesToTag(el, field.fieldVariables);
            }}
            regex-check={field.regexCheck}
            required={field.required}
            value={field.defaultValue}
        />
    ),

    Textarea: (field: DotCMSContentTypeField) => (
        <dot-textarea
            hint={field.hint}
            label={field.name}
            name={field.variable}
            ref={(el: HTMLElement) => {
                setAttributesToTag(el, field.fieldVariables);
            }}
            regex-check={field.regexCheck}
            required={field.required}
            value={field.defaultValue}
        />
    ),

    Checkbox: (field: DotCMSContentTypeField) => (
        <dot-checkbox
            hint={field.hint}
            label={field.name}
            name={field.variable}
            options={field.values}
            ref={(el: HTMLElement) => {
                setAttributesToTag(el, field.fieldVariables);
            }}
            required={field.required}
            value={field.defaultValue}
        />
    ),

    'Multi-Select': (field: DotCMSContentTypeField) => (
        <dot-multi-select
            hint={field.hint}
            label={field.name}
            name={field.variable}
            options={field.values}
            ref={(el: HTMLElement) => {
                setAttributesToTag(el, field.fieldVariables);
            }}
            required={field.required}
            value={field.defaultValue}
        />
    ),

    'Key-Value': (field: DotCMSContentTypeField) => (
        <dot-key-value
            field-type={field.fieldType}
            hint={field.hint}
            label={field.name}
            name={field.variable}
            required={field.required}
            value={field.defaultValue}
        />
    ),

    Select: (field: DotCMSContentTypeField) => (
        <dot-select
            hint={field.hint}
            label={field.name}
            name={field.variable}
            options={field.values}
            ref={(el: HTMLElement) => {
                setAttributesToTag(el, field.fieldVariables);
            }}
            required={field.required}
            value={field.defaultValue}
        />
    ),

    Radio: (field: DotCMSContentTypeField) => (
        <dot-radio
            hint={field.hint}
            label={field.name}
            name={field.variable}
            options={field.values}
            ref={(el: HTMLElement) => {
                setAttributesToTag(el, field.fieldVariables);
            }}
            required={field.required}
            value={field.defaultValue}
        />
    ),

    Date: (field: DotCMSContentTypeField) => (
        <dot-date
            hint={field.hint}
            label={field.name}
            name={field.variable}
            ref={(el: HTMLElement) => {
                setAttributesToTag(el, field.fieldVariables);
            }}
            required={field.required}
            value={field.defaultValue}
        />
    ),

    Time: (field: DotCMSContentTypeField) => (
        <dot-time
            hint={field.hint}
            label={field.name}
            name={field.variable}
            ref={(el: HTMLElement) => {
                setAttributesToTag(el, field.fieldVariables);
            }}
            required={field.required}
            value={field.defaultValue}
        />
    ),

    'Date-and-Time': (field: DotCMSContentTypeField) => (
        <dot-date-time
            hint={field.hint}
            label={field.name}
            name={field.variable}
            ref={(el: HTMLElement) => {
                setAttributesToTag(el, field.fieldVariables);
            }}
            required={field.required}
            value={field.defaultValue}
        />
    ),

    'Date-Range': (field: DotCMSContentTypeField) => (
        <dot-date-range
            hint={field.hint}
            label={field.name}
            name={field.variable}
            required={field.required}
            value={field.defaultValue}
        />
    ),

    Tag: (field: DotCMSContentTypeField) => (
        <dot-tags
            data={(): Promise<string[]> => {
                return fetch('/api/v1/tags')
                    .then((data) => data.json())
                    .then((items) => Object.keys(items))
                    .catch(() => []);
            }}
            hint={field.hint}
            label={field.name}
            name={field.variable}
            required={field.required}
            value={field.defaultValue}
        />
    ),

    Binary: (field: DotCMSContentTypeField) => (
        <dot-binary-file
            accept={getFieldVariableValue(field.fieldVariables, 'accept')}
            max-file-length={getFieldVariableValue(field.fieldVariables, 'maxFileLength')}
            hint={field.hint}
            label={field.name}
            name={field.variable}
            ref={(el: HTMLElement) => {
                setAttributesToTag(el, field.fieldVariables);
            }}
            required={field.required}
        />
    )
};
