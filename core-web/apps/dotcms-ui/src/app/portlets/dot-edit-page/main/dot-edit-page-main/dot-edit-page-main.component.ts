import { merge, Observable, Subject } from 'rxjs';

import { Component, inject, OnDestroy, OnInit } from '@angular/core';
import { Title } from '@angular/platform-browser';
import { ActivatedRoute } from '@angular/router';

import { pluck, takeUntil, tap } from 'rxjs/operators';

import { DotContentletEditorService } from '@components/dot-contentlet-editor/services/dot-contentlet-editor.service';
import { DotCustomEventHandlerService } from '@dotcms/app/api/services/dot-custom-event-handler/dot-custom-event-handler.service';
import {
    DotPageStateService,
    DotRouterService,
    DotSessionStorageService
} from '@dotcms/data-access';
import { DotPageRenderState } from '@dotcms/dotcms-models';

@Component({
    selector: 'dot-edit-page-main',
    templateUrl: './dot-edit-page-main.component.html',
    styleUrls: ['./dot-edit-page-main.component.scss']
})
export class DotEditPageMainComponent implements OnInit, OnDestroy {
    pageState$: Observable<DotPageRenderState>;
    private dotSessionStorageService: DotSessionStorageService = inject(DotSessionStorageService);
    private pageUrl: string;
    private languageId: string;
    private pageIsSaved = false;
    private destroy$: Subject<boolean> = new Subject<boolean>();
    private readonly customEventsHandler;

    constructor(
        private route: ActivatedRoute,
        private dotContentletEditorService: DotContentletEditorService,
        private dotPageStateService: DotPageStateService,
        private dotRouterService: DotRouterService,
        private dotCustomEventHandlerService: DotCustomEventHandlerService,
        private titleService: Title
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
                const newTitle = page.title;
                const currentTitle = this.titleService.getTitle().split(' - ');
                // This is the second part of the title, what comes after the `-`.
                const subtTitle =
                    currentTitle.length > 1 ? currentTitle[currentTitle.length - 1] : '';
                this.titleService.setTitle(`${newTitle}${subtTitle ? ` - ${subtTitle}` : ''}`);
                this.pageUrl = page.pageURI;
                this.languageId = page.languageId.toString();
            })
        );

        this.subscribeIframeCloseAction();
    }

    ngOnDestroy(): void {
        this.dotSessionStorageService.removeVariantId();
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
                this.pageIsSaved = false;
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
