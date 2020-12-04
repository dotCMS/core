import { NgModule } from '@angular/core';
import { DotMessagePipeModule } from '@pipes/dot-message/dot-message-pipe.module';
import { DotStarterResolver } from './dot-starter-resolver.service';
import { DotStarterRoutingModule } from './dot-starter-routing.module';
import { DotStarterComponent } from './dot-starter.component';

@NgModule({
    declarations: [DotStarterComponent],
    imports: [DotStarterRoutingModule, DotMessagePipeModule],
    providers: [DotStarterResolver]
})
export class DotStarterModule {}
