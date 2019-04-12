import { DotCMSContentTypeField } from '../../../../dotcms/src/lib/models';

export const DotFormFields = {
    Text: (field: DotCMSContentTypeField) => (
        <dot-textfield
            label={field.name}
            name={field.variable}
            hint={field.hint}
            value={field.defaultValue}
            required={field.required}
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
        <dot-checkbox
            label={field.name}
            hint={field.hint}
            value={field.defaultValue}
        />
    )
};
