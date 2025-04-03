import { NgComponentOutlet } from '@angular/common';
import { Component, Input } from '@angular/core';

import { ContentNode } from '@dotcms/uve/internal';

import { DotCMSBlockEditorItemComponent } from '../item/dotcms-block-editor-item.component';
@Component({
    selector: 'dotcms-block-editor-renderer-table',
    standalone: true,
    imports: [NgComponentOutlet],
    template: `
        <table>
            <thead>
                @for (rowNode of content.slice(0, 1); track rowNode.type) {
                    <tr>
                        @for (cellNode of rowNode.content; track cellNode.type) {
                            <th
                                [attr.colspan]="cellNode.attrs?.['colspan'] || 1"
                                [attr.rowspan]="cellNode.attrs?.['rowspan'] || 1">
                                <ng-container
                                    *ngComponentOutlet="
                                        blockEditorItem;
                                        inputs: { content: cellNode.content }
                                    "></ng-container>
                            </th>
                        }
                    </tr>
                }
            </thead>
            <tbody>
                @for (rowNode of content.slice(1); track rowNode.type) {
                    <tr>
                        @for (cellNode of rowNode.content; track cellNode.type) {
                            <td
                                [attr.colspan]="cellNode.attrs?.['colspan'] || 1"
                                [attr.rowspan]="cellNode.attrs?.['rowspan'] || 1">
                                <ng-container
                                    *ngComponentOutlet="
                                        blockEditorItem;
                                        inputs: { content: cellNode.content }
                                    "></ng-container>
                            </td>
                        }
                    </tr>
                }
            </tbody>
        </table>
    `
})
export class DotTableBlock {
    @Input() content!: ContentNode[];
    blockEditorItem = DotCMSBlockEditorItemComponent;
}
