import { CommonModule } from '@angular/common';
import { NgModule } from '@angular/core';

import { CheckboxModule } from 'primeng/checkbox';

import { DotMessagePipe } from '@dotcms/ui';

import { DotStarterResolver } from './dot-starter-resolver.service';
import { DotStarterRoutingModule } from './dot-starter-routing.module';
import { DotStarterComponent } from './dot-starter.component';

import { DotToolbarAnnouncementsComponent } from '../../view/components/dot-toolbar/components/dot-toolbar-announcements/dot-toolbar-announcements.component';

@NgModule({
    declarations: [DotStarterComponent],
    imports: [
        CommonModule,
        DotStarterRoutingModule,
        DotMessagePipe,
        CheckboxModule,
        DotToolbarAnnouncementsComponent
    ],
    providers: [DotStarterResolver]
})
export class DotStarterModule {}
