import { CommonModule } from '@angular/common';
import {
    ChangeDetectionStrategy,
    Component,
    ContentChild,
    TemplateRef,
    input
} from '@angular/core';

/**
 *  Component that renders a section with a title and an optional action template.
 */
@Component({
    selector: 'dot-edit-content-sidebar-section',
    imports: [CommonModule],
    templateUrl: './dot-edit-content-sidebar-section.component.html',
    changeDetection: ChangeDetectionStrategy.OnPush,
    host: {
        class: 'flex flex-col'
    }
})
export class DotEditContentSidebarSectionComponent {
    /**
     * The title of the section.
     */
    $title = input<string | null>(null, { alias: 'title' });

    /**
     * The action template for the section.
     */
    @ContentChild('sectionAction')
    actionTemplate: TemplateRef<unknown>;
}
