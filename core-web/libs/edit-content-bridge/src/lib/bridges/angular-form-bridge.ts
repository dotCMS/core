import { Observable, Subscription, take } from 'rxjs';

import { NgZone } from '@angular/core';
import { AbstractControl, FormGroup } from '@angular/forms';

import { DialogService, DynamicDialogRef } from 'primeng/dynamicdialog';

import { DotCMSContentlet, DotSite } from '@dotcms/dotcms-models';
import {
    buildAssetPickerConfig,
    buildAssetPickerDialogConfig,
    DotAssetPickerBrowseOptions,
    DotAssetPickerComponent
} from '@dotcms/ui';

import {
    DotBrowserController,
    DotBrowserItemKind,
    DotBrowserOptions,
    DotBrowserSelection,
    FieldCallback,
    FieldSubscription,
    FieldValidationState,
    FormBridge,
    FormFieldAPI,
    FormFieldValue
} from '../interfaces/form-bridge.interface';

/**
 * Bridge class that enables form editing interoperability in Angular environments.
 * Provides a unified API for getting/setting field values and handling field changes.
 *
 * Angular integration uses FormGroup for form state and NgZone for change detection.
 * Supports multiple callbacks per field, similar to addEventListener.
 *
 * Implements the Singleton pattern to ensure only one instance exists at a time.
 * Use getInstance() to obtain the singleton instance.
 */
export class AngularFormBridge implements FormBridge {
    private static instance: AngularFormBridge | null = null;
    private static refCount = 0;
    private static instanceStack: { instance: AngularFormBridge | null; refCount: number }[] = [];
    #fieldSubscriptions: Map<string, FieldSubscription> = new Map();
    #validationSubscriptions: Set<() => void> = new Set();
    #form: FormGroup;
    #zone: NgZone;
    #dialogService: DialogService;
    #dialogRef: DynamicDialogRef | null = null;
    #visibilityWarningEmitted: Record<'show' | 'hide', boolean> = { show: false, hide: false };

    /**
     * Optional callback invoked when a field's visibility changes via show()/hide().
     * Injected by the consumer (e.g. NativeFieldComponent) to decouple the bridge from the store.
     */
    #onFieldVisibilityChange?: (fieldVariable: string, visible: boolean) => void;

    /**
     * How the bridge finds the site the asset picker browses.
     *
     * The picker cannot browse without one, and the bridge has no injector to reach
     * `DotSiteService` itself — so the host that constructs it supplies this. A function rather
     * than a fixed `DotSite` because the user can switch site between two opens, and the site has
     * to be current each time.
     *
     * Optional: a host that never wires one up simply cannot browse, and `openBrowserModal` says so
     * instead of opening a picker with nothing in it.
     */
    #resolveSite?: () => Observable<DotSite | null>;

    private constructor(
        form: FormGroup,
        zone: NgZone,
        dialogService: DialogService,
        onFieldVisibilityChange?: (fieldVariable: string, visible: boolean) => void,
        resolveSite?: () => Observable<DotSite | null>
    ) {
        this.#form = form;
        this.#zone = zone;
        this.#dialogService = dialogService;
        this.#onFieldVisibilityChange = onFieldVisibilityChange;
        this.#resolveSite = resolveSite;
    }

