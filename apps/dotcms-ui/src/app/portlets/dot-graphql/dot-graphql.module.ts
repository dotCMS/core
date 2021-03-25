import { NgModule } from '@angular/core';
import { CommonModule } from '@angular/common';
import { DotGraphqlComponent } from './dot-graphql.component';
import { DotGraphqlRoutingModule } from './dot-graphql-routing.module';
import { DotPortletBaseModule } from '@components/dot-portlet-base/dot-portlet-base.module';
import { DotMessagePipeModule } from '../../view/pipes/dot-message/dot-message-pipe.module';

@NgModule({
    declarations: [DotGraphqlComponent],
    imports: [CommonModule, DotGraphqlRoutingModule, DotPortletBaseModule, DotMessagePipeModule],
    providers: []
})
export class DotGraphqlModule {}
