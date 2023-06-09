import { CommonModule } from '@angular/common';
import { NgModule } from '@angular/core';

import { CheckboxModule } from 'primeng/checkbox';

import { DotMessagePipeModule } from '@dotcms/ui';

import { DotStarterResolver } from './dot-starter-resolver.service';
import { DotStarterRoutingModule } from './dot-starter-routing.module';
import { DotStarterComponent } from './dot-starter.component';

@NgModule({
    declarations: [DotStarterComponent],
    imports: [CommonModule, DotStarterRoutingModule, DotMessagePipeModule, CheckboxModule],
    providers: [DotStarterResolver]
})
export class DotStarterModule {}
