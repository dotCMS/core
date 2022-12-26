import { NgModule } from '@angular/core';
import { CommonModule } from '@angular/common';

import { DotCategoriesRoutingModule } from './dot-categories-routing.module';
import { DotCategoriesUtillService } from '@dotcms/app/api/services/dot-categories/dot-categories-utill.service';

@NgModule({
    imports: [CommonModule, DotCategoriesRoutingModule],
    providers: [DotCategoriesUtillService]
})
export class DotCategoriesModule {}
