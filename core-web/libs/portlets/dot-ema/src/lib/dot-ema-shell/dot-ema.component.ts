import { CommonModule } from '@angular/common';
import { Component, ChangeDetectionStrategy } from '@angular/core';
import { RouterModule } from '@angular/router';

import { ConfirmationService, MessageService } from 'primeng/api';
import { ConfirmDialogModule } from 'primeng/confirmdialog';
import { ToastModule } from 'primeng/toast';

import { DotLanguagesService, DotPersonalizeService } from '@dotcms/data-access';

import { EditEmaStore } from './store/dot-ema.store';

import { EditEmaNavigationBarComponent } from '../components/edit-ema-navigation-bar/edit-ema-navigation-bar.component';
import { DotActionUrlService } from '../services/dot-action-url/dot-action-url.service';
import { DotPageApiService } from '../services/dot-page-api.service';
import { WINDOW } from '../shared/consts';
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
        DotActionUrlService,
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
export class DotEmaComponent {
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
}
