import { NgClass } from '@angular/common';
import { ChangeDetectionStrategy, Component } from '@angular/core';

import { ButtonModule } from 'primeng/button';
import { ToolbarModule } from 'primeng/toolbar';
import { TooltipModule } from 'primeng/tooltip';

import { DotMessagePipe } from '@dotcms/ui';

import { DotUVELanguageSelectorComponent } from '../dot-uve-language-selector/dot-uve-language-selector.component';
import { DotUVEWorkflowActionsComponent } from '../dot-uve-workflow-actions/dot-uve-workflow-actions.component';

@Component({
    selector: 'dot-uve-toolbar',
    imports: [
        NgClass,
        ToolbarModule,
        ButtonModule,
        TooltipModule,
        DotMessagePipe,
        DotUVELanguageSelectorComponent,
        DotUVEWorkflowActionsComponent
    ],
    templateUrl: './dot-uve-toolbar.component.html',
    styleUrl: './dot-uve-toolbar.component.scss',
    changeDetection: ChangeDetectionStrategy.OnPush
})
export class DotUVEToolbarComponent {}
