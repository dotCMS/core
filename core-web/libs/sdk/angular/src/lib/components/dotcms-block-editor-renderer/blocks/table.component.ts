import { NgComponentOutlet } from '@angular/common';
import { Component, Input } from '@angular/core';

import { BlockEditorNode } from '@dotcms/types';

import { DotCMSBlockEditorItemComponent } from '../item/dotcms-block-editor-item.component';
@Component({
    selector: 'dotcms-block-editor-renderer-table',
    imports: [NgComponentOutlet],
    template: `
        <table
            [attr.aria-label]="attrs?.['ariaLabel'] || null"
            [attr.aria-labelledby]="attrs?.['ariaLabelledby'] || null">
            @if (attrs?.['caption']) {
                <caption>{{ attrs?.['caption'] }}</caption>
            }
            <thead>
                @for (rowNode of content?.slice(0, 1); track rowNode.type) {
                    <tr>
                        @for (cellNode of rowNode.content; track cellNode.type) {
                            <th
                                [attr.colspan]="cellNode.attrs?.['colspan'] || 1"
                                [attr.rowspan]="cellNode.attrs?.['rowspan'] || 1"
                                [attr.scope]="cellNode.attrs?.['scope'] || null">
                                <ng-container
                                    *ngComponentOutlet="
                                        blockEditorItem;
                                        inputs: { content: cellNode.content }
                                    " />
                            </th>
                        }
                    </tr>
                }
            </thead>
            <tbody>
                @for (rowNode of content?.slice(1); track rowNode.type) {
                    <tr>
                        @for (cellNode of rowNode.content; track cellNode.type) {
                            <td
                                [attr.colspan]="cellNode.attrs?.['colspan'] || 1"
                                [attr.rowspan]="cellNode.attrs?.['rowspan'] || 1">
                                <ng-container
                                    *ngComponentOutlet="
                                        blockEditorItem;
                                        inputs: { content: cellNode.content }
                                    " />
                            </td>
                        }
                    </tr>
                }
            </tbody>
        </table>
    `
})
export class DotTableBlock {
    @Input() content: BlockEditorNode[] | undefined;
    /**
     * Table-node attributes (`caption`, `ariaLabel`, `ariaLabelledby`). Optional for
     * back-compat with older payloads that don't carry these.
     */
    @Input() attrs: BlockEditorNode['attrs'];
    blockEditorItem = DotCMSBlockEditorItemComponent;
}
