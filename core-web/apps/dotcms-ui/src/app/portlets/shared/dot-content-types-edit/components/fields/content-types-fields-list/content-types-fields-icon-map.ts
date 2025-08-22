import { FieldUtil } from '@dotcms/utils';

const COLUMN_BREAK = FieldUtil.createColumnBreak();

export const FIELD_ICONS = {
    'com.dotcms.contenttype.model.field.ImmutableBinaryField': 'note',
    'com.dotcms.contenttype.model.field.ImmutableCategoryField': 'format_list_bulleted',
    'com.dotcms.contenttype.model.field.ImmutableCheckboxField': 'check_box',
    'com.dotcms.contenttype.model.field.ImmutableConstantField': 'check_box_outline_blank',
    'com.dotcms.contenttype.model.field.ImmutableCustomField': 'code',
    'com.dotcms.contenttype.model.field.ImmutableDateField': 'date_range',
    'com.dotcms.contenttype.model.field.ImmutableDateTimeField': 'event_note',
    'com.dotcms.contenttype.model.field.ImmutableFileField': 'insert_drive_file',
    'com.dotcms.contenttype.model.field.ImmutableHiddenField': 'visibility_off',
    'com.dotcms.contenttype.model.field.ImmutableHostFolderField': 'storage',
    'com.dotcms.contenttype.model.field.ImmutableImageField': 'image',
    'com.dotcms.contenttype.model.field.ImmutableKeyValueField': 'vpn_key',
    'com.dotcms.contenttype.model.field.ImmutableMultiSelectField': 'menu',
    'com.dotcms.contenttype.model.field.ImmutablePermissionTabField': 'lock',
    'com.dotcms.contenttype.model.field.ImmutableRadioField': 'radio_button_checked',
    'com.dotcms.contenttype.model.field.ImmutableRelationshipsTabField': 'filter_none',
    'com.dotcms.contenttype.model.field.ImmutableSelectField': 'fiber_manual_record',
    'com.dotcms.contenttype.model.field.ImmutableTagField': 'local_offer',
    'com.dotcms.contenttype.model.field.ImmutableTextAreaField': 'format_textdirection_r_to_l',
    'com.dotcms.contenttype.model.field.ImmutableTextField': 'title',
    'com.dotcms.contenttype.model.field.ImmutableTimeField': 'access_time',
    'com.dotcms.contenttype.model.field.ImmutableWysiwygField': 'visibility',
    'com.dotcms.contenttype.model.field.ImmutableTabDividerField': 'folder',
    'com.dotcms.contenttype.model.field.ImmutableLineDividerField': 'more_horiz',
    'com.dotcms.contenttype.model.field.ImmutableRelationshipField': 'merge_type',
    'com.dotcms.contenttype.model.field.ImmutableStoryBlockField': 'post_add',
    'com.dotcms.contenttype.model.field.ImmutableJSONField': 'data_object',
    [COLUMN_BREAK.clazz]: 'view_column'
};
