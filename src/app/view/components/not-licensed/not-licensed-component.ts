import {Component, EventEmitter, Output, ViewEncapsulation, Input} from '@angular/core';
import {BaseComponent} from '../_common/_base/base-component';

@Component({
    encapsulation: ViewEncapsulation.None,

    selector: 'not-licensed-component',
    styles: [require('./not-licensed-component.scss')],
    templateUrl: 'not-licensed-component.html',
})
export class NotLicensedComponent {

}
