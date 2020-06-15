import { NgModule } from '@angular/core';
import { CommonModule } from '@angular/common';
import { InputTextModule } from 'primeng/primeng';

import { DotAppsListComponent } from './dot-apps-list.component';
import { DotAppsCardModule } from './dot-apps-card/dot-apps-card.module';
import { DotAppsService } from '@services/dot-apps/dot-apps.service';
import { DotAppsListResolver } from './dot-apps-list-resolver.service';
import { DotPipesModule } from '@pipes/dot-pipes.module';

@NgModule({
    imports: [InputTextModule, CommonModule, DotAppsCardModule, DotPipesModule],
    declarations: [DotAppsListComponent],
    exports: [DotAppsListComponent],
    providers: [DotAppsService, DotAppsListResolver]
})
export class DotAppsListModule {}
