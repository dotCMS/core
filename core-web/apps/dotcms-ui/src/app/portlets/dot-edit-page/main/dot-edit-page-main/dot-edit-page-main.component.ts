import { merge, Observable, Subject } from 'rxjs';

import { CommonModule } from '@angular/common';
import { Component, inject, OnDestroy, OnInit } from '@angular/core';
import { Title } from '@angular/platform-browser';
import { ActivatedRoute, RouterModule } from '@angular/router';

import { DialogModule } from 'primeng/dialog';
import { OverlayPanelModule } from 'primeng/overlaypanel';

import { pluck, takeUntil, tap } from 'rxjs/operators';

import {
    DotPageStateService,
    DotRouterService,
    DotSessionStorageService
} from '@dotcms/data-access';
import { DotPageRenderState } from '@dotcms/dotcms-models';

import { DotCustomEventHandlerService } from '../../../../api/services/dot-custom-event-handler/dot-custom-event-handler.service';
import { DotEditContentletComponent } from '../../../../view/components/dot-contentlet-editor/components/dot-edit-contentlet/dot-edit-contentlet.component';
import { DotContentletEditorService } from '../../../../view/components/dot-contentlet-editor/services/dot-contentlet-editor.service';
import { DotExperimentClassDirective } from '../../../shared/directives/dot-experiment-class.directive';
import { DotBlockEditorSidebarComponent } from '../../components/dot-block-editor-sidebar/dot-block-editor-sidebar.component';
import { DotEditPageNavDirective } from '../dot-edit-page-nav/directives/dot-edit-page-nav.directive';
import { DotEditPageNavComponent } from '../dot-edit-page-nav/dot-edit-page-nav.component';

@Component({
    selector: 'dot-edit-page-main',
    templateUrl: './dot-edit-page-main.component.html',
    styleUrls: ['./dot-edit-page-main.component.scss'],
    imports: [
        CommonModule,
        RouterModule,
        DotEditContentletComponent,
        DotBlockEditorSidebarComponent,
        DotEditPageNavDirective,
        DotEditPageNavComponent,
        DotExperimentClassDirective,
        OverlayPanelModule,
        DialogModule
    ]
})
export class DotEditPageMainComponent implements OnInit, OnDestroy {
    private route = inject(ActivatedRoute);
    private dotContentletEditorService = inject(DotContentletEditorService);
    private dotPageStateService = inject(DotPageStateService);
    private dotRouterService = inject(DotRouterService);
    private dotCustomEventHandlerService = inject(DotCustomEventHandlerService);
    private titleService = inject(Title);

    pageState$: Observable<DotPageRenderState>;
    private dotSessionStorageService: DotSessionStorageService = inject(DotSessionStorageService);
    private pageUrl: string;
    private languageId: string;
    private pageIsSaved = false;
    private destroy$: Subject<boolean> = new Subject<boolean>();
    private readonly customEventsHandler;

    constructor() {
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
