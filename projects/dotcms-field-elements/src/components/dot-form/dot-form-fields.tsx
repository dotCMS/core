import { DotCMSContentTypeField, DotCMSKeyValueField, DotCMSMultiSelectField } from '../../models';

export const DotFormFields = {
    Text: (field: DotCMSContentTypeField) => (
        <dot-textfield
            disabled={field.disabled}
            label={field.name}
            name={field.variable}
            regex-check={field.regexCheck}
            validation-message={field.validationMessage}
            placeholder={field.placeholder}
            hint={field.hint}
            value={field.defaultValue}
            required={field.required}
            required-message={field.requiredMessage}
        />
    ),

    Textarea: (field: DotCMSContentTypeField) => (
        <dot-textarea
            disabled={field.disabled}
            label={field.name}
            name={field.variable}
            regex-check={field.regexCheck}
            validation-message={field.validationMessage}
            hint={field.hint}
            value={field.defaultValue}
            required={field.required}
            required-message={field.requiredMessage}
        />
    ),

    Checkbox: (field: DotCMSContentTypeField) => (
        <dot-checkbox
            disabled={field.disabled}
            hint={field.hint}
            label={field.name}
            name={field.variable}
            options={field.values}
            required={field.required}
            required-message={field.requiredMessage}
            value={field.defaultValue}
        />
    ),

    'Multi-Select': (field: DotCMSMultiSelectField) => (
        <dot-multi-select
            disabled={field.disabled}
            hint={field.hint}
            label={field.name}
            name={field.variable}
            options={field.values}
            required={field.required}
            required-message={field.requiredMessage}
            size={+field.size}
            value={field.defaultValue}
        />
    ),

    'Key-Value': (field: DotCMSKeyValueField) => (
        <dot-key-value
            disabled={field.disabled}
            label={field.name}
            field-type={field.fieldType}
            save-btn-label={field.saveBtnLabel}
            name={field.variable}
            key-placeholder={field.keyPlaceholder}
            value-placeholder={field.valuePlaceholder}
            hint={field.hint}
            value={field.defaultValue}
            required={field.required}
            required-message={field.requiredMessage}
        />
    ),

    Select: (field: DotCMSContentTypeField) => (
        <dot-select
            disabled={field.disabled}
            hint={field.hint}
            label={field.name}
            name={field.variable}
            options={field.values}
            required={field.required}
            required-message={field.requiredMessage}
            value={field.defaultValue}
        />
    ),

    Radio: (field: DotCMSContentTypeField) => (
        <dot-radio
            disabled={field.disabled}
            hint={field.hint}
            label={field.name}
            name={field.variable}
            options={field.values}
            required={field.required}
            required-message={field.requiredMessage}
            value={field.defaultValue}
        />
    ),

    Date: (field: DotCMSContentTypeField) => (
        <dot-date
            disabled={field.disabled}
            label={field.name}
            name={field.variable}
            hint={field.hint}
            value={field.defaultValue}
            required={field.required}
            required-message={field.requiredMessage}
            validation-message={field.validationMessage}
            min={field.min}
            max={field.max}
            step={field.step}
        />
    )
};
