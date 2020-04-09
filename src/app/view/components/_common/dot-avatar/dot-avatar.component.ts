import { Component, Input } from '@angular/core';

@Component({
    selector: 'dot-avatar',
    styleUrls: ['./dot-avatar.component.scss'],
    templateUrl: './dot-avatar.component.html'
})
export class DotAvatarComponent {
    @Input()
    url: string;

    @Input()
    showDot = false;

    avatarPlaceholder: string;
    avatarStyles: { [key: string]: string };

    private _label: string;
    private _size = 32;

    @Input()
    set label(value: string) {
        this._label = value;
        this.avatarPlaceholder = this.getPlaceholder(this._label);
    }

    get label(): string {
        return this._label;
    }

    @Input()
    set size(value: number) {
        this._size = value;
        this.avatarStyles = {
            'font-size': value - (value * 25) / 100 + 'px',
            height: value + 'px',
            'line-height': value + 'px',
            width: value + 'px'
        };
    }

    get size(): number {
        return this._size;
    }

    /**
     * Return the avatar url
     *
     * @param {string} label
     * @returns {string}
     * @memberof DotAvatarComponent
     */
    getPlaceholder(label: string): string {
        return !!label ? label.charAt(0).toUpperCase() : null;
    }

    /**
     * Fallback to set url path as null and be able to display placeholder
     *
     * @memberof DotAvatarComponent
     */
    errorLoadingImg(): void {
        this.url = null;
    }
}
