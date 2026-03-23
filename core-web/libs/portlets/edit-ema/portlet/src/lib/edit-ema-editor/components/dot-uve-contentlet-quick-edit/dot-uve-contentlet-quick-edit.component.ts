import { of, timer } from 'rxjs';

import {
    ChangeDetectionStrategy,
    Component,
    computed,
    DestroyRef,
    effect,
    inject,
    input,
    signal,
    untracked
} from '@angular/core';
import { takeUntilDestroyed, toObservable } from '@angular/core/rxjs-interop';
import {
    AbstractControl,
    FormBuilder,
    FormGroup,
    ReactiveFormsModule,
    Validators
} from '@angular/forms';

import { MessageService } from 'primeng/api';
import { CheckboxModule } from 'primeng/checkbox';
import { InputTextModule } from 'primeng/inputtext';
import { MultiSelectModule } from 'primeng/multiselect';
import { RadioButtonModule } from 'primeng/radiobutton';
import { SelectModule } from 'primeng/select';
import { TextareaModule } from 'primeng/textarea';

import { debounce, distinctUntilChanged, filter, map, mergeMap, tap } from 'rxjs/operators';

import { DotMessageService } from '@dotcms/data-access';
import { DotCMSClazzes, DotCMSContentTypeField, DotCMSContentlet } from '@dotcms/dotcms-models';
import { DotEditContentBinaryFieldComponent, DotFileFieldComponent } from '@dotcms/edit-content';

import { UveOptimisticSaveService } from '../../../services/uve-optimistic-save/uve-optimistic-save.service';
import { STYLE_EDITOR_DEBOUNCE_TIME } from '../../../shared/consts';
import { UVE_STATUS } from '../../../shared/enums';
import { ActionPayload, ContainerPayload } from '../../../shared/models';
import { UVEStore } from '../../../store/dot-uve.store';
import { PageType } from '../../../store/models';
import { filterFormValues } from '../dot-uve-palette/utils';

/**
 * Pick only the fields needed for the quick-edit form from DotCMSContentTypeField.
 * Extends with options property for dropdown/checkbox/radio rendering.
 */
export type ContentletField = Pick<
    DotCMSContentTypeField,
    | 'name'
    | 'variable'
    | 'clazz'
    | 'required'
    | 'readOnly'
    | 'regexCheck'
    | 'dataType'
    | 'fieldVariables'
    | 'fieldType'
> & {
    options?: Array<{ label: string; value: string }>;
};

export interface ContentletEditData {
    container: ContainerPayload | undefined;
    contentlet: DotCMSContentlet;
    fields: ContentletField[];
}

/**
 * Smart component for quick-editing contentlet form fields in the right sidebar.
 * Handles auto-save with optimistic updates, debounce, and rollback on failure.
 */
@Component({
    selector: 'dot-uve-contentlet-quick-edit',
    standalone: true,
    imports: [
        ReactiveFormsModule,
        CheckboxModule,
        InputTextModule,
        MultiSelectModule,
        RadioButtonModule,
        SelectModule,
        TextareaModule,

        DotEditContentBinaryFieldComponent,
        DotFileFieldComponent
    ],
    providers: [UveOptimisticSaveService],
    templateUrl: './dot-uve-contentlet-quick-edit.component.html',
    host: { class: 'flex flex-col h-full' },
    changeDetection: ChangeDetectionStrategy.OnPush
})
export class DotUveContentletQuickEditComponent {
    private readonly fb = inject(FormBuilder);
    readonly #uveStore = inject(UVEStore);
    readonly #optimisticSave = inject(UveOptimisticSaveService);
    readonly #destroyRef = inject(DestroyRef);
    readonly #messageService = inject(MessageService);
    readonly #dotMessageService = inject(DotMessageService);

    // Inputs (data down from parent container)
    data = input.required<ContentletEditData>({ alias: 'data' });
    loading = input<boolean>(false, { alias: 'loading' });

    // Internal form state
    private readonly contentletForm = signal<FormGroup | null>(null);
    protected readonly $contentletForm = computed(() => this.contentletForm());

    private readonly currentIdentifier = signal<string | null>(null);

    /**
     * Snapshot of the form values at the last build or successful save.
     * Used to skip saves triggered by CVA initialization cycles (e.g. DotFileFieldComponent
     * resetting its value to '' during initLoad, then restoring it from HTTP — both fire
     * valueChanges but the final debounced value is identical to what was built into the form).
     */
    #savedSnapshot: Record<string, unknown> = {};

    protected readonly DotCMSClazzes = DotCMSClazzes;
    // Build form when data changes
    protected readonly $buildFormEffect = effect(() => {
        const { fields, contentlet } = this.data();

        if (!fields || fields.length === 0) {
            this.contentletForm.set(null);
            return;
        }

        if (this.currentIdentifier() !== contentlet.identifier) {
            this.currentIdentifier.set(contentlet.identifier);
            this.buildForm(fields, contentlet);
        }
    });

