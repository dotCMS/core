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
import { DotCMSClazzes, DotCMSContentlet, DotCMSContentTypeField } from '@dotcms/dotcms-models';
import {
    DotEditContentBinaryFieldComponent,
    DotEditContentService,
    DotFileFieldComponent,
    DotTagFieldComponent
} from '@dotcms/edit-content';
import { DotColorIconComponent, DotMessagePipe, DotSpinnerComponent } from '@dotcms/ui';

import { UveOptimisticSaveService } from '../../../services/uve-optimistic-save/uve-optimistic-save.service';
import { UVE_STATUS } from '../../../shared/enums';
import { ActionPayload, ContainerPayload, ContentletPayload } from '../../../shared/models';
import { UVEStore } from '../../../store/dot-uve.store';
import { PageType } from '../../../store/models';
import { getQuickEditFields, parseFieldValues } from '../../utils';
import { filterFormValues } from '../dot-uve-palette/utils';

/** Sentinel value emitted by the UVE SDK when hovering over an empty container. */
const TEMP_EMPTY_CONTENTLET_TYPE = 'TEMP_EMPTY_CONTENTLET_TYPE';

export const CopyMode = {
    ALL_PAGES: 'all-pages',
    THIS_PAGE: 'this-page'
} as const;

export type CopyMode = (typeof CopyMode)[keyof typeof CopyMode];

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

        DotColorIconComponent,
        DotEditContentBinaryFieldComponent,
        DotFileFieldComponent,
        DotTagFieldComponent,
        DotMessagePipe,
        DotSpinnerComponent
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

    /**
     * True while the content type for the active contentlet has not yet been loaded into cache.
     * The form renders only after this resolves.
     */
    readonly $isEmptyContainer = computed(
        () => this.data().contentlet?.contentType === TEMP_EMPTY_CONTENTLET_TYPE
    );

    readonly $isLoadingContentType = computed(() => {
        const contentType = this.data().contentlet?.contentType;

        return (
            !!contentType &&
            contentType !== TEMP_EMPTY_CONTENTLET_TYPE &&
            !this.#uveStore.contentTypeCache()[contentType]
        );
    });

    /**
     * Quick-edit fields derived from the cached content type.
     * Empty array until the content type is loaded.
     */
    readonly $fields = computed((): ContentletField[] => {
        const contentType = this.data().contentlet?.contentType;

        if (!contentType) {
            return [];
        }

        const cached = this.#uveStore.contentTypeCache()[contentType];

        if (!cached?.layout) {
            return [];
        }

        return getQuickEditFields(cached.layout).map((field) => ({
            ...field,
            options: parseFieldValues(field.values)
        }));
    });

    /** Triggers a cache load whenever the active contentlet's content type changes. */
    protected readonly $loadContentTypeEffect = effect(() => {
        const contentType = this.data().contentlet?.contentType;

        if (contentType && contentType !== TEMP_EMPTY_CONTENTLET_TYPE) {
            untracked(() => this.#uveStore.loadContentType(contentType));
        }
    });

    // Copy decision state
    readonly #copyDecisionMade = signal(false);
    readonly #selectedCopyMode = signal<CopyMode | null>(null);
    readonly #isCopying = signal(false);
    readonly #lastResetIdentifier = signal<string | undefined>(undefined);

    readonly $needsCopyDecision = computed(
        () => !this.#copyDecisionMade() && Number(this.data().contentlet?.onNumberOfPages ?? 1) > 1
    );

    protected readonly $selectedCopyMode = this.#selectedCopyMode;
    protected readonly $isCopying = this.#isCopying;

// Internal form state
    private readonly contentletForm = signal<FormGroup | null>(null);
    readonly $contentletForm = computed(() => this.contentletForm());

    private readonly currentInode = signal<string | null>(null);

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
    protected readonly CopyMode = CopyMode;

    // Build form when data or resolved fields change
    protected readonly $buildFormEffect = effect(() => {
        const { contentlet } = this.data();
        const fields = this.$fields();

        if (!fields || fields.length === 0) {
            this.contentletForm.set(null);
            this.currentInode.set(null);

            return;
        }

        // inode is a UUID unique to each contentlet version, so it changes both when
        // the user selects a different contentlet and when the same contentlet is saved
        // (e.g. after a save/publish in the full-editor dialog). One check covers both.
        if (this.currentInode() !== contentlet.inode) {
            this.currentInode.set(contentlet.inode);
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

    /** Resets copy decision only when the selected contentlet actually changes. */
    protected readonly $resetCopyDecisionEffect = effect(() => {
        const identifier = this.data().contentlet?.identifier;
        untracked(() => {
            if (identifier !== undefined && identifier !== this.#lastResetIdentifier()) {
                this.#lastResetIdentifier.set(identifier);
                this.#copyDecisionMade.set(false);
                this.#selectedCopyMode.set(null);
            }
        });
    });

    constructor() {
        this.#listenToFormChanges();
    }

    protected selectCopyMode(mode: CopyMode): void {
        if (this.#isCopying()) {
            return;
        }
        this.#selectedCopyMode.set(mode);
        this.confirmCopyDecision();
    }

    protected confirmCopyDecision(): void {
        const mode = this.#selectedCopyMode();

        if (!mode) {
            return;
        }

        if (mode === CopyMode.ALL_PAGES) {
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
        const fields = this.$fields();
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

    /**
     * Called by the Cancel button. Resets the form to the last saved snapshot.
     *
     * CVA components (e.g. file/image fields) track their own internal signal state.
     * When the user removes a file, the store calls onChange('') but the CVA's $value
     * signal is never updated — it still holds the original identifier. A straight
     * patchValue(snapshot) then calls writeValue(originalId) on a signal that already
     * holds originalId, so no change is detected and the asset preview is not restored.
     *
     * Fix: clear every control to null first (no event emitted, no optimistic update),
     * then restore via patchValue. Both happen synchronously so Angular batches change
     * detection and there is no visible flicker. Angular's signal effects compare the
     * signal version — not the value — so even though $value ends up at the same
     * identifier, the version change triggers handleValueChange and reloads the asset.
     */
    protected cancel(): void {
        const form = this.$contentletForm();

        // Reset form to last saved snapshot if dirty. The CVA reset
        // dance below is documented above this method's signature.
        if (form && this.$isDirty()) {
            Object.keys(form.controls).forEach((key) => {
                const ctrl = form.get(key);
                if (ctrl && !ctrl.disabled) {
                    ctrl.setValue(null, { emitEvent: false });
                }
            });
            form.patchValue(this.#savedSnapshot(), { emitEvent: true });
        }

        // Cancel always exits: clear the side-panel target and close
        // the panel so the user is back to the page view. The
        // selected overlay (border) remains so they can still see
        // what they were working on.
        this.#uveStore.resetActiveContentlet();
        this.#uveStore.setEditPanelOpen(false);
    }

    /** Called by the Save button. */
    protected save(): void {
        const form = this.$contentletForm();
        if (!form) {
            return;
        }

        // Surface validation messages on required/invalid fields when
        // the user clicks Save without filling everything in. Without
        // markAllAsTouched(), pristine fields stay visually neutral
        // even though the form is invalid.
        if (form.invalid) {
            form.markAllAsTouched();
            return;
        }

        if (!this.$isDirty()) {
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
                next: (response) => {
                    this.#savedSnapshot.set({ ...filteredFormValues });

                    // The API returns the full saved contentlet as a single map.
                    // Replace the entire contentlet on the active payload so that
                    // "Open Full Editor" receives the latest inode and field values.
                    const saved = response as DotCMSContentlet;
                    if (saved && activeContentlet) {
                        this.#uveStore.setActiveContentlet({
                            ...activeContentlet,
                            contentlet: { ...activeContentlet.contentlet, ...saved }
                        });
                    }

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
            const fieldNames = this.$fields().map((f) => f.variable);
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
