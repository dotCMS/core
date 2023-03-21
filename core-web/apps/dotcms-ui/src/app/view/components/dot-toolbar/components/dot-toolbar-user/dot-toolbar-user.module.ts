import { CommonModule } from '@angular/common';
import { NgModule } from '@angular/core';

import { MenuModule } from 'primeng/menu';

import { DotPipesModule } from '@pipes/dot-pipes.module';

import { DotToolbarUserComponent } from './dot-toolbar-user.component';

import { DotGravatarModule } from '../dot-gravatar/dot-gravatar.module';
import { DotLoginAsModule } from '../dot-login-as/dot-login-as.module';
import { DotMyAccountModule } from '../dot-my-account/dot-my-account.module';

@NgModule({
    imports: [
        CommonModule,
        DotGravatarModule,
        DotLoginAsModule,
        DotMyAccountModule,
        DotPipesModule,
        MenuModule
    ],
    declarations: [DotToolbarUserComponent],
    exports: [DotToolbarUserComponent]
})
export class DotToolbarUserModule {}
