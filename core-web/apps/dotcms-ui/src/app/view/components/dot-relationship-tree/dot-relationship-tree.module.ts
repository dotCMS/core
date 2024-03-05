import { CommonModule } from '@angular/common';
import { NgModule } from '@angular/core';

import { DotCopyButtonComponent, DotIconModule } from '@dotcms/ui';

import { DotRelationshipTreeComponent } from './dot-relationship-tree.component';

import { DotPipesModule } from '../../pipes/dot-pipes.module';

@NgModule({
    declarations: [DotRelationshipTreeComponent],
    exports: [DotRelationshipTreeComponent],
    imports: [CommonModule, DotCopyButtonComponent, DotPipesModule, DotIconModule]
})
export class DotRelationshipTreeModule {}
