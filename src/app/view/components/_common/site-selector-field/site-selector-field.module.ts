import { NgModule } from '@angular/core';
import { CommonModule } from '@angular/common';
import { SiteSelectorFieldComponent } from './site-selector-field.component';
import { SiteSelectorModule } from '../site-selector/site-selector.module';

@NgModule({
    declarations: [SiteSelectorFieldComponent],
    imports: [CommonModule, SiteSelectorModule],
    exports: [SiteSelectorFieldComponent],
    providers: []
})
export class SiteSelectorFieldModule {}
