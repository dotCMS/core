import { NgModule } from '@angular/core';
import { CommonModule } from '@angular/common';

import { NotLicensedComponent } from './not-licensed.component';
import { ButtonModule } from 'primeng/button';
import { DotIconModule } from '@components/_common/dot-icon/dot-icon.module';


@NgModule({
    imports: [CommonModule, ButtonModule, DotIconModule],
    declarations: [NotLicensedComponent],
    exports: [NotLicensedComponent]
})
export class NotLicensedModule {}
