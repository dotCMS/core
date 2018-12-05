import { DotMessageSeverity } from './dot-message-severity.model';
import { DotMessageType } from './dot-message-type.model';

export interface DotMessage {
    life: number;
    message: string;
    portletIdList: string[];
    severity: DotMessageSeverity;
    type: DotMessageType;
}
