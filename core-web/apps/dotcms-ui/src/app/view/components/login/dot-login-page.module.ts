import { CommonModule } from '@angular/common';
import { NgModule } from '@angular/core';

import { DotLoginPageResolver } from './dot-login-page-resolver.service';
import { DotLoginPageRoutingModule } from './dot-login-page-routing.module';

@NgModule({
    imports: [CommonModule, DotLoginPageRoutingModule],
    providers: [DotLoginPageResolver]
})
export class DotLoginPageModule {}
