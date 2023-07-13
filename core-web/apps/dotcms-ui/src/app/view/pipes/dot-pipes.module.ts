import { NgModule } from '@angular/core';

import { DotSafeUrlPipe, DotStringFormatPipe } from '@pipes/index';

import { DotSafeHtmlPipe } from './dot-safe-html/dot-safe-html.pipe';

@NgModule({
    declarations: [DotStringFormatPipe, DotSafeUrlPipe, DotSafeHtmlPipe],
    exports: [DotStringFormatPipe, DotSafeUrlPipe, DotSafeHtmlPipe]
})
export class DotPipesModule {}
