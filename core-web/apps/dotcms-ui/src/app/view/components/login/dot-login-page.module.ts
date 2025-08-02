import { CommonModule } from '@angular/common';
import { NgModule } from '@angular/core';

import { DotFieldValidationMessageComponent } from '@dotcms/ui';

import { DotLoginPageResolver } from './dot-login-page-resolver.service';
import { DotLoginPageRoutingModule } from './dot-login-page-routing.module';

@NgModule({
    imports: [CommonModule, DotLoginPageRoutingModule, DotFieldValidationMessageComponent],
    providers: [DotLoginPageResolver]
})
export class DotLoginPageModule {}
