import { Component, computed, inject, signal } from '@angular/core';
import { FormsModule } from '@angular/forms';

import { ButtonModule } from 'primeng/button';
import { DynamicDialogConfig, DynamicDialogRef } from 'primeng/dynamicdialog';
import { InputTextModule } from 'primeng/inputtext';
import { SelectButtonModule } from 'primeng/selectbutton';
import { TextareaModule } from 'primeng/textarea';

import { DotMessagePipe } from '@dotcms/ui';

export type DotAiIndexCreateMode = 'add' | 'delete';

/**
 * The server stores whatever it is given — `bad name with spaces` is accepted verbatim, and
 * `Blog` and `blog` become two separate indexes. Neither is useful, so the name is constrained
 * here rather than left to produce an index nobody can find again.
 */
const INDEX_NAME_PATTERN = /^[a-zA-Z0-9_-]+$/;

export interface DotAiIndexCreateResult {
    mode: DotAiIndexCreateMode;
    indexName: string;
    query: string;
    fields?: string;
    velocityTemplate?: string;
}

/**
 * One dialog, two modes.
 *
 * Add mode embeds the content the query matches; delete mode removes it from the index. The
 * submit label flips with the toggle so the destructive mode never hides behind a neutral
 * word (FR-030) — the legacy screen did the same remap.
 */
@Component({
    selector: 'dot-ai-index-create',
    imports: [
        FormsModule,
        ButtonModule,
        InputTextModule,
        TextareaModule,
        SelectButtonModule,
        DotMessagePipe
    ],
    templateUrl: './dot-ai-index-create.component.html'
})
export class DotAiIndexCreateComponent {
    readonly #dialogRef = inject(DynamicDialogRef);
    readonly #config = inject(DynamicDialogConfig<{ indexes: string[] }>);

    protected readonly existingIndexes = this.#config.data?.indexes ?? [];

    protected readonly $mode = signal<DotAiIndexCreateMode>('add');
    protected readonly $indexName = signal('');
    protected readonly $query = signal('');
    protected readonly $fields = signal('');
    protected readonly $velocityTemplate = signal('');

    protected readonly modes = [
        { label: 'dotai.index.create.mode.add', value: 'add' as const },
        { label: 'dotai.index.create.mode.delete', value: 'delete' as const }
    ];

    protected readonly $submitLabel = computed(() =>
        this.$mode() === 'delete'
            ? 'dotai.index.create.submit.delete'
            : 'dotai.index.create.submit.add'
    );

    /** Empty until the field has been touched, so the form does not scold you on open. */
    protected readonly $nameError = computed<string | null>(() => {
        const name = this.$indexName().trim();

        if (!name) {
            return null;
        }

        if (!INDEX_NAME_PATTERN.test(name)) {
            return 'dotai.index.create.name.invalid';
        }

        // Only for a build: deleting names an index that must already exist.
        if (this.$mode() === 'add' && this.existingIndexes.includes(name)) {
            return 'dotai.index.create.name.exists';
        }

        return null;
    });

    protected readonly $canSubmit = computed(
        () => !!this.$indexName().trim() && !!this.$query().trim() && !this.$nameError()
    );

    protected submit(): void {
        if (!this.$canSubmit()) {
            return;
        }

        const result: DotAiIndexCreateResult = {
            mode: this.$mode(),
            indexName: this.$indexName().trim(),
            query: this.$query().trim()
        };

        // Only meaningful when embedding; a delete is driven purely by the query.
        if (this.$mode() === 'add') {
            if (this.$fields().trim()) {
                result.fields = this.$fields().trim();
            }

            if (this.$velocityTemplate().trim()) {
                result.velocityTemplate = this.$velocityTemplate().trim();
            }
        }

        this.#dialogRef.close(result);
    }

    protected cancel(): void {
        this.#dialogRef.close();
    }
}
