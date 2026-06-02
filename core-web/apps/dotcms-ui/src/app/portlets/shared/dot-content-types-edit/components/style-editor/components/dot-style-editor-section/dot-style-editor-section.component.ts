import {
    ChangeDetectionStrategy,
    Component,
    computed,
    effect,
    inject,
    input,
    output,
    signal,
    untracked
} from '@angular/core';
import { FormsModule } from '@angular/forms';

import { ButtonModule } from 'primeng/button';
import { InputTextModule } from 'primeng/inputtext';
import { PanelModule } from 'primeng/panel';
import { TooltipModule } from 'primeng/tooltip';

import { DotMessageService } from '@dotcms/data-access';
import { DotMessagePipe } from '@dotcms/ui';

import { DotStyleEditorFieldFormComponent } from '../dot-style-editor-field/dot-style-editor-field-form.component';
import { BuilderField, BuilderSection, fieldHasErrors } from '../../models';

@Component({
    selector: 'dot-style-editor-section',
    imports: [
        FormsModule,
        InputTextModule,
        PanelModule,
        ButtonModule,
        TooltipModule,
        DotMessagePipe,
        DotStyleEditorFieldFormComponent
    ],
    templateUrl: './dot-style-editor-section.component.html',
    changeDetection: ChangeDetectionStrategy.OnPush
})
export class DotStyleEditorSectionComponent {
    readonly #dotMessageService = inject(DotMessageService);
    readonly $section = input.required<BuilderSection>({ alias: 'section' });
    readonly $isFirst = input<boolean>(false, { alias: 'isFirst' });
    readonly $isLast = input<boolean>(false, { alias: 'isLast' });
    readonly $showErrors = input<boolean>(false, { alias: 'showErrors' });
    readonly $duplicateIdentifiers = input<Set<string>>(new Set(), {
        alias: 'duplicateIdentifiers'
    });

    readonly moveUp = output<void>();
    readonly moveDown = output<void>();
    readonly delete = output<void>();
    readonly titleChange = output<string>();
    readonly addField = output<void>();
    readonly removeField = output<string>();
    readonly moveFieldUp = output<string>();
    readonly moveFieldDown = output<string>();
    readonly fieldChange = output<BuilderField>();

    readonly $title = signal('New Section');

    readonly $isCollapsed = signal(false);

    readonly $addFieldLabel = computed(() =>
        this.#dotMessageService.get('style.editor.form.builder.section.add.field', this.$title())
    );

    readonly $emptyError = computed(() => {
        if (!this.$showErrors()) return null;
        if (this.$section().fields.length > 0) return null;

        return this.#dotMessageService.get('style.editor.form.builder.section.empty.error');
    });

    readonly $hasFieldErrors = computed(() => {
        if (!this.$showErrors()) return false;
        if (this.$section().fields.length === 0) return true;
        const duplicates = this.$duplicateIdentifiers();

        return this.$section().fields.some(
            (f) => fieldHasErrors(f) || duplicates.has(f.identifier.trim())
        );
    });

    readonly $panelPT = computed(() => ({
        root: {
            class: [
                'w-full',
                'rounded-[16px]',
                '!shadow-none',
                'overflow-hidden',
                this.$hasFieldErrors() ? '!border-red-300' : ''
            ]
        },
        header: {
            class: ['!min-h-[4.786rem]', '!transition-colors', '!bg-slate-50']
        },
        content: {
            class: ['!bg-white']
        },
        pcToggleButton: {
            root: { class: '!hidden' }
        }
    }));

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

    setTitle(value: string): void {
        this.$title.set(value);
        this.titleChange.emit(value);
    }
}
