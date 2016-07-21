import {Component, ViewEncapsulation, Input} from '@angular/core';
import {INotification} from '../../../../api/services/dotcms-events-service';

@Component({
    moduleId: __moduleName, // REQUIRED to use relative path in styleUrls
    selector: 'dot-notifications-item',
    styleUrls: ['notifications-item.css'],
    template: `
        <li>
            <h4>{{data.title}}</h4>
            <p>{{data.message}}</p>
        </li>
    `,
    providers: [],
    directives: [],
    encapsulation: ViewEncapsulation.Emulated
})
export class NotificationsItem {
    @Input() data;
    constructor() {}
}

@Component({
    moduleId: __moduleName, // REQUIRED to use relative path in styleUrls
    selector: 'dot-notifications-list',
    styleUrls: ['notifications-list.css'],
    template: `
    <ul class="dot-notifications-list">
        <template ngFor let-notification [ngForOf]="notifications">
            <dot-notifications-item [data]="notification"></dot-notifications-item>
        </template>
    </ul>
    `,
    providers: [],
    directives: [NotificationsItem],
    encapsulation: ViewEncapsulation.Emulated
})
export class NotificationsList {
    @Input() notifications:INotification;

    constructor() {}
}