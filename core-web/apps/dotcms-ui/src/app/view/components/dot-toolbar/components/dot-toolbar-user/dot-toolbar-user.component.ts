import { combineLatest, map, of } from 'rxjs';

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

import { catchError, startWith } from 'rxjs/operators';

import { DotPropertiesService } from '@dotcms/data-access';
import { FeaturedFlags } from '@dotcms/dotcms-models';
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
    readonly #dotPropertiesService = inject(DotPropertiesService);

    readonly $showReportIssue = signal(false);
    // Flag check lives here (not in the store) because this is meant to be
    // removed after launch — easier to rip out from the component than to
    // unwind from the store wiring.
    readonly vm$ = combineLatest([
        this.store.vm$,
        this.#dotPropertiesService
            .getFeatureFlagWithDefault(FeaturedFlags.FEATURE_FLAG_REPORT_ISSUE_ENABLED, false)
            // startWith renders the toolbar immediately at flag=off; catchError keeps it
            // visible if /api/v1/configuration/config errors instead of taking it down.
            .pipe(
                startWith(false),
                catchError(() => of(false))
            )
    ]).pipe(
        map(([vm, reportIssueEnabled]) => ({
            ...vm,
            items: this.withReportIssueCommand(
                reportIssueEnabled
                    ? vm.items
                    : vm.items.filter((item) => item.id !== 'dot-toolbar-user-link-report-issue')
            )
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
