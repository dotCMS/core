import { Directive, ElementRef, HostListener, OnInit } from '@angular/core';

import { Avatar } from 'primeng/avatar';

@Directive({
    selector: 'p-avatar[dotAvatar]',
    standalone: true
})
export class DotAvatarDirective implements OnInit {
    private _label: string;

    constructor(private avatar: Avatar, private el: ElementRef) {
        this.avatar.shape = 'circle';
    }

    ngOnInit() {
        this._label = this.avatar.label ?? 'Unknown';

        // If theres an image we set the label to undefined
        //so if it fails on loading the event is triggered
        this.avatar.label = this.avatar.image ? undefined : this._label[0]?.toUpperCase();
    }

    // This event doesn't trigger when label has a value, but I need to trigger it because
    // "p-avatar-image" class is added to the element and breaks the styles
    @HostListener('onImageError')
    onImageError() {
        this.avatar.label = this._label[0]?.toUpperCase();
        this.el.nativeElement.children[0].classList.remove('p-avatar-image');
    }
}
