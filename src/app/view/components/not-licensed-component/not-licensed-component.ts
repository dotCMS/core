import {Component, EventEmitter, Output, ViewEncapsulation, Input} from '@angular/core';
import {BaseComponent} from '../common/_base/base-component';

@Component({
    encapsulation: ViewEncapsulation.None,

    selector: 'not-licensed-component',
    styleUrls: ['not-licensed-component.css'],
    templateUrl: 'not-licensed-component.html',
})
export class NotLicensedComponent {

}
