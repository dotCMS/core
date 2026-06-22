import { ChangeDetectionStrategy, Component, Input, computed } from '@angular/core';

import { BlockEditorNode } from '@dotcms/types';

/**
 * Default renderer for an inline contentlet reference (`dotInlineContent`).
 *
 * Renders the referenced contentlet's title inline. When a front-end URL is available on the
 * hydrated `attrs.data` (`urlMap` for URL-mapped content, or `url` for pages) it renders a link;
 * otherwise it falls back to a plain inline label. Consumers can fully override this output by
 * registering a custom renderer for the `dotInlineContent` node type — handled upstream by the
 * `customRenderers[node.type]` lookup in the renderer templates.
 */
@Component({
    selector: 'dotcms-block-editor-renderer-inline-content',
    changeDetection: ChangeDetectionStrategy.OnPush,
    template: `
        @if ($url(); as url) {
            <a class="dot-inline-content" [href]="url">{{ $title() }}</a>
        } @else {
            <span class="dot-inline-content">{{ $title() }}</span>
        }
    `
})
export class DotInlineContentBlock {
    @Input() node: BlockEditorNode | undefined;

    protected readonly $data = computed(() => this.node?.attrs?.['data']);

    protected readonly $title = computed(() => {
        const data = this.$data();
        return data?.title || data?.identifier || '';
    });

    protected readonly $url = computed(() => {
        const data = this.$data();
        return data?.urlMap ?? data?.url ?? null;
    });

    private readonly DOT_INLINE_CONTENT_NO_DATA_MESSAGE =
        '[DotCMSBlockEditorRenderer]: No data provided for an inline content reference (dotInlineContent). If the error persists, please contact the DotCMS support team.';

    ngOnInit() {
        if (!this.$data()) {
            console.error(this.DOT_INLINE_CONTENT_NO_DATA_MESSAGE);
        }
    }
}
