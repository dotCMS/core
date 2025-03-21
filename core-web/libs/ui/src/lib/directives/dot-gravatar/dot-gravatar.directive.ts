import { ChangeDetectorRef, Directive, input, OnInit } from '@angular/core';

import { Avatar } from 'primeng/avatar';

import { DotGravatarService } from '../../services/dot-gravatar/dot-gravatar.service';

/**
 * Directive that adds Gravatar functionality to PrimeNG Avatar component.
 * It fetches the user's Gravatar photo using their email and displays it in the avatar.
 * If no Gravatar is found or there's an error, it displays the first letter of the email as a fallback.
 */
@Directive({
    selector: 'p-avatar[dotGravatar]',
    standalone: true,
    providers: [DotGravatarService]
})
export class DotGravatarDirective implements OnInit {
    email = input<string>();

    constructor(
        private avatar: Avatar,
        private cd: ChangeDetectorRef,
        private gravatarService: DotGravatarService
    ) {
        this.avatar.shape = 'circle';
    }

    ngOnInit(): void {
        this.gravatarService.getPhoto(this.email()).subscribe(
            (url: string) => {
                this.avatar.image = url;
                this.cd.detectChanges();
            },
            () => {
                this.avatar.label = this.email()[0]?.toUpperCase();
                this.cd.detectChanges();
            }
        );
    }
}
