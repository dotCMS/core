import { LowerCasePipe } from '@angular/common';
import { Component, computed, input, output } from '@angular/core';

import { ButtonModule } from 'primeng/button';

import { DotCustomTimeModule } from '@components/_common/dot-custom-time.component/dot-custom-time.module';
import { DotMessagePipe } from '@dotcms/ui';
import { INotification, NotificationIcons } from '@models/notifications';

const notificationIcons: NotificationIcons = {
    ERROR: 'exclamation-triangle',
    INFO: 'info-circle',
    WARNING: 'ban'
};

@Component({
    selector: 'dot-notification-item',
    standalone: true,
    imports: [ButtonModule, DotCustomTimeModule, LowerCasePipe, DotMessagePipe],
    styleUrls: ['./dot-notification-item.component.scss'],
    templateUrl: 'dot-notification-item.component.html'
})
export class DotNotificationItemComponent {
    $data = input.required<INotification>({ alias: 'data' });

    clear = output<{ id: string }>();

    $showLinkAction = computed(() => {
        const data = this.$data();
        const actions = data?.actions ? data.actions[0] : null;

        return (
            actions &&
            actions.actionType === 'LINK' &&
            (actions.text || actions.text !== '') &&
            actions.action &&
            actions.action !== ''
        );
    });

    $getIconName = computed(() => {
        const data = this.$data();

        const iconName = notificationIcons[data.level];

        if (!iconName) {
            return '';
        }

        return 'notification-item__icon pi pi-' + iconName;
    });

    onClear(): void {
        const data = this.$data();
        this.clear.emit({ id: data.id });
    }
}
