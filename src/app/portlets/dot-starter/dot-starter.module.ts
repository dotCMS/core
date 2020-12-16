import { NgModule } from '@angular/core';
import { DotMessagePipeModule } from '@pipes/dot-message/dot-message-pipe.module';
import { DotStarterResolver } from './dot-starter-resolver.service';
import { DotStarterRoutingModule } from './dot-starter-routing.module';
import { DotStarterComponent } from './dot-starter.component';
import { CheckboxModule } from 'primeng/checkbox';
import { DotToolGroupService } from '@services/dot-tool-group/dot-tool-group.service';

@NgModule({
    declarations: [DotStarterComponent],
    imports: [DotStarterRoutingModule, DotMessagePipeModule, CheckboxModule],
    providers: [DotStarterResolver, DotToolGroupService]
})
export class DotStarterModule {}
