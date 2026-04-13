### Bugs

1. No hover style on the image list items sourced from dotCMS
2. The `/` menu scrolls with the content — it appears to be fixed but should stay in place
3. "Link" should not appear as an option in the `/` menu
4. The block editor container should have a fixed height of 500px, support vertical scrolling, and be vertically resizable
5. Only one toolbar dialog/modal/popup should be open at a time — all others must close when clicking outside of them

### Features

1. Add an **"Edit Image Properties"** button to the toolbar, allowing users to modify the image URL, title, alt text, and other accessibility attributes. The button should open a form pre-populated with the current image data for editing.
2. The component should accept an **`allowedBlocks`** input (alternatively: `enabledBlocks` or `blockAllowlist`) that determines which block types are available in the editor. This likely requires a block registry map keyed by block name for efficient lookup.
3. Add a **"Full Screen"** button that expands the editor into a dialog covering 90% of the viewport.
