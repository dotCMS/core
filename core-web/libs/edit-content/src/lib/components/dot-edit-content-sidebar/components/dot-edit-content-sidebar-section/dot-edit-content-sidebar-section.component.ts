import { CommonModule } from '@angular/common';
import {
    ChangeDetectionStrategy,
    Component,
    ContentChild,
    TemplateRef,
    input
} from '@angular/core';

@Component({
    selector: 'dot-edit-content-sidebar-section',
    standalone: true,
    imports: [CommonModule],
    templateUrl: './dot-edit-content-sidebar-section.component.html',
    styleUrl: './dot-edit-content-sidebar-section.component.scss',
    changeDetection: ChangeDetectionStrategy.OnPush
})
export class DotEditContentSidebarSectionComponent {
    title = input.required<string>();

    @ContentChild('sectionAction')
    actionTemplate?: TemplateRef<unknown>;
}
