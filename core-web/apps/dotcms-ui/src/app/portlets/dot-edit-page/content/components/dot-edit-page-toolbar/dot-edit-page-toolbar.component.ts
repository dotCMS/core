import { Observable, Subject } from 'rxjs';

import {
    Component,
    EventEmitter,
    Input,
    OnChanges,
    OnDestroy,
    OnInit,
    Output,
    inject
} from '@angular/core';

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

@Component({
    selector: 'dot-edit-page-toolbar',
    templateUrl: './dot-edit-page-toolbar.component.html',
    styleUrls: ['./dot-edit-page-toolbar.component.scss'],
    standalone: false
})
export class DotEditPageToolbarComponent implements OnInit, OnChanges, OnDestroy {
    private dotLicenseService = inject(DotLicenseService);
    private dotConfigurationService = inject(DotPropertiesService);

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

    ngOnInit() {
        // TODO: Remove next line when total functionality of Favorite page is done for release
        this.dotConfigurationService
            .getFeatureFlag(FeaturedFlags.DOTFAVORITEPAGE_FEATURE_ENABLE)
            .subscribe((enabled) => {
                this.showFavoritePageStar = enabled;
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
