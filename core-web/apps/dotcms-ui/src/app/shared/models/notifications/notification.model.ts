export interface INotification {
    id: string;
    title: string;
    message: string;
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
