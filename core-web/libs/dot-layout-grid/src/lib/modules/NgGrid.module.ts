import { NgModule } from '@angular/core';

import { NgGridPlaceholder } from './../components/NgGridPlaceholder';
import { NgGrid } from './../directives/NgGrid';
import { NgGridItem } from './../directives/NgGridItem';

@NgModule({
    declarations: [NgGrid, NgGridItem, NgGridPlaceholder],
    exports: [NgGrid, NgGridItem]
})
export class NgGridModule {}
