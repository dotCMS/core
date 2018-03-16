import { NgModule } from '@angular/core';
import { CommonModule } from '@angular/common';
import { DotPersonaSelectorComponent } from './dot-persona-selector.component';
import { DropdownModule } from 'primeng/primeng';
import { FormsModule } from '@angular/forms';

@NgModule({
    imports: [CommonModule, DropdownModule, FormsModule],
    declarations: [DotPersonaSelectorComponent],
    exports: [DotPersonaSelectorComponent]
})
export class DotPersonaSelectorModule {}
