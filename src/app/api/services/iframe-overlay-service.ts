import {Injectable} from '@angular/core';
import {BehaviorSubject, Observable} from 'rxjs/Rx';

@Injectable()
export class IframeOverlayService {
    public $overlay: BehaviorSubject<boolean> = new BehaviorSubject<boolean>(false);

    constructor() {}

    show(): void {
        this.$overlay.next(true);
    }

    hide(): void {
        this.$overlay.next(false);
    }

    toggle(): void {
        this.$overlay.next(!this.$overlay.getValue());
    }

    get overlay(): Observable<boolean> {
        return this.$overlay.asObservable();
    }
}