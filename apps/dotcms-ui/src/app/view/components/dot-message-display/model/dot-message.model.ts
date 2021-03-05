import { DotMessageSeverity } from './dot-message-severity.model';
import { DotMessageType } from './dot-message-type.model';

/**
 *Message send from the backend
 *
 * @export
 * @interface DotMessage
 */
export interface DotMessage {
    life: number;
    message: string;
    portletIdList?: string[];
    severity: DotMessageSeverity;
    type: DotMessageType;
}
