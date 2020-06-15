import { NgModule } from '@angular/core';
import { CommonModule } from '@angular/common';
import { DropdownModule } from 'primeng/primeng';
import { FormsModule } from '@angular/forms';
import { DotLanguageSelectorComponent } from './dot-language-selector.component';
import { DotIconModule } from '@components/_common/dot-icon/dot-icon.module';
import { DotLanguagesService } from '@services/dot-languages/dot-languages.service';
import { DotPipesModule } from '@pipes/dot-pipes.module';

@NgModule({
    imports: [CommonModule, DropdownModule, FormsModule, DotIconModule, DotPipesModule],
    declarations: [DotLanguageSelectorComponent],
    exports: [DotLanguageSelectorComponent],
    providers: [DotLanguagesService]
})
export class DotLanguageSelectorModule {}
