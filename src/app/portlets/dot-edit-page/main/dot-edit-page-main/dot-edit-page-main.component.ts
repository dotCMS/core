import { Component, OnInit, OnDestroy } from '@angular/core';
import { ActivatedRoute } from '@angular/router';
import { DotRenderedPageState } from '../../shared/models/dot-rendered-page-state.model';
import { Observable } from 'rxjs/Observable';
import { Subject } from 'rxjs/Subject';
import { takeUntil } from 'rxjs/operators';
import { DotPageStateService } from '../../content/services/dot-page-state/dot-page-state.service';
import { DotMessageService } from '../../../../api/services/dot-messages-service';
import { DotContentletEditorService } from '../../../../view/components/dot-contentlet-editor/services/dot-contentlet-editor.service';
import { DotRouterService } from '../../../../api/services/dot-router/dot-router.service';

@Component({
    selector: 'dot-edit-page-main',
    templateUrl: './dot-edit-page-main.component.html',
    styleUrls: ['./dot-edit-page-main.component.scss']
})
export class DotEditPageMainComponent implements OnInit, OnDestroy {
    pageState: Observable<DotRenderedPageState>;
    private pageUrl: string;
    private destroy$: Subject<boolean> = new Subject<boolean>();

    constructor(
        private route: ActivatedRoute,
        private dotContentletEditorService: DotContentletEditorService,
        private dotPageStateService: DotPageStateService,
        private dotRouterService: DotRouterService,
        public dotMessageService: DotMessageService
    ) {}

    ngOnInit() {
        this.pageState = this.route.data.pluck('content');
        this.pageUrl = this.route.snapshot.queryParams.url;
        this.subscribeIframeCloseAction();
        this.dotMessageService.getMessages(['editpage.toolbar.nav.properties']);
    }

    ngOnDestroy(): void {
        this.destroy$.next(true);
        this.destroy$.complete();
    }

    /**
     * Call reload method to refresh page based on url
     *
     * @param {any} event
     * @memberof DotEditPageMainComponent
     */
    onCustomEvent(event: any): void {
        if (event.detail.name === 'save-page' && event.detail.payload) {
            this.pageUrl = event.detail.payload.htmlPageReferer.split('?')[0];
        }
    }

    private subscribeIframeCloseAction(): void {
        this.dotContentletEditorService.close$.pipe(takeUntil(this.destroy$)).subscribe(() => {
            if (this.pageUrl !== this.route.snapshot.queryParams.url) {
                this.dotRouterService.goToEditPage(this.pageUrl);
            } else {
                this.dotPageStateService.reload(this.route.snapshot.queryParams.url);
            }
        });

        this.dotPageStateService.reload$.pipe(takeUntil(this.destroy$)).subscribe((page: DotRenderedPageState) => {
            this.pageState = Observable.of(page);
        });
    }
}
