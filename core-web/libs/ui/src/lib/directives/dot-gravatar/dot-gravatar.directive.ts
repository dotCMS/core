import { signalMethod } from '@ngrx/signals';
import md5 from 'md5';

import { ChangeDetectorRef, Directive, input, inject, computed } from '@angular/core';
import { takeUntilDestroyed } from '@angular/core/rxjs-interop';

import { Avatar } from 'primeng/avatar';

/**
 * Default letter to display when no email or Gravatar is available.
 */
const FALLBACK_AVATAR_LETTER = 'A';
/**
 * Directive that adds Gravatar functionality to PrimeNG Avatar component.
 * It generates a Gravatar URL from an email and applies it to the avatar.
 * If no email is provided, displays a default character.
 */
@Directive({
    selector: 'p-avatar[dotGravatar]',
    standalone: true
})
export class DotGravatarDirective {
    /** Reference to the PrimeNG Avatar component instance. */
    #avatar = inject(Avatar);

    /** Change detector reference for manual change detection. */
    #cd = inject(ChangeDetectorRef);

    /**
     * Email address input signal used to generate the Gravatar.
     * @required
     */
    $email = input.required<string>({ alias: 'email' });

    /** Base URL for Gravatar API. */
    #GRAVATAR_URL = 'https://www.gravatar.com/avatar/';

    /** Default size for Gravatar images in pixels. */
    #DEFAULT_SIZE = 48;

    /** Default rating for Gravatar images (g = suitable for all audiences). */
    #DEFAULT_RATING = 'g';

    /**
     * Computed signal that generates the Gravatar URL from the email input.
     * Returns null if the email is invalid or doesn't contain '@'.
     * The URL includes a 404 default parameter to detect if the Gravatar exists.
     * @returns The Gravatar URL or null if email is invalid
     */
    $gravatarUrl = computed(() => {
        const email = this.$email();
        const isEmail = email.includes('@');
        if (!isEmail) {
            return null;
        }
        const hash = md5(email.trim().toLowerCase());
        return `${this.#GRAVATAR_URL}${hash}?s=${this.#DEFAULT_SIZE}&r=${this.#DEFAULT_RATING}&d=404`;
    });

    /**
     * Computed signal that extracts the first letter from the email.
     * Used as a fallback when Gravatar is not available.
     * @returns The first letter of the email in uppercase, or 'A' if email is empty
     */
    $firstLetter = computed(() => {
        const email = this.$email();
        return email ? email[0]?.toUpperCase() : FALLBACK_AVATAR_LETTER;
    });

    /**
     * Sets the avatar to display a letter instead of an image.
     * Clears any existing image and triggers change detection.
     * @param letter - The letter to display in the avatar
     */
    #setLetter(): void {
        const letter = this.$firstLetter();

        this.#avatar.image = null;
        this.#avatar.label = letter;
        this.#cd.detectChanges();
    }

    /**
     * Sets the avatar to display an image.
     * Clears any existing label and triggers change detection.
     * @param avatar - The URL of the avatar image
     */
    #setAvatar(avatar: string): void {
        this.#avatar.label = null;
        this.#avatar.image = avatar;
        this.#cd.detectChanges();
    }

    /**
     * Initializes the directive and starts the Gravatar query process.
     */
    constructor() {
        this.handleGravatarQuery(this.$gravatarUrl);
        this.#avatar.onImageError.pipe(takeUntilDestroyed()).subscribe(() => this.#setLetter());
    }

    /**
     * Handles the Gravatar query logic.
     * If the Gravatar URL is provided, sets the avatar to display an image.
     * If the Gravatar URL is not provided, sets the avatar to display a letter.
     * @param gravatarUrl - The URL of the Gravatar image
     */
    readonly handleGravatarQuery = signalMethod<string>((gravatarUrl) => {
        if (gravatarUrl) {
            this.#setAvatar(gravatarUrl);
        } else {
            this.#setLetter();
        }
    });
}
