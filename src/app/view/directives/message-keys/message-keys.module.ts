import { NgModule } from '@angular/core';
import { CommonModule } from '@angular/common';

import { MessageKeyDirective } from './message-keys.directive';

@NgModule({
    imports: [
        CommonModule
    ],
    declarations: [
        MessageKeyDirective
    ],
    exports: [
        MessageKeyDirective
    ]
})
export class MessageKeysModule { }
