import { NgModule } from '@angular/core';

import { DotMessagePipeModule, DotMessagePipe } from '@dotcms/ui';
import { DotSafeUrlPipe, DotStringFormatPipe } from '@pipes/index';

import { DotSafeHtmlPipe } from './dot-safe-html/dot-safe-html.pipe';

@NgModule({
    declarations: [DotStringFormatPipe, DotSafeUrlPipe, DotSafeHtmlPipe],
    exports: [DotMessagePipe, DotStringFormatPipe, DotSafeUrlPipe, DotSafeHtmlPipe],
    imports: [DotMessagePipeModule]
})
export class DotPipesModule {}
