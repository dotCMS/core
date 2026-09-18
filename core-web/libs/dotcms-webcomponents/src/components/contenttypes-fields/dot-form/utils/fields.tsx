import { h } from '@stencil/core';
import { DotCMSContentTypeField } from '@dotcms/dotcms-models';
import { getFieldVariableValue, setAttributesToTag } from '../utils';

/**
 * `field.defaultValue` and `field.values` are `string | null | undefined` on the content-type
 * model — the API omits them for a field that has none, and sends null for some. Every Stencil
 * prop they feed is `string | undefined`, so each is collapsed with `?? undefined` at the binding
 * rather than widening a dozen component props to accept a null they would only have to re-handle.
 */
export const DotFormFields = {
    Text: (field: DotCMSContentTypeField) => (
        <dot-textfield
            hint={field.hint}
            label={field.name}
            name={field.variable}
            ref={(el) => {
                setAttributesToTag(el, field.fieldVariables);
            }}
            regex-check={field.regexCheck}
            required={field.required}
            value={field.defaultValue ?? undefined}
        />
    ),
    Textarea: (field: DotCMSContentTypeField) => (
        <dot-textarea
            hint={field.hint}
            label={field.name}
            name={field.variable}
            ref={(el) => {
                setAttributesToTag(el, field.fieldVariables);
            }}
            regex-check={field.regexCheck}
            required={field.required}
            value={field.defaultValue ?? undefined}
        />
    ),
    Checkbox: (field: DotCMSContentTypeField) => (
        <dot-checkbox
            hint={field.hint}
            label={field.name}
            name={field.variable}
            options={field.values ?? undefined}
            ref={(el) => {
                setAttributesToTag(el, field.fieldVariables);
            }}
            required={field.required}
            value={field.defaultValue ?? undefined}
        />
    ),
    'Multi-Select': (field: DotCMSContentTypeField) => (
        <dot-multi-select
            hint={field.hint}
            label={field.name}
            name={field.variable}
            options={field.values ?? undefined}
            ref={(el) => {
                setAttributesToTag(el, field.fieldVariables);
            }}
            required={field.required}
            value={field.defaultValue ?? undefined}
        />
    ),
    'Key-Value': (field: DotCMSContentTypeField) => (
        <dot-key-value
            field-type={field.fieldType}
            hint={field.hint}
            label={field.name}
            name={field.variable}
            required={field.required}
            value={field.defaultValue ?? undefined}
        />
    ),
    Select: (field: DotCMSContentTypeField) => (
        <dot-select
            hint={field.hint}
            label={field.name}
            name={field.variable}
            options={field.values ?? undefined}
            ref={(el) => {
                setAttributesToTag(el, field.fieldVariables);
            }}
            required={field.required}
            value={field.defaultValue ?? undefined}
        />
    ),
    Radio: (field: DotCMSContentTypeField) => (
        <dot-radio
            hint={field.hint}
            label={field.name}
            name={field.variable}
            options={field.values ?? undefined}
            ref={(el) => {
                setAttributesToTag(el, field.fieldVariables);
            }}
            required={field.required}
            value={field.defaultValue ?? undefined}
        />
    ),
    Date: (field: DotCMSContentTypeField) => (
        <dot-date
            hint={field.hint}
            label={field.name}
            name={field.variable}
            ref={(el) => {
                setAttributesToTag(el, field.fieldVariables);
            }}
            required={field.required}
            value={field.defaultValue ?? undefined}
        />
    ),
    Time: (field: DotCMSContentTypeField) => (
        <dot-time
            hint={field.hint}
            label={field.name}
            name={field.variable}
            ref={(el) => {
                setAttributesToTag(el, field.fieldVariables);
            }}
            required={field.required}
            value={field.defaultValue ?? undefined}
        />
    ),
    'Date-and-Time': (field: DotCMSContentTypeField) => (
        <dot-date-time
            hint={field.hint}
            label={field.name}
            name={field.variable}
            ref={(el) => {
                setAttributesToTag(el, field.fieldVariables);
            }}
            required={field.required}
            value={field.defaultValue ?? undefined}
        />
    ),
    'Date-Range': (field: DotCMSContentTypeField) => (
        <dot-date-range
            hint={field.hint}
            label={field.name}
            name={field.variable}
            required={field.required}
            value={field.defaultValue ?? undefined}
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
            value={field.defaultValue ?? undefined}
        />
    ),
    Binary: (field: DotCMSContentTypeField) => (
        <dot-binary-file
            accept={getFieldVariableValue(field.fieldVariables, 'accept') ?? undefined}
            max-file-length={getFieldVariableValue(field.fieldVariables, 'maxFileLength')}
            hint={field.hint}
            label={field.name}
            name={field.variable}
            ref={(el) => {
                setAttributesToTag(el, field.fieldVariables);
            }}
            required={field.required}
        />
    )
};
