import { NgModule } from '@angular/core';

import { NgGridPlaceholder } from './../components/NgGridPlaceholder';
import { NgGridItem } from './../directives/NgGridItem';
import { NgGrid } from './../directives/NgGrid';

@NgModule({
    declarations: [NgGrid, NgGridItem, NgGridPlaceholder],
    entryComponents: [NgGridPlaceholder],
    exports: [NgGrid, NgGridItem]
})
export class NgGridModule {}
