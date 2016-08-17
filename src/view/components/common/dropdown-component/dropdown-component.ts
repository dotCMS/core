import {Component, EventEmitter, Input, Output, ViewEncapsulation, ElementRef} from '@angular/core';
import { Router } from '@ngrx/router';
import {LoginService} from '../../../../api/services/login-service';


@Component({
    directives: [],
    encapsulation: ViewEncapsulation.Emulated,
    moduleId: __moduleName, // REQUIRED to use relative path in styleUrls
    pipes: [],
    providers: [],
    selector: 'dot-dropdown-component',
    styleUrls: ['dropdown-component.css'],
    templateUrl: ['dropdown-component.html'],
    host: {
        '(document:click)': 'handleClick($event)',
    }
})

export class DropdownComponent {
    @Input() disabled:boolean = false;
    @Input() icon:string = null;
    @Input() title:string = null;
    @Input() alignRight:boolean = false;

    @Output() open = new EventEmitter<>();
    @Output() toggle = new EventEmitter<boolean>();
    @Output() close = new EventEmitter<>();

    private show:boolean = false;

    constructor (private elementRef: ElementRef){}

    private onToggle():void{
        this.show = !this.show;

        if (this.show){
            this.open.emit();
        } else {
            this.close.emit();
        }

        this.toggle.emit( this.show );
    }

    //TODO: we need doing this globally for all the components that need to detect if the click was outside it.
    private handleClick($event) {
        let clickedComponent = $event.target;
        let inside = false;
        do {
            if (clickedComponent === this.elementRef.nativeElement) {
                inside = true;
            }
            clickedComponent = clickedComponent.parentNode;
        } while (clickedComponent);

        if (!inside) {
            this.show = false;
        }
    }
}
