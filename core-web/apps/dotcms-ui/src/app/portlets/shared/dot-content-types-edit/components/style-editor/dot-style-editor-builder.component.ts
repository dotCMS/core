import { Component, computed, effect, inject, input, signal, untracked } from '@angular/core';

import { ButtonModule } from 'primeng/button';

import { take } from 'rxjs/operators';

import { patchState, signalState } from '@ngrx/signals';

import {
    DotCrudService,
    DotHttpErrorManagerService,
    DotMessageDisplayService
} from '@dotcms/data-access';
import { DotCMSContentType, DotMessageSeverity, DotMessageType } from '@dotcms/dotcms-models';
import {
    StyleEditorField,
    StyleEditorFieldSchema,
    StyleEditorFormSchema,
    defineStyleEditorSchema,
    styleEditorField
} from '@dotcms/uve';

import { DotStyleEditorSectionComponent } from './dot-style-editor-section.component';
import { BuilderField, BuilderSection, createField, createSection } from './models';

const STYLE_EDITOR_SCHEMA_KEY = 'styleEditorSchema';

/** Internal reactive state for the style editor builder. */
interface DotStyleEditorBuilderState {
    /** Ordered list of sections in the form. */
    sections: BuilderSection[];
    /** Whether a save request is currently in flight. */
    saving: boolean;
}

@Component({
    selector: 'dot-style-editor-builder',
    imports: [ButtonModule, DotStyleEditorSectionComponent],
    templateUrl: './dot-style-editor-builder.component.html'
})
export class DotStyleEditorBuilderComponent {
    readonly #crudService = inject(DotCrudService);
    readonly #dotHttpErrorManagerService = inject(DotHttpErrorManagerService);
    readonly #dotMessageDisplayService = inject(DotMessageDisplayService);

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
        const contentType = this.$contentType();
        if (!contentType) return;

        const existingMetadata = { ...(contentType.metadata ?? {}) };

        let updatedMetadata: typeof existingMetadata;

        if (this.$sections().length === 0) {
            // Empty form — remove the key so metadata stays clean (no empty schema noise)
            const { [STYLE_EDITOR_SCHEMA_KEY]: _removed, ...rest } = existingMetadata;
            updatedMetadata = rest;
        } else {
            const schema = defineStyleEditorSchema({
                contentType: contentType.variable,
                sections: this.$sections().map((section) => ({
                    title: section.title,
                    fields: section.fields.map((field) => this.#toStyleEditorField(field))
                }))
            });
            updatedMetadata = { ...existingMetadata, [STYLE_EDITOR_SCHEMA_KEY]: JSON.stringify(schema) };
        }

        // `systemActionMappings` contains full workflow-action objects that the API
        // misinterprets as action IDs when round-tripped in a PUT body. Strip it out.
        const { systemActionMappings: _wf, ...contentTypeData } = contentType;

        const payload: DotCMSContentType = {
            ...contentTypeData,
            metadata: updatedMetadata
        };

        patchState(this.#state, { saving: true });
        this.#crudService
            .putData<DotCMSContentType>(`v1/contenttype/id/${contentType.id}`, payload)
            .pipe(take(1))
            .subscribe({
                next: () => {
                    patchState(this.#state, { saving: false });
                    this.#formSnapshot.set(JSON.stringify(this.$sections()));
                    this.#dotMessageDisplayService.push({
                        life: 3000,
                        message: 'Style Editor schema saved successfully',
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
        if (!raw) return;
        try {
            const schema = JSON.parse(raw as string) as StyleEditorFormSchema;
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
        return {
            uid: crypto.randomUUID(),
            type: field.type,
            label: field.label,
            identifier: field.id,
            inputType: (field.config?.inputType as 'text' | 'number') ?? 'text',
            placeholder: (field.config?.placeholder as string) ?? '',
            columns: (field.config?.columns as 1 | 2) ?? 1,
            options: (
                (field.config?.options as Array<{
                    label: string;
                    value?: string;
                    key?: string;
                    imageURL?: string;
                }>) ?? []
            ).map((o) =>
                field.type === 'checkboxGroup'
                    ? { label: o.label, key: o.key ?? '' }
                    : { label: o.label, value: o.value ?? '', ...(o.imageURL ? { imageURL: o.imageURL } : {}) }
            )
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

    /**
     * Appends a new empty section at the end of the sections list.
     */
    addSection(): void {
        patchState(this.#state, ({ sections }) => ({ sections: [...sections, createSection()] }));
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