    protected readonly $enableDisableFormEffect = effect(() => {
        const isLoading = this.loading();
        const form = untracked(() => this.$contentletForm());

        if (isLoading) {
            form?.disable({ emitEvent: false });
        } else {
            form?.enable({ emitEvent: false });
        }
    });

    constructor() {
        this.#listenToFormChanges();
    }

    private buildForm(fields: ContentletField[], contentlet: DotCMSContentlet): void {
        const formControls: Record<string, AbstractControl> = {};

        // Add hidden inode field
        if (contentlet?.inode) {
            formControls['inode'] = this.fb.control(contentlet.inode);
        }

        fields.forEach((field) => {
            let fieldValue: string | string[] | boolean | DotCMSContentlet =
                contentlet?.[field.variable] ?? '';
            const validators = [];

            // Handle checkbox with multiple options - value should be an array
            if (field.clazz === DotCMSClazzes.CHECKBOX && field.options?.length) {
                // Convert string value to array if needed
                if (typeof fieldValue === 'string' && fieldValue) {
                    fieldValue = fieldValue.split(',').map((v) => v.trim());
                } else if (!Array.isArray(fieldValue)) {
                    fieldValue = [];
                }
            }

            // Handle multi-select - value should be an array
            if (field.clazz === DotCMSClazzes.MULTI_SELECT) {
                if (typeof fieldValue === 'string' && fieldValue) {
                    fieldValue = fieldValue.split(',').map((v) => v.trim());
                } else if (!Array.isArray(fieldValue)) {
                    fieldValue = [];
                }
            }

            if (field.clazz === DotCMSClazzes.IMAGE) {
                if (typeof fieldValue === 'string' && fieldValue) {
                    fieldValue = fieldValue.trim();
                } else if (
                    fieldValue &&
                    typeof fieldValue === 'object' &&
                    'identifier' in fieldValue
                ) {
                    fieldValue = fieldValue.identifier ?? '';
                } else {
                    fieldValue = '';
                }
            }

            if (field.clazz === DotCMSClazzes.FILE) {
                if (typeof fieldValue === 'string' && fieldValue) {
                    fieldValue = fieldValue.trim();
                } else if (
                    fieldValue &&
                    typeof fieldValue === 'object' &&
                    'identifier' in fieldValue
                ) {
                    fieldValue = fieldValue.identifier ?? '';
                } else {
                    fieldValue = '';
                }
            }

            if (field.clazz === DotCMSClazzes.BINARY) {
                if (typeof fieldValue === 'string' && fieldValue) {
                    fieldValue = fieldValue.trim();
                } else if (
                    fieldValue &&
                    typeof fieldValue === 'object' &&
                    'identifier' in fieldValue
                ) {
                    fieldValue = fieldValue.identifier ?? '';
                } else {
                    fieldValue = '';
                }
            }

            if (field.required) {
                validators.push(Validators.required);
            }

            if (field.regexCheck) {
                try {
                    // Validate the regex pattern before using it
                    new RegExp(field.regexCheck);
                    validators.push(Validators.pattern(field.regexCheck));
                } catch (error) {
                    // Skip invalid regex patterns
                    console.warn(
                        `Invalid regex pattern for field ${field.variable}: ${field.regexCheck}`,
                        error
                    );
                }
            }

            formControls[field.variable] = this.fb.control(
                fieldValue,
                validators.length > 0 ? validators : null
            );

            if (field.readOnly) {
                formControls[field.variable].disable();
            }
        });

        // Clear form first so the template destroys the form block and unbinds old controls.
        // queueMicrotask delays setting the new form until after Angular has processed the null,
        // ensuring ControlValueAccessor writeValue() calls complete before valueChanges subscription starts.
        this.contentletForm.set(null);
        queueMicrotask(() => {
            const form = this.fb.group(formControls);
            // Snapshot uses form.value (not getRawValue) to match what valueChanges emits —
            // getRawValue includes disabled controls but valueChanges excludes them.
            this.#savedSnapshot = form.value as Record<string, unknown>;
            this.contentletForm.set(form);
        });
    }

