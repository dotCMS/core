import { ButtonModule } from 'primeng/button';
import { SearchableDropDownModule } from '../_common/searchable-dropdown/searchable-dropdown.module';
import { FormsModule } from '@angular/forms';
import { CommonModule } from '@angular/common';
import { DotContainerSelectorLayoutComponent } from './dot-container-selector-layout.component';
import { NgModule } from '@angular/core';
import { PaginatorService } from '@services/paginator';
import { DotPipesModule } from '@pipes/dot-pipes.module';
import { UiDotIconButtonModule } from '@components/_common/dot-icon-button/dot-icon-button.module';
import { DotContainerSelectorModule } from '@components/dot-container-selector/dot-container-selector.module';

@NgModule({
    declarations: [DotContainerSelectorLayoutComponent],
    exports: [DotContainerSelectorLayoutComponent],
    imports: [
        CommonModule,
        FormsModule,
        ButtonModule,
        SearchableDropDownModule,
        DotPipesModule,
        UiDotIconButtonModule,
        DotContainerSelectorModule
    ],
    providers: [PaginatorService]
})
export class DotContainerSelectorLayoutModule {}
