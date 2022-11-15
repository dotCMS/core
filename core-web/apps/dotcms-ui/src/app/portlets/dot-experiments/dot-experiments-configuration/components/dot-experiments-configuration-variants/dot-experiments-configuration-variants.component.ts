import { ChangeDetectionStrategy, Component, OnDestroy, ViewChild } from '@angular/core';
import { CommonModule } from '@angular/common';
import { CardModule } from 'primeng/card';
import { DotMessagePipeModule } from '@pipes/dot-message/dot-message-pipe.module';
import { ButtonModule } from 'primeng/button';
import {
    DEFAULT_VARIANT_ID,
    MAX_VARIANTS_ALLOWED
} from '@portlets/dot-experiments/shared/models/dot-experiments-constants';
import { DotExperimentsConfigurationItemsCountComponent } from '@portlets/dot-experiments/dot-experiments-configuration/components/dot-experiments-configuration-items-count/dot-experiments-configuration-items-count.component';
import { DotIconModule } from '@dotcms/ui';
import { DotDynamicDirective } from '@portlets/shared/directives/dot-dynamic.directive';
import { DotExperimentsConfigurationVariantsAddComponent } from '@portlets/dot-experiments/dot-experiments-configuration/components/dot-experiments-configuration-variants-add/dot-experiments-configuration-variants-add.component';
import { DotExperimentsConfigurationStore } from '@portlets/dot-experiments/dot-experiments-configuration/store/dot-experiments-configuration-store.service';
import { Subject } from 'rxjs';
import { delay, takeUntil } from 'rxjs/operators';
import { Variant } from '@portlets/dot-experiments/shared/models/dot-experiments.model';
import { DotExperimentsSessionStorageService } from '@portlets/dot-experiments/shared/services/dot-experiments-session-storage.service';
import { Router } from '@angular/router';
import { UiDotIconButtonModule } from '@components/_common/dot-icon-button/dot-icon-button.module';
import { UiDotIconButtonTooltipModule } from '@components/_common/dot-icon-button-tooltip/dot-icon-button-tooltip.module';

/**
 * Container Component to handle al related to add/delete variants
 */
@Component({
    selector: 'dot-experiments-configuration-variants',
    standalone: true,
    imports: [
        CommonModule,
        DotExperimentsConfigurationItemsCountComponent,
        DotMessagePipeModule,
        DotIconModule,
        DotDynamicDirective,
        UiDotIconButtonModule,
        //PrimeNg
        CardModule,
        ButtonModule,
        UiDotIconButtonTooltipModule
    ],
    templateUrl: './dot-experiments-configuration-variants.component.html',
    styleUrls: ['./dot-experiments-configuration-variants.component.scss'],
    changeDetection: ChangeDetectionStrategy.OnPush
})
export class DotExperimentsConfigurationVariantsComponent implements OnDestroy {
    @ViewChild(DotDynamicDirective, { static: true })
    dotDynamicHost!: DotDynamicDirective;

    vm$ = this.dotExperimentsConfigurationStore.variantsVm$;

    maxVariantsAllowed = MAX_VARIANTS_ALLOWED;
    defaultVariantId = DEFAULT_VARIANT_ID;

    private destroy$: Subject<boolean> = new Subject<boolean>();

    constructor(
        private readonly dotExperimentsConfigurationStore: DotExperimentsConfigurationStore,
        private readonly dotExperimentsSessionStorageService: DotExperimentsSessionStorageService,
        private readonly router: Router
    ) {}

    ngOnDestroy(): void {
        this.destroy$.next(true);
        this.destroy$.complete();
    }

    /**
     * Load dynamically the sidebar and form
     * to add a new variant
     * @returns void
     * @memberof DotExperimentsConfigurationVariantsComponent
     */
    addNewVariant() {
        const viewContainerRef = this.dotDynamicHost.viewContainerRef;
        const componentRef =
            viewContainerRef.createComponent<DotExperimentsConfigurationVariantsAddComponent>(
                DotExperimentsConfigurationVariantsAddComponent
            );

        componentRef.instance.closedSidebar
            .pipe(takeUntil(this.destroy$), delay(500))
            .subscribe(() => {
                viewContainerRef.clear();
            });
    }

    /**
     * Call the sidebar of change Traffic Proportion
     * @returns void
     * @memberof DotExperimentsConfigurationVariantsComponent
     */
    changeTrafficProportionType() {
        // to  be implemented
    }

    /**
     * Go to Edit Page / Content, set the VariantId to SessionStorage
     * @returns void
     * @memberof DotExperimentsConfigurationVariantsComponent
     */
    viewVariant(variant: Variant) {
        this.dotExperimentsSessionStorageService.setVariationId(variant.id);
        this.router.navigate(['edit-page/content'], { queryParamsHandling: 'preserve' });
    }

    /**
     * Delete a specific variant
     * @param {Variant} variant
     * @returns void
     * @memberof DotExperimentsConfigurationVariantsComponent
     */
    deleteVariant(variant: Variant) {
        this.dotExperimentsConfigurationStore.deleteVariant(variant);
    }
}
