import {
    Component,
    OnInit,
    Input,
    EventEmitter,
    Output,
    OnChanges,
    OnDestroy
} from '@angular/core';
import { Observable, Subject } from 'rxjs';
import { DotLicenseService } from '@services/dot-license/dot-license.service';
import { DotPageRenderState } from '@portlets/dot-edit-page/shared/models';
import { DotPageMode } from '@models/dot-page/dot-page-mode.enum';
import { DotCMSContentlet } from '@dotcms/dotcms-models';
import { DotPropertiesService } from '@dotcms/app/api/services/dot-properties/dot-properties.service';
import { take } from 'rxjs/operators';
import { FeaturedFlags } from '@dotcms/app/portlets/shared/models/shared-models';
@Component({
    selector: 'dot-edit-page-toolbar',
    templateUrl: './dot-edit-page-toolbar.component.html',
    styleUrls: ['./dot-edit-page-toolbar.component.scss']
})
export class DotEditPageToolbarComponent implements OnInit, OnChanges, OnDestroy {
    @Input() pageState: DotPageRenderState;
    @Output() cancel = new EventEmitter<boolean>();
    @Output() actionFired = new EventEmitter<DotCMSContentlet>();
    @Output() favoritePage = new EventEmitter<boolean>();
    @Output() whatschange = new EventEmitter<boolean>();

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
            !('persona' in this.pageState.viewAs);
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
