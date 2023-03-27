import * as md5 from 'md5';
import { Observable } from 'rxjs';

import { AsyncPipe } from '@angular/common';
import { Component, Input, OnChanges, SimpleChanges } from '@angular/core';

import { AvatarModule } from 'primeng/avatar';

import { DotGravatarService } from '@dotcms/app/api/services/dot-gravatar-service';
import { DotAvatarDirective } from '@dotcms/app/view/directives/dot-avatar/dot-avatar.directive';

@Component({
    selector: 'dot-gravatar',
    styleUrls: ['./dot-gravatar.component.scss'],
    templateUrl: './dot-gravatar.component.html',
    standalone: true,
    imports: [AsyncPipe, AvatarModule, DotAvatarDirective]
})
export class DotGravatarComponent implements OnChanges {
    @Input()
    email: string;

    @Input()
    size: number;

    avatarUrl$: Observable<string>;

    constructor(private gravatarService: DotGravatarService) {}

    ngOnChanges(changes: SimpleChanges): void {
        if (changes.email && changes.email.currentValue) {
            const hash = md5(this.email);
            this.avatarUrl$ = this.gravatarService.getPhoto(hash);
        }
    }
}
