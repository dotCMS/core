import { CommonModule } from '@angular/common';
import { NgModule } from '@angular/core';

import { DotIconModule } from '@dotcms/ui';
import { DotPipesModule } from '@pipes/dot-pipes.module';
import { ButtonModule } from 'primeng/button';
import { NotLicensedComponent } from './not-licensed.component';

@NgModule({
    imports: [CommonModule, ButtonModule, DotIconModule, DotPipesModule],
    declarations: [NotLicensedComponent],
    exports: [NotLicensedComponent]
})
export class NotLicensedModule {}
