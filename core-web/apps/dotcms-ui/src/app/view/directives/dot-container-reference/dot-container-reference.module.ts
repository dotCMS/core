import { CommonModule } from '@angular/common';
import { NgModule } from '@angular/core';

import { DotContainerReferenceDirective } from '@directives/dot-container-reference/dot-container-reference.directive';

@NgModule({
    imports: [CommonModule],
    declarations: [DotContainerReferenceDirective],
    exports: [DotContainerReferenceDirective]
})
export class DotContainerReferenceModule {}
