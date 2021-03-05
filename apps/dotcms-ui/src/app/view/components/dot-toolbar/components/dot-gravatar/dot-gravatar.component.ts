import { Component, Input, OnChanges, SimpleChanges } from '@angular/core';
import { DotGravatarService } from '@services/dot-gravatar-service';
import * as md5 from 'md5';
import { Observable } from 'rxjs';

@Component({
    selector: 'dot-gravatar',
    styleUrls: ['./dot-gravatar.component.scss'],
    templateUrl: './dot-gravatar.component.html'
})
export class DotGravatarComponent implements OnChanges {
    @Input()
    email: string;

    @Input()
    size: number;

    avatarUrl: Observable<string>;

    constructor(private gravatarService: DotGravatarService) {}

    ngOnChanges(changes: SimpleChanges): void {
        if (changes.email && changes.email.currentValue) {
            const hash = md5(this.email);
            this.avatarUrl = this.gravatarService.getPhoto(hash);
        }
    }
}
