import { combineLatest, Observable, Subject } from 'rxjs';

import { CommonModule } from '@angular/common';
import { Component, inject, OnDestroy, OnInit, ViewChild } from '@angular/core';
import { ActivatedRoute, Router, RouterModule } from '@angular/router';

import { ConfirmationService, MessageService } from 'primeng/api';
import { ConfirmDialogModule } from 'primeng/confirmdialog';
import { DialogService } from 'primeng/dynamicdialog';
import { ToastModule } from 'primeng/toast';

import { map, skip, take, takeUntil } from 'rxjs/operators';

import {
    DotESContentService,
    DotExperimentsService,
    DotFavoritePageService,
    DotLanguagesService,
    DotPageLayoutService,
    DotPageRenderService,
    DotSeoMetaTagsService,
    DotSeoMetaTagsUtilService
} from '@dotcms/data-access';
import { SiteService } from '@dotcms/dotcms-js';
import { DotPageToolUrlParams } from '@dotcms/dotcms-models';
import { DotPageToolsSeoComponent } from '@dotcms/portlets/dot-ema/ui';
import { DotInfoPageComponent, DotNotLicenseComponent, InfoPage, SafeUrlPipe } from '@dotcms/ui';

import { EditEmaNavigationBarComponent } from './components/edit-ema-navigation-bar/edit-ema-navigation-bar.component';
import { EditEmaStore } from './store/dot-ema.store';

import { DotEmaDialogComponent } from '../components/dot-ema-dialog/dot-ema-dialog.component';
import { EditEmaEditorComponent } from '../edit-ema-editor/edit-ema-editor.component';
import { DotActionUrlService } from '../services/dot-action-url/dot-action-url.service';
import { DotPageApiParams, DotPageApiService } from '../services/dot-page-api.service';
import { WINDOW } from '../shared/consts';
import { NG_CUSTOM_EVENTS } from '../shared/enums';
import { NavigationBarItem } from '../shared/models';

@Component({
    selector: 'dot-ema-shell',
    standalone: true,
    providers: [
        EditEmaStore,
        DotPageApiService,
        DotActionUrlService,
        ConfirmationService,
        DotLanguagesService,
        MessageService,
        DotPageLayoutService,
        DotFavoritePageService,
        DotESContentService,
        DialogService,
        DotPageRenderService,
        DotSeoMetaTagsService,
        DotSeoMetaTagsUtilService,
        {
            provide: WINDOW,
            useValue: window
        },
        DotExperimentsService
    ],
    templateUrl: './dot-ema-shell.component.html',
    styleUrls: ['./dot-ema-shell.component.scss'],
    imports: [
        CommonModule,
        ConfirmDialogModule,
        ToastModule,
        EditEmaNavigationBarComponent,
        RouterModule,
        DotPageToolsSeoComponent,
        DotEmaDialogComponent,
        SafeUrlPipe,
        DotInfoPageComponent,
        DotNotLicenseComponent
    ]
})
export class DotEmaShellComponent implements OnInit, OnDestroy {
    @ViewChild('dialog') dialog!: DotEmaDialogComponent;
    @ViewChild('pageTools') pageTools!: DotPageToolsSeoComponent;
    readonly store = inject(EditEmaStore);
    EMA_INFO_PAGES: Record<'NOT_FOUND' | 'ACCESS_DENIED', InfoPage> = {
        NOT_FOUND: {
            icon: 'compass',
            title: 'editema.infopage.notfound.title',
            description: 'editema.infopage.notfound.description',
            buttonPath: '/pages',
            buttonText: 'editema.infopage.button.gotopages'
        },
        ACCESS_DENIED: {
            icon: 'ban',
            title: 'editema.infopage.accessdenied.title',
            description: 'editema.infopage.accessdenied.description',
            buttonPath: '/pages',
            buttonText: 'editema.infopage.button.gotopages'
        }
    };
    // We need to move the logic to a function, we still need to add enterprise logic
    shellProperties$: Observable<{
        items: NavigationBarItem[];
        canRead: boolean;
        seoProperties: DotPageToolUrlParams;
        error?: number;
    }> = this.store.shellProperties$.pipe(
        map(({ currentUrl, page, host, languageId, siteId, templateDrawed, error }) => {
            const isLayoutDisabled = !page.canEdit || !templateDrawed;

            if (
                isLayoutDisabled &&
                this.activatedRoute.firstChild.snapshot.url[0].path === 'layout'
            ) {
                this.router.navigate(['./content'], { relativeTo: this.activatedRoute });
            }

            return {
                items: [
                    {
                        icon: 'pi-file',
                        label: 'editema.editor.navbar.content',
                        href: 'content'
                    },
                    {
                        icon: 'pi-table',
                        label: 'editema.editor.navbar.layout',
                        href: 'layout',
                        isDisabled: isLayoutDisabled,
                        tooltip: templateDrawed
                            ? null
                            : 'editema.editor.navbar.layout.tooltip.cannot.edit.advanced.template'
                    },
                    {
                        icon: 'pi-sliders-h',
                        label: 'editema.editor.navbar.rules',
                        href: `rules/${page.identifier}`,
                        isDisabled: !page.canEdit
                    },
                    {
                        iconURL: 'experiments',
                        label: 'editema.editor.navbar.experiments',
                        href: `experiments/${page.identifier}`,
                        isDisabled: !page.canEdit
                    },
                    {
                        icon: 'pi-th-large',
                        label: 'editema.editor.navbar.page-tools',
                        action: () => {
                            this.pageTools.toggleDialog();
                        }
                    },
                    {
                        icon: 'pi-ellipsis-v',
                        label: 'editema.editor.navbar.properties',
                        action: () => {
                            this.dialog.editContentlet({
                                inode: page.inode,
                                title: page.title,
                                identifier: page.identifier,
                                contentType: page.contentType
                            });
                        }
                    }
                ],
                canRead: page.canRead,
                seoProperties: {
                    currentUrl,
                    languageId,
                    siteId,
                    requestHostName: host
                },
                error
            };
        })
    );
    private readonly activatedRoute = inject(ActivatedRoute);
    private readonly router = inject(Router);
    private readonly siteService = inject(SiteService);
    private readonly destroy$ = new Subject<boolean>();
    private currentComponent: unknown;

    // We can internally navigate, so the PageID can change

    get queryParams(): DotPageApiParams {
        const queryParams = this.activatedRoute.snapshot.queryParams;

        return {
            language_id: queryParams['language_id'],
            url: queryParams['url'],
            'com.dotmarketing.persona.id': queryParams['com.dotmarketing.persona.id'],
            variantName: queryParams['variantName']
        };
    }

    ngOnInit(): void {
        combineLatest([this.activatedRoute.data, this.activatedRoute.queryParams])
            .pipe(takeUntil(this.destroy$))
            .subscribe(([{ data }, queryParams]) => {
                this.store.load({
                    ...(queryParams as DotPageApiParams),
                    clientHost: data?.url
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

    handleNgEvent({ event }: { event: CustomEvent }) {
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
                    clientHost: data?.url,
                    ...this.queryParams
                });
            });
        }
    }

    /**
     * Reloads the component from the dialog.
     */
    reloadFromDialog() {
        this.store.reload({ params: this.queryParams });
    }

    private navigate(queryParams) {
        this.router.navigate([], {
            queryParams,
            queryParamsHandling: 'merge'
        });
    }
}