    /**
     * Gets the singleton instance of AngularFormBridge.
     * If an instance already exists, returns it. Otherwise, creates a new one.
     *
     * @param form - The Angular FormGroup to bridge
     * @param zone - The NgZone for change detection
     * @param dialogService - The PrimeNG DialogService for opening dialogs
     * @param onFieldVisibilityChange - Optional callback to handle field visibility changes from show()/hide()
     * @param resolveSite - Optional site resolver for `openBrowserModal`; see `#resolveSite`
     * @returns The singleton instance of AngularFormBridge
     */
    static getInstance(
        form: FormGroup,
        zone: NgZone,
        dialogService: DialogService,
        onFieldVisibilityChange?: (fieldVariable: string, visible: boolean) => void,
        resolveSite?: () => Observable<DotSite | null>
    ): AngularFormBridge {
        if (!AngularFormBridge.instance) {
            AngularFormBridge.instance = new AngularFormBridge(
                form,
                zone,
                dialogService,
                onFieldVisibilityChange,
                resolveSite
            );
        } else if (
            AngularFormBridge.instance.#form !== form ||
            AngularFormBridge.instance.#zone !== zone
        ) {
            // FormGroup or NgZone changed — the form component was destroyed and recreated
            // (e.g., navigation between content items, manual locale translation, or save +
            // re-open of "new"). The previous instance is bound to a stale form whose controls
            // may still report touched=true from a prior Save, leaking validation state to
            // freshly rendered custom fields. There is no reliable external call site for
            // resetInstance() because Angular tears down the form silently via @if; detecting
            // the change here and resetting is the only safe option.
            if (AngularFormBridge.refCount > 1) {
                // refCount === 1 is the routine single-consumer navigation case and
                // is silently handled by resetInstance(). Higher counts mean multiple
                // NativeFieldComponents still believe the old bridge is live — worth
                // surfacing because their references will go stale.
                console.warn(
                    `AngularFormBridge: replacing instance while refCount=${AngularFormBridge.refCount}. ` +
                        'Some custom fields may still hold a reference to the old bridge. ' +
                        'Ensure all NativeFieldComponent instances are destroyed before the FormGroup changes.'
                );
            }
            AngularFormBridge.resetInstance();
            AngularFormBridge.instance = new AngularFormBridge(
                form,
                zone,
                dialogService,
                onFieldVisibilityChange,
                resolveSite
            );
        } else {
            if (onFieldVisibilityChange !== undefined) {
                AngularFormBridge.instance.#onFieldVisibilityChange = onFieldVisibilityChange;
            }

            if (dialogService !== undefined) {
                AngularFormBridge.instance.#dialogService = dialogService;
            }

            if (resolveSite !== undefined) {
                AngularFormBridge.instance.#resolveSite = resolveSite;
            }
        }

        AngularFormBridge.refCount++;

        return AngularFormBridge.instance;
    }

    /**
     * Resets the singleton instance, allowing a new instance to be created.
     * This will force-destroy the current instance regardless of ref count.
     */
    static resetInstance(): void {
        if (AngularFormBridge.instance) {
            AngularFormBridge.instance.forceDestroy();
            AngularFormBridge.instance = null;
            AngularFormBridge.refCount = 0;
        }
    }

    /**
     * Saves the current singleton instance onto a stack and clears it,
     * allowing a new instance to be created for a nested context (e.g. a dialog).
     * The parent's custom field components retain their direct reference to the
     * stashed instance, so they remain functional behind the modal.
     *
     * Always records a stack frame (even when `instance` is null) so {@link popInstance}
     * restores symmetrically and cannot pop an unrelated prior push.
     */
    static pushInstance(): void {
        AngularFormBridge.instanceStack.push({
            instance: AngularFormBridge.instance,
            refCount: AngularFormBridge.refCount
        });
        AngularFormBridge.instance = null;
        AngularFormBridge.refCount = 0;
    }

    /**
     * Destroys the current singleton instance and restores the previous one
     * from the stack. Call this when a nested context (e.g. a dialog) is closed.
     */
    static popInstance(): void {
        if (AngularFormBridge.instance) {
            AngularFormBridge.instance.forceDestroy();
            AngularFormBridge.instance = null;
            AngularFormBridge.refCount = 0;
        }

        const previous = AngularFormBridge.instanceStack.pop();
        if (previous) {
            AngularFormBridge.instance = previous.instance;
            AngularFormBridge.refCount = previous.refCount;
        }
    }

    /**
     * Retrieves the value of a field from the Angular form.
     *
     * @param fieldId - The ID of the field to retrieve the value from.
     * @returns The value of the field, or null if the field is not found.
     */
    get(fieldId: string): FormFieldValue {
        return this.#form.get(fieldId)?.value;
    }

