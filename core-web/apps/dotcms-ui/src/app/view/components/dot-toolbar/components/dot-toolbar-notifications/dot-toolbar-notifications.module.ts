import { CommonModule } from '@angular/common';
import { NgModule } from '@angular/core';

import { ButtonModule } from 'primeng/button';
import { DividerModule } from 'primeng/divider';

import { DotMessagePipe } from '@dotcms/ui';

import {
    DotNotificationsItemComponent,
    DotNotificationsListComponent
} from './components/dot-notifications/dot-notifications.component';
import { DotToolbarNotificationsComponent } from './dot-toolbar-notifications.component';

import { DotShowHideFeatureDirective } from '../../../../../shared/directives/dot-show-hide-feature/dot-show-hide-feature.directive';
import { DotPipesModule } from '../../../../pipes/dot-pipes.module';
import { DotCustomTimeModule } from '../../../_common/dot-custom-time.component/dot-custom-time.module';
import { DotDropdownModule } from '../../../_common/dot-dropdown-component/dot-dropdown.module';
import { DotToolbarAnnouncementsComponent } from '../dot-toolbar-announcements/dot-toolbar-announcements.component';
import { AnnouncementsStore } from '../dot-toolbar-announcements/store/dot-announcements.store';

@NgModule({
    imports: [
        CommonModule,
        DotDropdownModule,
        ButtonModule,
        DotCustomTimeModule,
        DotPipesModule,
        DotMessagePipe,
        DividerModule,
        DotToolbarAnnouncementsComponent,
        DotShowHideFeatureDirective
    ],
    exports: [DotToolbarNotificationsComponent],
    declarations: [
        DotToolbarNotificationsComponent,
        DotNotificationsItemComponent,
        DotNotificationsListComponent
    ],
    providers: [AnnouncementsStore]
})
export class DotToolbarNotificationModule {}
