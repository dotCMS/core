import { Observable, Subject, merge } from 'rxjs';

import { take, pluck, takeUntil } from 'rxjs/operators';
import { Component, OnInit, OnDestroy } from '@angular/core';
import { ActivatedRoute } from '@angular/router';
import { DotPageRenderState } from '../../shared/models/dot-rendered-page-state.model';
import { DotPageStateService } from '../../content/services/dot-page-state/dot-page-state.service';
import { DotMessageService } from '@services/dot-messages-service';
import { DotContentletEditorService } from '@components/dot-contentlet-editor/services/dot-contentlet-editor.service';
import { DotRouterService } from '@services/dot-router/dot-router.service';
import { DotCustomEventHandlerService } from '@services/dot-custom-event-handler/dot-custom-event-handler.service';

@Component({
    selector: 'dot-edit-page-main',
    templateUrl: './dot-edit-page-main.component.html',
    styleUrls: ['./dot-edit-page-main.component.scss']
})
export class DotEditPageMainComponent implements OnInit, OnDestroy {
    pageState$: Observable<DotPageRenderState>;
    private pageUrl: string;
    private destroy$: Subject<boolean> = new Subject<boolean>();
    private readonly customEventsHandler;

    constructor(
        private route: ActivatedRoute,
        private dotContentletEditorService: DotContentletEditorService,
        private dotPageStateService: DotPageStateService,
        private dotRouterService: DotRouterService,
        private dotCustomEventHandlerService: DotCustomEventHandlerService,
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
        this.pageState$ = merge(
            this.route.data.pipe(pluck('content')),
            this.dotPageStateService.state$
        ).pipe(takeUntil(this.destroy$));

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
     * @param CustomEvent $event
     * @memberof DotEditPageMainComponent
     */
    onCustomEvent($event: CustomEvent): void {
        if (this.customEventsHandler[$event.detail.name]) {
            this.customEventsHandler[$event.detail.name]($event);
        }
        this.dotCustomEventHandlerService.handle($event);
    }

    private subscribeIframeCloseAction(): void {
        this.dotContentletEditorService.close$.pipe(takeUntil(this.destroy$)).subscribe(() => {
            this.pageState$.pipe(take(1)).subscribe((pageState: DotPageRenderState) => {
                if (this.pageUrl !== this.route.snapshot.queryParams.url) {
                    this.dotRouterService.goToEditPage({
                        url: this.pageUrl,
                        language_id: pageState.page.languageId.toString()
                    });
                } else {
                    this.dotPageStateService.get({
                        url: this.route.snapshot.queryParams.url,
                        viewAs: {
                            language: pageState.page.languageId
                        }
                    });
                }
            });
        });
    }
}
