import { EventEmitter } from '@angular/core';

export interface DotFormModel<T, K> {
    data?: T;
    valid: EventEmitter<boolean>;
    value: EventEmitter<K>;
}
