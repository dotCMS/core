import { CommonModule } from '@angular/common';
import { NgModule } from '@angular/core';
import { RouterModule } from '@angular/router';

import { dotCategoriesRoutes } from './dot-categories.routes';

@NgModule({
    imports: [CommonModule, RouterModule.forChild(dotCategoriesRoutes)]
})
export class DotCategoriesModule {}
