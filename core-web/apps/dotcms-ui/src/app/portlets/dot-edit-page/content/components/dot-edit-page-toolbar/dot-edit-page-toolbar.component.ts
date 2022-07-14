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

    private destroy$: Subject<boolean> = new Subject<boolean>();

    constructor(
        private dotLicenseService: DotLicenseService,
        private dialogService: DialogService,
        private dotMessageService: DotMessageService
    ) {}

    ngOnInit() {
        this.isEnterpriseLicense$ = this.dotLicenseService.isEnterprise();
        this.apiLink = `api/v1/page/render${this.pageState.page.pageURI}?language_id=${this.pageState.page.languageId}`;
    }

    ngOnChanges(): void {
        this.updateRenderedHtml();

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
                    order: 1,
                    pageState: this.pageState,
                    pageRenderedHtml: this.pageRenderedHtml || null
                }
                // onSave: (value: DotFavoritePage) => {
                // console.log('*** DotFavoritePageComponent Saved!', value);
                // }
            }
        });
    }

    private updateRenderedHtml(): void {
        this.pageRenderedHtml =
            this.pageState?.params.viewAs.mode === DotPageMode.PREVIEW
                ? this.pageState.params.page.rendered
                : this.pageRenderedHtml;
    }
}
