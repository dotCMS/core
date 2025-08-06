import { CommonModule } from '@angular/common';
import { NgModule } from '@angular/core';

import { DotContainersRoutingModule } from './dot-containers-routing.module';

import { DotContainersService } from '../../api/services/dot-containers/dot-containers.service';

@NgModule({
    imports: [CommonModule, DotContainersRoutingModule],
    providers: [DotContainersService]
})
export class DotContainersModule {}
