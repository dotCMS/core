<script setup lang="ts" generic="T extends DotCMSBasicContentlet">
import Editor from '@tinymce/tinymce-vue';
import { computed, onBeforeUnmount, onMounted, ref, watch, withDefaults } from 'vue';

import type { DotCMSBasicContentlet } from '@dotcms/types';
import { DotCMSUVEAction, UVE_MODE } from '@dotcms/types';
import { __DOTCMS_UVE_EVENT__ } from '@dotcms/types/internal';
import { getUVEState, sendMessageToUVE } from '@dotcms/uve';
import { __TINYMCE_PATH_ON_DOTCMS__ } from '@dotcms/uve/internal';

import { TINYMCE_CONFIG, type DotCMSEditableTextProps } from './utils';

/**
 * Enables inline editing of a single contentlet text field via TinyMCE while
 * inside the Universal Visual Editor. Outside edit mode it renders the field's
 * current HTML.
 *
 * @example
 * ```vue
 * <h2><DotCMSEditableText :contentlet="contentlet" field-name="title" /></h2>
 * ```
 */
const props = withDefaults(defineProps<DotCMSEditableTextProps<T>>(), {
    mode: 'plain',
    format: 'text'
});

/** Minimal TinyMCE editor surface used by this component. */
interface TinyMCEEditor {
    setContent(content: string, args?: { format?: string }): void;
    getContent(args?: { format?: string }): string;
    isDirty(): boolean;
    hasFocus(): boolean;
    focus(): void;
}

const editorRef = ref<TinyMCEEditor | null>(null);
const scriptSrc = ref('');
const initEditor = ref(false);
const content = ref<string>((props.contentlet?.[props.fieldName] as string) || '');

const editorConfig = computed(() => TINYMCE_CONFIG[props.mode]);

watch(
    () => [props.fieldName, props.contentlet] as const,
    () => {
        content.value = (props.contentlet?.[props.fieldName] as string) || '';
        editorRef.value?.setContent(content.value, { format: props.format });
    }
);

const onEditorInit = (_event: unknown, editor: TinyMCEEditor) => {
    editorRef.value = editor;
};

const onMouseDown = (event: MouseEvent) => {
    const onNumberOfPages = Number(
        (props.contentlet as DotCMSBasicContentlet).onNumberOfPages ?? 1
    );

    if (onNumberOfPages <= 1 || editorRef.value?.hasFocus()) {
        return;
    }

    event.stopPropagation();
    event.preventDefault();

    const { inode, languageId: language } = props.contentlet;

    sendMessageToUVE({
        action: DotCMSUVEAction.COPY_CONTENTLET_INLINE_EDITING,
        payload: {
            dataset: { inode, language, fieldName: props.fieldName }
        }
    });
};

const onFocusOut = () => {
    const editedContent = editorRef.value?.getContent({ format: props.format }) || '';

    if (!editorRef.value?.isDirty() || editedContent === content.value) {
        return;
    }

    const { inode, languageId: langId } = props.contentlet;

    sendMessageToUVE({
        action: DotCMSUVEAction.UPDATE_CONTENTLET_INLINE_EDITING,
        payload: {
            content: editedContent,
            dataset: { inode, langId, fieldName: props.fieldName }
        }
    });
};

const onMessage = ({ data }: MessageEvent) => {
    const { name, payload } = data ?? {};

    if (name !== __DOTCMS_UVE_EVENT__.UVE_COPY_CONTENTLET_INLINE_EDITING_SUCCESS) {
        return;
    }

    const { oldInode, inode, fieldName: focusedFieldName } = payload ?? {};
    const currentInode = props.contentlet.inode;
    const matchesInode = currentInode === oldInode || currentInode === inode;

    // Match the field too: all of a contentlet's fields share one inode, so an
    // inode-only check would focus every editable field (last one wins).
    if (matchesInode && focusedFieldName === props.fieldName) {
        editorRef.value?.focus();
    }
};

onMounted(() => {
    const state = getUVEState();

    initEditor.value = state?.mode === UVE_MODE.EDIT && !!state?.dotCMSHost?.length;

    if (!props.contentlet || !props.fieldName) {
        console.error(
            '[DotCMSEditableText]: contentlet or fieldName is missing. Ensure all needed props are passed to view and edit the content.'
        );

        return;
    }

    if (state && state.mode !== UVE_MODE.EDIT) {
        console.warn('[DotCMSEditableText]: TinyMCE is not available in the current mode');

        return;
    }

    if (!state?.dotCMSHost) {
        return;
    }

    scriptSrc.value = new URL(__TINYMCE_PATH_ON_DOTCMS__, state.dotCMSHost).toString();

    window.addEventListener('message', onMessage);
});

onBeforeUnmount(() => {
    window.removeEventListener('message', onMessage);
});
</script>

<template>
  <!-- eslint-disable-next-line vue/no-v-html -- renders the contentlet field's
       stored HTML, matching the React SDK's dangerouslySetInnerHTML behavior. -->
  <span
    v-if="!initEditor"
    v-html="content"
  />
  <div
    v-else
    :style="{ outline: '2px solid #006ce7', borderRadius: '4px' }"
  >
    <Editor
      :tinymce-script-src="scriptSrc"
      :inline="true"
      :init="editorConfig"
      :initial-value="content"
      @init="onEditorInit"
      @mousedown="onMouseDown"
      @focusout="onFocusOut"
    />
  </div>
</template>
