import { NgModule } from '@angular/core';
import { CommonModule } from '@angular/common';

import { DotDropdownModule } from '@components/_common/dot-dropdown-component/dot-dropdown.module';
import { DotGravatarModule } from '../dot-gravatar/dot-gravatar.module';
import { DotLoginAsModule } from '../dot-login-as/dot-login-as.module';
import { DotMyAccountModule } from '../dot-my-account/dot-my-account.module';
import { DotToolbarUserComponent } from './dot-toolbar-user.component';
import { DotPipesModule } from '@pipes/dot-pipes.module';

@NgModule({
    imports: [
        CommonModule,
        DotDropdownModule,
        DotGravatarModule,
        DotLoginAsModule,
        DotMyAccountModule,
        DotPipesModule
    ],
    declarations: [DotToolbarUserComponent],
    exports: [DotToolbarUserComponent]
})
export class DotToolbarUserModule {}
