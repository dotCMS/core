import { Observable, Subject, merge } from 'rxjs';

import { pluck, takeUntil, tap } from 'rxjs/operators';
import { Component, OnInit, OnDestroy } from '@angular/core';
import { ActivatedRoute } from '@angular/router';
import { DotPageRenderState } from '../../shared/models/dot-rendered-page-state.model';
import { DotPageStateService } from '../../content/services/dot-page-state/dot-page-state.service';
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
    private languageId: string;
    private pageIsSaved: boolean = false;
    private destroy$: Subject<boolean> = new Subject<boolean>();
    private readonly customEventsHandler;

    constructor(
        private route: ActivatedRoute,
        private dotContentletEditorService: DotContentletEditorService,
        private dotPageStateService: DotPageStateService,
        private dotRouterService: DotRouterService,
        private dotCustomEventHandlerService: DotCustomEventHandlerService
    ) {
        if (!this.customEventsHandler) {
            this.customEventsHandler = {
                'save-page': ({ detail: { payload } }: CustomEvent) => {
                    this.pageUrl = payload.htmlPageReferer.split('?')[0];
                    this.pageIsSaved = true;
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
        ).pipe(
            takeUntil(this.destroy$),
            tap(({ page }: DotPageRenderState) => {
                this.pageUrl = page.pageURI;
                this.languageId = page.languageId.toString();
            })
        );

        this.subscribeIframeCloseAction();
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
            if (this.pageIsSaved) {
                if (this.pageUrl !== this.route.snapshot.queryParams.url) {
                    this.dotRouterService.goToEditPage({
                        url: this.pageUrl,
                        language_id: this.languageId
                    });
                } else {
                    this.dotPageStateService.get({
                        url: this.pageUrl,
                        viewAs: {
                            language: parseInt(this.languageId, 10)
                        }
                    });
                }
            }
        });
    }
}
