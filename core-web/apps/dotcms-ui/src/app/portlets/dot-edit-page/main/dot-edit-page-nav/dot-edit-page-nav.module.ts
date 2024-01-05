import { CommonModule } from '@angular/common';
import { NgModule } from '@angular/core';
import { RouterModule } from '@angular/router';

import { TooltipModule } from 'primeng/tooltip';

//TODO: Remove @nx/enforce-module-boundaries exception
// The static import of the lazy-loaded 'portlets-dot-ema' library in the following line is an exception to the general rule.
// This library is usually lazy loaded as specified in 'apps/dotcms-ui/src/app/app-routing.module.ts'.
// However, in this particular instance, we need to statically import something from this library.
// The problem here is this static import would have violated the '@nx/enforce-module-boundaries' rule set by Nx,
// eslint-disable-next-line @nx/enforce-module-boundaries
import { DotPageToolsSeoComponent } from '@dotcms/portlets/dot-ema';
import { DotIconModule, DotMessagePipe } from '@dotcms/ui';
import { DotPipesModule } from '@pipes/dot-pipes.module';

import { DotEditPageNavComponent } from './dot-edit-page-nav.component';

@NgModule({
    imports: [
        CommonModule,
        RouterModule,
        TooltipModule,
        DotIconModule,
        DotPipesModule,
        DotMessagePipe,
        DotPageToolsSeoComponent
    ],
    declarations: [DotEditPageNavComponent],
    exports: [DotEditPageNavComponent]
})
export class DotEditPageNavModule {}
