import { NgModule } from '@angular/core';
import { DotMessagePipe } from '@pipes/dot-message/dot-message.pipe';

@NgModule({
    declarations: [DotMessagePipe],
    exports: [DotMessagePipe]
})
export class DotPipesModule {}
