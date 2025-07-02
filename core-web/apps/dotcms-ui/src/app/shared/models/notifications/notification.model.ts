export interface INotification {
    id: string;
    title: string;
    message: string;
    level: string;
    timeSent: string;
    actions?: Array<{
        action: string;
        actionType: string;
        text: string;
    }>;
    notificationData?: Record<string, unknown>;
}

export interface DotNotificationResponse {
    notifications: INotification[];
    total: number;
    totalUnreadNotifications: number;
}

export interface NotificationIcons {
    ERROR: string;
    INFO: string;
    WARNING: string;
}
