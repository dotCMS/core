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
import { FormBuilder, FormGroup, ReactiveFormsModule } from '@angular/forms';

import { MessageService } from 'primeng/api';
import { ButtonModule } from 'primeng/button';
import { CheckboxModule } from 'primeng/checkbox';
import { InputTextModule } from 'primeng/inputtext';
import { MultiSelectModule } from 'primeng/multiselect';
import { RadioButtonModule } from 'primeng/radiobutton';
import { SelectModule } from 'primeng/select';
import { TextareaModule } from 'primeng/textarea';

import { distinctUntilChanged, filter, map, mergeMap, tap } from 'rxjs/operators';

import { DotMessageService } from '@dotcms/data-access';
import { DotCMSClazzes, DotCMSContentlet } from '@dotcms/dotcms-models';
import {
    DotEditContentService,
    DotFileFieldComponent,
    DotTagFieldComponent
} from '@dotcms/edit-content';
import { DotMessagePipe } from '@dotcms/ui';

import { UveOptimisticSaveService } from '../../../../services/uve-optimistic-save/uve-optimistic-save.service';
import { UVE_STATUS } from '../../../../shared/enums';
import { ActionPayload } from '../../../../shared/models';
import { UVEStore } from '../../../../store/dot-uve.store';
import { PageType } from '../../../../store/models';
import { filterFormValues } from '../../dot-uve-palette/utils';
import { buildQuickEditFormGroup, toPageAssetProperties } from '../quick-edit-form-builder';
import { ContentletEditData, ContentletField } from '../types';

/**
 * The quick-edit form itself: takes a resolved set of fields, builds
 * a `FormGroup`, runs optimistic-update wiring on every value change
 * (headless only), and exposes Save / Cancel / Open-full-editor.
 *
 * Extracted from `DotUveContentletQuickEditComponent`. The host
 * resolves the content type and decides whether to render this
 * component vs. the copy-decision flow vs. an empty state.
 */
@Component({
    selector: 'dot-uve-quick-edit-form',
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
        DotFileFieldComponent,
        DotTagFieldComponent,
        DotMessagePipe
    ],
    providers: [UveOptimisticSaveService, DotEditContentService],
    templateUrl: './dot-uve-quick-edit-form.component.html',
    host: { class: 'flex flex-col h-full' },
    changeDetection: ChangeDetectionStrategy.OnPush
})
export class DotUveQuickEditFormComponent {
    readonly #fb = inject(FormBuilder);
    readonly #uveStore = inject(UVEStore);
    readonly #optimisticSave = inject(UveOptimisticSaveService);
    readonly #destroyRef = inject(DestroyRef);
    readonly #messageService = inject(MessageService);
    readonly #dotMessageService = inject(DotMessageService);

    readonly data = input.required<ContentletEditData>();
    readonly fields = input.required<ContentletField[]>();
    readonly loading = input<boolean>(false);

    readonly openFullEditor = output<void>();
    /** Emitted when the user clicks Cancel. The host decides what to do (typically: close the panel). */
    readonly closed = output<void>();

    readonly #form = signal<FormGroup | null>(null);
    readonly $contentletForm = this.#form.asReadonly();

    readonly #currentInode = signal<string | null>(null);

    /** Last successful-save snapshot — drives `$isDirty`. */
    readonly #savedSnapshot = signal<Record<string, unknown>>({});

    /** Live form values, updated on every valueChanges emission. */
    readonly #currentFormValues = signal<Record<string, unknown>>({});

