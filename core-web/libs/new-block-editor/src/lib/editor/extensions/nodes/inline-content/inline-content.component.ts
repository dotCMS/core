import { AngularNodeViewComponent } from 'ngx-tiptap';

import { ChangeDetectionStrategy, Component, computed, inject } from '@angular/core';

import { DotMessageService } from '@dotcms/data-access';

import { type ContentletData, INLINE_CONTENT_HOST_CLASS } from './inline-content.types';

/**
 * Inline node view for a contentlet reference. Renders a compact, baseline-aligned token
 * (content-type icon + title) within the surrounding paragraph text — NOT the block card.
 *
 * The content type name is surfaced via the host's native `title` attribute (a plain browser
 * tooltip). We deliberately do NOT use PrimeNG `pTooltip` here: this element lives inside
 * ProseMirror's `contenteditable` as a dynamically-rendered atom node view, where ProseMirror
 * owns pointer interaction (it marks the node `contenteditable="false"` and intercepts mouse
 * events for node selection), so the overlay's hover trigger is unreliable. The native tooltip
 * needs no overlay/zone/positioning machinery and works in the web-component embedding and in
 * fullscreen.
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
        <span class="dot-inline-content-token__icon material-symbols-outlined" aria-hidden="true">
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

    /** Native tooltip on the token: the referenced content type's name. */
    protected readonly hoverTitle = computed(() => this.data()?.contentType ?? null);

    protected readonly identifierAttr = computed(() => {
        const id = this.data()?.identifier;
        return id ? String(id) : null;
    });

    protected readonly languageIdAttr = computed(() => {
        const id = this.data()?.languageId;
        return id != null ? String(id) : null;
    });
}
