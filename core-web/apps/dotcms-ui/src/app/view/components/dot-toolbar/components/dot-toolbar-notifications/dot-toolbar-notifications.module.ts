import { CommonModule } from '@angular/common';
import { NgModule } from '@angular/core';

import { ButtonModule } from 'primeng/button';

import { DotCustomTimeModule } from '@components/_common/dot-custom-time.component/dot-custom-time.module';
import { DotDropdownModule } from '@components/_common/dot-dropdown-component/dot-dropdown.module';
import { UiDotIconButtonModule } from '@components/_common/dot-icon-button/dot-icon-button.module';
import { DotPipesModule } from '@pipes/dot-pipes.module';

import {
    DotNotificationsItemComponent,
    DotNotificationsListComponent
} from './components/dot-notifications/dot-notifications.component';
import { DotToolbarNotificationsComponent } from './dot-toolbar-notifications.component';

@NgModule({
    imports: [
        CommonModule,
        DotDropdownModule,
        ButtonModule,
        DotCustomTimeModule,
        DotPipesModule,
        UiDotIconButtonModule
    ],
    exports: [DotToolbarNotificationsComponent],
    declarations: [
        DotToolbarNotificationsComponent,
        DotNotificationsItemComponent,
        DotNotificationsListComponent
    ],
    providers: []
})
export class DotToolbarNotificationModule {}
