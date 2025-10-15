declare module '@analytics/queue-utils' {
    export interface QueueOptions {
        max?: number;
        interval?: number;
        throttle?: boolean;
    }

    export interface Queue<T = unknown> {
        push: (item: T) => number;
        pause: (toFlush?: boolean) => void;
        resume: () => void;
        flush: (all?: boolean) => void;
        size: () => number;
    }

    function smartQueue<T = unknown>(
        callback: (items: T[], rest: T[]) => void,
        options?: QueueOptions
    ): Queue<T>;

    export default smartQueue;
}

