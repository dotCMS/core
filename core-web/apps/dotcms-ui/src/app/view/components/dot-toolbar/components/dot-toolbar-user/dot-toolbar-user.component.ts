import { AsyncPipe, NgIf } from '@angular/common';
import { ChangeDetectionStrategy, Component, OnInit } from '@angular/core';

import { AvatarModule } from 'primeng/avatar';
import { MenuModule } from 'primeng/menu';

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

    constructor(private store: DotToolbarUserStore) {}

    ngOnInit(): void {
        this.store.init();
    }
}
