import { NgIf, TitleCasePipe } from '@angular/common';
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
import { DEFAULT_VARIANT_ID, DotResultSimpleVariant } from '@dotcms/dotcms-models';
import { DotMessagePipeModule } from '@pipes/dot-message/dot-message-pipe.module';

@Component({
    selector: 'dot-experiments-publish-variant',
    templateUrl: './dot-experiments-publish-variant.component.html',
    styleUrls: ['./dot-experiments-publish-variant.component.scss'],
    changeDetection: ChangeDetectionStrategy.OnPush,
    imports: [
        DotMessagePipeModule,
        TableModule,
        TagModule,
        ButtonModule,
        ConfirmDialogModule,
        DotDialogModule,
        FormsModule,
        NgIf,
        TitleCasePipe
    ],
    standalone: true
})
export class DotExperimentsPublishVariantComponent implements OnInit {
    data: DotResultSimpleVariant[];
    selectedVariant;
    dialogActions: DotDialogActions;
    DEFAULT_VARIANT_ID = DEFAULT_VARIANT_ID;

    @ViewChild('dialog', { static: true }) dialog: DotDialogComponent;

    @Output() hide = new EventEmitter();
    @Output() publish = new EventEmitter<string>();
    constructor(private dotMessageService: DotMessageService) {}

    ngOnInit(): void {
        this.setDialogActions();
    }

    enableButton() {
        this.dialogActions.accept.disabled = this.selectedVariant === null;
    }

    private setDialogActions(): void {
        this.dialogActions = {
            accept: {
                action: () => {
                    this.publish.emit(this.selectedVariant);
                },
                label: this.dotMessageService.get('experiments.report.promote.assign.variant'),
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
