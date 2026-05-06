# TODO

Pending items to revisit tomorrow.

## Features

- [x] Add a feature flag to let users switch between the legacy and the new block editor — avoids breaking existing instances until full QA is done

## Bugs

- [x] Add a mask / transparent overlay as a background for popovers when they open inline — solved differently: scroll-lock the editor while any popover/slash menu is open (so the cursor stays anchored beneath the overlay), keeping the existing click-outside-to-close behavior. No visual mask needed.
- [x] Add the "video/image by URL" option to the assets modal — shipped as a dedicated `media_link` toolbar popover (segmented Image / Video / YouTube selector + URL input) instead of bolting another tab onto the existing modal; smaller surface, faster path for power users.
- [x] Investigate why normal text cannot be edited after AI-generated content is inserted
- [x] Add styles to the code block
- [ ] Improve the styles for the selected node when highlighted — likely fixable by reducing the X-axis padding on the block editor and adding a small amount of padding to all blocks
