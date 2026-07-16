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
            <tbody>
                @for (rowNode of content; track $index) {
                    <tr>
                        @for (cellNode of rowNode.content; track $index) {
                            <!--
                                Cell type — not row index — decides th vs td. Matches the
                                VTL renderer (storyblock/render.vtl) so headless markup
                                stays consistent with server-rendered pages.
                            -->
                            @if (cellNode.type === 'tableHeader') {
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
                            } @else {
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
