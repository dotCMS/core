import { ChangeDetectorRef, Directive, HostListener, Input, OnInit, inject } from '@angular/core';

import { Avatar } from 'primeng/avatar';

@Directive({
    selector: 'p-avatar[dotAvatar]',
    standalone: true
})
export class DotAvatarDirective implements OnInit {
    private avatar = inject(Avatar);
    private cd = inject(ChangeDetectorRef);

    @Input() text = 'Default';

    constructor() {
        this.avatar.shape = 'circle';
    }

    ngOnInit(): void {
        this.avatar.label = this.avatar.image ? undefined : this.text[0]?.toUpperCase();
    }

    @HostListener('onImageError', ['$event'])
    onImageError() {
        this.avatar.label = this.text[0]?.toUpperCase();
        this.avatar.image = null;
        this.cd.detectChanges();
    }
}
