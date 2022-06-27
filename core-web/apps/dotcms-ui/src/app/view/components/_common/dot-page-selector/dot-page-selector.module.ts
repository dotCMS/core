import { NgModule } from '@angular/core';
import { CommonModule } from '@angular/common';
import { DotPageSelectorComponent } from './dot-page-selector.component';
import { DotPageSelectorService } from './service/dot-page-selector.service';
import { AutoCompleteModule } from 'primeng/autocomplete';
import { FormsModule } from '@angular/forms';
import { DotDirectivesModule } from '@shared/dot-directives.module';
import { DotFieldHelperModule } from '@components/dot-field-helper/dot-field-helper.module';
import { DotPipesModule } from '@pipes/dot-pipes.module';

@NgModule({
    imports: [
        CommonModule,
        AutoCompleteModule,
        FormsModule,
        DotDirectivesModule,
        DotFieldHelperModule,
        DotPipesModule
    ],
    declarations: [DotPageSelectorComponent],
    providers: [DotPageSelectorService],
    exports: [DotPageSelectorComponent]
})
export class DotPageSelectorModule {}
