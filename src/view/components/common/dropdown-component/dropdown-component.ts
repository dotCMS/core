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

/**
 * Dropdown component.<br>
 *
 * It has the follow attributes:<br>
 *     <ul>
 *         <li>disabled: disabled if it is true</li>
 *         <li>icon: icon to show in the dropdown, just allow icon define in angular-material</li>
 *         <li>title: title to show in the dropdown</li>
 *         <li>alignRight: if it is true align the content to the right, by default the content is align to the left</li>
 *      </ul>
 *
 * It has the follow events:<br>
 *     <ul>
 *         <li>open: fire when the dropdown is opened</li>
 *         <li>close: fire when the dropdown is closed</li>
 *         <li>toggle: fire when thw dropdown is closed or opened, if the dropdown is opened the event object is true
 *         it is false otherwise</li>
 *      </ul>
 *
 * <b>Example:</b>
 *
 * <pre>
 * <dot-dropdown-component title="Dropdowns example"
 *                          (open)="print('Text Dropdown is open')"
 *                          (close)="print('Text Dropdown is close')"
 *                          (toggle)="print('Text Dropdown is', $event)">
 * </pre>
 */
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
        }else{
            this.close.emit();
        }

        this.toggle.emit( this.show );
    }

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
