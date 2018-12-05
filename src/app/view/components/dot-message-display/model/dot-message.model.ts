namespace Dot {

    /**
    *Message send from the backend
    *
    * @export
    * @interface DotMessage
    */
    export interface Message {
        life: number;
        message: string;
        portletIdList: string[];
        severity: Message.Severity;
        type: Message.Type;
    }

    export namespace Message {
        /**
        *{@link DotMessage} type
        *
        * @export
        * @enum {number}
        */
        export enum Type {
            SIMPLE_MESSAGE = 'RAW_MESSAGE',
            CONFIRMATION_MESSAGE = 'CONFIRMATION_MESSAGE',
        }


        /**
        *{@link DotMessage}'s severity
        *
        * @export
        * @enum {number}
        */
        export enum Severity {
            INFO = 'INFO',
            WARNING = 'WARNING',
            ERROR = 'ERROR'
        }
    }
}
