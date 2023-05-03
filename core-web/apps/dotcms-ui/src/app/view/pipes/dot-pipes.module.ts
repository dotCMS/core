import { NgModule } from '@angular/core';

import { DotMessagePipe, DotSafeUrlPipe, DotStringFormatPipe } from '@pipes/index';

import { DotMessagePipeModule } from './dot-message/dot-message-pipe.module';
import { DotSafeHtmlPipe } from './dot-safe-html/dot-safe-html.pipe';

@NgModule({
    declarations: [DotStringFormatPipe, DotSafeUrlPipe, DotSafeHtmlPipe],
    exports: [DotMessagePipe, DotStringFormatPipe, DotSafeUrlPipe, DotSafeHtmlPipe],
    imports: [DotMessagePipeModule]
})
export class DotPipesModule {}
