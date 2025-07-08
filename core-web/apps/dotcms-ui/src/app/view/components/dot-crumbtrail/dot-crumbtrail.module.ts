import { CommonModule } from '@angular/common';
import { NgModule } from '@angular/core';

import { DotCollapseBreadcrumbComponent } from '@dotcms/ui';

import { DotCrumbtrailComponent } from './dot-crumbtrail.component';
import { DotCrumbtrailService } from './service/dot-crumbtrail.service';

@NgModule({
    imports: [CommonModule, DotCollapseBreadcrumbComponent],
    declarations: [DotCrumbtrailComponent],
    exports: [DotCrumbtrailComponent],
    providers: [DotCrumbtrailService]
})
export class DotCrumbtrailModule {}
