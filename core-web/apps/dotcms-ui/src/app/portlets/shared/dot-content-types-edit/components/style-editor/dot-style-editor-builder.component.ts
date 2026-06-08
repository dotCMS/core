import { patchState, signalState } from '@ngrx/signals';

import { HttpClient } from '@angular/common/http';
import {
    ChangeDetectionStrategy,
    Component,
    DestroyRef,
    computed,
    effect,
    inject,
    input,
    signal,
    untracked
} from '@angular/core';
import { takeUntilDestroyed } from '@angular/core/rxjs-interop';

import { ButtonModule } from 'primeng/button';
import { DialogModule } from 'primeng/dialog';
import { TooltipModule } from 'primeng/tooltip';

import { take } from 'rxjs/operators';

import {
    DotHttpErrorManagerService,
    DotMessageDisplayService,
    DotMessageService
} from '@dotcms/data-access';
import {
    DotCMSContentType,
    DotCMSResponse,
    DotMessageSeverity,
    DotMessageType
} from '@dotcms/dotcms-models';
import { StyleEditorFieldSchema, StyleEditorFormSchema } from '@dotcms/types/internal';
import { DotMessagePipe } from '@dotcms/ui';
import { StyleEditorField, defineStyleEditorSchema, styleEditorField } from '@dotcms/uve/internal';

import { DotStyleEditorSectionComponent } from './components/dot-style-editor-section/dot-style-editor-section.component';
import {
    BuilderField,
    BuilderOption,
    BuilderSection,
    createField,
    createSection,
    fieldHasErrors,
    getDuplicateIdentifiers
} from './models';

const STYLE_EDITOR_SCHEMA_KEY = 'DOT_STYLE_EDITOR_SCHEMA';

/** A single button action within a confirmation dialog. */
interface ConfirmAction {
    label: string;
    /** Render as text-only (no border/background). */
    text: boolean;
    /** Render with an outline border. */
    outlined: boolean;
    /** PrimeNG button severity. */
    severity: 'primary' | 'secondary' | 'danger' | 'contrast';
    callback: () => void;
}

/** Configuration for the single shared confirmation dialog. */
interface ConfirmDialogConfig {
    header: string;
    message: string;
    actions: ConfirmAction[];
}

/** Internal reactive state for the style editor builder. */
interface DotStyleEditorBuilderState {
    /** Ordered list of sections in the form. */
    sections: BuilderSection[];
    /** Whether a save request is currently in flight. */
    saving: boolean;
}

@Component({
    selector: 'dot-style-editor-builder',
    imports: [
        ButtonModule,
        DialogModule,
        TooltipModule,
        DotMessagePipe,
        DotStyleEditorSectionComponent
    ],
    templateUrl: './dot-style-editor-builder.component.html',
    changeDetection: ChangeDetectionStrategy.OnPush
})
export class DotStyleEditorBuilderComponent {
    readonly #http = inject(HttpClient);
    readonly #dotHttpErrorManagerService = inject(DotHttpErrorManagerService);
    readonly #dotMessageDisplayService = inject(DotMessageDisplayService);
    readonly #dotMessageService = inject(DotMessageService);
    readonly #destroyRef = inject(DestroyRef);

    /** Signal state holding sections and saving status. */
    readonly #state = signalState<DotStyleEditorBuilderState>({ sections: [], saving: false });

    /**
     * JSON snapshot of `$sections` at the last load or successful save.
     * Used to derive `$isDirty`: the Save button is enabled only when the
     * current sections differ from this baseline, preventing unnecessary
     * saves and correctly handling the case where a user edits then reverts
     * back to the original values.
     */
    readonly #formSnapshot = signal<string>('[]');

    /** Prevents re-loading the schema on subsequent input changes. */
    #schemaLoaded = false;

    /** The content type being edited, passed from the parent. */
    readonly $contentType = input<DotCMSContentType | undefined>(undefined, {
        alias: 'contentType'
    });

    /** Read-only signal of the current form sections derived from state. */
    readonly $sections = this.#state.sections;

    /** Whether a save operation is currently in progress. */
    readonly $saving = this.#state.saving;

