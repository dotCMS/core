import {
    ChangeDetectionStrategy,
    Component,
    computed,
    DestroyRef,
    effect,
    inject,
    input,
    output,
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
import { ButtonModule } from 'primeng/button';
import { CheckboxModule } from 'primeng/checkbox';
import { InputTextModule } from 'primeng/inputtext';
import { MultiSelectModule } from 'primeng/multiselect';
import { RadioButtonModule } from 'primeng/radiobutton';
import { SelectModule } from 'primeng/select';
import { TextareaModule } from 'primeng/textarea';

import { distinctUntilChanged, filter, map, mergeMap, tap } from 'rxjs/operators';

import {
    DotCopyContentService,
    DotHttpErrorManagerService,
    DotMessageService
} from '@dotcms/data-access';
import { DotCMSClazzes, DotCMSContentTypeField, DotCMSContentlet } from '@dotcms/dotcms-models';
import {
    DotEditContentBinaryFieldComponent,
    DotEditContentService,
    DotFileFieldComponent,
    DotTagFieldComponent
} from '@dotcms/edit-content';
import { DotMessagePipe } from '@dotcms/ui';

import { UveOptimisticSaveService } from '../../../services/uve-optimistic-save/uve-optimistic-save.service';
import { UVE_STATUS } from '../../../shared/enums';
import { ActionPayload, ContainerPayload, ContentletPayload } from '../../../shared/models';
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
 * Optimistic updates run on every change (headless only).
 * Saves happen explicitly via the Save button.
 */
@Component({
    selector: 'dot-uve-contentlet-quick-edit',
    standalone: true,
    imports: [
        ReactiveFormsModule,
        ButtonModule,
        CheckboxModule,
        InputTextModule,
        MultiSelectModule,
        RadioButtonModule,
        SelectModule,
        TextareaModule,

        DotEditContentBinaryFieldComponent,
        DotFileFieldComponent,
        DotTagFieldComponent,
        DotMessagePipe
    ],
    providers: [
        UveOptimisticSaveService,
        DotEditContentService,
        DotCopyContentService,
        DotHttpErrorManagerService
    ],
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
    readonly #dotCopyContentService = inject(DotCopyContentService);
    readonly #dotHttpErrorManagerService = inject(DotHttpErrorManagerService);

    // Inputs (data down from parent container)
    data = input.required<ContentletEditData>({ alias: 'data' });
    loading = input<boolean>(false, { alias: 'loading' });

    readonly openFullEditor = output<void>();

    // Copy decision state
    readonly #copyDecisionMade = signal(false);
    readonly #selectedCopyMode = signal<'all-pages' | 'this-page' | null>(null);
    readonly #isCopying = signal(false);

    readonly $needsCopyDecision = computed(
        () => !this.#copyDecisionMade() && Number(this.data().contentlet?.onNumberOfPages ?? 1) > 1
    );

    protected readonly $selectedCopyMode = this.#selectedCopyMode;
    protected readonly $isCopying = this.#isCopying;

    // Internal form state
    private readonly contentletForm = signal<FormGroup | null>(null);
    readonly $contentletForm = computed(() => this.contentletForm());

    private readonly currentIdentifier = signal<string | null>(null);

    /**
     * Snapshot of the form values at the last build or successful save.
     * Stored as a signal so $isDirty can react to it.
     */
    readonly #savedSnapshot = signal<Record<string, unknown>>({});

    /** Current form values, updated on every valueChanges emission. */
    readonly #currentFormValues = signal<Record<string, unknown>>({});

    /** True when current form values differ from the last saved snapshot. */
    protected readonly $isDirty = computed(() => {
        const current = filterFormValues(this.#currentFormValues());
        const saved = this.#savedSnapshot();
        return JSON.stringify(current) !== JSON.stringify(saved);
    });

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

    /** Resets copy decision whenever the selected contentlet changes. */
    protected readonly $resetCopyDecisionEffect = effect(() => {
        const identifier = this.data().contentlet?.identifier;
        untracked(() => {
            if (identifier !== undefined) {
                this.#copyDecisionMade.set(false);
                this.#selectedCopyMode.set(null);
            }
        });
    });

    constructor() {
        this.#listenToFormChanges();
    }

    protected selectCopyMode(mode: 'all-pages' | 'this-page'): void {
        this.#selectedCopyMode.set(mode);
    }

    protected confirmCopyDecision(): void {
        const mode = this.#selectedCopyMode();

        if (!mode) {
            return;
        }

        if (mode === 'all-pages') {
            this.#copyDecisionMade.set(true);

            return;
        }

        this.#isCopying.set(true);
        const { container, contentlet } = this.data();
        const treeNode = this.#uveStore.getCurrentTreeNode(container, contentlet);

        this.#dotCopyContentService
            .copyInPage(treeNode)
            .pipe(takeUntilDestroyed(this.#destroyRef))
            .subscribe({
                next: (copiedContentlet) => {
                    const newContentletPayload: ContentletPayload = {
                        ...contentlet,
                        identifier: copiedContentlet.identifier,
                        inode: copiedContentlet.inode,
                        title: copiedContentlet.title,
                        contentType: copiedContentlet.contentType,
                        onNumberOfPages: 1
                    };

                    const activeContentlet = this.#uveStore.getPageSavePayload({
                        container,
                        contentlet: newContentletPayload
                    });

                    this.#uveStore.setActiveContentlet(activeContentlet);

                    // We need to reload the page, so the page has the new inode
                    this.#uveStore.pageReload();
                    this.#isCopying.set(false);
                },
                error: (error) => {
                    this.#dotHttpErrorManagerService.handle(error).subscribe();
                    this.#isCopying.set(false);
                }
            });
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
                } else if (fieldValue && typeof fieldValue === 'object') {
                    const binary = fieldValue as Record<string, string>;
                    fieldValue = binary.idPath || binary.versionPath || binary.identifier || '';
                } else {
                    fieldValue = '';
                }
            }

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

            formControls[field.variable] = this.fb.control(
                fieldValue,
                validators.length > 0 ? validators : null
            );

            if (field.readOnly) {
                formControls[field.variable].disable();
            }
        });

        // Clear form first so the template destroys the form block and unbinds old controls.
        this.contentletForm.set(null);
        queueMicrotask(() => {
            const form = this.fb.group(formControls);
            const snapshot = form.value as Record<string, unknown>;
            this.#savedSnapshot.set(snapshot);
            this.#currentFormValues.set(snapshot);
            this.contentletForm.set(form);
        });
    }

    /**
     * Reconstructs the page-asset-compatible properties for IMAGE, FILE, and BINARY fields.
     */
    #toPageAssetProperties(formValues: Record<string, unknown>): Record<string, unknown> {
        const { fields } = this.data();
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

    /**
     * Listens to form value changes for optimistic iframe updates (headless only).
     * Does NOT auto-save — saving is triggered explicitly via the Save button.
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
                            formValues,
                            activeContentlet: this.#uveStore.editorActiveContentlet(),
                            isTraditionalPage: this.#uveStore.pageType() === PageType.TRADITIONAL
                        })),
                        tap(({ formValues, activeContentlet, isTraditionalPage }) => {
                            this.#currentFormValues.set(formValues);

                            if (!isTraditionalPage && activeContentlet) {
                                // eslint-disable-next-line @typescript-eslint/no-unused-vars
                                const { inode: _inode, ...contentProperties } = formValues;
                                this.#optimisticSave.updateIframeOptimistically(
                                    activeContentlet,
                                    this.#toPageAssetProperties(contentProperties)
                                );
                            }
                        })
                    )
                ),
                takeUntilDestroyed(this.#destroyRef)
            )
            .subscribe();
    }

    /** Called by the Save button. */
    protected save(): void {
        const form = this.$contentletForm();

        if (!form || form.invalid || !this.$isDirty()) {
            return;
        }

        const filteredFormValues = filterFormValues(this.#currentFormValues());
        const activeContentlet = this.#uveStore.editorActiveContentlet();
        const isTraditionalPage = this.#uveStore.pageType() === PageType.TRADITIONAL;

        this.#saveFields(filteredFormValues, activeContentlet, isTraditionalPage);
    }

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
                    this.#savedSnapshot.set({ ...filteredFormValues });

                    if (isTraditionalPage) {
                        this.#uveStore.pageReload();
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
