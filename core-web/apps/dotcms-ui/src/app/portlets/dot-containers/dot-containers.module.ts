import { NgModule } from '@angular/core';
import { CommonModule } from '@angular/common';

import { DotContainersRoutingModule } from './dot-containers-routing.module';
import { DotContainersService } from '@dotcms/app/api/services/dot-containers/dot-containers.service';

@NgModule({
    imports: [CommonModule, DotContainersRoutingModule],
    providers: [DotContainersService]
})
export class DotContainersModule {}
