import { INotification } from '@models/notifications';

export interface IToolbarNotification extends INotification {
    level: string;
    timeSent: string;
    actions?: Array<{
        action: string;
        actionType: string;
        text: string;
    }>;
    notificationData?: Record<string, unknown>;
}
