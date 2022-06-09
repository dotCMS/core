import { DotContentCompareEvent } from '@components/dot-content-compare/dot-content-compare.component';
import { DotGlobalMessage } from '@models/dot-global-message/dot-global-message.model';

/**
 * Interface for custom events.
 *
 * @interface
 */
export interface DotEvent {
    name: string;
    data?: DotGlobalMessage | DotContentCompareEvent | number[];
}
