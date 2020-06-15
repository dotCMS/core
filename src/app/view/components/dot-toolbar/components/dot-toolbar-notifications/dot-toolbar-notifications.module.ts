import { CommonModule } from '@angular/common';
import { NgModule } from '@angular/core';
import { ButtonModule } from 'primeng/button';

import { DotToolbarNotificationsComponent } from './dot-toolbar-notifications.component';
import {
    DotNotificationsItemComponent,
    DotNotificationsListComponent
} from './components/dot-notifications/dot-notifications.component';
import { DotDropdownModule } from '@components/_common/dot-dropdown-component/dot-dropdown.module';
import { DotCustomTimeModule } from '@components/_common/dot-custom-time.component/dot-custom-time.module';
import { DotPipesModule } from '@pipes/dot-pipes.module';

@NgModule({
    imports: [CommonModule, DotDropdownModule, ButtonModule, DotCustomTimeModule, DotPipesModule],
    exports: [DotToolbarNotificationsComponent],
    declarations: [
        DotToolbarNotificationsComponent,
        DotNotificationsItemComponent,
        DotNotificationsListComponent
    ],
    providers: []
})
export class DotToolbarNotificationModule {}
