import { AppConfig } from './app.config';
import { Inject, Injectable, NgModule } from '@angular/core';

/**
 * Used by the NotificationService to set Desktop Notifications
 */
declare class Notification {
    constructor(title: string, options?: Object);
}

/**
 * NotificationService will notify using Desktop Notifications
 * https://developer.mozilla.org/en-US/docs/Web/API/notification
 * Can change icons by setting iconPath in the AppConfig if needed
 */
@Injectable()
@Inject('config')
export class NotificationService {
    iconPath: string;

    constructor(config: AppConfig) {
        this.iconPath = config.iconPath;
    }

    /**
     * Displays an error message
     * @param body
     */
    displayErrorMessage(body: string): void {
        this.displayMessage('Error', body, 'error');
    }

    /**
     * Displays a success message
     * @param body
     */
    displaySuccessMessage(body: string): void {
        this.displayMessage('Success', body, 'success');
    }

    /**
     * Displays an Info message
     * @param body
     */
    displayInfoMessage(body: string): void {
        this.displayMessage('Info', body, 'info');
    }

    /**
     * Display message for passed in type
     * @param title
     * @param body
     * @param type
     */
    displayMessage(_title: string, body: string, type: string): Notification {
        let myNotification: Notification;
        myNotification = new Notification(type, {
            body: body,
            icon: this.iconPath + '/' + type + '.png'
        });

        return myNotification;
    }
}

@NgModule({
    providers: [AppConfig, NotificationService]
})
export class DotNotificationModule {}
