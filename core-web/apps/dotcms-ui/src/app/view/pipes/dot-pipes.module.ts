import { NgModule } from '@angular/core';
import { DotMessagePipe, DotSafeUrlPipe, DotStringFormatPipe } from '@pipes/index';
import { DotMessagePipeModule } from './dot-message/dot-message-pipe.module';

@NgModule({
    declarations: [DotStringFormatPipe, DotSafeUrlPipe],
    exports: [DotMessagePipe, DotStringFormatPipe, DotSafeUrlPipe],
    imports: [DotMessagePipeModule]
})
export class DotPipesModule {}
