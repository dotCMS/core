import { AngularNodeViewComponent } from 'ngx-tiptap';

import { ChangeDetectionStrategy, Component, computed, inject } from '@angular/core';

import { DotMessageService } from '@dotcms/data-access';

import { CONTENTLET_CARD_HOST_CLASS, type ContentletData } from './contentlet.types';

import { EditorStore } from '../../../store/editor.store';

@Component({
    selector: 'dot-contentlet-node-view',
    standalone: true,
    changeDetection: ChangeDetectionStrategy.OnPush,
    host: {
        class: CONTENTLET_CARD_HOST_CLASS,
        '[attr.data-type]': "'dot-content'",
        '[class.is-selected]': 'selected()',
        '[attr.data-identifier]': 'identifierAttr()',
        '[attr.data-language-id]': 'languageIdAttr()',
        '[attr.data-inode]': 'inodeAttr()',
        '[attr.data-content-type]': 'contentTypeAttr()'
    },
    template: `
        @if (data(); as d) {
            <div class="mb-2 flex flex-wrap items-center gap-2">
                <span
                    class="inline-flex max-w-full items-center rounded-full bg-indigo-100 px-2.5 py-0.5 text-xs font-medium text-indigo-800 dark:bg-indigo-900/50 dark:text-indigo-200">
                    {{ d.contentType || fallbackTypeLabel }}
                </span>
                @if (editorStore.languageIso(); as iso) {
                    <span
                        class="inline-flex max-w-full items-center rounded-full border border-green-200 bg-green-100 px-2.5 py-0.5 font-mono text-xs font-medium text-teal-900 dark:border-teal-700 dark:bg-teal-900/60 dark:text-teal-100"
                        [attr.title]="languageTitle()">
                        {{ iso }}
                    </span>
                }
            </div>
            <p class="text-base font-semibold text-gray-900 dark:text-gray-100">
                {{ displayTitle() }}
            </p>
            <p class="mt-1 font-mono text-xs text-gray-500 dark:text-gray-400">
                {{ d.identifier ?? '' }}
            </p>
            @if (d.modDate) {
                <p class="mt-2 text-xs text-gray-400 dark:text-gray-500">{{ updatedLabel() }}</p>
            }
        } @else {
            <p class="text-sm text-gray-500 dark:text-gray-400">{{ fallbackTitleLabel }}</p>
        }
    `
})
export class DotContentletNodeViewComponent extends AngularNodeViewComponent {
    /** Editor UI language (ISO); distinct from the contentlet's own `languageId` in attrs. */
    protected readonly editorStore = inject(EditorStore);
    private readonly dotMessageService = inject(DotMessageService);

    /** Resolved at construction so the template can use a static literal. */
    protected readonly fallbackTypeLabel = this.dotMessageService.get(
        'dot.block.editor.contentlet.fallback-type'
    );
    protected readonly fallbackTitleLabel = this.dotMessageService.get(
        'dot.block.editor.contentlet.fallback-title'
    );

    protected readonly data = computed(() => this.node().attrs['data'] as ContentletData | null);

    protected readonly displayTitle = computed(() => {
        const d = this.data();
        return d?.title || d?.identifier || this.fallbackTitleLabel;
    });

    protected readonly languageTitle = computed(() =>
        this.dotMessageService.get(
            'dot.block.editor.contentlet.editor-language',
            String(this.editorStore.languageId())
        )
    );

    protected readonly updatedLabel = computed(() =>
        this.dotMessageService.get(
            'dot.block.editor.contentlet.updated',
            String(this.data()?.modDate ?? '')
        )
    );

    protected readonly identifierAttr = computed(() => {
        const id = this.data()?.identifier;
        return id ? String(id) : null;
    });

    protected readonly languageIdAttr = computed(() => {
        const id = this.data()?.languageId;
        return id != null ? String(id) : null;
    });

    protected readonly inodeAttr = computed(() => {
        const inode = this.data()?.inode;
        return inode ? String(inode) : null;
    });

    protected readonly contentTypeAttr = computed(() => {
        const ct = this.data()?.contentType;
        return ct ? String(ct) : null;
    });
}
