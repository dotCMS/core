import { Observable } from 'rxjs';

import { Signal } from '@angular/core';

import { DotCMSContentTypeField, DotFieldVariable } from '@dotcms/dotcms-models';

/**
 * Contract that every Settings tab section must implement.
 * DotCustomFieldSettingsComponent uses this interface to drive
 * save, dirty-check, and validity — adding a new section only
 * requires wiring it in the parent.
 */
export interface FieldSettingsSection {
    /** True when the user has modified any control in this section. */
    isDirty: boolean;

    /** Signal that reflects current form validity. */
    isValid: Signal<boolean>;

    /** Emits on every form value change — used to refresh the Save button state. */
    valueChanges$: Observable<unknown>;

    /** Persists this section's data and returns the saved variable. */
    save(field: DotCMSContentTypeField): Observable<DotFieldVariable>;
}
