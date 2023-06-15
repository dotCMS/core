import { Observable } from 'rxjs';

import { AsyncPipe, NgIf, PercentPipe, TitleCasePipe } from '@angular/common';
import { ChangeDetectionStrategy, Component, inject } from '@angular/core';
import { FormsModule } from '@angular/forms';

import { ButtonModule } from 'primeng/button';
import { ConfirmDialogModule } from 'primeng/confirmdialog';
import { TableModule } from 'primeng/table';
import { TagModule } from 'primeng/tag';
import { TooltipModule } from 'primeng/tooltip';

import { tap } from 'rxjs/operators';

import { DotDialogActions } from '@components/dot-dialog/dot-dialog.component';
import { DotDialogModule } from '@components/dot-dialog/dot-dialog.module';
import { DotMessageService } from '@dotcms/data-access';
import { DEFAULT_VARIANT_ID, Variant } from '@dotcms/dotcms-models';
import { DotMessagePipeModule } from '@dotcms/ui';

import {
    DotExperimentsReportsStore,
    VmPromoteVariant
} from '../../store/dot-experiments-reports-store';

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
        TitleCasePipe,
        AsyncPipe,
        PercentPipe,
        TooltipModule
    ],
    standalone: true
})
export class DotExperimentsPublishVariantComponent {
    readonly dotMessageService: DotMessageService = inject(DotMessageService);
    readonly store: DotExperimentsReportsStore = inject(DotExperimentsReportsStore);

    readonly vm$: Observable<VmPromoteVariant> = this.store.promotedDialogVm$.pipe(
        tap(({ experimentId }) => {
            this.configDialogActions(experimentId);
        })
    );

    selectedVariant: Variant;

    dialogActions: DotDialogActions = {};
    protected readonly DEFAULT_VARIANT_ID = DEFAULT_VARIANT_ID;

    enableButton() {
        this.dialogActions.accept.disabled = this.selectedVariant === null;
    }

    closeDialog() {
        this.store.hidePromoteDialog();
    }

    private configDialogActions(experimentId: string): void {
        this.dialogActions = {
            accept: {
                action: () => {
                    if (this.selectedVariant) {
                        this.store.promoteVariant({
                            experimentId,
                            variant: this.selectedVariant
                        });
                    }
                },
                label: this.dotMessageService.get('experiments.report.promote.variant'),
                disabled: true
            },
            cancel: {
                action: () => {
                    this.store.hidePromoteDialog();
                },
                label: this.dotMessageService.get('cancel')
            }
        };
    }
}
