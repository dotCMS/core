import { DotCMSContentTypeField } from '../../models/dot-content-type-field';

export const DotFormFields = {
    Text: (field: DotCMSContentTypeField) => (
        <dot-textfield
            disabled={field.disabled}
            label={field.name}
            name={field.variable}
            regexcheck={field.regexCheck}
            regexcheckmessage={field.regexCheckMessage}
            placeholder={field.placeholder}
            hint={field.hint}
            value={field.defaultValue}
            required={field.required}
            requiredmessage={field.requiredMessage}
        />
    ),

    Textarea: (field: DotCMSContentTypeField) => (
        <dot-textarea
            disabled={field.disabled}
            label={field.name}
            name={field.variable}
            regexcheck={field.regexCheck}
            regexcheckmessage={field.regexCheckMessage}
            hint={field.hint}
            value={field.defaultValue}
            required={field.required}
            requiredmessage={field.requiredMessage}
        />
    ),

    Checkbox: (field: DotCMSContentTypeField) => (
        <dot-checkbox
            label={field.name}
            hint={field.hint}
            value={field.defaultValue}
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
            requiredmessage={field.requiredMessage}
            value={field.defaultValue}
        />
    )
};
