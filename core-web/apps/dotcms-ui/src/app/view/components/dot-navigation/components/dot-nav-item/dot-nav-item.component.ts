import { CommonModule } from '@angular/common';
import {
    Component,
    ElementRef,
    EventEmitter,
    HostBinding,
    HostListener,
    Input,
    Output,
    ViewChild,
    inject
} from '@angular/core';

import { DotMenu, DotMenuItem } from '@dotcms/dotcms-models';
import { DotIconComponent } from '@dotcms/ui';

import {
    LABEL_IMPORTANT_ICON,
    DotRandomIconPipe
} from '../../../../pipes/dot-radom-icon/dot-random-icon.pipe';
import { DotNavIconComponent } from '../dot-nav-icon/dot-nav-icon.component';
import { DotSubNavComponent } from '../dot-sub-nav/dot-sub-nav.component';

@Component({
    selector: 'dot-nav-item',
    templateUrl: './dot-nav-item.component.html',
    styleUrls: ['./dot-nav-item.component.scss'],
    imports: [
        CommonModule,
        DotIconComponent,
        DotSubNavComponent,
        DotNavIconComponent,
        DotRandomIconPipe
    ],
    standalone: true
})
export class DotNavItemComponent {
    private hostElRef = inject(ElementRef);

    @ViewChild('subnav', { static: true }) subnav: DotSubNavComponent;

    @Input() data: DotMenu;

    @Output()
    menuClick: EventEmitter<{ originalEvent: MouseEvent; data: DotMenu; toggleOnly?: boolean }> =
        new EventEmitter();

    @Output()
    itemClick: EventEmitter<{ originalEvent: MouseEvent; data: DotMenuItem }> = new EventEmitter();

    @HostBinding('class.dot-nav-item__collapsed')
    @Input()
    collapsed: boolean;

    customStyles = {};
    mainHeaderHeight = 60;

    private windowHeight = window.innerHeight;
    labelImportantIcon = LABEL_IMPORTANT_ICON;

    @HostListener('mouseleave', ['$event'])
    menuUnhovered() {
        this.resetSubMenuPosition();
    }

    /**
     * Handle click on menu section title
     *
     * @param MouseEvent $event
     * @param DotMenu data
     * @memberof DotNavItemComponent
     */
    clickHandler($event: MouseEvent, data: DotMenu): void {
        this.menuClick.emit({
            originalEvent: $event,
            data: data
        });
    }

    /**
     * Handle toggle click on the last third of the nav item
     * Only toggles the menu open/close state without navigation
     *
     * @param MouseEvent $event
     * @param DotMenu data
     * @memberof DotNavItemComponent
     */
    toggleHandler($event: MouseEvent, data: DotMenu): void {
        $event.stopPropagation();
        this.menuClick.emit({
            originalEvent: $event,
            data: data,
            toggleOnly: true
        });
    }

    /**
     * Align the submenu top or bottom depending of the browser window
     *
     * @memberof DotNavItemComponent
     */
    setSubMenuPosition(): void {
        if (this.collapsed) {
            const [rects] = this.subnav.ul.nativeElement.getClientRects();

            if (window.innerHeight !== this.windowHeight) {
                this.customStyles = {};
                this.windowHeight = window.innerHeight;
            }

            if (
                !this.isThereEnoughBottomSpace(rects.bottom) &&
                !this.isThereEnoughTopSpace(rects.height)
            ) {
                this.customStyles = {
                    'max-height': `${this.windowHeight - this.mainHeaderHeight}px`,
                    overflow: 'auto',
                    top: `-${rects.top - this.mainHeaderHeight - 16}px`
                };
            } else if (!this.isThereEnoughBottomSpace(rects.bottom)) {
                this.customStyles = {
                    bottom: '0',
                    top: 'auto'
                };
            }
        }
    }

    /**
     * Sets styles of the SubMenu with default properties
     *
     * @memberof DotNavItemComponent
     */
    resetSubMenuPosition(): void {
        if (this.collapsed) {
            this.customStyles = {
                overflow: 'hidden'
            };
        }
    }

    /**
     * Handle click in dot-sub-nav items
     *
     * @param { originalEvent: MouseEvent; data: DotMenuItem } $event
     * @memberof DotNavItemComponent
     */
    handleItemClick(event: { originalEvent: MouseEvent; data: DotMenuItem }) {
        this.itemClick.emit(event);
    }

    private isThereEnoughBottomSpace(subMenuBottomY: number): boolean {
        return subMenuBottomY < window.innerHeight;
    }

    private isThereEnoughTopSpace(subMenuHeight: number): boolean {
        const availableTopSpace =
            this.hostElRef.nativeElement.getBoundingClientRect().bottom - this.mainHeaderHeight;

        return availableTopSpace > subMenuHeight;
    }
}
