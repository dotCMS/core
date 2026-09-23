import { merge } from 'rxjs';

import { afterNextRender, computed, DestroyRef, inject, Signal, signal } from '@angular/core';
import { takeUntilDestroyed } from '@angular/core/rxjs-interop';
import { ControlContainer, FormControl, TouchedChangeEvent, Validators } from '@angular/forms';

import { filter } from 'rxjs/operators';

import { DotCMSContentlet, DotCMSContentTypeField } from '@dotcms/dotcms-models';

import { DotEditContentStore } from '../../store/edit-content.store';

/**
 * Base class for all wrapper field components that provides common functionality
 * for form control management, validation, and state handling.
 *
 * Note: Child components must define the $field input property.
 */
export abstract class BaseWrapperField {
    protected destroyRef = inject(DestroyRef);
    protected controlContainer = inject(ControlContainer);

    /**
     * Optional on purpose. Subclasses only render inside the editor, where the store exists, but
     * parts of this library also compile into the dotcms-binary-field-builder bundle, which has no
     * store at all. Falling back to "not submitted" there costs nothing and cannot throw.
     */
    protected editContentStore = inject(DotEditContentStore, { optional: true });

    /**
     * `Signal`, not `InputSignal`: this class only ever *reads* these, and `InputSignal<T>` is
     * invariant in `T` — declaring one here would force all 17 subclasses to use byte-identical
     * input types, which they legitimately do not. The custom and JSON fields default `field` to
     * null, the text area declares both as `input.required<T | null>`, and the other 14 use
     * `input.required<T>`. `Signal<T>` is covariant, so every one of those satisfies this.
     *
     * Nullable because this class's own members already assume it: `$showLabel` returns early on
     * `!field` and `isRequired` reads `field?.required`.
     */
    abstract $field: Signal<DotCMSContentTypeField | null>;
    abstract $contentlet: Signal<DotCMSContentlet | null>;

    /**
     * Whether the field should present itself as being in error.
     *
     * Gated on a save or publish having been attempted, NOT on the control's `touched` flag. Blur
     * marks a control touched, so the previous `control.invalid && control.touched` turned an
     * empty required field red the moment the author tabbed out of it — before they had asked for
     * anything to be saved.
     *
     * `control.invalid` is still tracked live by the subscription below, so the error clears as
     * soon as the field holds a valid value, with no second save attempt needed.
     *
     * Computed rather than written, because it has two independent sources: the control's validity
     * and a form-level flag. Setting it from a subscription alone would miss the moment the author
     * presses Save, since that fires no control event.
     */
    $hasError = computed(() => {
        if (!(this.editContentStore?.hasAttemptedSubmit() ?? false)) {
            return false;
        }

        // Read purely as a dependency: it is what makes this recompute when the control's validity
        // changes. The validity itself is read live from the control rather than from a mirrored
        // signal, because the subscription that would populate such a mirror is installed in
        // `afterNextRender`, which does not always run before a field is first asserted on.
        this.$controlRevision();

        return this.formControl?.invalid ?? false;
    });

    /** Bumped on every control event, to give `$hasError` something to depend on. */
    protected $controlRevision = signal(0);

    constructor() {
        afterNextRender(() => {
            const control = this.formControl;
            if (!control) return;

            const updateState = () => {
                this.$controlRevision.update((n) => n + 1);
            };

            // Initial state
            updateState();

            merge(control.valueChanges, control.statusChanges, control.events)
                .pipe(takeUntilDestroyed(this.destroyRef))
                .subscribe(() => {
                    updateState();
                });
        });
    }

    $showLabel = computed(() => {
        const field = this.$field();
        if (!field) return true;

        return field.fieldVariables.find(({ key }) => key === 'hideLabel')?.value !== 'true';
    });

    get isRequired(): boolean {
        // First check the field definition (source of truth)
        const field = this.$field();
        if (field?.required) {
            return true;
        }

        // Fallback to checking the validator (for fields using standard Validators.required)
        const control = this.formControl;
        if (!control) {
            return false;
        }

        return control.hasValidator(Validators.required);
    }

    get isDisabled(): boolean {
        const control = this.formControl;
        if (!control) {
            return false;
        }
        return control.disabled;
    }

    /**
     * `| null` states what the body always did: `ControlContainer.control` is nullable, `get()`
     * returns null for an unknown name, and the field itself may not be bound yet. The old
     * `as FormControl` hid all three — which is why every caller in this class and its subclasses
     * already checks the result before using it.
     */
    get formControl(): FormControl | null {
        // `$field` is `input.required` on most subclasses but NOT all: custom-field and json-field
        // both declare it with a `null` default, so destructuring it unguarded throws a TypeError
        // rather than yielding "no control". Every caller here and in the subclasses already
        // handles a null control; the getter is what did not.
        const field = this.$field();
        if (!field) {
            return null;
        }

        // `controlContainer.control` is itself nullable — a subclass rendered outside a form
        // directive has no container control to ask.
        return (this.controlContainer.control?.get(field.variable) as FormControl) ?? null;
    }

    get statusChanges$() {
        return this.formControl?.events.pipe(
            takeUntilDestroyed(this.destroyRef),
            filter((event) => event instanceof TouchedChangeEvent)
        );
    }
}
