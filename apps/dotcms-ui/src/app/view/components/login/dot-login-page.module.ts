import { NgModule } from '@angular/core';
import { CommonModule } from '@angular/common';
import { DotLoginPageRoutingModule } from '@components/login/dot-login-page-routing.module';
import { DotLoginPageResolver } from '@components/login/dot-login-page-resolver.service';

@NgModule({
    imports: [CommonModule, DotLoginPageRoutingModule],
    providers: [DotLoginPageResolver]
})
export class DotLoginPageModule {}
