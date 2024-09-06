export type DotcmsClientListener = {
    action: string;
    event: string;
    // eslint-disable-next-line @typescript-eslint/no-explicit-any
    callback: (...args: any[]) => void;
};
