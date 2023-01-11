import { CommonModule } from '@angular/common';
import { NgModule } from '@angular/core';

import { DotSpinnerModule } from '@dotcms/ui';
import { DotLoadingIndicatorService } from '@dotcms/utils';

import { DotLoadingIndicatorComponent } from './dot-loading-indicator.component';


@NgModule({
    imports: [CommonModule, DotSpinnerModule],
    declarations: [DotLoadingIndicatorComponent],
    exports: [DotLoadingIndicatorComponent],
    providers: [DotLoadingIndicatorService]
})
export class DotLoadingIndicatorModule {}
