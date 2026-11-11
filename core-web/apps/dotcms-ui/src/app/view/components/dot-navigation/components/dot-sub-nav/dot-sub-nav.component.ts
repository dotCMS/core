import { animate, state, style, transition, trigger } from '@angular/animations';
import { CdkMenu, CdkMenuItem, CdkMenuTrigger } from '@angular/cdk/menu';
import {
    ChangeDetectionStrategy,
    Component,
    DestroyRef,
    ElementRef,
    ViewChild,
    computed,
    inject,
    input,
    output,
    signal,
    viewChildren
} from '@angular/core';
import { RouterModule } from '@angular/router';

import { DotMenu, DotMenuItem, MenuGroup } from '@dotcms/dotcms-models';
import { DotMessagePipe } from '@dotcms/ui';

import { DotNavMenuItemDirective, DotNavMenuTriggerDirective } from './dot-nav-menu.directive';

interface NavigationEntry {
    parent?: string;
    items: DotMenuItem[];
    active: boolean;
}

@Component({
    animations: [
        trigger('expandAnimation', [
            state(
                'expanded',
                style({
                    height: '!',
                    overflow: 'hidden'
                })
            ),
            state(
                'collapsed',
                style({
                    height: '0px',
                    overflow: 'hidden'
                })
            ),
            transition('expanded <=> collapsed', animate('250ms ease-in-out'))
        ])
    ],
    selector: 'dot-sub-nav',
    templateUrl: './dot-sub-nav.component.html',
    styleUrls: ['./dot-sub-nav.component.scss'],
    changeDetection: ChangeDetectionStrategy.Eager,
    // Keep the default CDK trigger out of scope; the persistent navigation trigger owns its input.
    imports: [
        CdkMenu,
        CdkMenuItem,
        DotMessagePipe,
        DotNavMenuItemDirective,
        DotNavMenuTriggerDirective,
        RouterModule
    ],
    host: {
        '(document:keydown.escape)': 'closeFlyouts()',
        '[@expandAnimation]': 'getAnimation',
        '[class.dot-nav-sub--flyout-open]': '$flyoutOpen()',
        '[attr.inert]': '!$collapsed() && !$data().isOpen ? "" : null'
    }
})
export class DotSubNavComponent {
    @ViewChild('ul', { static: true }) ul: ElementRef<HTMLUListElement>;

    readonly $data = input.required<DotMenu | MenuGroup>({ alias: 'data' });
    readonly $collapsed = input(false, { alias: 'collapsed' });
    readonly itemClick = output<{ originalEvent: MouseEvent; data: DotMenuItem }>();

    #closeTimeout: ReturnType<typeof setTimeout> | undefined;

    protected readonly $parentMenus = viewChildren(CdkMenuTrigger);
    protected readonly $flyoutOpen = signal(false);
    protected readonly $entries = computed(() => {
        const entries: NavigationEntry[] = [];
        const parents = new Map<string, NavigationEntry>();

        for (const item of this.$data().menuItems) {
            const parent = item.initParams?.['parent']?.trim();
            if (!parent) {
                entries.push({ items: [item], active: item.active });
                continue;
            }

            let group = parents.get(parent);
            if (!group) {
                group = { parent, items: [], active: false };
                parents.set(parent, group);
                entries.push(group);
            }
            group.items.push(item);
            group.active ||= item.active;
        }

        return entries;
    });

    constructor() {
        inject(DestroyRef).onDestroy(() => this.cancelFlyoutClose());
    }

    get getAnimation(): string {
        return !this.$collapsed() && this.$data().isOpen ? 'expanded' : 'collapsed';
    }

    /** Allows the pointer to cross between the parent and its overlay before closing. */
    protected scheduleFlyoutClose(menu: CdkMenuTrigger): void {
        this.cancelFlyoutClose();
        if (menu.isOpen()) {
            this.#closeTimeout = setTimeout(() => menu.close(), 300);
        }
    }

    protected cancelFlyoutClose(): void {
        clearTimeout(this.#closeTimeout);
        this.#closeTimeout = undefined;
    }

    /** Also dismisses hover-opened flyouts when keyboard focus is outside the menu. */
    protected closeFlyouts(): void {
        this.cancelFlyoutClose();
        if (this.$flyoutOpen()) {
            for (const menu of this.$parentMenus()) {
                menu.close();
            }
        }
    }

    /** Emits the original portlet, preserving its layout and routing metadata. */
    onItemClick(event: MouseEvent, item: DotMenuItem): void {
        this.itemClick.emit({ originalEvent: event, data: item });
    }
}
