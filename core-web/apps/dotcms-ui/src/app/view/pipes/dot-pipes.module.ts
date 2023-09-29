import { NgModule } from '@angular/core';

import { DotStringFormatPipe } from '@pipes/index';

import { DotSafeHtmlPipe } from './dot-safe-html/dot-safe-html.pipe';

@NgModule({
    declarations: [DotStringFormatPipe, DotSafeHtmlPipe],
    exports: [DotStringFormatPipe, DotSafeHtmlPipe]
})
export class DotPipesModule {}
