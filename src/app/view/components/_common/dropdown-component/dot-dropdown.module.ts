import { NgModule } from '@angular/core';
import { CommonModule } from '@angular/common';
import { ButtonModule } from 'primeng/primeng';
import { DotDropdownComponent } from './dot-dropdown.component';
import { GravatarModule } from '../gravatar/gravatar.module';

@NgModule({
    imports: [
        CommonModule,
        ButtonModule,
        GravatarModule
    ],
    declarations: [
        DotDropdownComponent
    ],
    exports: [
        DotDropdownComponent
    ]
})
export class DotDropdownModule { }
