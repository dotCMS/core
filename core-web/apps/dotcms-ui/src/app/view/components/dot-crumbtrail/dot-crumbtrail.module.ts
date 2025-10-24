import { CommonModule } from '@angular/common';
import { NgModule } from '@angular/core';

import { DotCollapseBreadcrumbComponent } from '@dotcms/ui';

import { DotCrumbtrailComponent } from './dot-crumbtrail.component';

@NgModule({
    imports: [CommonModule, DotCollapseBreadcrumbComponent],
    declarations: [DotCrumbtrailComponent],
    exports: [DotCrumbtrailComponent]
})
export class DotCrumbtrailModule {}
