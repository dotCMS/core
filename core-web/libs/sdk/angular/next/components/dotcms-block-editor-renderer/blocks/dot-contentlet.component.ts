import { AsyncPipe, NgComponentOutlet } from '@angular/common';
import { ChangeDetectionStrategy, Component, computed, Input } from '@angular/core';

import { BlockEditorNode, UVE_MODE } from '@dotcms/types';
import { getUVEState } from '@dotcms/uve';

import { DynamicComponentEntity } from '../../../models';
import { CustomRenderer } from '../dotcms-block-editor-renderer.component';

@Component({
    selector: 'dotcms-unknown-content-type',
    standalone: true,
    template: `
        <div
            data-testId="unknown-content-type"
            style="
                background-color: #fffaf0;
                color: #333;
                padding: 1rem;
                border-radius: 0.5rem;
                margin-bottom: 1rem;
                margin-top: 1rem;
                border: 1px solid #ed8936;
            ">
            <strong style="color: #c05621">Dev Warning</strong>
            : The content type
            <strong style="color: #c05621">{{ contentType || 'Unknown' }}</strong>
            is not recognized. Please ensure a custom renderer is provided for this content type.
            <br />
            Learn more about how to create a custom renderer in the
            <a
                href="https://dev.dotcms.com/docs/block-editor"
                target="_blank"
                rel="noopener noreferrer"
                style="color: #c05621">
                Block Editor Custom Renderers
            </a>
            .
        </div>
    `
})
export class UnknownContentTypeComponent {
    @Input() contentType: string | undefined;
}

/**
 * DotContent component that renders content based on content type
 */
@Component({
    selector: 'dotcms-block-editor-renderer-contentlet',
    standalone: true,
    imports: [NgComponentOutlet, AsyncPipe, UnknownContentTypeComponent],
    changeDetection: ChangeDetectionStrategy.OnPush,
    template: `
        @if (contentComponent) {
            <ng-container
                *ngComponentOutlet="
                    contentComponent | async;
                    inputs: { contentlet: $data() }
                "></ng-container>
        } @else if (isDevMode) {
            <dotcms-unknown-content-type [contentType]="$data()?.contentType" />
        }
    `
})
export class DotContentletBlock {
    @Input() customRenderers: CustomRenderer | undefined;
    @Input() attrs: BlockEditorNode['attrs'];

    contentComponent: DynamicComponentEntity | undefined;
    protected readonly $data = computed(() => this.attrs?.['data']);
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
