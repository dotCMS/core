
import {Injectable} from '@angular/core';
import { Observable } from 'rxjs/Observable';

/**
 * Provide method to handle with the Field Types
 */
@Injectable()
export class FieldTypesService {
    loadFieldTypes(): Observable<FieldType[]> {
        return Observable.of([
            {
                name: 'Text'
            },
            {
                name: 'Date'
            },
            {
                name: 'Checkbox'
            },
            {
                name: 'Image'
            }
        ]);
    }
}

export interface FieldType {
    name: string;
}

export interface Field {
    fixed: boolean;
    indexed: boolean;
    name: string;
    required: boolean;
    type: string;
    velocityVarName: string;
}