import { NgModule } from '@angular/core';
import { CommonModule } from '@angular/common';

// Components
import { ActionButtonComponent } from './action-button.component';
import { SplitButtonModule } from 'primeng/primeng';

@NgModule({
    declarations: [ActionButtonComponent],
    exports: [ActionButtonComponent],
    imports: [CommonModule, SplitButtonModule]
})

export class ActionButtonModule {

}