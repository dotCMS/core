import { AngularNodeViewComponent } from 'ngx-tiptap';

import { ChangeDetectionStrategy, Component, computed, inject } from '@angular/core';

import { DotMessageService } from '@dotcms/data-access';

import { type ContentletData, INLINE_CONTENT_HOST_CLASS } from './inline-content.types';

/**
 * Inline node view for a contentlet reference. Renders a compact, baseline-aligned token
 * (content-type icon + title) within the surrounding paragraph text — NOT the block card.
 *
 * Editing context only: the token is not a link here (clicking selects the node). The live
 * `<a>` to the contentlet's front-end URL is produced server-side (VTL) and by the headless
 * SDK renderers. Broken references (missing `data` / deleted source) fall back to the last
 * known title as a non-interactive "missing" token instead of throwing.
 */
@Component({
    selector: 'dot-inline-content-node-view',
    standalone: true,
    changeDetection: ChangeDetectionStrategy.OnPush,
    host: {
        class: INLINE_CONTENT_HOST_CLASS,
        '[attr.data-type]': "'dot-inline-content'",
        '[class.is-selected]': 'selected()',
        '[class.is-missing]': '!data()',
        '[attr.data-identifier]': 'identifierAttr()',
        '[attr.data-language-id]': 'languageIdAttr()',
        '[attr.title]': 'hoverTitle()'
    },
    template: `
        <span class="dot-inline-content-token__icon material-symbols-rounded" aria-hidden="true">
            {{ icon() }}
        </span>
        <span class="dot-inline-content-token__label">{{ displayTitle() }}</span>
    `
})
export class DotInlineContentNodeViewComponent extends AngularNodeViewComponent {
    private readonly dotMessageService = inject(DotMessageService);

    /** Shown when the reference can no longer be resolved (deleted/unavailable source). */
    protected readonly fallbackTitleLabel = this.dotMessageService.get(
        'dot.block.editor.contentlet.fallback-title'
    );

    protected readonly data = computed(() => this.node().attrs['data'] as ContentletData | null);

    protected readonly displayTitle = computed(() => {
        const d = this.data();
        return d?.title || d?.identifier || this.fallbackTitleLabel;
    });

    /** Material Symbols ligature; `link_off` signals a broken/last-known reference. */
    protected readonly icon = computed(() => (this.data() ? 'article' : 'link_off'));

    protected readonly hoverTitle = computed(() => this.data()?.identifier ?? null);

    protected readonly identifierAttr = computed(() => {
        const id = this.data()?.identifier;
        return id ? String(id) : null;
    });

    protected readonly languageIdAttr = computed(() => {
        const id = this.data()?.languageId;
        return id != null ? String(id) : null;
    });
}
