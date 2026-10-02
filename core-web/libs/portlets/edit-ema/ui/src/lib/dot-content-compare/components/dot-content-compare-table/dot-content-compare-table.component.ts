import { JsonPipe } from '@angular/common';
import {
    Component,
    EventEmitter,
    Input,
    Output,
    inject,
    input,
    ChangeDetectionStrategy
} from '@angular/core';
import { FormsModule } from '@angular/forms';

import { ButtonModule } from 'primeng/button';
import { SelectModule } from 'primeng/select';
import { SelectButtonModule } from 'primeng/selectbutton';
import { TableModule } from 'primeng/table';

import { DotMessageService } from '@dotcms/data-access';
import { DotCMSContentlet } from '@dotcms/dotcms-models';
import { DotMessagePipe, DotRelativeDatePipe, DotDiffPipe } from '@dotcms/ui';

import { DotContentCompareTableData } from '../../store/dot-content-compare.store';
import { DotContentCompareBlockEditorComponent } from '../dot-content-compare-block-editor/dot-content-compare-block-editor.component';
import { DotContentComparePreviewFieldComponent } from '../fields/dot-content-compare-preview-field/dot-content-compare-preview-field.component';

@Component({
    selector: 'dot-content-compare-table',
    templateUrl: './dot-content-compare-table.component.html',
    styleUrls: ['./dot-content-compare-table.component.scss'],
    changeDetection: ChangeDetectionStrategy.Eager,
    imports: [
        FormsModule,
        TableModule,
        SelectModule,
        SelectButtonModule,
        ButtonModule,
        DotMessagePipe,
        DotRelativeDatePipe,
        DotDiffPipe,
        DotContentComparePreviewFieldComponent,
        DotContentCompareBlockEditorComponent,
        JsonPipe
    ]
})
export class DotContentCompareTableComponent {
    private dotMessageService = inject(DotMessageService);

    // TODO: Skipped for migration because:
    //  This input is used in a control flow expression (e.g. `@if` or `*ngIf`)
    //  and migrating would break narrowing currently.
    @Input() data!: DotContentCompareTableData;
    // Defaults true, matching DotContentCompareStore's initial state and the `dotDiff`
    // pipe's own default; `false` would silently disable diffing for unbound consumers.
    // TODO: Skipped for migration because:
    //  Your application code writes to the input. This prevents migration.
    @Input() showDiff = true;
    $showActions = input<boolean>(true, { alias: 'showActions' });
    /**
     * When `true`, swap the column order so the PREVIOUS (compare) version
     * renders on the LEFT and the CURRENT (working) version on the RIGHT.
     * Default `false` preserves the legacy layout (current LEFT / previous RIGHT).
     */
    readonly $reverseColumns = input<boolean>(false, { alias: 'reverseColumns' });

    @Output() changeVersion = new EventEmitter<DotCMSContentlet>();
    @Output() changeDiff = new EventEmitter<boolean>();
    @Output() bringBack = new EventEmitter<string>();

    displayOptions = [
        { label: this.dotMessageService.get('diff'), value: true },
        { label: this.dotMessageService.get('plain'), value: false }
    ];
}
