import { DotLayoutSidebarComponent } from './dot-layout-property-sidebar.component';
import { CommonModule } from '@angular/common';
import { NgModule } from '@angular/core';
import { DotLayoutPropertiesItemModule } from '../dot-layout-properties-item/dot-layout-properties-item.module';
import { FormsModule } from '@angular/forms';
import { DotPipesModule } from '@pipes/dot-pipes.module';

@NgModule({
    declarations: [DotLayoutSidebarComponent],
    imports: [CommonModule, DotLayoutPropertiesItemModule, FormsModule, DotPipesModule],
    exports: [DotLayoutSidebarComponent],
    providers: []
})
export class DotLayoutSidebarModule {}
