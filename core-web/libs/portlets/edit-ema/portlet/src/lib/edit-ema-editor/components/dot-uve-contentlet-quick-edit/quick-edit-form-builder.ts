import { AbstractControl, FormBuilder, FormGroup, Validators } from '@angular/forms';

import { DotCMSClazzes, DotCMSContentlet } from '@dotcms/dotcms-models';

import { ContentletField } from './types';

/**
 * Pure helpers used to construct the quick-edit form group from a
 * resolved set of fields + a contentlet. Extracted from the component
 * so the per-field-type value coercion is independently testable
 * without instantiating Angular.
 */

/**
 * Coerce the raw contentlet value for `field` into the shape the form
 * control expects. Different field types store data differently (BINARY
 * as a `{ idPath, identifier, ... }` object, IMAGE/FILE as objects with
 * `identifier`, CHECKBOX/MULTI_SELECT as comma-joined strings).
 */
export function coerceFieldValue(
    field: ContentletField,
    contentlet: DotCMSContentlet | undefined
): string | string[] | boolean | DotCMSContentlet {
    const rawValue = contentlet?.[field.variable];
    const hasContentletValue = rawValue !== undefined && rawValue !== null && rawValue !== '';

    if (!hasContentletValue && field.clazz === DotCMSClazzes.RADIO && field.defaultValue) {
        return field.defaultValue;
    }

    const value: string | string[] | boolean | DotCMSContentlet = rawValue ?? '';

    // CHECKBOX with options + MULTI_SELECT: array of selected values.
    if (
        (field.clazz === DotCMSClazzes.CHECKBOX && field.options?.length) ||
        field.clazz === DotCMSClazzes.MULTI_SELECT
    ) {
        if (typeof value === 'string' && value) {
            return value.split(',').map((v) => v.trim());
        }
        if (Array.isArray(value)) {
            return value;
        }
        if (!hasContentletValue && field.clazz === DotCMSClazzes.CHECKBOX && field.defaultValue) {
            return field.defaultValue.split(',').map((v) => v.trim());
        }
        return [];
    }

    // IMAGE / FILE: identifier string, optionally extracted from object.
    if (field.clazz === DotCMSClazzes.IMAGE || field.clazz === DotCMSClazzes.FILE) {
        if (typeof value === 'string' && value) {
            return value.trim();
        }
        if (value && typeof value === 'object' && 'identifier' in value) {
            return value.identifier ?? '';
        }
        return '';
    }

    // BINARY: idPath / versionPath / identifier, in that order of preference.
    if (field.clazz === DotCMSClazzes.BINARY) {
        if (typeof value === 'string' && value) {
            return value.trim();
        }
        if (value && typeof value === 'object') {
            const binary = value as Record<string, string>;
            return binary.idPath || binary.versionPath || binary.identifier || '';
        }
        return '';
    }

    return value;
}

/**
 * Build the validator array for a field. Required + optional regex
 * pattern validation; invalid regex strings are logged and dropped
 * rather than crashing the form construction.
 */
export function buildValidators(field: ContentletField) {
    const validators = [];

    if (field.required) {
        validators.push(Validators.required);
    }

    if (field.regexCheck) {
        try {
            new RegExp(field.regexCheck);
            validators.push(Validators.pattern(field.regexCheck));
        } catch (error) {
            console.warn(
                `Invalid regex pattern for field ${field.variable}: ${field.regexCheck}`,
                error
            );
        }
    }

    return validators;
}

/**
 * Construct a fresh `FormGroup` from `fields` populated with
 * `contentlet`'s values. Read-only fields are disabled. The hidden
 * inode control is always added when the contentlet has one — the
 * save flow needs it.
 */
export function buildQuickEditFormGroup(
    fb: FormBuilder,
    fields: ContentletField[],
    contentlet: DotCMSContentlet
): FormGroup {
    const formControls: Record<string, AbstractControl> = {};

    if (contentlet?.inode) {
        formControls['inode'] = fb.control(contentlet.inode);
    }

    fields.forEach((field) => {
        const value = coerceFieldValue(field, contentlet);
        const validators = buildValidators(field);

        formControls[field.variable] = fb.control(value, validators.length > 0 ? validators : null);

        if (field.readOnly) {
            formControls[field.variable].disable();
        }
    });

    return fb.group(formControls);
}

/**
 * Reconstruct the page-asset-compatible properties for IMAGE/FILE/BINARY
 * fields. The form stores identifier strings; the page asset stores
 * objects keyed by `identifier` (image/file) or `idPath` (binary).
 * Used by the optimistic-update path to push form values back into
 * the iframe's page tree.
 */
export function toPageAssetProperties(
    fields: ContentletField[],
    formValues: Record<string, unknown>
): Record<string, unknown> {
    const result: Record<string, unknown> = { ...formValues };

    for (const field of fields) {
        if (
            field.clazz !== DotCMSClazzes.IMAGE &&
            field.clazz !== DotCMSClazzes.FILE &&
            field.clazz !== DotCMSClazzes.BINARY
        ) {
            continue;
        }

        if (field.clazz === DotCMSClazzes.IMAGE || field.clazz === DotCMSClazzes.FILE) {
            result[field.variable] = { identifier: formValues[field.variable] };
        } else if (field.clazz === DotCMSClazzes.BINARY) {
            result[field.variable] = { idPath: formValues[field.variable] };
        }
    }

    return result;
}