    /**
     * Reconstructs the page-asset-compatible properties for IMAGE, FILE, and BINARY fields.
     * The form stores only the identifier string for these fields, but the page asset may hold
     * the full contentlet object (e.g. { identifier, title, url, ... }). Writing a plain string
     * back would corrupt the shape that the headless renderer expects, so we restore the original
     * object structure with only the identifier updated.
     *
     * For text/other fields the value is returned as-is.
     */
    #toPageAssetProperties(
        formValues: Record<string, unknown>
    ): Record<string, unknown> {
        const { fields, contentlet } = this.data();
        const result: Record<string, unknown> = { ...formValues };

        for (const field of fields) {
            if (
                field.clazz !== DotCMSClazzes.IMAGE &&
                field.clazz !== DotCMSClazzes.FILE &&
                field.clazz !== DotCMSClazzes.BINARY
            ) {
                continue;
            }

            const newValue = formValues[field.variable];

            if (typeof newValue !== 'string') {
                continue;
            }

            const original = contentlet?.[field.variable];

            if (original && typeof original === 'object' && 'identifier' in original) {
                // Preserve the original object shape but swap in the new identifier
                result[field.variable] = { ...(original as object), identifier: newValue };
            }
            // Otherwise (already a string in the page asset) leave newValue as-is
        }

        return result;
    }

    /**
     * Listens to form changes and handles:
     * 1. Immediate optimistic iframe updates (no debounce, headless only)
     * 2. Debounced API saves via workflow EDIT action
     *
     * Uses mergeMap so that if the form is rebuilt during rollback, both the old
     * and new form subscriptions stay active — ensuring pending debounced saves complete.
     */
    #listenToFormChanges(): void {
        toObservable(this.$contentletForm)
            .pipe(
                filter((form): form is FormGroup => form !== null),
                mergeMap((form) =>
                    form.valueChanges.pipe(
                        distinctUntilChanged(
                            (prev, curr) => JSON.stringify(prev) === JSON.stringify(curr)
                        ),
                        map((formValues) => ({
                            form,
                            formValues,
                            activeContentlet: this.#uveStore.editorActiveContentlet(),
                            isTraditionalPage: this.#uveStore.pageType() === PageType.TRADITIONAL
                        })),
                        tap(({ formValues, activeContentlet, isTraditionalPage }) => {
                            if (!isTraditionalPage && activeContentlet) {
                                // Exclude inode from optimistic update — it's metadata, not content
                                // eslint-disable-next-line @typescript-eslint/no-unused-vars
                                const { inode: _inode, ...contentProperties } = formValues;
                                this.#optimisticSave.updateIframeOptimistically(
                                    activeContentlet,
                                    this.#toPageAssetProperties(contentProperties)
                                );
                            }
                        }),
                        debounce((changeEvent) =>
                            changeEvent.isTraditionalPage
                                ? of(0)
                                : timer(STYLE_EDITOR_DEBOUNCE_TIME)
                        )
                    )
                ),
                takeUntilDestroyed(this.#destroyRef)
            )
            .subscribe(({ form, formValues, activeContentlet, isTraditionalPage }) => {
                const filteredFormValues = filterFormValues(formValues);

                if (
                    form.invalid ||
                    JSON.stringify(filteredFormValues) === JSON.stringify(this.#savedSnapshot)
                ) {
                    return;
                }

                this.#saveFields(filteredFormValues, activeContentlet, isTraditionalPage);
            });
    }

    /**
     * Saves contentlet field values to the API after debounce.
     * Saves current state to history before API call for rollback support.
     */
    #saveFields(
        filteredFormValues: Record<string, unknown>,
        activeContentlet: ActionPayload | null,
        isTraditionalPage = false
    ): void {
        if (!activeContentlet || Object.keys(filteredFormValues).length === 0) {
            return;
        }

        this.#uveStore.addCurrentPageToHistory();

        if (isTraditionalPage) {
            this.#uveStore.setUveStatus(UVE_STATUS.LOADING);
        }

        this.#uveStore
            .saveQuickEditFields(filteredFormValues as Record<string, string>)
            .pipe(takeUntilDestroyed(this.#destroyRef))
            .subscribe({
                next: () => {
                    // Advance the snapshot so future saves only fire on real changes
                    this.#savedSnapshot = { ...filteredFormValues };

                    if (isTraditionalPage) {
                        this.#uveStore.setUveStatus(UVE_STATUS.LOADED);
                    }

                    this.#messageService.add({
                        severity: 'success',
                        summary: this.#dotMessageService.get('message.content.saved'),
                        detail: this.#dotMessageService.get(
                            'message.content.note.already.published'
                        ),
                        life: 2000
                    });
                },
                error: (error) => {
                    this.#restoreFormFromRollback();

                    if (isTraditionalPage) {
                        this.#uveStore.setUveStatus(UVE_STATUS.LOADED);
                    }

                    this.#messageService.add({
                        severity: 'error',
                        summary: this.#dotMessageService.get(
                            'editpage.content.update.contentlet.error'
                        ),
                        detail: error?.message || '',
                        life: 2000
                    });
                }
            });
    }

    /**
     * Restores form field values from the already-rolled-back page asset state.
     */
    #restoreFormFromRollback(): void {
        const activeContentlet = this.#uveStore.editorActiveContentlet();
        const form = this.$contentletForm();

        if (!activeContentlet || !form) {
            return;
        }

        try {
            const fieldNames = this.data().fields.map((f) => f.variable);
            const extracted = this.#optimisticSave.extractFromRollback(
                activeContentlet,
                fieldNames
            );
            form.patchValue(extracted, { emitEvent: false });
        } catch (error) {
            console.error('Error restoring form from rollback:', error);
        }
    }
}
