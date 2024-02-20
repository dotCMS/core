import { CommonModule } from '@angular/common';
import { NgModule } from '@angular/core';

import { DotCopyButtonComponent, DotIconModule } from '@dotcms/ui';
import { DotPipesModule } from '@pipes/dot-pipes.module';

import { DotRelationshipTreeComponent } from './dot-relationship-tree.component';

@NgModule({
    declarations: [DotRelationshipTreeComponent],
    exports: [DotRelationshipTreeComponent],
    imports: [CommonModule, DotCopyButtonComponent, DotPipesModule, DotIconModule]
})
export class DotRelationshipTreeModule {}
