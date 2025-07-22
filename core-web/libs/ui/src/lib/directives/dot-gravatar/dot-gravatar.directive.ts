import md5 from 'md5';

import { ChangeDetectorRef, Directive, input, OnInit, inject } from '@angular/core';

import { Avatar } from 'primeng/avatar';

const FALLBACK_AVATAR_LETTER = 'A';
const DEFAULT_AVATAR_SHAPE = 'circle';
/**
 * Directive that adds Gravatar functionality to PrimeNG Avatar component.
 * It generates a Gravatar URL from an email and applies it to the avatar.
 * If no email is provided, displays a default character.
 */
@Directive({
    selector: 'p-avatar[dotGravatar]',
    standalone: true
})
export class DotGravatarDirective implements OnInit {
    private avatar = inject(Avatar);
    private cd = inject(ChangeDetectorRef);

    email = input<string>('');

    private readonly GRAVATAR_URL = 'https://www.gravatar.com/avatar/';
    private readonly DEFAULT_SIZE = 48;
    private readonly DEFAULT_RATING = 'g';

    constructor() {
        this.avatar.shape = DEFAULT_AVATAR_SHAPE;
    }

    ngOnInit(): void {
        const email = this.email();

        if (!email) {
            this.setFallbackAvatar(FALLBACK_AVATAR_LETTER);

            return;
        }

        const hash = md5(email.trim().toLowerCase());
        const gravatarUrl = `${this.GRAVATAR_URL}${hash}?s=${this.DEFAULT_SIZE}&r=${this.DEFAULT_RATING}`;

        this.avatar.image = gravatarUrl;
        this.cd.detectChanges();
    }

    private setFallbackAvatar(letter: string): void {
        this.avatar.image = null;
        this.avatar.label = letter;
        this.cd.detectChanges();
    }
}
