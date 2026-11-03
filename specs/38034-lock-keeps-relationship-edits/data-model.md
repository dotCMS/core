# Data Model: Locking or unlocking content reverts unsaved Relationship field changes

No persisted data changes. The only "model" involved is the client-side input the Relationship field reacts to.

## RelationshipFieldInputs (`$inputs` in `DotRelationshipFieldComponent`)

| Field | Type | Source | Part of the re-init key? |
|-------|------|--------|--------------------------|
| `field` | `ContentTypeRelationshipField` | `field` input | Yes (`variable` + identity) |
| `contentlet` | `DotCMSContentlet \| null` | `contentlet` input (from `DotEditContentStore.contentlet()`) | Only its `inode` |
| `targetLanguageId` | `number \| undefined` | `currentLocale().id` while `isCopyingLocale()` | Yes |
| `targetLanguage` | `DotLanguage \| undefined` | same | No (it follows `targetLanguageId`) |

**Re-init rule**: the field store re-initializes only when the key `(field, contentlet?.inode, targetLanguageId)` changes.

## Contentlet state transitions that reach the field

| Transition | `inode` changes? | Re-initializes? |
|------------|------------------|-----------------|
| Lock for Editing / Unlock / Release Lock | No (only `locked*` fields) | **No** (the fix) |
| Save or workflow action that checks in | Yes | Yes |
| View a historical version / back to current | Yes | Yes |
| Navigate to another contentlet | Yes | Yes |
| Manual translation / populate locale | `targetLanguageId` changes (contentlet may be `null`) | Yes |
