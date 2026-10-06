import type { FormBridge } from './form-bridge.interface';

/**
 * A `Window` carrying the global that custom fields read their API from.
 *
 * Custom fields are author-supplied HTML/JS, so the bridge has to reach them through a global
 * rather than an import. Every writer of that global goes through this type instead of a bare
 * `window['DotCustomFieldApi']` index — `Window` declares only a *numeric* index signature, so
 * a string index there is an implicit `any` and typing the assignment is the only way the
 * compiler can check that what we publish really is a `FormBridge`.
 *
 * Deliberately a named interface rather than a `declare global` augmentation: an augmentation
 * would add the property to every `Window` in every program that transitively includes this
 * file, including the ones that never load the bridge.
 */
export interface DotCustomFieldApiWindow extends Window {
    DotCustomFieldApi: FormBridge;
}
