import { map } from 'rxjs';

import { AsyncPipe } from '@angular/common';
import {
    ChangeDetectionStrategy,
    Component,
    OnInit,
    inject,
    signal,
    viewChild
} from '@angular/core';

import { MenuItem } from 'primeng/api';
import { AvatarModule } from 'primeng/avatar';
import { Menu, MenuModule } from 'primeng/menu';

import { DotGravatarDirective } from '@dotcms/ui';

import { DotToolbarUserStore } from './store/dot-toolbar-user.store';

import { DotReportIssueComponent } from '../../../dot-report-issue/dot-report-issue.component';
import { DotLoginAsComponent } from '../dot-login-as/dot-login-as.component';
import { DotMyAccountComponent } from '../dot-my-account/dot-my-account.component';

@Component({
    providers: [DotToolbarUserStore],
    selector: 'dot-toolbar-user',
    templateUrl: './dot-toolbar-user.component.html',
    changeDetection: ChangeDetectionStrategy.OnPush,
    imports: [
        DotGravatarDirective,
        AvatarModule,
        DotLoginAsComponent,
        DotMyAccountComponent,
        DotReportIssueComponent,
        MenuModule,
        AsyncPipe
    ]
})
export class DotToolbarUserComponent implements OnInit {
    readonly store = inject(DotToolbarUserStore);

    readonly $showReportIssue = signal(false);
    readonly vm$ = this.store.vm$.pipe(
        map((vm) => ({
            ...vm,
            items: this.withReportIssueCommand(vm.items)
        }))
    );
    $menu = viewChild<Menu>('menu');
    $showMask = signal<boolean>(false);

    ngOnInit(): void {
        this.store.init();
    }

    /**
     * Toggle the user menu popup and the backdrop mask.
     *
     * @param event - Click event used by the PrimeNG menu popup.
     */
    toggleMenu(event: Event): void {
        this.$menu().toggle(event);
        this.$showMask.update((value) => !value);
    }

    /**
     * Hide the backdrop mask after the menu closes.
     */
    hideMask(): void {
        this.$showMask.update(() => false);
    }

    /**
     * Open the report issue dialog from the user menu.
     */
    openReportIssue(): void {
        this.$showReportIssue.set(true);
    }

    /**
     * Close the report issue dialog.
     */
    closeReportIssue(): void {
        this.$showReportIssue.set(false);
    }

    /**
     * Attach the local dialog open handler to the report issue menu item.
     *
     * @param items - Menu items emitted by the toolbar user store.
     * @returns A menu model with the report issue command bound locally.
     */
    private withReportIssueCommand(items: MenuItem[]): MenuItem[] {
        return items.map((item) =>
            item.id === 'dot-toolbar-user-link-report-issue'
                ? {
                      ...item,
                      command: () => this.openReportIssue()
                  }
                : item
        );
    }
}
