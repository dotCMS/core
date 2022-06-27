import { NgModule } from '@angular/core';
import { CommonModule } from '@angular/common';
import { DotLoadingIndicatorComponent } from './dot-loading-indicator.component';
import { DotLoadingIndicatorService } from './dot-loading-indicator.service';
import { DotSpinnerModule } from '@dotcms/ui';

@NgModule({
    imports: [CommonModule, DotSpinnerModule],
    declarations: [DotLoadingIndicatorComponent],
    exports: [DotLoadingIndicatorComponent],
    providers: [DotLoadingIndicatorService]
})
export class DotLoadingIndicatorModule {}
