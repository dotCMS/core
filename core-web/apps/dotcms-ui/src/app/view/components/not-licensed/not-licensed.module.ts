import { CommonModule } from '@angular/common';
import { NgModule } from '@angular/core';

import { ButtonModule } from 'primeng/button';

import { DotIconModule, DotMessagePipe } from '@dotcms/ui';
import { DotPipesModule } from '@pipes/dot-pipes.module';

import { NotLicensedComponent } from './not-licensed.component';

@NgModule({
    imports: [CommonModule, ButtonModule, DotIconModule, DotPipesModule, DotMessagePipe],
    declarations: [NotLicensedComponent],
    exports: [NotLicensedComponent]
})
export class NotLicensedModule {}