    /**
     * True when the current form state differs from the last saved/loaded snapshot.
     * Drives the enabled/disabled state of the Save button.
     */
    readonly $isDirty = computed(() => JSON.stringify(this.$sections()) !== this.#formSnapshot());

    /** Config for the shared confirmation dialog. Null means the dialog is closed. */
    readonly #confirmState = signal<ConfirmDialogConfig | null>(null);

    /**
     * Becomes true after the first save attempt; drives error display in child components.
     * Kept as an independent signal (not in signalState) so that section/field state updates
     * during typing do not touch this signal's reactive graph.
     */
    readonly #saveAttempted = signal(false);

    /** Public read-only signal consumed by the template to thread showErrors into sections. */
    readonly $saveAttempted = this.#saveAttempted.asReadonly();

    /**
     * Set of field identifiers that appear more than once across all sections.
     * Passed down to section and field components to drive per-field duplicate errors.
     */
    readonly $duplicateIdentifiers = computed(() => getDuplicateIdentifiers(this.$sections()));

    /**
     * True when every field in every section passes all validation rules,
     * including globally unique identifiers.
     * Evaluated after each state change so it is always current.
     */
    readonly $isFormValid = computed(() => {
        if (this.$duplicateIdentifiers().size > 0) return false;

        return this.$sections().every(
            (section) =>
                section.fields.length > 0 && section.fields.every((f) => !fieldHasErrors(f))
        );
    });

    /** Public read-only view of the confirmation dialog state for template binding. */
    readonly $confirmState = this.#confirmState.asReadonly();

    constructor() {
        // Load the schema from metadata once, when the content type is first available.
        // Subsequent saves update $sections in-place, so we don't reload on every input change.
        effect(() => {
            const contentType = this.$contentType();
            if (contentType && !this.#schemaLoaded) {
                this.#schemaLoaded = true;
                untracked(() => this.#loadFromMetadata(contentType));
            }
        });
    }

    /**
     * Persists the current builder state to the content type metadata via the CRUD API.
     *
     * When the form has sections, serializes them as a `StyleEditorFormSchema` and
     * stores the result under the `styleEditorSchema` metadata key.
     *
     * When the form is empty (no sections), the key is removed from the metadata
     * entirely rather than saving an empty schema, keeping the metadata clean.
     */
    save(): void {
        // Always mark save as attempted so validation errors become visible
        this.#saveAttempted.set(true);

        if (!this.$isFormValid()) return;

        const contentType = this.$contentType();
        if (!contentType) return;

        let metadataPatch: Record<string, string | null>;

        if (this.$sections().length === 0) {
            // Null tells the PATCH endpoint to remove the key entirely
            metadataPatch = { [STYLE_EDITOR_SCHEMA_KEY]: null };
        } else {
            const schema = defineStyleEditorSchema({
                contentType: contentType.variable,
                sections: this.$sections().map((section) => ({
                    title: section.title,
                    fields: section.fields.map((field) => this.#toStyleEditorField(field))
                }))
            });
            metadataPatch = { [STYLE_EDITOR_SCHEMA_KEY]: JSON.stringify(schema) };
        }

        patchState(this.#state, { saving: true });
        this.#http
            .patch<DotCMSResponse>(`v1/contenttype/id/${contentType.id}/metadata`, metadataPatch)
            .pipe(take(1), takeUntilDestroyed(this.#destroyRef))
            .subscribe({
                next: () => {
                    patchState(this.#state, { saving: false });
                    this.#saveAttempted.set(false);
                    this.#formSnapshot.set(JSON.stringify(this.$sections()));
                    this.#dotMessageDisplayService.push({
                        life: 3000,
                        message: this.#dotMessageService.get(
                            'style.editor.form.builder.saved.message'
                        ),
                        severity: DotMessageSeverity.SUCCESS,
                        type: DotMessageType.SIMPLE_MESSAGE
                    });
                },
                error: (err) => {
                    patchState(this.#state, { saving: false });
                    this.#dotHttpErrorManagerService.handle(err);
                }
            });
    }

    /**
     * Parses the raw JSON schema stored in the content type metadata and
     * populates the builder state, also setting the baseline snapshot.
     *
     * @param contentType - The content type whose metadata may contain a schema.
     */
    #loadFromMetadata(contentType: DotCMSContentType): void {
        const raw = contentType.metadata?.[STYLE_EDITOR_SCHEMA_KEY];
        if (!raw || typeof raw !== 'string') {
            console.warn('[StyleEditorBuilder] Invalid schema in metadata');
            return;
        }

        try {
            const schema = JSON.parse(raw) as StyleEditorFormSchema;
            const sections = this.#schemaToBuilderSections(schema);
            patchState(this.#state, { sections });
            this.#formSnapshot.set(JSON.stringify(sections));
        } catch {
            console.warn('[StyleEditorBuilder] Invalid schema in metadata');
        }
    }

    /**
     * Converts a serialized `StyleEditorFormSchema` into the mutable
     * `BuilderSection[]` structure used by the builder UI.
     *
     * @param schema - The deserialized schema from metadata.
     * @returns An array of builder sections with newly generated UIDs.
     */
    #schemaToBuilderSections(schema: StyleEditorFormSchema): BuilderSection[] {
        return schema.sections.map((section) => ({
            uid: crypto.randomUUID(),
            title: section.title,
            fields: section.fields.map((field) => this.#schemaFieldToBuilderField(field))
        }));
    }

    /**
     * Converts a single `StyleEditorFieldSchema` into a `BuilderField`
     * with a freshly generated UID.
     *
     * @param field - The raw schema field descriptor.
     * @returns A `BuilderField` suitable for editing in the UI.
     */
    #schemaFieldToBuilderField(field: StyleEditorFieldSchema): BuilderField {
        const rawOptions = (field.config.options as BuilderOption[] | undefined) ?? [];
        const options = rawOptions.map((o): BuilderOption => {
            if (field.type === 'checkboxGroup') {
                return { label: o.label, key: o.value ?? '' };
            }

            const option: BuilderOption = { label: o.label, value: o.value ?? '' };
            if (o.imageURL) {
                option.imageURL = o.imageURL;
            }

            return option;
        });

        return {
            uid: crypto.randomUUID(),
            type: field.type,
            label: field.label,
            identifier: field.id,
            inputType: field.config.inputType ?? 'text',
            placeholder: field.config.placeholder ?? '',
            columns: field.config.columns ?? 1,
            options
        };
    }

    /**
     * Converts a `BuilderField` into the `StyleEditorField` union type
     * expected by `defineStyleEditorSchema`, filtering out blank options.
     *
     * @param field - The builder field to serialize.
     * @returns The corresponding strongly-typed `StyleEditorField` variant.
     */
    #toStyleEditorField(field: BuilderField): StyleEditorField {
        // Filter out blank options — check both value (dropdown/radio) and key (checkboxGroup)
        const validOptions = field.options.filter((opt) => opt.label || opt.value || opt.key);

        switch (field.type) {
            case 'input':
                return styleEditorField.input({
                    id: field.identifier,
                    label: field.label,
                    inputType: field.inputType,
                    ...(field.placeholder ? { placeholder: field.placeholder } : {})
                });

            case 'dropdown':
                return styleEditorField.dropdown({
                    id: field.identifier,
                    label: field.label,
                    options: validOptions.map((o) => ({ label: o.label, value: o.value }))
                });

            case 'radio':
                return styleEditorField.radio({
                    id: field.identifier,
                    label: field.label,
                    columns: field.columns,
                    options: validOptions.map((o) => ({
                        label: o.label,
                        value: o.value,
                        ...(o.imageURL ? { imageURL: o.imageURL } : {})
                    }))
                });

            case 'checkboxGroup':
                return styleEditorField.checkboxGroup({
                    id: field.identifier,
                    label: field.label,
                    options: validOptions.map((o) => ({ label: o.label, key: o.key ?? '' }))
                });
        }
    }

    /** Closes the confirmation dialog without taking any action. */
    closeConfirm(): void {
        this.#confirmState.set(null);
    }

    /**
     * Shows an "Unsaved Changes" confirmation before discarding edits.
     * Triggered by the Cancel button in the bottom action bar.
     */
    requestCancel(): void {
        this.#confirmState.set({
            header: this.#dotMessageService.get('style.editor.form.builder.dialog.unsaved.header'),
            message: this.#dotMessageService.get(
                'style.editor.form.builder.dialog.unsaved.message'
            ),
            actions: [
                {
                    label: this.#dotMessageService.get('style.editor.form.builder.dialog.cancel'),
                    text: true,
                    outlined: false,
                    severity: 'secondary',
                    callback: () => this.#confirmState.set(null)
                },
                {
                    label: this.#dotMessageService.get('style.editor.form.builder.dialog.leave'),
                    text: false,
                    outlined: true,
                    severity: 'secondary',
                    callback: () => {
                        this.#confirmState.set(null);
                        this.#discardChanges();
                    }
                },
                {
                    label: this.#dotMessageService.get(
                        'style.editor.form.builder.dialog.save.close'
                    ),
                    text: false,
                    outlined: false,
                    severity: 'primary',
                    callback: () => {
                        this.#confirmState.set(null);
                        this.save();
                    }
                }
            ]
        });
    }

    /**
     * Shows a "Delete Section" confirmation before removing the section.
     *
     * @param index - Zero-based position of the section to delete.
     */
    requestRemoveSection(index: number): void {
        const section = this.$sections()[index];
        this.#confirmState.set({
            header: this.#dotMessageService.get(
                'style.editor.form.builder.dialog.delete.section.header'
            ),
            message: this.#dotMessageService.get(
                'style.editor.form.builder.dialog.delete.section.message',
                section.title
            ),
            actions: [
                {
                    label: this.#dotMessageService.get('style.editor.form.builder.dialog.cancel'),
                    text: true,
                    outlined: false,
                    severity: 'secondary',
                    callback: () => this.#confirmState.set(null)
                },
                {
                    label: this.#dotMessageService.get('style.editor.form.builder.dialog.delete'),
                    text: false,
                    outlined: false,
                    severity: 'primary',
                    callback: () => {
                        this.#confirmState.set(null);
                        this.removeSection(index);
                    }
                }
            ]
        });
    }

    /**
     * Shows a "Delete Field" confirmation before removing the field.
     *
     * @param sectionIndex - Zero-based position of the parent section.
     * @param fieldUid - Unique identifier of the field to delete.
     */
    requestRemoveField(sectionIndex: number, fieldUid: string): void {
        const field = this.$sections()[sectionIndex]?.fields.find((f) => f.uid === fieldUid);
        this.#confirmState.set({
            header: this.#dotMessageService.get(
                'style.editor.form.builder.dialog.delete.field.header'
            ),
            message: this.#dotMessageService.get(
                'style.editor.form.builder.dialog.delete.field.message',
                field?.label || this.#dotMessageService.get('style.editor.form.builder.field.new')
            ),
            actions: [
                {
                    label: this.#dotMessageService.get('style.editor.form.builder.dialog.cancel'),
                    text: true,
                    outlined: false,
                    severity: 'secondary',
                    callback: () => this.#confirmState.set(null)
                },
                {
                    label: this.#dotMessageService.get('style.editor.form.builder.dialog.delete'),
                    text: false,
                    outlined: false,
                    severity: 'primary',
                    callback: () => {
                        this.#confirmState.set(null);
                        this.removeField(sectionIndex, fieldUid);
                    }
                }
            ]
        });
    }

    /** Resets the form back to the last saved/loaded snapshot, discarding all unsaved changes. */
    #discardChanges(): void {
        const sections = JSON.parse(this.#formSnapshot()) as BuilderSection[];
        patchState(this.#state, { sections });
    }

    /**
     * Appends a new empty section at the end of the sections list.
     */
    addSection(): void {
        const title = this.#dotMessageService.get(
            'style.editor.form.builder.section.default.title'
        );
        patchState(this.#state, ({ sections }) => ({
            sections: [...sections, createSection(title)]
        }));
    }

    /**
     * Removes the section at the given index.
     *
     * @param index - Zero-based position of the section to remove.
     */
    removeSection(index: number): void {
        patchState(this.#state, ({ sections }) => ({
            sections: sections.filter((_, i) => i !== index)
        }));
    }

    /**
     * Moves the section at `index` one position upward.
     *
     * @param index - Zero-based position of the section to move up.
     */
    moveSectionUp(index: number): void {
        if (index === 0) return;
        patchState(this.#state, ({ sections }) => {
            const updated = [...sections];
            [updated[index - 1], updated[index]] = [updated[index], updated[index - 1]];

            return { sections: updated };
        });
    }

    /**
     * Moves the section at `index` one position downward.
     *
     * @param index - Zero-based position of the section to move down.
     */
    moveSectionDown(index: number): void {
        patchState(this.#state, ({ sections }) => {
            if (index === sections.length - 1) return { sections };
            const updated = [...sections];
            [updated[index], updated[index + 1]] = [updated[index + 1], updated[index]];

            return { sections: updated };
        });
    }

    /**
     * Updates the title of the section at the given index.
     *
     * @param index - Zero-based position of the section.
     * @param title - New title string.
     */
    updateSectionTitle(index: number, title: string): void {
        patchState(this.#state, ({ sections }) => {
            const updated = [...sections];
            updated[index] = { ...updated[index], title };

            return { sections: updated };
        });
    }

    /**
     * Appends a new empty field to the section at the given index.
     *
     * @param sectionIndex - Zero-based position of the parent section.
     */
    addField(sectionIndex: number): void {
        patchState(this.#state, ({ sections }) => {
            const updated = [...sections];
            updated[sectionIndex] = {
                ...updated[sectionIndex],
                fields: [...updated[sectionIndex].fields, createField()]
            };

            return { sections: updated };
        });
    }

    /**
     * Removes the field identified by `fieldUid` from the section at `sectionIndex`.
     *
     * @param sectionIndex - Zero-based position of the parent section.
     * @param fieldUid - Unique identifier of the field to remove.
     */
    removeField(sectionIndex: number, fieldUid: string): void {
        patchState(this.#state, ({ sections }) => {
            const updated = [...sections];
            updated[sectionIndex] = {
                ...updated[sectionIndex],
                fields: updated[sectionIndex].fields.filter((f) => f.uid !== fieldUid)
            };

            return { sections: updated };
        });
    }

    /**
     * Moves the field identified by `fieldUid` one position upward within its section.
     *
     * @param sectionIndex - Zero-based position of the parent section.
     * @param fieldUid - Unique identifier of the field to move up.
     */
    moveFieldUp(sectionIndex: number, fieldUid: string): void {
        patchState(this.#state, ({ sections }) => {
            const updated = [...sections];
            const fields = [...updated[sectionIndex].fields];
            const idx = fields.findIndex((f) => f.uid === fieldUid);
            if (idx <= 0) return { sections };
            [fields[idx - 1], fields[idx]] = [fields[idx], fields[idx - 1]];
            updated[sectionIndex] = { ...updated[sectionIndex], fields };

            return { sections: updated };
        });
    }

    /**
     * Moves the field identified by `fieldUid` one position downward within its section.
     *
     * @param sectionIndex - Zero-based position of the parent section.
     * @param fieldUid - Unique identifier of the field to move down.
     */
    moveFieldDown(sectionIndex: number, fieldUid: string): void {
        patchState(this.#state, ({ sections }) => {
            const updated = [...sections];
            const fields = [...updated[sectionIndex].fields];
            const idx = fields.findIndex((f) => f.uid === fieldUid);
            if (idx === -1 || idx === fields.length - 1) return { sections };
            [fields[idx], fields[idx + 1]] = [fields[idx + 1], fields[idx]];
            updated[sectionIndex] = { ...updated[sectionIndex], fields };

            return { sections: updated };
        });
    }

    /**
     * Replaces the field whose `uid` matches `updatedField.uid` inside the
     * section at `sectionIndex` with the provided updated value.
     *
     * @param sectionIndex - Zero-based position of the parent section.
     * @param updatedField - The new field value to store.
     */
    updateField(sectionIndex: number, updatedField: BuilderField): void {
        patchState(this.#state, ({ sections }) => {
            const updated = [...sections];
            updated[sectionIndex] = {
                ...updated[sectionIndex],
                fields: updated[sectionIndex].fields.map((f) =>
                    f.uid === updatedField.uid ? updatedField : f
                )
            };

            return { sections: updated };
        });
    }
}
