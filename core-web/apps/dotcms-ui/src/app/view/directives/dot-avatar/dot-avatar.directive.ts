import {
    Directive,
    ElementRef,
    HostListener,
    Input,
    OnChanges,
    SimpleChanges
} from '@angular/core';

import { Avatar } from 'primeng/avatar';

@Directive({
    selector: 'p-avatar[dotAvatar]',
    standalone: true
})
export class DotAvatarDirective implements OnChanges {
    private _label: string;
    @Input() readonly image: string;
    @Input() readonly label: string;

    constructor(private avatar: Avatar, private el: ElementRef) {
        this.avatar.shape = 'circle';
    }

    ngOnChanges(changes: SimpleChanges) {
        if (changes.label) {
            this._label = changes.label.currentValue
                ? changes.label.currentValue.charAt(0).toUpperCase()
                : 'U';
            this.avatar.label = this._label;
        }

        if (changes.image)
            // If theres an image we set the label to undefined
            //so if it fails on loading the event is triggered
            this.avatar.label = changes.image.currentValue
                ? undefined
                : this._label[0]?.toUpperCase();
    }

    // This event doesn't trigger when label has a value, but I need to trigger it because
    // "p-avatar-image" class is added to the element and breaks the styles
    @HostListener('onImageError')
    onImageError() {
        this.avatar.label = this._label[0]?.toUpperCase();
        this.el.nativeElement.children[0].classList.remove('p-avatar-image');
    }
}
