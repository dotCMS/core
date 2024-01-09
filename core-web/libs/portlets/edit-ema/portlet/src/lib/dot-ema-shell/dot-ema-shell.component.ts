import { Observable, Subject, combineLatest, fromEvent } from 'rxjs';

import { CommonModule } from '@angular/common';
import { Component, OnInit, inject, OnDestroy, ViewChild, ElementRef } from '@angular/core';
import { ActivatedRoute, Router, RouterModule } from '@angular/router';

import { ConfirmationService, MessageService } from 'primeng/api';
import { ConfirmDialogModule } from 'primeng/confirmdialog';
import { DialogModule } from 'primeng/dialog';
import { DialogService } from 'primeng/dynamicdialog';
import { ToastModule } from 'primeng/toast';

import { map, skip, take, takeUntil } from 'rxjs/operators';

import {
    DotESContentService,
    DotFavoritePageService,
    DotLanguagesService,
    DotPageLayoutService,
    DotPageRenderService,
    DotPersonalizeService
} from '@dotcms/data-access';
import { SiteService } from '@dotcms/dotcms-js';
import { DotPageToolUrlParams } from '@dotcms/dotcms-models';
import { DotPageToolsSeoComponent } from '@dotcms/portlets/dot-ema/ui';
import { SafeUrlPipe } from '@dotcms/ui';

import { EditEmaNavigationBarComponent } from './components/edit-ema-navigation-bar/edit-ema-navigation-bar.component';
import { EditEmaStore } from './store/dot-ema.store';

import { EditEmaEditorComponent } from '../edit-ema-editor/edit-ema-editor.component';
import { DotActionUrlService } from '../services/dot-action-url/dot-action-url.service';
import { DotPageApiParams, DotPageApiService } from '../services/dot-page-api.service';
import { WINDOW } from '../shared/consts';
import { NG_CUSTOM_EVENTS } from '../shared/enums';
import { NavigationBarItem } from '../shared/models';

@Component({
    selector: 'dot-ema-shell',
    standalone: true,
    imports: [
        CommonModule,
        ConfirmDialogModule,
        ToastModule,
        EditEmaNavigationBarComponent,
        RouterModule,
        DotPageToolsSeoComponent,
        DialogModule,
        SafeUrlPipe
    ],
    providers: [
        EditEmaStore,
        DotPageApiService,
        DotActionUrlService,
        ConfirmationService,
        DotLanguagesService,
        DotPersonalizeService,
        MessageService,
        DotPageLayoutService,
        DotFavoritePageService,
        DotESContentService,
        DialogService,
        DotPageRenderService,
        {
            provide: WINDOW,
            useValue: window
        }
    ],
    templateUrl: './dot-ema-shell.component.html',
    styleUrls: ['./dot-ema-shell.component.scss']
})
export class DotEmaShellComponent implements OnInit, OnDestroy {
    @ViewChild('dialogIframe') dialogIframe!: ElementRef<HTMLIFrameElement>;
    @ViewChild('pageTools') pageTools!: DotPageToolsSeoComponent;

    private readonly activatedRoute = inject(ActivatedRoute);
    private readonly router = inject(Router);
    private readonly siteService = inject(SiteService);

    readonly store = inject(EditEmaStore);

    private readonly destroy$ = new Subject<boolean>();

    private currentComponent: unknown;

    get queryParams(): DotPageApiParams {
        const queryParams = this.activatedRoute.snapshot.queryParams;

        return {
            language_id: queryParams['language_id'],
            url: queryParams['url'],
            'com.dotmarketing.persona.id': queryParams['com.dotmarketing.persona.id']
        };
    }

    dialogState$ = this.store.dialogState$;

    // We can internally navigate, so the PageID can change
    // We need to move the logic to a function, we still need to add enterprise logic
    shellProperties$: Observable<{
        items: NavigationBarItem[];
        seoProperties: DotPageToolUrlParams;
    }> = this.store.shellProperties$.pipe(
        map(({ currentUrl, page, host, languageId, siteId }) => ({
            items: [
                {
                    icon: 'pi-file',
                    label: 'Content',
                    href: 'content'
                },
                {
                    icon: 'pi-table',
                    label: 'Layout',
                    href: 'layout'
                },
                {
                    icon: 'pi-sliders-h',
                    label: 'Rules',
                    href: `rules/${page.identifier}`
                },
                {
                    iconURL: 'experiments',
                    label: 'A/B',
                    href: 'experiments'
                },
                {
                    icon: 'pi-th-large',
                    label: 'Page Tools',
                    action: () => {
                        this.pageTools.toggleDialog();
                    }
                },
                {
                    icon: 'pi-ellipsis-v',
                    label: 'Properties',
                    action: () => {
                        this.store.initActionEdit({
                            inode: page.inode,
                            title: page.title,
                            type: 'shell'
                        });
                    }
                }
            ],
            seoProperties: {
                currentUrl,
                languageId,
                siteId,
                requestHostName: host
            }
        }))
    );

    ngOnInit(): void {
        combineLatest([this.activatedRoute.data, this.activatedRoute.queryParams])
            .pipe(takeUntil(this.destroy$))
            .subscribe(([{ data }]) => {
                this.store.load({
                    ...this.queryParams,
                    clientHost: data.url
                });
            });

        // We need to skip one because it's the initial value
        this.siteService.switchSite$.pipe(skip(1)).subscribe(() => {
            this.router.navigate(['/pages']);
        });
    }

    ngOnDestroy(): void {
        this.destroy$.next(true);
        this.destroy$.complete();
    }

    onActivateRoute(event) {
        this.currentComponent = event;
    }

    onIframeLoad() {
        this.store.setDialogIframeLoading(false);

        fromEvent(
            // The events are getting sended to the document
            this.dialogIframe.nativeElement.contentWindow.document,
            'ng-event'
        )
            .pipe(takeUntil(this.destroy$))
            .subscribe((event: CustomEvent) => {
                if (event.detail.name === NG_CUSTOM_EVENTS.SAVE_PAGE) {
                    const url = event.detail.payload.htmlPageReferer.split('?')[0].replace('/', '');

                    if (this.queryParams.url !== url) {
                        this.navigate({
                            url
                        });

                        return;
                    }

                    if (this.currentComponent instanceof EditEmaEditorComponent) {
                        this.currentComponent.reloadIframe();
                    }

                    this.activatedRoute.data.pipe(take(1)).subscribe(({ data }) => {
                        this.store.load({
                            clientHost: data.url,
                            ...this.queryParams
                        });
                    });
                }
            });
    }

    private navigate(queryParams) {
        this.router.navigate([], {
            queryParams,
            queryParamsHandling: 'merge'
            // replaceUrl: true,
            // skipLocationChange: false,
        });
    }
}
