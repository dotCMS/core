import { z } from 'zod';

/**
 * A boolean that also accepts the string forms tool-calling clients (MCP and others) often
 * send ("true"/"false"/"1"/"0"). Plain `z.coerce.boolean()` is wrong here: it uses JS
 * truthiness, so the string "false" becomes `true`. This maps the string forms to their
 * intended value and leaves real booleans untouched.
 *
 * The default is declared twice, on purpose. The inner one covers a blank string, which the
 * preprocess turns into `undefined` AFTER the outer wrapper has already let it through. The
 * outer one is what marks the field optional in the generated JSON Schema: a preprocess hides
 * the inner default from `z.toJSONSchema`, so without it the schema a model reads lists the
 * field as REQUIRED even though omitting it is the normal case.
 */
export const lenientBoolean = (defaultValue: boolean) =>
    z
        .preprocess((value) => {
            if (typeof value === 'string') {
                const v = value.trim().toLowerCase();
                // An empty (or whitespace-only) string means "I did not set this", so it has to
                // become `undefined` — `.default()` only substitutes on `undefined`, so returning
                // the original `''` skipped the default entirely and failed validation with
                // "Expected boolean, received string" for an argument the caller never set.
                if (v === '') return undefined;
                if (v === 'false' || v === '0' || v === 'no') return false;
                if (v === 'true' || v === '1' || v === 'yes') return true;
            }

            return value;
        }, z.boolean().default(defaultValue))
        .default(defaultValue);