    /**
     * Sets the value of a field in the Angular form.
     *
     * @param fieldId - The ID of the field to set the value for.
     * @param value - The value to set for the field.
     */
    set(fieldId: string, value: FormFieldValue): void {
        this.#zone.run(() => {
            const control = this.#form.get(fieldId);
            if (control && control.value !== value) {
                control.setValue(value, { emitEvent: true });
                control.markAsTouched();
                control.markAsDirty();
                control.updateValueAndValidity({ emitEvent: true });
            }
        });
    }

    /**
     * Subscribes to field changes in the Angular form.
     * Supports multiple callbacks per field.
     *
     * @param fieldId - The ID of the field to subscribe to.
     * @param callback - The callback function to execute when the field changes.
     * @returns A function to unsubscribe this specific callback.
     */
    onChangeField(fieldId: string, callback: (value: FormFieldValue) => void): () => void {
        const control = this.#form.get(fieldId);
        if (!control) {
            console.warn(`Field '${fieldId}' not found in form`);

            // eslint-disable-next-line @typescript-eslint/no-empty-function
            return () => {};
        }

        const callbackId = Symbol('fieldCallback');
        const fieldCallback: FieldCallback = { id: callbackId, callback };

        let fieldSubscription = this.#fieldSubscriptions.get(fieldId);

        if (!fieldSubscription) {
            // Create new subscription for this field
            const subscription = control.valueChanges.subscribe((value) => {
                const currentFieldSubscription = this.#fieldSubscriptions.get(fieldId);
                if (currentFieldSubscription) {
                    // Execute all callbacks for this field
                    currentFieldSubscription.callbacks.forEach(({ callback: cb }) => {
                        this.#zone.run(() => cb(value));
                    });
                }
            });

            fieldSubscription = {
                subscription,
                callbacks: [fieldCallback]
            };
            this.#fieldSubscriptions.set(fieldId, fieldSubscription);
        } else {
            // Add callback to existing subscription
            fieldSubscription.callbacks.push(fieldCallback);
        }

        // Return unsubscribe function for this specific callback
        return () => this.unsubscribeCallback(fieldId, callbackId);
    }

    /**
     * Unsubscribes a specific callback from field changes.
     *
     * @param fieldId - The ID of the field.
     * @param callbackId - The ID of the callback to remove.
     */
    private unsubscribeCallback(fieldId: string, callbackId: symbol): void {
        const fieldSubscription = this.#fieldSubscriptions.get(fieldId);
        if (!fieldSubscription) return;

        // Remove the specific callback
        fieldSubscription.callbacks = fieldSubscription.callbacks.filter(
            ({ id }) => id !== callbackId
        );

        // If no more callbacks, clean up the subscription
        if (fieldSubscription.callbacks.length === 0) {
            fieldSubscription.subscription.unsubscribe();
            this.#fieldSubscriptions.delete(fieldId);
        }
    }

    /**
     * Emits a one-time console warning when show()/hide() is called
     * but no onFieldVisibilityChange callback was provided.
     */
    private warnIfNoVisibilityCallback(fieldId: string, method: 'show' | 'hide'): void {
        if (!this.#onFieldVisibilityChange && !this.#visibilityWarningEmitted[method]) {
            this.#visibilityWarningEmitted[method] = true;
            console.warn(
                `AngularFormBridge: ${method}() called on field '${fieldId}' but no onFieldVisibilityChange callback is configured. ` +
                    'Field visibility changes will have no effect. ' +
                    'Pass onFieldVisibilityChange when creating the bridge to enable show()/hide() support.'
            );
        }
    }

    /**
     * Decrements the reference count and only truly destroys the singleton
     * when all consumers have released it. Safe to call from each
     * NativeFieldComponent's ngOnDestroy without breaking other instances.
     */
    destroy(): void {
        // Orphan instances (replaced when the FormGroup changed) must not affect
        // the live bridge's ref count.
        if (this !== AngularFormBridge.instance) {
            return;
        }

        AngularFormBridge.refCount = Math.max(0, AngularFormBridge.refCount - 1);

        if (AngularFormBridge.refCount === 0) {
            this.forceDestroy();

            if (AngularFormBridge.instance === this) {
                AngularFormBridge.instance = null;
            }
        }
    }

    /**
     * Unconditionally tears down the bridge: unsubscribes all field
     * subscriptions, closes open dialogs. Used by resetInstance() and
     * as the final step of ref-counted destroy().
     */
    private forceDestroy(): void {
        this.#fieldSubscriptions.forEach((fieldSubscription) => {
            fieldSubscription.subscription.unsubscribe();
        });
        this.#fieldSubscriptions.clear();

        this.#validationSubscriptions.forEach((unsubscribe) => unsubscribe());
        this.#validationSubscriptions.clear();

        this.#dialogRef?.close();
        this.#dialogRef = null;
    }

    /**
     * Gets a field API object for a specific field, providing a convenient interface
     * to interact with the field (get/set value, onChange, enable/disable, show/hide).
     *
     * @param fieldId - The ID of the field to get the API for.
     * @returns A FormFieldAPI object for the specified field.
     */
    getField(fieldId: string): FormFieldAPI {
        return {
            getValue: (): FormFieldValue => {
                return this.get(fieldId);
            },

            setValue: (value: FormFieldValue): void => {
                this.set(fieldId, value);
            },

            onChange: (callback: (value: FormFieldValue) => void): (() => void) => {
                return this.onChangeField(fieldId, callback);
            },

            getValidationState: (): FieldValidationState => {
                const control = this.#form.get(fieldId);
                if (!control) {
                    // Neutral state — "no opinion". Matches DojoFormBridge so VTL templates
                    // that read `state.valid` get the same answer in both editors.
                    // Real validity flows in once the control registers.
                    return {
                        valid: true,
                        invalid: false,
                        touched: false,
                        dirty: false,
                        errors: null
                    };
                }

                return {
                    valid: control.valid,
                    invalid: control.invalid,
                    touched: control.touched,
                    dirty: control.dirty,
                    errors: control.errors
                };
            },

            onValidationChange: (callback: (state: FieldValidationState) => void): (() => void) => {
                // The control may not be registered yet when this method is called
                // (the custom field renders inside `@defer` and its template script
                // can run before the FormGroup has registered every field's control).
                // We listen to the form-level events so we re-attach to the control
                // as soon as it appears, and re-emit on every change after that.
                let activeControl: AbstractControl | null = null;
                let activeControlSub: Subscription | null = null;

                const emit = (control: AbstractControl) => {
                    this.#zone.run(() =>
                        callback({
                            valid: control.valid,
                            invalid: control.invalid,
                            touched: control.touched,
                            dirty: control.dirty,
                            errors: control.errors
                        })
                    );
                };

                const reconcile = () => {
                    const control = this.#form.get(fieldId);
                    if (control === activeControl) {
                        return;
                    }

                    activeControlSub?.unsubscribe();
                    activeControl = control;
                    activeControlSub = control
                        ? control.events.subscribe(() => emit(control))
                        : null;

                    if (control) {
                        emit(control);
                    }
                };

                reconcile();
                const formSub = this.#form.events.subscribe(() => reconcile());

                const unsubscribe = () => {
                    formSub.unsubscribe();
                    activeControlSub?.unsubscribe();
                    activeControl = null;
                    activeControlSub = null;
                    this.#validationSubscriptions.delete(unsubscribe);
                };

                this.#validationSubscriptions.add(unsubscribe);

                return unsubscribe;
            },

            enable: (): void => {
                this.#zone.run(() => {
                    const control = this.#form.get(fieldId);
                    if (control) {
                        control.enable({ emitEvent: true });
                    }
                });
            },

            disable: (): void => {
                this.#zone.run(() => {
                    const control = this.#form.get(fieldId);
                    if (control) {
                        control.disable({ emitEvent: true });
                    }
                });
            },

            show: (): void => {
                this.#zone.run(() => {
                    this.warnIfNoVisibilityCallback(fieldId, 'show');
                    this.#onFieldVisibilityChange?.(fieldId, true);
                });
            },

            hide: (): void => {
                this.#zone.run(() => {
                    this.warnIfNoVisibilityCallback(fieldId, 'hide');
                    this.#onFieldVisibilityChange?.(fieldId, false);
                });
            }
        };
    }

    /**
     * Executes callback when bridge is ready, handling iframe load.
     *
     * @param callback - The callback function to execute when the bridge is ready.
     */
    ready(callback: (api: FormBridge) => void): void {
        callback(this);
    }

    /**
     * Opens the asset browser for a custom-field template and resolves with what the editor picked.
     *
     * Backs `DotCustomFieldApi.openBrowserModal()`. Opens the new AssetPicker — the same picker the
     * File and Image fields use — widened here to also return pages, folders and menu links, which
     * a browser has to offer and an asset picker does not.
     *
     * **Opening is asynchronous.** The picker needs a site, and finding one is a request, so the
     * dialog appears a tick or more after this returns — which is exactly why the outcome arrives
     * through `onClose` rather than as a return value. The controller comes back immediately, and
     * `close()` works even before the dialog exists: it cancels the pending open.
     *
     * @param options What to browse, and what to do with the result. Every field is optional; the
     * defaults browse assets only.
     * @returns A controller for closing the dialog programmatically.
     *
     * @example
     * bridge.openBrowserModal({
     *   title: 'Select a Page',
     *   kinds: ['page', 'link'],
     *   status: 'live',
     *   sort: { field: 'modDate', direction: 'desc' },
     *   onClose: (selection) => {
     *     if (selection) field.setValue(selection.url);
     *   }
     * });
     */
    openBrowserModal(options: DotBrowserOptions = {}): DotBrowserController {
        // Whoever gets there first wins, and every later call is a no-op. PrimeNG can emit its own
        // close after a programmatic `close()`, and a cancelled open races the site lookup —
        // without this, `onClose` would fire twice.
        let settled = false;
        const finish = (selection: DotBrowserSelection | null) => {
            if (settled) {
                return;
            }

            settled = true;
            options.onClose?.(selection);
        };

        if (!this.#resolveSite) {
            console.warn(
                'DotCustomFieldApi.openBrowserModal: this host did not give the bridge a way to ' +
                    'resolve a site, so there is nothing to browse. Pass `resolveSite` to createFormBridge().'
            );
            finish(null);

            return { close: () => undefined };
        }

        // Per-call, not the shared `#dialogRef`: two custom fields can each hold an open picker, and
        // a shared field would let one field's `close()` reach the other's dialog. `#dialogRef` is
        // still set so `destroy()` can close whatever is open.
        let ref: DynamicDialogRef | null = null;
        let cancelled = false;

        const siteSub = this.#resolveSite()
            .pipe(take(1))
            .subscribe({
                next: (site) => {
                    // `close()` beat the lookup — never open a dialog nobody is waiting for.
                    if (cancelled || !site) {
                        finish(null);

                        return;
                    }

                    this.#zone.run(() => {
                        ref = this.#dialogService.open(
                            DotAssetPickerComponent,
                            buildAssetPickerDialogConfig(
                                buildAssetPickerConfig({
                                    mode: 'browse',
                                    site,
                                    title: options.title,
                                    initialAssetPath: options.path,
                                    allowedBaseTypes: baseTypesFor(options.kinds),
                                    browse: browseOptionsFor(options),
                                    // `'archived'` seeds the Status chip rather than pinning a
                                    // flag: it now means *archived only*, and the editor can clear
                                    // it from inside the dialog (FR-014b).
                                    ...(options.status === 'archived'
                                        ? { status: ['ARCHIVED'] }
                                        : {}),
                                    ...(options.mimeTypes?.length
                                        ? { mimeTypes: options.mimeTypes }
                                        : {})
                                })
                            )
                        );
                        this.#dialogRef = ref;

                        ref.onClose.subscribe((item) => {
                            finish(item ? toSelection(item) : null);
                        });
                    });
                },
                // Nothing to browse — same as the File field, which simply does not open when no
                // site resolves. Logged because this is the 5xx / offline / expired-session branch:
                // to the template author `onClose(null)` is indistinguishable from the user
                // pressing Cancel, so without this a "the browse button does nothing" report has
                // nothing behind it.
                error: (error) => {
                    console.error(
                        'DotCustomFieldApi.openBrowserModal: could not resolve the current site, ' +
                            'so the picker did not open.',
                        error
                    );
                    finish(null);
                }
            });

        return {
            close: () => {
                cancelled = true;
                siteSub.unsubscribe();
                ref?.close();
                finish(null);
            }
        };
    }
}

