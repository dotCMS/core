import { CommonModule } from '@angular/common';
import { Component, ChangeDetectionStrategy, inject, OnInit } from '@angular/core';
import { ActivatedRoute, NavigationEnd, Router, RouterModule } from '@angular/router';

import { ConfirmationService, MessageService } from 'primeng/api';
import { ConfirmDialogModule } from 'primeng/confirmdialog';
import { ToastModule } from 'primeng/toast';

import { filter } from 'rxjs/operators';

import { DotLanguagesService, DotPersonalizeService } from '@dotcms/data-access';

import { EditEmaStore } from './store/dot-ema.store';

import { EditEmaNavigationBarComponent } from '../components/edit-ema-navigation-bar/edit-ema-navigation-bar.component';
import { DotPageApiService } from '../services/dot-page-api.service';
import { EXPERIMENTS_ACTIVE_ICON, EXPERIMENTS_ICON, WINDOW } from '../shared/consts';
import { NavigationBarItem } from '../shared/models';

@Component({
    selector: 'dot-ema',
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
        ConfirmationService,
        DotLanguagesService,
        DotPersonalizeService,
        MessageService,
        {
            provide: WINDOW,
            useValue: window
        }
    ],
    templateUrl: './dot-ema.component.html',
    styleUrls: ['./dot-ema.component.scss'],
    changeDetection: ChangeDetectionStrategy.OnPush
})
export class DotEmaComponent implements OnInit {
    readonly router = inject(Router);
    readonly route = inject(ActivatedRoute);

    currentRoute = this.route.snapshot.firstChild.url[0].path;

    // This needs more logic. Because some of this buttons are for enterprise only or Feature Flags.
    items: NavigationBarItem[] = [
        {
            icon: 'pi-file',
            label: 'Content',
            key: 'content'
        },
        {
            icon: 'pi-table',
            label: 'Layout',
            key: 'layout'
        },
        {
            icon: 'pi-sliders-h',
            label: 'Rules',
            key: 'rules'
        },
        {
            iconURLActive: EXPERIMENTS_ACTIVE_ICON,
            iconURL: EXPERIMENTS_ICON,
            label: 'A/B',
            key: 'experiments'
        },
        {
            icon: 'pi-th-large',
            label: 'Page Tools',
            key: 'page-tools'
        },
        {
            icon: 'pi-ellipsis-v',
            label: 'Properties',
            key: 'edit-content'
        }
    ];

    private readonly actionsMap = {
        content: () => {
            this.router.navigate(['edit-ema', 'content'], {
                queryParamsHandling: 'merge'
            });
        },
        layout: () => {
            this.router.navigate(['edit-ema', 'layout'], {
                queryParamsHandling: 'merge'
            });
        },
        rules: () => {
            this.router.navigate(['edit-ema', 'rules'], {
                queryParamsHandling: 'merge'
            });
        },
        experiments: () => {
            this.router.navigate(['edit-ema', 'experiments'], {
                queryParamsHandling: 'merge'
            });
        },
        'page-tools': () => {
            /* Noop for now */
        },
        'edit-content': () => {
            /* Noop for now */
        }
    };

    ngOnInit(): void {
        this.router.events.pipe(filter((e) => e instanceof NavigationEnd)).subscribe(() => {
            this.currentRoute = this.route.snapshot.firstChild.url[0].path;
        });
    }

    executeAction(event: NavigationBarItem): void {
        this.actionsMap[event.key]();

        // Neither seo tools nor edit content has route, so we need to set the currentRoute here
    }
}
