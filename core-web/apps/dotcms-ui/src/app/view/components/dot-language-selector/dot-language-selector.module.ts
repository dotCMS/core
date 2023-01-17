import { CommonModule } from '@angular/common';
import { NgModule } from '@angular/core';
import { FormsModule } from '@angular/forms';

import { DropdownModule } from 'primeng/dropdown';

import { DotLanguagesService } from '@dotcms/data-access';
import { DotIconModule } from '@dotcms/ui';
import { DotPipesModule } from '@pipes/dot-pipes.module';

import { DotLanguageSelectorComponent } from './dot-language-selector.component';

@NgModule({
    imports: [CommonModule, DropdownModule, FormsModule, DotIconModule, DotPipesModule],
    declarations: [DotLanguageSelectorComponent],
    exports: [DotLanguageSelectorComponent],
    providers: [DotLanguagesService]
})
export class DotLanguageSelectorModule {}
