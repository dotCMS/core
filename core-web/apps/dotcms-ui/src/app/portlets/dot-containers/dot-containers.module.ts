import { CommonModule } from '@angular/common';
import { NgModule } from '@angular/core';

import { DotContainersService } from '@dotcms/app/api/services/dot-containers/dot-containers.service';

import { DotContainersRoutingModule } from './dot-containers-routing.module';

@NgModule({
    imports: [CommonModule, DotContainersRoutingModule],
    providers: [DotContainersService]
})
export class DotContainersModule {}
