import {Component, ViewEncapsulation, Input} from '@angular/core';
import {INotification} from '../../../../api/services/notifications-service';

@Component({
    directives: [],
    encapsulation: ViewEncapsulation.Emulated,
    moduleId: __moduleName,
    providers: [],
    selector: 'dot-notifications-item',
    styleUrls: ['notifications-item.css'],
    templateUrl: ['notifications-item.html'],

})
export class NotificationsItem {
    @Input() data;
    constructor() {}
}

@Component({
    directives: [NotificationsItem],
    encapsulation: ViewEncapsulation.Emulated,
    moduleId: __moduleName,
    providers: [],
    selector: 'dot-notifications-list',
    styleUrls: ['notifications-list.css'],
    templateUrl: ['notifications-list.html'],
})
export class NotificationsList {
    @Input() notifications:INotification;

    constructor() {}
}