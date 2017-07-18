import { Injectable } from '@angular/core';
import { Observable } from 'rxjs/Observable';
import { Field, FieldType, FieldRow } from '../';

/**
 * Provide method to handle with the Field Types
 */
@Injectable()
export class FieldService {

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