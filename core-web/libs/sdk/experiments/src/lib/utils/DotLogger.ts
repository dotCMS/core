/**
 * Logger for the dotCMS SDK
 */
export class DotLogger {
    private readonly isDebug: boolean;
    private readonly packageName: string;

    constructor(isDebug: boolean, packageName: string) {
        this.isDebug = isDebug;
        this.packageName = packageName;
    }

    public group(label: string) {
        if (this.isDebug) {
            // eslint-disable-next-line no-console
            console.group(label);
        }
    }

    public groupEnd() {
        if (this.isDebug) {
            // eslint-disable-next-line no-console
            console.groupEnd();
        }
    }

    public time(label: string) {
        if (this.isDebug) {
            // eslint-disable-next-line no-console
            console.time(label);
        }
    }

    public timeEnd(label: string) {
        if (this.isDebug) {
            // eslint-disable-next-line no-console
            console.timeEnd(label);
        }
    }

    public log(message: string) {
        if (this.isDebug) {
            // eslint-disable-next-line no-console
            console.log(`[dotCMS ${this.packageName}] ${message}`);
        }
    }

    public warn(message: string) {
        if (this.isDebug) {
            console.warn(`[dotCMS ${this.packageName}] ${message}`);
        }
    }

    public error(message: string) {
        console.error(`[dotCMS ${this.packageName}] ${message}`);
    }
}
