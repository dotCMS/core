### Features

1. Convert the block editor component into a form-friendly component using `ControlValueAccessor`
2. Add a **Grid block** that allows users to:
   - Create columns
   - Resize them

   Two references for this behavior:
   - Local: `/Users/rjvelazco/Desktop/dotcms/core/core-web/libs/sdk/react/src/lib/next/components/DotCMSBlockEditorRenderer/components/blocks/GridBlock.tsx`
   - Remote: https://github.com/hunghg255/reactjs-tiptap-editor/tree/main/src/extensions/Column
3. In the **Link form**, add a checkbox to let the user toggle whether the link should open in a new tab (`target="_blank"`)
4. Add a **selected state style** for the following node types: images, videos, and contentlets