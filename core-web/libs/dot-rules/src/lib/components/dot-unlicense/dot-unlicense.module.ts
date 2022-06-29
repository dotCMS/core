import { NgModule } from '@angular/core';
import { DotUnlicenseComponent } from './dot-unlicense.component';
import { I18nService } from '../../services/system/locale/I18n';

@NgModule({
    imports: [],
    declarations: [DotUnlicenseComponent],
    providers: [I18nService],
    exports: [DotUnlicenseComponent]
})
export class DotUnlicenseModule {}
