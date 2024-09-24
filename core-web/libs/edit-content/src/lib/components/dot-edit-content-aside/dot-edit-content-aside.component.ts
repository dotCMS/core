import { animate, state, style, transition, trigger } from '@angular/animations';
import { NgClass } from '@angular/common';
import {
    ChangeDetectionStrategy,
    Component,
    EventEmitter,
    HostBinding,
    Input,
    Output
} from '@angular/core';

import { SidebarModule } from 'primeng/sidebar';

import { DotCMSContentType, DotCMSContentlet } from '@dotcms/dotcms-models';
import { DotMessagePipe } from '@dotcms/ui';

import { DotContentAsideInformationComponent } from './components/dot-content-aside-information/dot-content-aside-information.component';
import { DotContentAsideWorkflowComponent } from './components/dot-content-aside-workflow/dot-content-aside-workflow.component';

@Component({
    selector: 'dot-edit-content-aside',
    standalone: true,
    templateUrl: './dot-edit-content-aside.component.html',
    styleUrls: ['./dot-edit-content-aside.component.scss'],
    changeDetection: ChangeDetectionStrategy.OnPush,
    imports: [
        DotMessagePipe,
        DotContentAsideInformationComponent,
        DotContentAsideWorkflowComponent,
        SidebarModule,
        NgClass
    ],
    animations: [
        trigger('collapseAnimation', [
            state(
                'true',
                style({
                    width: '0px',
                    visibility: 'hidden'
                })
            ),
            state(
                'false',
                style({
                    width: '350px',
                    visibility: 'visible'
                })
            ),
            transition('closed <=> open', animate('300ms ease-in-out'))
        ])
    ],
    host: {
        '[class.dot-edit-content-aside--open]': 'collapsed',
        '[class.dot-edit-content-aside--closed]': '!collapsed'
    }
})
export class DotEditContentAsideComponent {
    @Input() contentlet!: DotCMSContentlet;
    @Input() contentType!: DotCMSContentType;
    @Input() loading!: boolean;
    @Input() collapsed: boolean;
    @Output() toggle: EventEmitter<boolean> = new EventEmitter();

    // @HostBinding('@collapseAnimation') get sidebarState() {
    //     console.log('collapseAnimation', this.collapsed);
    //
    //     return this.collapsed;
    // }
    //
    @HostBinding('class.collapsed') get isCollapsedClass() {
        return !this.collapsed;
    }

    toggleSidebar() {
        this.collapsed = !this.collapsed;
        this.toggle.emit(this.collapsed);
    }
}
