import { CommonModule } from '@angular/common';
import { ChangeDetectionStrategy, Component } from '@angular/core';
import { RouterModule } from '@angular/router';

import { ButtonModule } from 'primeng/button';
import { TooltipModule } from 'primeng/tooltip';

import { DotMessagePipe } from '@dotcms/ui';

@Component({
    selector: 'dot-uve-navigation-bar',
    templateUrl: './dot-uve-navigation-bar.component.html',
    styleUrls: ['./dot-uve-navigation-bar.component.scss'],
    changeDetection: ChangeDetectionStrategy.OnPush,
    imports: [CommonModule, RouterModule, DotMessagePipe, TooltipModule, ButtonModule]
})
export class DotUVENavigationBarComponent {
    protected items = [
        {
            label: 'Content',
            icon: 'pi pi-file',
            active: true
        },
        {
            label: 'Design',
            icon: 'pi pi-palette',
            active: false
        },
        {
            label: 'Settings',
            icon: 'pi pi-cog',
            active: false
        },
        {
            label: 'Rules',
            icon: 'pi pi-file',
            active: false
        }
    ];
}
