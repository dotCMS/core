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
import { DialogService } from 'primeng/dynamicdialog';
import { DotFavoritePageComponent } from '../../../components/dot-favorite-page/dot-favorite-page.component';
import { DotMessageService } from '@dotcms/app/api/services/dot-message/dot-messages.service';
import { LoggerService } from '@dotcms/dotcms-js';
import { DotESContentService } from '@dotcms/app/api/services/dot-es-content/dot-es-content.service';
import { take } from 'rxjs/operators';
import { ESContent } from '@dotcms/app/shared/models/dot-es-content/dot-es-content.model';
import { generateDotFavoritePageUrl } from '@dotcms/app/shared/dot-utils';
@Component({
    selector: 'dot-edit-page-toolbar',
    templateUrl: './dot-edit-page-toolbar.component.html',
    styleUrls: ['./dot-edit-page-toolbar.component.scss']
})
export class DotEditPageToolbarComponent implements OnInit, OnChanges, OnDestroy {
    @Input() pageState: DotPageRenderState;
    @Output() cancel = new EventEmitter<boolean>();
    @Output() actionFired = new EventEmitter<DotCMSContentlet>();
    @Output() whatschange = new EventEmitter<boolean>();

    isEnterpriseLicense$: Observable<boolean>;
    showWhatsChanged: boolean;
    apiLink: string;
    pageRenderedHtml: string;
    dotFavoritePageIconName = 'star_outline';

    // TODO: Remove next line when total functionality of Favorite page is done for release
    showFavoritePageStar: boolean;

    private destroy$: Subject<boolean> = new Subject<boolean>();

    constructor(
        private dotLicenseService: DotLicenseService,
        private dialogService: DialogService,
        private dotMessageService: DotMessageService,
        private dotESContentService: DotESContentService,

        // TODO: Remove next line when total functionality of Favorite page is done for release
        private loggerService: LoggerService
    ) {}

    ngOnInit() {
        // TODO: Remove next line when total functionality of Favorite page is done for release
        this.showFavoritePageStar = this.loggerService.shouldShowLogs();

        this.isEnterpriseLicense$ = this.dotLicenseService.isEnterprise();
        this.apiLink = `api/v1/page/render${this.pageState.page.pageURI}?language_id=${this.pageState.page.languageId}`;

        this.loadFavoritePageData();
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

    /**
     * Instantiate dialog to Add Favorite Page
     *
     * @memberof DotEditPageToolbarComponent
     */
    addFavoritePage(): void {
        this.dialogService.open(DotFavoritePageComponent, {
            header: this.dotMessageService.get('favoritePage.dialog.header.add.page'),
            width: '40rem',
            data: {
                page: {
                    pageState: this.pageState,
                    pageRenderedHtml: this.pageRenderedHtml || null
                },
                onSave: () => {
                    this.setDotFavoritePageHighlighted();
                    this.loadFavoritePageData();
                }
            }
        });
    }

    /**
     * Calls ES endpoint to verify if DotFavoritePage contentlet exist on current page
     *
     * @memberof DotEditPageToolbarComponent
     */
    loadFavoritePageData(): void {
        const urlParam = generateDotFavoritePageUrl(this.pageState);

        this.dotESContentService
            .get({
                itemsPerPage: 10,
                offset: '0',
                query: `+contentType:FavoritePage +favoritePage.url_dotraw:${urlParam}`
            })
            .pipe(take(1))
            .subscribe((response: ESContent) => {
                if (response.resultsSize > 0) {
                    this.setDotFavoritePageHighlighted();
                }
            });
    }

    private updateRenderedHtml(): string {
        return this.pageState?.params.viewAs.mode === DotPageMode.PREVIEW
            ? this.pageState.params.page.rendered
            : this.pageRenderedHtml;
    }

    private setDotFavoritePageHighlighted(): void {
        this.dotFavoritePageIconName = 'grade';
    }
}
