import { ChangeDetectorRef, Directive, HostListener, Input } from '@angular/core';

import { Avatar } from 'primeng/avatar';

@Directive({
    selector: 'p-avatar[dotAvatar]',
    standalone: true
})
export class DotAvatarDirective {
    @Input() readonly text: string = 'Default';

    constructor(private avatar: Avatar, private cd: ChangeDetectorRef) {
        this.avatar.shape = 'circle';
    }

    @HostListener('onImageError', ['$event'])
    onImageError() {
        this.avatar.label = this.text[0]?.toUpperCase();
        this.avatar.image = null;
        this.cd.detectChanges();
    }
}
