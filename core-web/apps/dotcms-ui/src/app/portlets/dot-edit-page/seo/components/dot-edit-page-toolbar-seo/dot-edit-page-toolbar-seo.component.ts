import { Observable, Subject } from 'rxjs';

import { CommonModule } from '@angular/common';
import {
    Component,
    EventEmitter,
    Input,
    OnChanges,
    OnDestroy,
    OnInit,
    Output
} from '@angular/core';
import { FormsModule } from '@angular/forms';
import { RouterLink } from '@angular/router';

import { ButtonModule } from 'primeng/button';
import { CheckboxModule } from 'primeng/checkbox';
import { DialogService } from 'primeng/dynamicdialog';
import { TagModule } from 'primeng/tag';
import { ToolbarModule } from 'primeng/toolbar';
import { TooltipModule } from 'primeng/tooltip';

import { DotSecondaryToolbarModule } from '@components/dot-secondary-toolbar';
import { DotGlobalMessageModule } from '@dotcms/app/view/components/_common/dot-global-message/dot-global-message.module';
import { DotLicenseService, DotPropertiesService } from '@dotcms/data-access';
import {
    DotCMSContentlet,
    DotExperiment,
    DotPageMode,
    DotPageRenderState,
    DotVariantData,
    FeaturedFlags,
    RUNNING_UNTIL_DATE_FORMAT
} from '@dotcms/dotcms-models';
import {
    DotFavoritePageComponent,
    DotDeviceSelectorSeoComponent
} from '@dotcms/portlets/dot-ema/ui';
import { DotIconModule, DotMessagePipe, DotSafeHtmlPipe } from '@dotcms/ui';
import { DotEditPageWorkflowsActionsModule } from '@portlets/dot-edit-page/content/components/dot-edit-page-workflows-actions/dot-edit-page-workflows-actions.module';
import { DotEditPageNavDirective } from '@portlets/dot-edit-page/main/dot-edit-page-nav/directives/dot-edit-page-nav.directive';

import { DotEditPageInfoSeoComponent } from '../dot-edit-page-info-seo/dot-edit-page-info-seo.component';
import { DotEditPageStateControllerSeoComponent } from '../dot-edit-page-state-controller-seo/dot-edit-page-state-controller-seo.component';
import { DotEditPageViewAsControllerSeoComponent } from '../dot-edit-page-view-as-controller-seo/dot-edit-page-view-as-controller-seo.component';

@Component({
    standalone: true,
    selector: 'dot-edit-page-toolbar-seo',
    templateUrl: './dot-edit-page-toolbar-seo.component.html',
    styleUrls: ['./dot-edit-page-toolbar-seo.component.scss'],
    providers: [DialogService, DotPropertiesService],
    imports: [
        ButtonModule,
        CommonModule,
        CheckboxModule,
        DotEditPageWorkflowsActionsModule,
        DotEditPageViewAsControllerSeoComponent,
        DotSecondaryToolbarModule,
        FormsModule,
        ToolbarModule,
        TooltipModule,
        DotSafeHtmlPipe,
        DotGlobalMessageModule,
        DotFavoritePageComponent,
        DotIconModule,
        DotEditPageNavDirective,
        RouterLink,
        TagModule,
        DotEditPageInfoSeoComponent,
        DotDeviceSelectorSeoComponent,
        DotEditPageStateControllerSeoComponent,
        DotMessagePipe
    ]
})
export class DotEditPageToolbarSeoComponent implements OnInit, OnChanges, OnDestroy {
    @Input() pageState: DotPageRenderState;
    @Input() variant: DotVariantData | null = null;
    @Input() runningExperiment: DotExperiment | null = null;
    @Output() cancel = new EventEmitter<boolean>();
    @Output() actionFired = new EventEmitter<DotCMSContentlet>();
    @Output() favoritePage = new EventEmitter<boolean>();
    @Output() whatschange = new EventEmitter<boolean>();
    @Output() backToExperiment = new EventEmitter<boolean>();
    isEnterpriseLicense$: Observable<boolean>;
    showWhatsChanged: boolean;
    apiLink: string;
    pageRenderedHtml: string;
    // TODO: Remove next line when total functionality of Favorite page is done for release
    showFavoritePageStar = false;
    runningUntilDateFormat = RUNNING_UNTIL_DATE_FORMAT;

    private destroy$: Subject<boolean> = new Subject<boolean>();

    constructor(
        private dotLicenseService: DotLicenseService,
        private dotConfigurationService: DotPropertiesService
    ) {}

    ngOnInit() {
        // TODO: Remove next line when total functionality of Favorite page is done for release
        this.dotConfigurationService
            .getFeatureFlag(FeaturedFlags.DOTFAVORITEPAGE_FEATURE_ENABLE)
            .subscribe((enabled) => {
                this.showFavoritePageStar = enabled;
            });

        this.isEnterpriseLicense$ = this.dotLicenseService.isEnterprise();
        this.apiLink = this.getApiLink();
    }

    ngOnChanges(): void {
        this.pageRenderedHtml = this.updateRenderedHtml();
        this.apiLink = this.getApiLink();
        this.showWhatsChanged =
            this.pageState.state.mode === DotPageMode.PREVIEW &&
            !('persona' in this.pageState.viewAs) &&
            !this.variant;
    }

    ngOnDestroy(): void {
        this.destroy$.next(true);
        this.destroy$.complete();
    }

    /**
     * Hide what's change when state change
     *
     * @memberof DotEditPageToolbarComponent
     */
    stateChange(): void {
        if (this.showWhatsChanged) {
            this.showWhatsChanged = false;
            this.whatschange.emit(this.showWhatsChanged);
        }
    }

    private updateRenderedHtml(): string {
        return this.pageState?.params.viewAs.mode === DotPageMode.PREVIEW
            ? this.pageState.params.page.rendered
            : this.pageRenderedHtml;
    }

    private getApiLink(): string {
        return `api/v1/page/render${this.pageState.page.pageURI}?language_id=${this.pageState.page.languageId}`;
    }
}
