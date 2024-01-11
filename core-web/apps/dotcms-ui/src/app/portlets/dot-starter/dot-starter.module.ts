import { CommonModule } from '@angular/common';
import { NgModule } from '@angular/core';

import { CheckboxModule } from 'primeng/checkbox';

import { DotToolbarAnnouncementsComponent } from '@components/dot-toolbar/components/dot-toolbar-announcements/dot-toolbar-announcements.component';
import { DotMessagePipe } from '@dotcms/ui';

import { DotStarterResolver } from './dot-starter-resolver.service';
import { DotStarterRoutingModule } from './dot-starter-routing.module';
import { DotStarterComponent } from './dot-starter.component';

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
