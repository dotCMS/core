import { AsyncPipe, NgIf } from '@angular/common';
import { ChangeDetectionStrategy, Component, OnInit, ViewChild, signal } from '@angular/core';

import { AvatarModule } from 'primeng/avatar';
import { Menu, MenuModule } from 'primeng/menu';

import { DotGravatarDirective } from '@directives/dot-gravatar/dot-gravatar.directive';
import { DotPipesModule } from '@dotcms/app/view/pipes/dot-pipes.module';

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
        DotPipesModule,
        MenuModule,
        AsyncPipe,
        NgIf
    ]
})
export class DotToolbarUserComponent implements OnInit {
    vm$ = this.store.vm$;
    @ViewChild('menu') menu: Menu;
    showMask = signal<boolean>(false);
    constructor(private store: DotToolbarUserStore) {}

    ngOnInit(): void {
        this.store.init();
    }

    toogleMenu(event: CustomEvent): void {
        this.menu.toggle(event);
        this.showMask.update((value) => !value);
    }
}
