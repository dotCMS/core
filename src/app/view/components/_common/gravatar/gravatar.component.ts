import { Component, Input } from '@angular/core';
import { GravatarService } from '../../../../api/services/gravatar-service';
let md5 = require('md5');

@Component({
    selector: 'gravatar',
    styles: [require('./gravatar.component.scss')],
    templateUrl: './gravatar.component.html'
})

export class GravatarComponent {
    @Input() email;
    @Input() size;
    public gravatarProfileStyles;
    public gravatarProfile;
    public gravatarPlaceholder;
    public gravatarPlaceholderStyles;
    public avatarUrl;

    constructor(private gravatarService: GravatarService) {

    }

    ngOnChanges(): void {
        const hash = md5(this.email);
        const profile$ = this.gravatarService.loadGravatarProfile(hash);
        profile$.subscribe(
            data => {
                this.gravatarProfileStyles = {
                    'height': this.size + 'px',
                    'width': this.size + 'px'
                };

                this.gravatarProfile = true;
                this.avatarUrl = data.entry[0].photos[0].value + '?s=' + this.size;
            },
            error => {
                this.gravatarProfile = false;
                this.gravatarPlaceholder = this.getDotAvatar(this.email);
                this.gravatarPlaceholderStyles = {
                    'font-size': this.size - ((this.size * 25) / 100) + 'px',
                    'height': this.size + 'px',
                    'width': this.size + 'px'
                };
            }
        );
    }

    public getDotAvatar(email): any {
        const firstLetter = email.substring(0, 1).toUpperCase();
        return firstLetter;
    }
}