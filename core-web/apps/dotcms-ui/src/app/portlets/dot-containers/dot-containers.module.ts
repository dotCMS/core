import { CommonModule } from '@angular/common';
import { NgModule } from '@angular/core';

import { DotContainersService } from '../../api/services/dot-containers/dot-containers.service';

@NgModule({
    imports: [CommonModule],
    providers: [DotContainersService]
})
export class DotContainersModule {}
