import { AngularNodeViewComponent } from 'ngx-tiptap';

import { ChangeDetectionStrategy, Component, computed } from '@angular/core';

import { CONTENTLET_CARD_HOST_CLASS, type ContentletData } from './contentlet.types';

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
            <span
                class="mb-2 inline-flex max-w-full items-center rounded-full bg-indigo-100 px-2.5 py-0.5 text-xs font-medium text-indigo-800 dark:bg-indigo-900/50 dark:text-indigo-200">
                {{ d.contentType || 'Content' }}
            </span>
            <p class="text-base font-semibold text-gray-900 dark:text-gray-100">{{ displayTitle() }}</p>
            <p class="mt-1 font-mono text-xs text-gray-500 dark:text-gray-400">{{ d.identifier ?? '' }}</p>
            @if (d.modDate) {
                <p class="mt-2 text-xs text-gray-400 dark:text-gray-500">Updated {{ d.modDate }}</p>
            }
        } @else {
            <p class="text-sm text-gray-500 dark:text-gray-400">Contentlet</p>
        }
    `
})
export class DotContentletNodeViewComponent extends AngularNodeViewComponent {
    protected readonly data = computed(() => this.node().attrs['data'] as ContentletData | null);

    protected readonly displayTitle = computed(() => {
        const d = this.data();
        return d?.title || d?.identifier || 'Contentlet';
    });

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
