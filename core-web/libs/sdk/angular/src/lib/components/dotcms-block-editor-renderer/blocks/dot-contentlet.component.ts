import { AsyncPipe, NgComponentOutlet } from '@angular/common';
import { ChangeDetectionStrategy, Component, computed, Input } from '@angular/core';

import { BlockEditorNode, UVE_MODE } from '@dotcms/types';
import { getUVEState } from '@dotcms/uve';

import { DynamicComponentEntity } from '../../../models';
import { CustomRenderer } from '../dotcms-block-editor-renderer.component';

@Component({
    selector: 'dotcms-no-component-provided',
    template: `
        <div data-testid="no-component-provided" [style]="style">
            <strong style="color: #c05621">Dev Warning</strong>
            : No component or custom renderer provided for content type
            <strong style="color: #c05621">{{ contentType || 'Unknown' }}</strong>
            .
            <br />
            Please refer to the
            <a
                href="https://dev.dotcms.com/docs/block-editor"
                target="_blank"
                rel="noopener noreferrer"
                style="color: #c05621">
                Block Editor Custom Renderers Documentation
            </a>
            for guidance.
        </div>
    `
})
export class NoComponentProvided {
    @Input() contentType: string | undefined;
    protected readonly style = {
        backgroundColor: '#fffaf0',
        color: '#333',
        padding: '1rem',
        borderRadius: '0.5rem',
        marginBottom: '1rem',
        marginTop: '1rem',
        border: '1px solid #ed8936'
    };
}

/**
 * DotContent component that renders content based on content type
 */
@Component({
    selector: 'dotcms-block-editor-renderer-contentlet',
    imports: [NgComponentOutlet, AsyncPipe, NoComponentProvided],
    changeDetection: ChangeDetectionStrategy.OnPush,
    template: `
        @if (contentComponent) {
            <ng-container
                *ngComponentOutlet="
                    contentComponent | async;
                    inputs: { node: node }
                "></ng-container>
        } @else if (isDevMode) {
            <dotcms-no-component-provided [contentType]="$data()?.contentType" />
        }
    `
})
export class DotContentletBlock {
    @Input() customRenderers: CustomRenderer | undefined;
    @Input() node: BlockEditorNode | undefined;

    contentComponent: DynamicComponentEntity | undefined;
    protected readonly $data = computed(() => this.node?.attrs?.['data']);

    private readonly DOT_CONTENT_NO_DATA_MESSAGE =
        '[DotCMSBlockEditorRenderer]: No data provided for Contentlet Block. Try to add a contentlet to the block editor. If the error persists, please contact the DotCMS support team.';
    private readonly DOT_CONTENT_NO_MATCHING_COMPONENT_MESSAGE = (contentType: string) =>
        `[DotCMSBlockEditorRenderer]: No matching component found for content type: ${contentType}. Provide a custom renderer for this content type to fix this error.`;
    protected get isDevMode() {
        return getUVEState()?.mode === UVE_MODE.EDIT;
    }

    ngOnInit() {
        if (!this.$data()) {
            console.error(this.DOT_CONTENT_NO_DATA_MESSAGE);

            return;
        }

        const contentType = this.$data()?.contentType || '';
        this.contentComponent = this.customRenderers?.[contentType];

        if (!this.contentComponent) {
            console.warn(this.DOT_CONTENT_NO_MATCHING_COMPONENT_MESSAGE(contentType));
        }
    }
}
