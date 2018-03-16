import { NgModule } from '@angular/core';
import { CommonModule } from '@angular/common';
import { DropdownModule } from 'primeng/primeng';
import { FormsModule } from '@angular/forms';
import { DotLanguageSelectorComponent } from './dot-language-selector.component';

@NgModule({
    imports: [CommonModule, DropdownModule, FormsModule],
    declarations: [DotLanguageSelectorComponent],
    exports: [DotLanguageSelectorComponent]
})
export class DotLanguageSelectorModule {}
