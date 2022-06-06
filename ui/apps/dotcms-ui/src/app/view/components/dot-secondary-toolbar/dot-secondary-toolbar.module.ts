import { NgModule } from '@angular/core';
import { CommonModule } from '@angular/common';
import { DotSecondaryToolbarComponent } from '@components/dot-secondary-toolbar/dot-secondary-toolbar.component';

@NgModule({
    imports: [CommonModule],
    declarations: [DotSecondaryToolbarComponent],
    exports: [DotSecondaryToolbarComponent],
    providers: []
})
export class DotSecondaryToolbarModule {}
