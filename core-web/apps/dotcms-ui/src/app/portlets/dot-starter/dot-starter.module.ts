import { CommonModule } from '@angular/common';
import { NgModule } from '@angular/core';
import { RouterModule } from '@angular/router';

import { CheckboxModule } from 'primeng/checkbox';

import { DotMessagePipe } from '@dotcms/ui';

import { DotStarterResolver } from './dot-starter-resolver.service';
import { DotStarterComponent } from './dot-starter.component';
import { dotStarterRoutes } from './dot-starter.routes';

import { DotToolbarAnnouncementsComponent } from '../../view/components/dot-toolbar/components/dot-toolbar-announcements/dot-toolbar-announcements.component';

@NgModule({
    declarations: [DotStarterComponent],
    imports: [
        CommonModule,
        RouterModule.forChild(dotStarterRoutes),
        DotMessagePipe,
        CheckboxModule,
        DotToolbarAnnouncementsComponent
    ],
    providers: [DotStarterResolver]
})
export class DotStarterModule {}
