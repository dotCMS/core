import { NgModule } from '@angular/core';
import { DotMessagePipe, DotSafeUrlPipe, DotStringFormatPipe } from '@pipes/index';

@NgModule({
    declarations: [DotMessagePipe, DotStringFormatPipe, DotSafeUrlPipe],
    exports: [DotMessagePipe, DotStringFormatPipe, DotSafeUrlPipe]
})
export class DotPipesModule {}
