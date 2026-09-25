import { NgComponentOutlet } from '@angular/common';
import { Component, Input, ChangeDetectionStrategy } from '@angular/core';

import { BlockEditorNode } from '@dotcms/types';

import { CustomRenderer } from '../dotcms-block-editor-renderer.component';
import { DotCMSBlockEditorItemComponent } from '../item/dotcms-block-editor-item.component';

@Component({
    selector: 'dotcms-block-editor-renderer-grid-block',
    imports: [NgComponentOutlet],
    // Default, not Eager: Eager only exists from Angular 21.2, and this SDK is built with and
    // supports Angular 21.0 (libs/sdk/angular/toolchain/README.md). Both mean check-always.
    changeDetection: ChangeDetectionStrategy.Default,
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
                    [style.grid-column]="'span ' + columnSpan($index)">
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

    /**
     * Grid span for the column at `index`. The template walks `node.content`, which can hold
     * more columns than the two in `columnSpans`, so any extra column falls back to 6.
     */
    columnSpan(index: number): number {
        const span: number | undefined = this.columnSpans[index];

        return span ?? 6;
    }
}
