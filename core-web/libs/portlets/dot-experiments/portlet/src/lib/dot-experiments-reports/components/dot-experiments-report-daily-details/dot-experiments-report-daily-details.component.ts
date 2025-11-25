import { NgClass, TitleCasePipe } from '@angular/common';
import { ChangeDetectionStrategy, Component, inject, Input } from '@angular/core';

import { ConfirmationService } from 'primeng/api';
import { ButtonModule } from 'primeng/button';
import { ConfirmPopupModule } from 'primeng/confirmpopup';
import { TagModule } from 'primeng/tag';

import { DotMessageService } from '@dotcms/data-access';
import { DEFAULT_VARIANT_ID, DotExperimentVariantDetail, Variant } from '@dotcms/dotcms-models';
import { DotMessagePipe } from '@dotcms/ui';

import { DotExperimentsDetailsTableComponent } from '../../../shared/ui/dot-experiments-details-table/dot-experiments-details-table.component';
import { DotExperimentsReportsStore } from '../../store/dot-experiments-reports-store';

@Component({
    selector: 'dot-experiments-report-daily-details',
    imports: [
        NgClass,
        TitleCasePipe,
        DotExperimentsDetailsTableComponent,
        DotMessagePipe,
        TagModule,
        ButtonModule,
        ConfirmPopupModule
    ],
    templateUrl: './dot-experiments-report-daily-details.component.html',
    styleUrls: ['./dot-experiments-report-daily-details.component.scss'],
    changeDetection: ChangeDetectionStrategy.OnPush
})
export class DotExperimentsReportDailyDetailsComponent {
    @Input()
    detailData: DotExperimentVariantDetail[] = [];
    @Input()
    hasEnoughSessions: boolean;
    @Input()
    experimentId: string;
    @Input()
    promotedVariantId: Variant;

    protected readonly defaultVariantId = DEFAULT_VARIANT_ID;

    private readonly dotMessageService = inject(DotMessageService);
    private readonly store = inject(DotExperimentsReportsStore);
    private readonly confirmationService = inject(ConfirmationService);

    /**
     * Promote Variant
     * @param {MouseEvent} $event
     * @param experimentId
     * @param variant
     * @returns void
     * @memberof DotExperimentsReportsComponent
     */
    promoteVariant($event: MouseEvent, experimentId: string, variant: DotExperimentVariantDetail) {
        this.confirmationService.confirm({
            target: $event.target,
            message: this.dotMessageService.get('experiment.reports.promote.warning'),
            icon: 'pi pi-info-circle',
            acceptLabel: this.dotMessageService.get('Yes'),
            rejectLabel: this.dotMessageService.get('No'),
            accept: () => {
                this.store.promoteVariant({ experimentId, variant });
            }
        });
    }
}
