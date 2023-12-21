import { Observable, Subject } from 'rxjs';

import { CommonModule } from '@angular/common';
import { Component, OnInit, inject, OnDestroy } from '@angular/core';
import { ActivatedRoute, Params, Router, RouterModule } from '@angular/router';

import { ConfirmationService, MessageService } from 'primeng/api';
import { ConfirmDialogModule } from 'primeng/confirmdialog';
import { ToastModule } from 'primeng/toast';

import { map, skip, takeUntil } from 'rxjs/operators';

import {
    DotLanguagesService,
    DotPageLayoutService,
    DotPersonalizeService
} from '@dotcms/data-access';
import { SiteService } from '@dotcms/dotcms-js';

import { EditEmaStore } from './store/dot-ema.store';

import { EditEmaNavigationBarComponent } from '../components/edit-ema-navigation-bar/edit-ema-navigation-bar.component';
import { DotActionUrlService } from '../services/dot-action-url/dot-action-url.service';
import { DotPageApiService } from '../services/dot-page-api.service';
import { DEFAULT_LANGUAGE_ID, DEFAULT_PERSONA, DEFAULT_URL, WINDOW } from '../shared/consts';
import { NavigationBarItem } from '../shared/models';

@Component({
    selector: 'dot-ema-shell',
    standalone: true,
    imports: [
        CommonModule,
        ConfirmDialogModule,
        ToastModule,
        EditEmaNavigationBarComponent,
        RouterModule
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
        {
            provide: WINDOW,
            useValue: window
        }
    ],
    templateUrl: './dot-ema-shell.component.html',
    styleUrls: ['./dot-ema-shell.component.scss']
})
export class DotEmaShellComponent implements OnInit, OnDestroy {
    private readonly route = inject(ActivatedRoute);
    private readonly router = inject(Router);
    private readonly store = inject(EditEmaStore);
    private readonly siteService = inject(SiteService);

    private readonly destroy$ = new Subject<boolean>();

    // We can internally navigate, so the PageID can change
    // We need to move the logic to a function, we still need to add enterprise logic
    items$: Observable<NavigationBarItem[]> = this.store.editorState$.pipe(
        map(({ editor }) => [
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
                href: `rules/${editor.page.identifier}`
            },
            {
                iconURL: 'experiments',
                label: 'A/B',
                href: 'experiments'
            },
            {
                icon: 'pi-th-large',
                label: 'Page Tools',
                href: 'page-tools'
            },
            {
                icon: 'pi-ellipsis-v',
                label: 'Properties',
                href: 'edit-content'
            }
        ])
    );

    ngOnInit(): void {
        this.route.queryParams.pipe(takeUntil(this.destroy$)).subscribe((queryParams: Params) => {
            this.store.load({
                language_id: queryParams['language_id'] ?? DEFAULT_LANGUAGE_ID,
                url: queryParams['url'] ?? DEFAULT_URL,
                persona_id: queryParams['com.dotmarketing.persona.id'] ?? DEFAULT_PERSONA.identifier
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
}
