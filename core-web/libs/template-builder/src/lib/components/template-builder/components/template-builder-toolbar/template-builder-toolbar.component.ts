import { CommonModule } from '@angular/common';
import { ChangeDetectionStrategy, Component } from '@angular/core';

import { ToolbarModule } from 'primeng/toolbar';

import { AddWidgetComponent } from '../add-widget/add-widget.component';

@Component({
    selector: 'dotcms-template-builder-toolbar',
    standalone: true,
    imports: [CommonModule, AddWidgetComponent, ToolbarModule],
    templateUrl: './template-builder-toolbar.component.html',
    styleUrls: ['./template-builder-toolbar.component.scss'],
    changeDetection: ChangeDetectionStrategy.OnPush
})
export class TemplateBuilderToolbarComponent {}
