import { NgModule } from '@angular/core';



import { DotSafeHtmlPipe } from './dot-safe-html/dot-safe-html.pipe';

@NgModule({
    declarations: [ DotSafeHtmlPipe],
    exports: [ DotSafeHtmlPipe]
})
export class DotPipesModule {}
