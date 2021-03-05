import { NgModule } from '@angular/core';
import { CommonModule } from '@angular/common';
import { DotCrumbtrailComponent } from './dot-crumbtrail.component';
import { DotCrumbtrailService } from './service/dot-crumbtrail.service';
import { BreadcrumbModule } from 'primeng/breadcrumb';

@NgModule({
    imports: [CommonModule, BreadcrumbModule],
    declarations: [DotCrumbtrailComponent],
    exports: [DotCrumbtrailComponent],
    providers: [DotCrumbtrailService]
})
export class DotCrumbtrailModule {}
