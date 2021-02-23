import { NgModule } from '@angular/core';
import { CommonModule } from '@angular/common';
import { DotRelationshipTreeComponent } from './dot-relationship-tree.component';
import { DotCopyButtonModule } from '@components/dot-copy-button/dot-copy-button.module';
import { DotPipesModule } from '@pipes/dot-pipes.module';
import { DotIconModule } from '@components/_common/dot-icon/dot-icon.module';

@NgModule({
    declarations: [DotRelationshipTreeComponent],
    exports: [DotRelationshipTreeComponent],
    imports: [CommonModule, DotCopyButtonModule, DotPipesModule, DotIconModule]
})
export class DotRelationshipTreeModule {}
