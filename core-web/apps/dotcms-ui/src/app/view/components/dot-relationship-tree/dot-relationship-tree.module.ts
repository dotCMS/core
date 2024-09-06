import { CommonModule } from '@angular/common';
import { NgModule } from '@angular/core';

import { DotCopyButtonComponent, DotIconModule, DotSafeHtmlPipe } from '@dotcms/ui';

import { DotRelationshipTreeComponent } from './dot-relationship-tree.component';

@NgModule({
    declarations: [DotRelationshipTreeComponent],
    exports: [DotRelationshipTreeComponent],
    imports: [CommonModule, DotCopyButtonComponent, DotSafeHtmlPipe, DotIconModule]
})
export class DotRelationshipTreeModule {}
