import { CommonModule, JsonPipe } from '@angular/common';
import {
    ChangeDetectionStrategy,
    Component,
    EventEmitter,
    OnInit,
    Output,
    ViewChild
} from '@angular/core';
import { FormsModule } from '@angular/forms';

import { ButtonModule } from 'primeng/button';
import { ConfirmDialogModule } from 'primeng/confirmdialog';
import { TableModule } from 'primeng/table';
import { TagModule } from 'primeng/tag';

import { DotDialogActions, DotDialogComponent } from '@components/dot-dialog/dot-dialog.component';
import { DotDialogModule } from '@components/dot-dialog/dot-dialog.module';
import { DotMessageService } from '@dotcms/data-access';
import { DotMessagePipeModule } from '@pipes/dot-message/dot-message-pipe.module';

@Component({
    selector: 'dot-dot-experiments-publish-variant',
    templateUrl: './dot-experiments-publish-variant.component.html',
    styleUrls: ['./dot-experiments-publish-variant.component.scss'],
    changeDetection: ChangeDetectionStrategy.OnPush,
    imports: [
        CommonModule,
        DotMessagePipeModule,
        JsonPipe,
        TableModule,
        TagModule,
        ButtonModule,
        ConfirmDialogModule,
        DotDialogModule,
        FormsModule
    ],
    standalone: true
})
export class DotExperimentsPublishVariantComponent implements OnInit {
    data;
    selectedVariant;
    dialogActions: DotDialogActions;

    @ViewChild('dialog', { static: true }) dialog: DotDialogComponent;

    @Output() hide = new EventEmitter();
    constructor(private dotMessageService: DotMessageService) {}

    ngOnInit(): void {
        this.data = {
            variants: [
                {
                    id: '1',
                    name: 'variant 1variant 1variant 1variant 1variant 1variant 1',
                    weight: '10'
                },
                { id: '2', name: 'variant 2', weight: '90', winner: true }
            ]
        };
        this.setDialogActions();
    }

    enableButton() {
        this.dialogActions.accept.disabled = this.selectedVariant === null;
    }

    selectVariant() {
        // console.log('test');
    }

    private setDialogActions(): void {
        this.dialogActions = {
            accept: {
                action: () => {
                    this.selectVariant();
                },
                label: this.dotMessageService.get('experiments.report.publish.assign.variant'),
                disabled: true
            },
            cancel: {
                action: () => {
                    this.hide.emit();
                },
                label: this.dotMessageService.get('cancel')
            }
        };
    }
}
