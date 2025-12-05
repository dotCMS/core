import { CommonModule } from '@angular/common';
import {
    Component,
    ElementRef,
    HostListener,
    ViewChild,
    inject,
    input,
    output
} from '@angular/core';

import { DotMenuItem, MenuGroup } from '@dotcms/dotcms-models';
import { GlobalStore } from '@dotcms/store';

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
        DotSubNavComponent,
        DotNavIconComponent,
        DotRandomIconPipe
    ],
    host: {
        '[class.dot-nav-item__collapsed]': '$collapsed()'
    }
})
export class DotNavItemComponent {
    private hostElRef = inject(ElementRef);

    @ViewChild('subnav', { static: true }) subnav: DotSubNavComponent;

    readonly #globalStore = inject(GlobalStore);

    $data = input.required<MenuGroup>({ alias: 'data' });

    menuClick = output<{
        originalEvent: MouseEvent;
        data: MenuGroup;
        toggleOnly?: boolean;
    }>();

    itemClick = output<{ originalEvent: MouseEvent; data: DotMenuItem }>();

    $collapsed = input.required<boolean>({ alias: 'collapsed' });

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
     * @param MenuGroup data
     * @memberof DotNavItemComponent
     */
    clickHandler($event: MouseEvent, data: MenuGroup): void {
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
     * @param MenuGroup data
     * @memberof DotNavItemComponent
     */
    toggleHandler($event: MouseEvent, data: MenuGroup): void {
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
        if (this.$collapsed()) {
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
        if (this.$collapsed()) {
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
