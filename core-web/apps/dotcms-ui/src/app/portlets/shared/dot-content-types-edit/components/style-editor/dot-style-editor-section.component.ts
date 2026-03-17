import { Component, effect, input, output, signal, untracked } from '@angular/core';
import { FormsModule } from '@angular/forms';

import { InputTextModule } from 'primeng/inputtext';

import { DotStyleEditorFieldFormComponent } from './dot-style-editor-field-form.component';
import { BuilderField, BuilderSection } from './models';

@Component({
    selector: 'dot-style-editor-section',
    imports: [FormsModule, InputTextModule, DotStyleEditorFieldFormComponent],
    templateUrl: './dot-style-editor-section.component.html'
})
export class DotStyleEditorSectionComponent {
    /** The section data passed from the builder. */
    readonly $section = input.required<BuilderSection>({ alias: 'section' });
    /** Whether this is the first section in the list (disables move-up). */
    readonly $isFirst = input<boolean>(false, { alias: 'isFirst' });
    /** Whether this is the last section in the list (disables move-down). */
    readonly $isLast = input<boolean>(false, { alias: 'isLast' });

    /** Emits when the user clicks move-up for this section. */
    readonly moveUp = output<void>();
    /** Emits when the user clicks move-down for this section. */
    readonly moveDown = output<void>();
    /** Emits when the user clicks delete for this section. */
    readonly delete = output<void>();
    /** Emits the new title string whenever the user edits the section title. */
    readonly titleChange = output<string>();
    /** Emits when the user clicks the "Add Field" button. */
    readonly addField = output<void>();
    /** Emits the UID of the field the user wants to remove. */
    readonly removeField = output<string>();
    /** Emits the UID of the field the user wants to move up. */
    readonly moveFieldUp = output<string>();
    /** Emits the UID of the field the user wants to move down. */
    readonly moveFieldDown = output<string>();
    /** Emits the updated `BuilderField` whenever any field form changes. */
    readonly fieldChange = output<BuilderField>();

    /** Local title signal to prevent cursor jumping on re-render. */
    readonly $title = signal('New Section');

    /** UID of the last rendered section — used to detect when a different section is shown. */
    #lastSectionUid = '';

    constructor() {
        effect(() => {
            const section = this.$section();
            if (section.uid !== this.#lastSectionUid) {
                this.#lastSectionUid = section.uid;
                untracked(() => {
                    this.$title.set(section.title);
                });
            }
        });
    }

    /**
     * Updates the local title signal and notifies the parent of the new value.
     *
     * @param value - The new section title entered by the user.
     */
    setTitle(value: string): void {
        this.$title.set(value);
        this.titleChange.emit(value);
    }
}
