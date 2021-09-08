import { ButtonModule } from 'primeng/button';
import { SearchableDropDownModule } from '../_common/searchable-dropdown/searchable-dropdown.module';
import { FormsModule } from '@angular/forms';
import { CommonModule } from '@angular/common';
import { DotContainerSelectorComponent } from './dot-container-selector.component';
import { NgModule } from '@angular/core';
import { PaginatorService } from '@services/paginator';
import { DotPipesModule } from '@pipes/dot-pipes.module';
import { UiDotIconButtonModule } from '@components/_common/dot-icon-button/dot-icon-button.module';

@NgModule({
    declarations: [DotContainerSelectorComponent],
    exports: [DotContainerSelectorComponent],
    imports: [
        CommonModule,
        FormsModule,
        ButtonModule,
        SearchableDropDownModule,
        DotPipesModule,
        UiDotIconButtonModule
    ],
    providers: [PaginatorService]
})
export class DotContainerSelectorModule {}
