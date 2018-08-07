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
    private readonly customEventsHandler;

    constructor(
        private route: ActivatedRoute,
        private dotContentletEditorService: DotContentletEditorService,
        private dotPageStateService: DotPageStateService,
        private dotRouterService: DotRouterService,
        public dotMessageService: DotMessageService
    ) {
        if (!this.customEventsHandler) {
            this.customEventsHandler = {
                'save-page': (e: CustomEvent) => {
                    if (e.detail.payload) {
                        this.pageUrl = e.detail.payload.htmlPageReferer.split('?')[0];
                    }
                },
                'deleted-page': () => {
                    this.dotRouterService.goToSiteBrowser();
                }
            };
        }
    }

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
     * Handle custom events from contentlet editor
     *
     * @param {CustomEvent} $event
     * @memberof DotEditPageMainComponent
     */
    onCustomEvent($event: CustomEvent): void {
        if (this.customEventsHandler[$event.detail.name]) {
            this.customEventsHandler[$event.detail.name]($event);
        }
    }

    private subscribeIframeCloseAction(): void {
        this.dotContentletEditorService.close$.pipe(takeUntil(this.destroy$)).subscribe(() => {
            this.pageState.take(1).subscribe((pageState: DotRenderedPageState) => {
                if (this.pageUrl !== this.route.snapshot.queryParams.url) {
                    this.dotRouterService.goToEditPage(this.pageUrl, pageState.page.languageId.toString());
                } else {
                    this.dotPageStateService.reload(this.route.snapshot.queryParams.url, pageState.page.languageId);
                }
            });
        });

        this.dotPageStateService.reload$.pipe(takeUntil(this.destroy$)).subscribe((page: DotRenderedPageState) => {
            this.pageState = Observable.of(page);
        });
    }
}
