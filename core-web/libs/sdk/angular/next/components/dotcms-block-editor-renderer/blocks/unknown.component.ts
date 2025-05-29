import { Component, Input } from '@angular/core';

import { BlockEditorNode, UVE_MODE } from '@dotcms/types';
import { getUVEState } from '@dotcms/uve';

@Component({
    selector: 'dotcms-block-editor-renderer-unknown',
    standalone: true,
    template: `
        @if (isEditMode) {
            <div [style]="style" data-testid="unknown-block-type">
                <strong style="color: #c53030">Warning:</strong>
                The block type
                <strong>{{ node.type }}</strong>
                is not recognized. Please check your
                <a
                    href="https://dev.dotcms.com/docs/block-editor"
                    target="_blank"
                    rel="noopener noreferrer">
                    configuration
                </a>
                or contact support for assistance.
            </div>
        }
    `
})
export class DotUnknownBlockComponent {
    @Input() node!: BlockEditorNode;

    get isEditMode() {
        return getUVEState()?.mode === UVE_MODE.EDIT;
    }

    protected readonly style = {
        backgroundColor: '#fff5f5',
        color: '#333',
        padding: '1rem',
        borderRadius: '0.5rem',
        marginBottom: '1rem',
        marginTop: '1rem',
        border: '1px solid #fc8181'
    };
}