/** Base types a `kinds` list asks the selector to offer. */
const KIND_BASE_TYPES: Partial<Record<DotBrowserItemKind, string>> = {
    file: 'FILEASSET',
    dotasset: 'DOTASSET',
    page: 'HTMLPAGE'
};

/**
 * Which base types the picker may offer.
 *
 * `folder` and `link` are absent on purpose — neither is a base type, so they travel as browse
 * flags instead. An empty result means the caller asked for nothing a base type can express, and
 * the picker falls back to its asset-only default.
 */
function baseTypesFor(kinds?: DotBrowserItemKind[]): string[] | undefined {
    const baseTypes = (kinds ?? [])
        .map((kind) => KIND_BASE_TYPES[kind])
        .filter((baseType): baseType is string => Boolean(baseType));

    return baseTypes.length ? baseTypes : undefined;
}

/**
 * Translates the public options into the picker's own browse vocabulary.
 *
 * Always returns an object, even an empty one: its presence is what marks this as a `browse` open,
 * which is what keeps folders, links and pages structurally unreachable from every other entry
 * point.
 */
function browseOptionsFor(options: DotBrowserOptions): DotAssetPickerBrowseOptions {
    const kinds = options.kinds ?? [];
    const wantsLinks = kinds.includes('link');

    if (wantsLinks && options.mimeTypes?.length) {
        // The browse endpoint drops links whenever a mimetype filter is set, because a link has no
        // file metadata to match against. Surfaced rather than worked around: silently returning
        // fewer kinds than asked for is the worse outcome.
        console.warn(
            'DotCustomFieldApi.openBrowserModal: `mimeTypes` cannot be combined with the `link` ' +
                'kind — menu links carry no MIME type, so the server will omit them.'
        );
    }

    return {
        ...(kinds.includes('folder') ? { showFolders: true } : {}),
        ...(wantsLinks ? { showLinks: true } : {}),
        // Version state only. `'archived'` no longer sets a second flag here — content condition
        // has exactly one representation now, the Status filter, seeded below (FR-014b) — so this
        // line answers a single question: is the query narrowed to published content?
        ...(options.status ? { showWorking: options.status !== 'live' } : {}),
        ...(options.sort
            ? { sortField: options.sort.field, sortByDesc: options.sort.direction === 'desc' }
            : {})
    };
}

