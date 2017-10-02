import { NgModule } from '@angular/core';
import { CommonModule } from '@angular/common';
import { DotLoadingIndicatorComponent } from './dot-loading-indicator.component';
import { DotLoadingIndicatorService } from './dot-loading-indicator.service';

@NgModule({
    imports: [CommonModule],
    declarations: [DotLoadingIndicatorComponent],
    exports: [DotLoadingIndicatorComponent],
    providers: [DotLoadingIndicatorService]
})
export class DotLoadingIndicatorModule {}
