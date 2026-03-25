import { NgComponentOutlet } from '@angular/common';
import { Component, Input } from '@angular/core';

import { BlockEditorNode } from '@dotcms/types';

import { CustomRenderer } from '../dotcms-block-editor-renderer.component';
import { DotCMSBlockEditorItemComponent } from '../item/dotcms-block-editor-item.component';

@Component({
    selector: 'dotcms-block-editor-renderer-grid-block',
    imports: [NgComponentOutlet],
    template: `
        <div
            data-type="gridBlock"
            class="grid-block"
            [style.display]="'grid'"
            [style.grid-template-columns]="'repeat(12, 1fr)'"
            [style.gap]="'1rem'">
            @for (column of node?.content; track $index) {
                <div
                    data-type="gridColumn"
                    class="grid-block__column"
                    [style.grid-column]="'span ' + (columnSpans[$index] ?? 6)">
                    <ng-container
                        *ngComponentOutlet="
                            blockEditorItem;
                            inputs: { content: column.content, customRenderers: customRenderers }
                        " />
                </div>
            }
        </div>
    `
})
export class DotGridBlock {
    @Input() node: BlockEditorNode | undefined;
    @Input() customRenderers: CustomRenderer | undefined;

    blockEditorItem = DotCMSBlockEditorItemComponent;

    get columnSpans(): number[] {
        const rawCols = Array.isArray(this.node?.attrs?.['columns'])
            ? this.node.attrs['columns']
            : [6, 6];

        return rawCols.length === 2 &&
            rawCols.every((v: unknown) => typeof v === 'number' && Number.isFinite(v))
            ? rawCols
            : [6, 6];
    }
}
