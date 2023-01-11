import { CommonModule } from '@angular/common';
import { NgModule } from '@angular/core';

import { DotCopyButtonModule } from '@components/dot-copy-button/dot-copy-button.module';
import { DotIconModule } from '@dotcms/ui';
import { DotPipesModule } from '@pipes/dot-pipes.module';

import { DotRelationshipTreeComponent } from './dot-relationship-tree.component';

@NgModule({
    declarations: [DotRelationshipTreeComponent],
    exports: [DotRelationshipTreeComponent],
    imports: [CommonModule, DotCopyButtonModule, DotPipesModule, DotIconModule]
})
export class DotRelationshipTreeModule {}
