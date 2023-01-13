import { CommonModule } from '@angular/common';
import { NgModule } from '@angular/core';

import { BreadcrumbModule } from 'primeng/breadcrumb';

import { DotCrumbtrailComponent } from './dot-crumbtrail.component';
import { DotCrumbtrailService } from './service/dot-crumbtrail.service';

@NgModule({
    imports: [CommonModule, BreadcrumbModule],
    declarations: [DotCrumbtrailComponent],
    exports: [DotCrumbtrailComponent],
    providers: [DotCrumbtrailService]
})
export class DotCrumbtrailModule {}
