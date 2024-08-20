import { AsyncPipe } from '@angular/common';
import {
    ChangeDetectionStrategy,
    Component,
    OnInit,
    inject,
    signal,
    viewChild
} from '@angular/core';

import { AvatarModule } from 'primeng/avatar';
import { Menu, MenuModule } from 'primeng/menu';

import { DotGravatarDirective } from '@directives/dot-gravatar/dot-gravatar.directive';
import { DotSafeHtmlPipe } from '@dotcms/ui';

import { DotToolbarUserStore } from './store/dot-toolbar-user.store';

import { DotLoginAsModule } from '../dot-login-as/dot-login-as.module';
import { DotMyAccountModule } from '../dot-my-account/dot-my-account.module';

@Component({
    providers: [DotToolbarUserStore],
    selector: 'dot-toolbar-user',
    styleUrls: ['./dot-toolbar-user.component.scss'],
    templateUrl: 'dot-toolbar-user.component.html',
    changeDetection: ChangeDetectionStrategy.OnPush,
    standalone: true,
    imports: [
        DotGravatarDirective,
        AvatarModule,
        DotLoginAsModule,
        DotMyAccountModule,
        DotSafeHtmlPipe,
        MenuModule,
        AsyncPipe
    ]
})
export class DotToolbarUserComponent implements OnInit {
    readonly store = inject(DotToolbarUserStore);

    vm$ = this.store.vm$;
    $menu = viewChild<Menu>('menu');
    $showMask = signal<boolean>(false);

    ngOnInit(): void {
        this.store.init();
    }

    toggleMenu(event: Event): void {
        this.$menu().toggle(event);
        this.$showMask.update((value) => !value);
    }

    hideMask(): void {
        this.$showMask.update(() => false);
    }
}
