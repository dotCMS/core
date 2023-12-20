import { CommonModule } from '@angular/common';
import { Component, ChangeDetectionStrategy, OnInit, inject } from '@angular/core';
import { ActivatedRoute, Params, RouterModule } from '@angular/router';

import { ConfirmationService, MessageService } from 'primeng/api';
import { ConfirmDialogModule } from 'primeng/confirmdialog';
import { ToastModule } from 'primeng/toast';

import {
    DotLanguagesService,
    DotPageLayoutService,
    DotPersonalizeService
} from '@dotcms/data-access';

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
    styleUrls: ['./dot-ema-shell.component.scss'],
    changeDetection: ChangeDetectionStrategy.OnPush
})
export class DotEmaShellComponent implements OnInit {
    private readonly route = inject(ActivatedRoute);
    private readonly store = inject(EditEmaStore);

    // This needs more logic. Because some of this buttons are for enterprise only or Feature Flags.
    readonly items: NavigationBarItem[] = [
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
            href: 'rules'
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
    ];

    ngOnInit(): void {
        this.route.queryParams.subscribe((queryParams: Params) => {
            this.store.load({
                language_id: queryParams['language_id'] ?? DEFAULT_LANGUAGE_ID,
                url: queryParams['url'] ?? DEFAULT_URL,
                persona_id: queryParams['com.dotmarketing.persona.id'] ?? DEFAULT_PERSONA.identifier
            });
        });
    }
}
