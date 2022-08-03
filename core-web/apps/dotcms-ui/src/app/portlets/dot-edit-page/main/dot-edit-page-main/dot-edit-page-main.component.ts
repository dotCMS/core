import { Observable, Subject, merge } from 'rxjs';

import { pluck, take, takeUntil, tap } from 'rxjs/operators';
import { Component, OnInit, OnDestroy, ViewChild, ElementRef } from '@angular/core';
import { ActivatedRoute } from '@angular/router';
import { DotPageRenderState } from '../../shared/models/dot-rendered-page-state.model';
import { DotPageStateService } from '../../content/services/dot-page-state/dot-page-state.service';
import { DotContentletEditorService } from '@components/dot-contentlet-editor/services/dot-contentlet-editor.service';
import { DotRouterService } from '@services/dot-router/dot-router.service';
import { DotCustomEventHandlerService } from '@services/dot-custom-event-handler/dot-custom-event-handler.service';
import { Title } from '@angular/platform-browser';
import { DotEventsService } from '@services/dot-events/dot-events.service';
import { DotWorkflowActionsFireService } from '@services/dot-workflow-actions-fire/dot-workflow-actions-fire.service';
import { DotEditBlockEditorComponent } from '@portlets/dot-edit-page/components/dot-edit-block-editor/dot-edit-block-editor.component';

@Component({
    selector: 'dot-edit-page-main',
    templateUrl: './dot-edit-page-main.component.html',
    styleUrls: ['./dot-edit-page-main.component.scss']
})
export class DotEditPageMainComponent implements OnInit, OnDestroy {
    @ViewChild('blockEditor') blockEditor: DotEditBlockEditorComponent;
    pageState$: Observable<DotPageRenderState>;
    blockEditorData;
    private editElement: HTMLDivElement;
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
        private titleService: Title,
        private dotEventsService: DotEventsService,
        private dotWorkflowActionsFireService: DotWorkflowActionsFireService
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

        this.dotEventsService
            .listen<HTMLDivElement>('edit-block-editor')
            .pipe(takeUntil(this.destroy$))
            .subscribe((event) => {
                debugger;
                this.blockEditorData = {
                    ...event.data.dataset,
                    content: JSON.parse(event.data.dataset.content)
                };
                this.editElement = event.data;
            });
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

    saveEditorChanges(): void {
        this.dotWorkflowActionsFireService
            .saveContentlet({
                [this.blockEditorData.fieldName]: JSON.stringify(this.blockEditor.editor.getJSON()),
                inode: this.blockEditorData.iode
            })
            .pipe(take(1))
            .subscribe(() => {
                const customEvent = new CustomEvent('ng-event', { detail: { name: 'in-iframe' } });
                window.top.document.dispatchEvent(customEvent);
                this.blockEditorData = null;
            });
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
