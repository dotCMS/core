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

import { take } from 'rxjs/operators';

import { DotSecondaryToolbarModule } from '@components/dot-secondary-toolbar';
import { DotGlobalMessageModule } from '@dotcms/app/view/components/_common/dot-global-message/dot-global-message.module';
import { DotLicenseService, DotPropertiesService } from '@dotcms/data-access';
import {
    DotCMSContentlet,
    DotExperiment,
    DotPageMode,
    DotPageRenderState,
    DotVariantData,
    FeaturedFlags
} from '@dotcms/dotcms-models';
import { DotIconModule, DotMessagePipe, UiDotIconButtonModule } from '@dotcms/ui';
import { DotPipesModule } from '@pipes/dot-pipes.module';
import { DotFavoritePageModule } from '@portlets/dot-edit-page/components/dot-favorite-page/dot-favorite-page.module';
import { DotEditPageStateControllerModule } from '@portlets/dot-edit-page/content/components/dot-edit-page-state-controller/dot-edit-page-state-controller.module';
import { DotEditPageViewAsControllerModule } from '@portlets/dot-edit-page/content/components/dot-edit-page-view-as-controller/dot-edit-page-view-as-controller.module';
import { DotEditPageWorkflowsActionsModule } from '@portlets/dot-edit-page/content/components/dot-edit-page-workflows-actions/dot-edit-page-workflows-actions.module';
import { DotEditPageNavDirective } from '@portlets/dot-edit-page/main/dot-edit-page-nav/directives/dot-edit-page-nav.directive';

import { DotDeviceSelectorSeoComponent } from '../dot-device-selector-seo/dot-device-selector-seo.component';
import { DotEditPageInfoSeoComponent } from '../dot-edit-page-info-seo/dot-edit-page-info-seo.component';
import { DotEditPageStateControllerSeoComponent } from '../dot-edit-page-state-controller-seo/dot-edit-page-state-controller-seo.component';

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
        DotEditPageViewAsControllerModule,
        DotEditPageStateControllerModule,
        DotSecondaryToolbarModule,
        FormsModule,
        ToolbarModule,
        TooltipModule,
        DotPipesModule,
        DotGlobalMessageModule,
        DotFavoritePageModule,
        UiDotIconButtonModule,
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

    private destroy$: Subject<boolean> = new Subject<boolean>();

    constructor(
        private dotLicenseService: DotLicenseService,
        private dotConfigurationService: DotPropertiesService
    ) {}

    ngOnInit() {
        // TODO: Remove next line when total functionality of Favorite page is done for release
        this.dotConfigurationService
            .getKey(FeaturedFlags.DOTFAVORITEPAGE_FEATURE_ENABLE)
            .pipe(take(1))
            .subscribe((enabled: string) => {
                this.showFavoritePageStar = enabled === 'true';
            });

        this.isEnterpriseLicense$ = this.dotLicenseService.isEnterprise();
        this.apiLink = `api/v1/page/render${this.pageState.page.pageURI}?language_id=${this.pageState.page.languageId}`;
    }

    ngOnChanges(): void {
        this.pageRenderedHtml = this.updateRenderedHtml();

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
}
