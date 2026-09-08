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

/**
 * Icons by notification level. An index signature because `DotNotification.level` is a plain
 * string — the three below are the levels that have an icon, and the reader falls back for the rest.
 */
export interface NotificationIcons {
    [level: string]: string;
    ERROR: string;
    INFO: string;
    WARNING: string;
}
