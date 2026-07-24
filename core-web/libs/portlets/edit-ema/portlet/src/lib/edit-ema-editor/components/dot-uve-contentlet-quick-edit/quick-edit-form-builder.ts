import { AbstractControl, FormBuilder, FormGroup, Validators } from '@angular/forms';

import { DotCMSClazzes, DotCMSDataTypes, DotCMSContentlet } from '@dotcms/dotcms-models';

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

    // SELECT / RADIO: p-select and p-radioButton match the control value
    // against string option values (`optionValue="value"`). The Page API
    // can return a real boolean (BOOL-backed field) or a number, neither of
    // which `===` a string option — so the control shows its placeholder
    // while `showClear` still sees a truthy value (the stray "X"). Map the
    // raw value back to its matching option's string value.
    if (field.clazz === DotCMSClazzes.SELECT || field.clazz === DotCMSClazzes.RADIO) {
        return matchOptionValue(field, value);
    }

    return value;
}

/**
 * Normalize a single-select value (SELECT / RADIO) so it matches one of
 * the field's string option values. Options are always strings (parsed
 * from `label|value`), but the Page API may return a real boolean for a
 * BOOL-backed field or a number. Boolean values are matched through the
 * same `toBoolean` semantics the backend uses (so `true` selects the
 * `Yes|1` option); numbers fall back to string comparison. Values that
 * are already strings (or arrays) are returned untouched.
 */
function matchOptionValue(
    field: ContentletField,
    value: string | string[] | boolean | DotCMSContentlet
): string | string[] | boolean | DotCMSContentlet {
    if (typeof value === 'string' || Array.isArray(value)) {
        return value;
    }

    const options = field.options ?? [];

    if (typeof value === 'boolean') {
        const match = options.find((option) => commonsLangToBoolean(option.value) === value);
        return match ? match.value : String(value);
    }

    if (typeof value === 'number') {
        const asString = String(value);
        const match = options.find((option) => option.value === asString);
        return match ? match.value : asString;
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
 * Values commons-lang3 `BooleanUtils.toBoolean(String)` treats as `true`
 * (compared case-insensitively). Everything else — `"0"`, `"false"`,
 * `"off"`, `"2"`, untrimmed `" 1"`, `""` — is `false`.
 */
const BOOLEAN_TRUE_TOKENS = new Set(['true', 'yes', 'y', 't', 'on', '1']);

/**
 * Faithful port of `org.apache.commons.lang3.BooleanUtils.toBoolean(String)`
 * (commons-lang3 3.18.0). The backend `booleanStrategy` runs every saved
 * value through this on check-in, so mirroring it makes the optimistic
 * preview match exactly what the Page API returns after save.
 *
 * Note: commons-lang3 does NOT trim, so neither do we — `" 1"` is `false`.
 */
function commonsLangToBoolean(value: string): boolean {
    return BOOLEAN_TRUE_TOKENS.has(value.toLowerCase());
}

/**
 * Coerce a raw form-control value to the primitive type its `dataType`
 * implies, so the optimistic page-asset value matches what the Page API
 * returns for the same contentlet.
 *
 * Angular form controls hold strings (`pInputText`) or the option's
 * `value` string (`p-select` / `p-radioButton` with `optionValue`), so a
 * BOOL field configured `Yes|1 / No|0` arrives as `"1"`/`"0"` and an
 * INTEGER field as `"5"`. Pushing those raw into the iframe stringifies
 * types the server would return as real booleans/numbers — and the string
 * `"0"` is truthy in JS, breaking type-sensitive conditional rendering.
 *
 * Each branch mirrors the matching backend write strategy in
 * `FieldHandlerStrategyFactory` so the preview equals the post-save value:
 * - BOOL → commons-lang3 `BooleanUtils.toBoolean` (see above).
 * - INTEGER → `Long.parseLong` (strict: optional sign + digits, no
 *   whitespace, no decimals). Non-integer strings are left as-is — a save
 *   would itself throw and roll back.
 * - FLOAT → `Float.parseFloat` (trims, accepts decimals/scientific).
 *
 * `null`/`undefined`/array (multi-value) values are always left untouched.
 */
export function coerceValueToDataType(dataType: string | undefined, value: unknown): unknown {
    if (value === null || value === undefined || Array.isArray(value)) {
        return value;
    }

    switch (dataType) {
        case DotCMSDataTypes.BOOLEAN:
            // Backend keeps real Booleans as-is and runs everything else
            // (including numbers, stringified) through toBoolean.
            return typeof value === 'boolean' ? value : commonsLangToBoolean(String(value));

        case DotCMSDataTypes.INTEGER: {
            if (typeof value === 'number') {
                return value;
            }
            const str = String(value);
            return /^[+-]?\d+$/.test(str) ? Number(str) : value;
        }

        case DotCMSDataTypes.FLOAT: {
            if (typeof value === 'number') {
                return value;
            }
            const str = String(value).trim();
            if (str === '') {
                return value;
            }
            const parsed = Number(str);
            return Number.isFinite(parsed) ? parsed : value;
        }

        default:
            return value;
    }
}

/**
 * Reconstruct the page-asset-compatible properties for the optimistic
 * update path. Three normalizations happen here:
 *
 * 1. IMAGE/FILE/BINARY: the form stores identifier strings; the page asset
 *    stores objects keyed by `identifier` (image/file) or `idPath` (binary).
 * 2. CHECKBOX (with options) / MULTI_SELECT: the form stores an array of
 *    selected values, but the Page API represents them as a comma-joined
 *    string ("a,b,c"). Re-join so the iframe doesn't receive an array where
 *    the headless app expects a string (arrays interpolate without commas,
 *    e.g. "a,b,c" → "abc").
 * 3. Scalar fields: coerce each value to the primitive its `dataType`
 *    implies (BOOL → boolean, INTEGER/FLOAT → number) so the iframe
 *    receives the same types the Page API returns instead of raw strings.
 *
 * Used by the optimistic-update path to push form values back into the
 * iframe's page tree.
 */
export function toPageAssetProperties(
    fields: ContentletField[],
    formValues: Record<string, unknown>
): Record<string, unknown> {
    const result: Record<string, unknown> = { ...formValues };

    for (const field of fields) {
        if (!(field.variable in formValues)) {
            continue;
        }

        const value = formValues[field.variable];

        if (field.clazz === DotCMSClazzes.IMAGE || field.clazz === DotCMSClazzes.FILE) {
            result[field.variable] = { identifier: value };
        } else if (field.clazz === DotCMSClazzes.BINARY) {
            result[field.variable] = { idPath: value };
        } else if (
            (field.clazz === DotCMSClazzes.CHECKBOX ||
                field.clazz === DotCMSClazzes.MULTI_SELECT) &&
            Array.isArray(value)
        ) {
            result[field.variable] = value.join(',');
        } else {
            result[field.variable] = coerceValueToDataType(field.dataType, value);
        }
    }

    return result;
}
