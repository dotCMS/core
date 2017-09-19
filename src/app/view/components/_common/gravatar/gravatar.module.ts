import { NgModule } from '@angular/core';
import { CommonModule } from '@angular/common';

import { GravatarComponent } from './gravatar.component';

@NgModule({
    imports: [
        CommonModule
    ],
    declarations: [
        GravatarComponent
    ],
    exports: [
        GravatarComponent
    ]
})
export class GravatarModule { }
