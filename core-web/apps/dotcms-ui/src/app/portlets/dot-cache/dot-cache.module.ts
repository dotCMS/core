import { CommonModule } from '@angular/common';
import { NgModule } from '@angular/core';

import { DotCacheService } from '@services/dot-containers/dot-cache.service';

import { DotCacheRoutingModule } from './dot-cache-routing.module';

@NgModule({
    imports: [CommonModule, DotCacheRoutingModule],
    providers: [DotCacheService]
})
export class DotCacheModule {}