    protected readonly $isDirty = computed(() => {
        const current = filterFormValues(this.#currentFormValues());
        const saved = this.#savedSnapshot();
        return JSON.stringify(current) !== JSON.stringify(saved);
    });

    protected readonly DotCMSClazzes = DotCMSClazzes;

    /**
     * Rebuild the form when the contentlet's inode changes — covers
     * both "user picked a different contentlet" and "same contentlet
     * was saved and got a fresh inode."
     */
    protected readonly $buildFormEffect = effect(() => {
        const { contentlet } = this.data();
        const fields = this.fields();

        if (!fields || fields.length === 0) {
            this.#form.set(null);
            this.#currentInode.set(null);
            return;
        }

        if (this.#currentInode() !== contentlet.inode) {
            this.#currentInode.set(contentlet.inode);
            this.#buildForm(fields, contentlet);
        }
    });

    protected readonly $enableDisableFormEffect = effect(() => {
        const isLoading = this.loading();
        const form = untracked(() => this.#form());

        if (isLoading) {
            form?.disable({ emitEvent: false });
        } else {
            form?.enable({ emitEvent: false });
        }
    });

    constructor() {
        this.#listenToFormChanges();
    }

    /**
     * Reset the form to the last saved snapshot, then close the panel
     * via the `closed` output. The CVA reset dance (clear-then-patch)
     * is here because component-driven fields like file/image track
     * their own internal signal state — a straight `patchValue(snapshot)`
     * can no-op on an unchanged identifier and leave the asset preview
     * out of sync.
     */
    protected cancel(): void {
        const form = this.#form();

        if (form && this.$isDirty()) {
            Object.keys(form.controls).forEach((key) => {
                const ctrl = form.get(key);
                if (ctrl && !ctrl.disabled) {
                    ctrl.setValue(null, { emitEvent: false });
                }
            });
            form.patchValue(this.#savedSnapshot(), { emitEvent: true });
        }

        this.closed.emit();
    }

    protected save(): void {
        const form = this.#form();
        if (!form) {
            return;
        }

        if (form.invalid) {
            // Surface required/invalid fields when the user clicks Save
            // without filling them in — pristine fields stay visually
            // neutral until touched.
            form.markAllAsTouched();
            return;
        }

        if (!this.$isDirty()) {
            return;
        }

        const filteredFormValues = filterFormValues(this.#currentFormValues());
        const activeContentlet = this.#uveStore.editorSelected()?.payload;
        const isTraditionalPage = this.#uveStore.pageType() === PageType.TRADITIONAL;

        this.#saveFields(filteredFormValues, activeContentlet, isTraditionalPage);
    }

    #buildForm(fields: ContentletField[], contentlet: DotCMSContentlet): void {
        // Clear the form first so the template tears down the form block
        // and unbinds old controls before we install the new group.
        // CVA components (file/image fields) track their own internal
        // signal state — `writeValue` on a same-identifier value can
        // no-op and leave the asset preview out of sync. The microtask
        // ensures the old block fully unmounts before the new one is
        // installed so CVAs remount cleanly.
        this.#form.set(null);
        queueMicrotask(() => {
            const form = buildQuickEditFormGroup(this.#fb, fields, contentlet);
            const snapshot = form.value as Record<string, unknown>;
            this.#savedSnapshot.set(snapshot);
            this.#currentFormValues.set(snapshot);
            this.#form.set(form);
        });
    }

    /**
     * Push form value changes into the iframe optimistically (headless
     * only). Saving still requires an explicit Save click.
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
                            activeContentlet: this.#uveStore.editorSelected()?.payload,
                            isTraditionalPage: this.#uveStore.pageType() === PageType.TRADITIONAL
                        })),
                        tap(({ formValues, activeContentlet, isTraditionalPage }) => {
                            this.#currentFormValues.set(formValues);

                            if (!isTraditionalPage && activeContentlet) {
                                // eslint-disable-next-line @typescript-eslint/no-unused-vars
                                const { inode: _inode, ...contentProperties } = formValues;
                                this.#optimisticSave.updateIframeOptimistically(
                                    activeContentlet,
                                    toPageAssetProperties(this.fields(), contentProperties)
                                );
                            }
                        })
                    )
                ),
                takeUntilDestroyed(this.#destroyRef)
            )
            .subscribe();
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

                    // The API returns the full saved contentlet. Patch
                    // the selection's payload (preserving bounds) so
                    // "Edit in full editor" sees the latest inode + values.
                    const saved = response as DotCMSContentlet;
                    if (saved && activeContentlet) {
                        this.#uveStore.setSelectedPayload({
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
        const activeContentlet = this.#uveStore.editorSelected()?.payload;
        const form = this.#form();

        if (!activeContentlet || !form) {
            return;
        }

        try {
            const fieldNames = this.fields().map((f) => f.variable);
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