/** What the picker returned, in the terms the item itself reports. */
function kindOf(item: Record<string, unknown>): DotBrowserItemKind {
    if (item['type'] === 'folder') {
        return 'folder';
    }

    if (item['type'] === 'link' || item['extension'] === 'link') {
        return 'link';
    }

    const baseType = String(item['baseType'] ?? '');

    if (baseType === 'HTMLPAGE') {
        return 'page';
    }

    return baseType === 'DOTASSET' ? 'dotasset' : 'file';
}

/**
 * Maps a picked row onto the published selection shape.
 *
 * `url` is the one guarantee: a contentlet reports `url` (or `urlMap`), a folder its path, a link
 * its target. Contentlet-only fields are attached only for the kinds that actually have them, so a
 * consumer can never read a mimetype off a folder.
 */
/**
 * A string, or nothing.
 *
 * The row arrives as `Record<string, unknown>`, so a malformed one could hand back a number or an
 * object where the contract promises a string. Narrowing here means a consumer reading `.mimeType`
 * gets either a real string or `undefined`, never something that only claims to be one.
 */
function asString(value: unknown): string | undefined {
    return typeof value === 'string' ? value : undefined;
}

function toSelection(item: DotCMSContentlet | Record<string, unknown>): DotBrowserSelection {
    const row = item as Record<string, unknown>;
    const kind = kindOf(row);
    const base = {
        kind,
        identifier: String(row['identifier'] ?? ''),
        inode: String(row['inode'] ?? ''),
        title: String(row['title'] ?? ''),
        url: String(row['url'] ?? row['urlMap'] ?? row['path'] ?? '')
    };

    if (kind === 'folder' || kind === 'link') {
        return base as DotBrowserSelection;
    }

    if (kind === 'page') {
        return {
            ...base,
            kind,
            baseType: asString(row['baseType']),
            contentType: asString(row['contentType'])
        };
    }

    return {
        ...base,
        kind,
        name: asString(row['name']) ?? asString(row['fileName']),
        mimeType: asString(row['mimeType']),
        baseType: asString(row['baseType']),
        contentType: asString(row['contentType'])
    };
}
