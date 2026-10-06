import { Observable, of } from 'rxjs';

import { Injectable } from '@angular/core';

import { FeaturedFlags } from '@dotcms/dotcms-models';
import { MockDotMessageService } from '@dotcms/utils-testing';

/**
 * Shared test doubles for the content types form specs.
 *
 * `content-types-form.component.spec.ts` and
 * `content-types-form-dialog-focus.integration.spec.ts` both drive the same component and were
 * duplicating these verbatim. Test-only: nothing here is reachable from `src/main.ts`, so it never
 * reaches the app bundle.
 */
@Injectable()
export class MockDotLicenseService {
    isEnterprise(): Observable<boolean> {
        return of(false);
    }
}

/** Union of the labels both specs need. */
export const CONTENT_TYPES_FORM_MESSAGES = {
    'contenttypes.action.cancel': 'Cancel',
    'contenttypes.action.create': 'Create',
    'contenttypes.action.delete': 'Delete',
    'contenttypes.action.edit': 'Edit',
    'contenttypes.action.form.cancel': 'Cancel',
    'contenttypes.action.save': 'Save',
    'contenttypes.action.update': 'Update',
    'contenttypes.content.content': 'Content',
    'contenttypes.content.contenttype': 'content type',
    'contenttypes.content.fileasset': 'fileasset',
    'contenttypes.content.form': 'Form',
    'contenttypes.content.htmlpage': 'Page',
    'contenttypes.content.key_value': 'Key Value',
    'contenttypes.content.persona': 'Persona',
    'contenttypes.content.vanity_url:': 'Vanity Url',
    'contenttypes.content.variable': 'Variable',
    'contenttypes.content.widget': 'Widget',
    'contenttypes.form.field.detail.page': 'Detail Page',
    'contenttypes.form.field.expire.date.field': 'Expire Date Field',
    'contenttypes.form.field.host_folder.label': 'Host or Folder',
    'contenttypes.form.identifier': 'Identifier',
    'contenttypes.form.label.URL.pattern': 'URL Pattern',
    'contenttypes.form.label.description': 'Description',
    'contenttypes.form.label.icon': 'Icon',
    'contenttypes.form.label.publish.date.field': 'Publish Date Field',
    'contenttypes.form.label.workflow': 'Workflow',
    'contenttypes.form.label.workflow.actions': 'Workflow Actions',
    'contenttypes.form.name': 'Name',
    'contenttypes.form.name.error.required': 'Error is wrong',
    'contenttypes.hint.URL.map.pattern.hint1': 'Hello World',
    'content.type.form.banner.message': 'Try the new content editor'
};

export const createContentTypesFormMessageServiceMock = (): MockDotMessageService =>
    new MockDotMessageService(CONTENT_TYPES_FORM_MESSAGES);

/**
 * Fresh `ActivatedRoute` stub per call, so specs never share mutable feature-flag state.
 */
export const buildActivatedRouteMock = (newContentEditorEnabled = true) => ({
    snapshot: {
        data: {
            featuredFlags: {
                [FeaturedFlags.FEATURE_FLAG_CONTENT_EDITOR2_ENABLED]: newContentEditorEnabled
            }
        }
    }
});
