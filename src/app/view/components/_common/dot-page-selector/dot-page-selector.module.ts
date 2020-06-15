import { NgModule } from '@angular/core';
import { CommonModule } from '@angular/common';
import { DotPageSelectorComponent } from './dot-page-selector.component';
import { DotPageSelectorService } from './service/dot-page-selector.service';
import { AutoCompleteModule } from 'primeng/primeng';
import { FormsModule } from '@angular/forms';
import { DotDirectivesModule } from '@shared/dot-directives.module';
import { MdInputTextModule } from '@directives/md-inputtext/md-input-text.module';
import { DotFieldHelperModule } from '@components/dot-field-helper/dot-field-helper.module';
import { DotPipesModule } from '@pipes/dot-pipes.module';

@NgModule({
    imports: [
        CommonModule,
        AutoCompleteModule,
        FormsModule,
        DotDirectivesModule,
        MdInputTextModule,
        DotFieldHelperModule,
        DotPipesModule
    ],
    declarations: [DotPageSelectorComponent],
    providers: [DotPageSelectorService],
    exports: [DotPageSelectorComponent]
})
export class DotPageSelectorModule {}
