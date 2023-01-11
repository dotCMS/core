import { CommonModule } from '@angular/common';
import { NgModule } from '@angular/core';

import { DotLoginPageResolver } from '@components/login/dot-login-page-resolver.service';
import { DotLoginPageRoutingModule } from '@components/login/dot-login-page-routing.module';

@NgModule({
    imports: [CommonModule, DotLoginPageRoutingModule],
    providers: [DotLoginPageResolver]
})
export class DotLoginPageModule {}
