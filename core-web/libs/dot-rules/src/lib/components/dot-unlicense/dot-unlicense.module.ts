import { NgModule } from '@angular/core';

import { ButtonModule } from 'primeng/button';

import { DotUnlicenseComponent } from './dot-unlicense.component';

import { I18nService } from '../../services/system/locale/I18n';

@NgModule({
    imports: [ButtonModule],
    declarations: [DotUnlicenseComponent],
    providers: [I18nService],
    exports: [DotUnlicenseComponent]
})
export class DotUnlicenseModule {}
