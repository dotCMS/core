import { CommonModule } from '@angular/common';
import { NgModule } from '@angular/core';
import { FormsModule } from '@angular/forms';

import { InputSwitchModule } from 'primeng/inputswitch';
import { MenuModule } from 'primeng/menu';
import { SelectButtonModule } from 'primeng/selectbutton';
import { TooltipModule } from 'primeng/tooltip';

import { DotContentletEditorModule } from '@components/dot-contentlet-editor/dot-contentlet-editor.module';
import { DotMessagePipe, DotSafeHtmlPipe, DotTabButtonsComponent } from '@dotcms/ui';

import { DotEditPageLockInfoComponent } from './components/dot-edit-page-lock-info/dot-edit-page-lock-info.component';
import { DotEditPageStateControllerComponent } from './dot-edit-page-state-controller.component';

@NgModule({
    declarations: [DotEditPageStateControllerComponent, DotEditPageLockInfoComponent],
    exports: [DotEditPageStateControllerComponent],
    imports: [
        CommonModule,
        FormsModule,
        InputSwitchModule,
        SelectButtonModule,
        DotSafeHtmlPipe,
        TooltipModule,
        DotMessagePipe,
        DotTabButtonsComponent,
        MenuModule,
        DotContentletEditorModule
    ]
})
export class DotEditPageStateControllerModule {}
