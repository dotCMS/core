# Feature Specification: Grid view for Content Drive

**Feature Branch**: `issue-37930-content-drive-grid-view`

**Created**: 2026-10-08

**Status**: Draft

**Type**: New Feature

**Input**: GitHub issue dotCMS/core#37930. Content Drive needs a grid view as an alternative to the
current table view, to reach parity with Content Search's card view. Users switch between the two
views and their choice is remembered in the browser; with no remembered choice the Drive opens in
the table. (The ticket also mentions the custom tool's stored view mode, `dataViewMode`, which
Content Search uses; reading it is split out to #37964, see Clarifications.) Each card shows the
item's image, title, status, language and content type, the avatar of the user who last edited it
(name on hover), the lock marker, a "more actions" menu that appears on hover, plus a checkbox for
multi-selection. Titles stay on one line, all cards share one size, the grid fits as
many cards per row as the screen allows, and an item with no usable image shows its content type
icon instead. Reference images: a four-column grid of cards inside the Drive, and a single card
(image on top with the checkbox over its corner; title and a "Published" badge on one line; user
avatar, language chip and content type below). The ticket asked for "creation date / owner"; the PR
review changed this to the last editor's avatar alone, with no date, so the card matches the table
(see Clarifications).

## Clarifications

### Session 2026-10-08

- Q: How should a custom tool end up opening Content Drive, so the Drive can read its
  `dataViewMode`? → A: Out of scope. This ticket covers the grid view and the switcher only.
  Reading `dataViewMode` from the tool's portlet configuration is a separate ticket (#37964);
  until then the Drive opens in the remembered view, or the table.
- Q: Should the card show the creation date as visible text, or only when hovering the owner
  avatar? → A: Superseded on 2026-10-09 (see below): the card shows no date at all, and the
  avatar is the last editor's, with their name on hover.
- Q: What should a single click on the card body (not the checkbox) do? → A: The same as a table
  row: a click selects the card, Shift-click and Ctrl/Cmd-click extend the selection, a
  double-click opens the item, and clicking the title opens it.
- Q: What picture should the owner avatar show? → A: The user's Gravatar, looked up from their
  email, as the toolbar already does; their initials when they have no Gravatar.

### Session 2026-10-09

- Q: Do the Content Drive keyboard shortcuts (#32591) apply to the grid, and how do arrow keys move
  between cards? → A: Yes, all of them. Left/Right move one card, Up/Down move one row of cards
  (the standard keyboard pattern for grids). Everything else matches the table.

### Session 2026-10-09 (PR #37959 review)

- Q: Which person should the card's avatar and hover show? → A: The same user as the table's Edited
  By column: for content, the user who last edited it; for folders, the folder's owner, as the table
  shows. This replaces "owner and creation date" so the grid and the table never disagree. The card
  shows no date (see the later answer below).
- Q: What should a card show at the top for PDFs and videos? → A: The same preview the table's
  thumbnail shows (image, SVG, PDF page), except that videos show a still frame with no player
  controls, so clicks on the card still select and open it.
- Q: How should a keyboard-only user open a card? → A: As in the table: Enter and Space select, and
  there is no keyboard way to open an item in either view today. Adding one is a follow-up for both
  views together, not part of this feature.
- Q: Does the grid keep its sort order after a reload? → A: No, the same as the table. The Drive
  does not keep the sort across a reload today (it starts from the default sort), so the grid keeps
  the sort in effect while the user stays on the page, and a reload resets it in both views.
- Q: How wide is a card? → A: Like Content Search's card view: at least 260px wide, with the columns
  stretching evenly to fill the row, so all cards in the grid share one width.
- Q: After switching views, where do focus and scroll go? → A: The focused item and the selection
  anchor stay the same if they are on the page, and the focused item is scrolled into view;
  otherwise the listing falls back to its single Tab stop.
- Q: Can keyboard, screen reader and touch users reach what is shown on hover? → A: Yes. It is part
  of the card's label read by screen readers, and tapping the element on a touch screen shows it.
- Q: Does the card show a date, and what does hovering its parts reveal? → A: No date at all; the
  card shows only the last editor's avatar. Hovering the title shows the full title, hovering the
  avatar shows the editor's name, and hovering the content type shows its full name. The content
  type is cut with an ellipsis like the title when it does not fit.
- Q: Does the card carry the table's lock marker and its "more actions" menu? → A: Yes, both. The
  lock marker shows as in the table, and the ⋮ "more actions" button appears when the card is
  hovered (or has keyboard focus), opening the same menu as the table row's button.
- Q: Do busy items and loading keep the table's treatment? → A: Yes. A card busy with a running bulk
  action looks and behaves like a busy table row (dimmed, marked busy, not clickable or selectable,
  with a spinner in place of its preview), and while results load the grid shows skeleton cards.
- Q: Is the Gravatar a new mechanism? → A: No. The Drive reuses the Gravatar directive the toolbar
  already uses; it may need a cache so a page of cards does not repeat lookups.

## User Scenarios & Testing *(mandatory)*

### User Story 1 - Browse the Drive as a grid of cards (Priority: P1)

A content editor looking for a picture, a blog post with a recognisable hero image, or a page,
finds it faster by sight than by reading rows of titles. Today Content Drive only has a table. With
this feature they can switch the listing to a grid of cards. Each card shows the item's image (or
its content type icon when there is none) and the same key facts the table does, so nothing is lost
by switching.

**Why this priority**: The grid itself is the feature. Everything else (remembering the choice,
selecting from cards) only matters once the grid exists.

**Independent Test**: Open Content Drive in a folder that holds folders, pages, images and plain
content. Switch to the grid. Every item in the current page of results appears as one card, in the
same order as the table, with the details listed below.

**Acceptance Scenarios**:

1. **Given** the user is on Content Drive in the table view,
   **When** they choose the grid option in the view switcher beside the search box,
   **Then** the same items, in the same order and on the same page of results, appear as cards.
2. **Given** the grid view is showing,
   **When** the user chooses the table option,
   **Then** the table returns with the same items, page and filters.
3. **Given** an item the table shows a preview for (an image or SVG, a PDF, or content whose image
   field holds one),
   **When** its card is drawn,
   **Then** the top of the card shows that preview, cropped to fill the space without distortion.
4. **Given** a video file,
   **When** its card is drawn,
   **Then** the top of the card shows a still frame of the video with no player controls, and
   clicking it selects or opens the card like anywhere else on the card body.
5. **Given** a folder, a page, or content with no preview,
   **When** its card is drawn,
   **Then** the top of the card shows the icon for its type (folder icon, page icon, the content
   type's own icon) centred on a plain background.
6. **Given** an item whose preview fails to load (deleted, no permission, broken file),
   **When** its card is drawn,
   **Then** the card shows its content type icon instead, never a broken-image marker.
7. **Given** a content item,
   **When** its card is drawn,
   **Then** below the image it shows: the title and the status badge on the first line; the avatar
   of the user who last edited it, the language and the content type name on the second line.
8. **Given** any card,
   **When** the user hovers the avatar (or taps it on a touch screen),
   **Then** the name the table shows in its Edited By column appears; it is not shown as text on
   the card otherwise, and it is part of the card's screen reader label. The card shows no date.
9. **Given** a card whose title or content type name is too long to fit,
   **When** the user hovers it,
   **Then** the full title or full content type name appears.
10. **Given** an item the table shows as locked,
    **When** its card is drawn,
    **Then** the card shows the same lock marker.
11. **Given** the grid is showing and the user is not read-only,
    **When** the user hovers a card or it has keyboard focus,
    **Then** a ⋮ "more actions" button appears on it, and clicking it opens the same menu as the
    table row's ⋮ button; it stays hidden otherwise, and never shows in a read-only grid.
12. **Given** a folder,
   **When** its card is drawn,
   **Then** it shows its name, its owner's avatar and the word "Folder", with no language and no
   status, exactly as the table shows folders.

---

### User Story 2 - Cards stay tidy whatever the content and the screen (Priority: P1)

Titles in a real site range from "Home" to a 120-character blog headline. Screens range from a
laptop to a wide monitor. The grid must look orderly in all of them: every card the same size, long
titles cut to one line, and as many cards per row as fit.

**Why this priority**: A grid with ragged card heights or titles spilling out of the card looks
broken. This is part of shipping the grid at all, not polish to add later.

**Independent Test**: Load a folder that mixes very short and very long titles. Resize the browser
from narrow to wide. Card size stays uniform and the number of columns changes with the width.

**Acceptance Scenarios**:

1. **Given** an item whose title is longer than the card is wide,
   **When** its card is drawn,
   **Then** the title is shown on one line, cut with an ellipsis, and the full title is available
   by hovering it.
2. **Given** a page of results mixing items with and without images and with titles of any length,
   **When** the grid is drawn,
   **Then** every card has the same width and height, and the gaps between cards are equal both
   across and down.
3. **Given** the grid is showing,
   **When** the user widens or narrows the browser window, or opens or closes the folder tree
   beside the listing,
   **Then** the grid shows as many cards per row as fit at the minimum card width (260px), the
   columns stretch evenly to fill the row, and there is no horizontal scrolling.
4. **Given** the listing area is narrower than the minimum card width,
   **When** the grid is drawn,
   **Then** it shows one card per row and the card shrinks to fit rather than overflowing.

---

### User Story 3 - Select several items from the grid (Priority: P1)

The user picks several cards and runs the same bulk actions the table offers today (move, publish,
delete, add to bundle, workflow actions). Selection is the same selection in both views: switching
views does not lose it.

**Why this priority**: Without selection the grid is only for looking. Bulk work is a core Drive
task, and the reference cards carry a checkbox for exactly this.

**Independent Test**: In the grid, tick three cards. The Drive's selection count and its bulk
actions reflect three items. Switch to the table: the same three rows are ticked.

**Acceptance Scenarios**:

1. **Given** the grid is showing,
   **When** the user ticks the checkbox on a card,
   **Then** the card is marked as selected and the Drive's bulk actions apply to it.
2. **Given** several cards are ticked,
   **When** the user switches to the table view,
   **Then** the same items are ticked in the table, and the reverse holds when switching back.
3. **Given** an unselectable item (see Terms),
   **When** it appears in the grid,
   **Then** its checkbox is unavailable in the same way.
4. **Given** some cards are selected,
   **When** the user clicks the body of another card (anywhere but the checkbox and the title),
   **Then** that card replaces the selection, as clicking a table row does, while ticking a
   card's checkbox adds or removes just that card and keeps the rest.
5. **Given** one card is selected,
   **When** the user Shift-clicks another card,
   **Then** every card between them, in grid order, is selected; Ctrl-click (Cmd-click on Mac)
   adds or removes a single card instead, as in the table.
6. **Given** some cards are selected,
   **When** the user double-clicks another card,
   **Then** the item opens, and the selection ends up as a single click on that card would leave it,
   the same as double-clicking a table row.
7. **Given** the same content exists in two languages and both are listed,
   **When** the user ticks one language's card,
   **Then** only that card is selected, matching the table's per-language selection.

---

### User Story 4 - The Drive remembers my view (Priority: P2)

A user who prefers the grid should not have to switch to it every time. Once they pick a view, the
Drive opens in that view on their next visit in the same browser.

**Why this priority**: Without it the grid is a one-click detour on every visit, which most users
would stop taking. It is not needed to prove the grid works, so it follows the P1 stories.

**Independent Test**: Switch to the grid, navigate away, then open Content Drive again. It opens in
the grid. Switch to the table, reload: it opens in the table. In a browser with no remembered view,
open the Drive: it opens in the table.

Reading the custom tool's `dataViewMode` is left to a separate ticket (see Clarifications). This
story only covers the user's own remembered choice.

**Acceptance Scenarios**:

1. **Given** the user switched to the grid,
   **When** they reload the page or come back to Content Drive later in the same browser,
   **Then** the Drive opens in the grid.
2. **Given** the user has never chosen a view in this browser,
   **When** they open Content Drive,
   **Then** it opens in the table.
3. **Given** the browser refuses to store the choice (private window, storage blocked or full),
   **When** the user switches views,
   **Then** the switch still works for the current visit and the Drive shows no error.
4. **Given** the stored choice is unreadable (cleared, edited by hand, or an unknown value),
   **When** the user opens Content Drive,
   **Then** it opens in the table.

---

### User Story 5 - Work with a card the way I work with a row (Priority: P2)

Users already know how to open, act on and move items in the table. A card should respond to the
same gestures so the grid is not a second-class view.

**Why this priority**: Parity with the table is what makes the grid usable for daily work rather
than only for browsing. It builds on the cards from Story 1.

**Independent Test**: In the grid, double-click a content card (it opens in the editor), right-click
a card (the same menu as the table row appears), open a folder card (the Drive moves into it), and
drag a card onto a folder card (the item moves).

**Acceptance Scenarios**:

1. **Given** the grid is showing,
   **When** the user double-clicks a card or clicks its title,
   **Then** the item opens exactly as it does from the table: a folder is entered, content opens in
   its editor.
2. **Given** the grid is showing,
   **When** the user right-clicks a card,
   **Then** the same context menu the table row shows appears, with the same actions.
3. **Given** the grid is showing,
   **When** the user moves between pages of results,
   **Then** the grid uses the same pagination as the table, and the page size and position are kept
   when switching views.
4. **Given** the grid is showing,
   **When** the user drags cards onto a folder, either a folder card or a folder in the tree,
   **Then** the items move to that folder exactly as they do when dragged from the table, with the
   same confirmation, permission checks and result messages.
5. **Given** several cards are selected,
   **When** the user drags one of them,
   **Then** all selected items are dragged together, as in the table.
6. **Given** the grid is showing,
   **When** the user drops files from their computer onto the listing,
   **Then** the upload starts into the current folder exactly as it does over the table.

---

### User Story 6 - The grid keeps the table's sort order (Priority: P3)

In the table, the user sorts by clicking a column header. The grid has no headers and gets no sort
control of its own in this feature: it shows items in the sort order currently in effect, which the
user sets from the table.

**Why this priority**: Sorting matters but the user can still switch to the table to sort, and the
grid keeps whatever order was set there.

**Independent Test**: Sort the table by title, switch to the grid. Cards appear in title order.

**Acceptance Scenarios**:

1. **Given** the table is sorted by any column,
   **When** the user switches to the grid,
   **Then** the cards keep that order.
2. **Given** the grid is showing,
   **When** the user wants a different order,
   **Then** they switch to the table, sort there, and switch back; the grid shows the new order.
3. **Given** the grid is showing with a sort the user picked in the table,
   **When** the user reloads the page,
   **Then** the sort goes back to the default, exactly as the table does after a reload today.

---

### User Story 7 - Use the Drive's keyboard shortcuts in the grid (Priority: P2)

Content Drive already has keyboard shortcuts (#32591): one Tab stop into the listing, arrows to
move between rows, Shift+Arrow to extend a selection, Escape to clear it, and shortcuts for search
and the folder tree. A keyboard user who switches to the grid keeps all of them. Because cards sit
in rows and columns, the arrows move in two directions: Left/Right move one card, Up/Down move one
row of cards.

**Why this priority**: Losing the shortcuts would make the grid a step back for keyboard users and
break the accessibility the Drive already offers. It builds on the selection from Story 3.

Opening an item from the keyboard is not part of this story: the table has no keyboard way to
open an item today, and adding one is a follow-up for both views together (see Clarifications).

**Independent Test**: In the grid, press Tab until focus enters the listing, then use the arrows,
Shift+Arrow and Escape without the mouse. Focus, selection and the folder tree respond as described
below.

**Acceptance Scenarios**:

1. **Given** the grid is showing,
   **When** the user tabs into the listing,
   **Then** focus lands on one card only; one more Tab leaves the grid, as with the table.
2. **Given** a card has focus,
   **When** the user presses Right or Left,
   **Then** focus moves to the next or previous card in reading order, wrapping onto the next or
   previous row, without changing the selection.
3. **Given** a card has focus,
   **When** the user presses Down or Up,
   **Then** focus moves to the card in the same column one row below or above; when that row is
   shorter (the last row), focus goes to its last card.
4. **Given** focus is on the first or last card of the page,
   **When** the user presses an arrow that would leave the page,
   **Then** focus stays where it is; no arrow key changes the page.
5. **Given** a card has focus,
   **When** the user presses Shift with any arrow,
   **Then** the selection extends from the anchor card to the newly focused card in reading order,
   and reversing direction shrinks it back toward the anchor, as Shift+Arrow does in the table.
6. **Given** one card is selected,
   **When** the user Shift-clicks another card's checkbox,
   **Then** every card between them in reading order is selected (the same rule as Story 3,
   scenario 5, applied to the checkbox).
7. **Given** some cards are unselectable,
   **When** the user moves with the arrows or extends a range,
   **Then** those cards are skipped and left out of the range, as in the table.
8. **Given** cards are selected,
   **When** the user presses Escape,
   **Then** the selection clears and filters, search text and folder stay as they are.
9. **Given** the grid is showing,
   **When** the user presses the search shortcut or the folder tree shortcut,
   **Then** they work exactly as in the table view.
10. **Given** a card has focus,
    **When** the user presses Enter or Space,
    **Then** the card's selection changes as a focused table row's does; neither key opens it.
11. **Given** the user sorts, filters, searches or changes page in the grid,
    **When** the new results appear,
    **Then** the grid is still reachable with a single Tab, as the table is after the same changes.
12. **Given** a card has focus or is the selection anchor,
    **When** the user switches to the table and back,
    **Then** the same item keeps focus and anchor and is scrolled into view; if it is no longer on
    the page, the listing falls back to its single Tab stop.

---

### Edge Cases

- **Loading**: While results load, the grid shows skeleton cards of the same size, not an empty area
  that jumps when cards arrive.
- **Busy items**: An item busy with a running bulk action keeps its card in place, dimmed with a
  spinner over its preview, rather than being replaced by a skeleton.
- **No results**: An empty folder or a search with no matches shows the same empty message the table
  shows.
- **Search failed**: The search-failed message shown above the table also shows above the grid.
- **Shared assets and links**: Items the table marks as shared from elsewhere, or as locked, carry
  the same marker on their card.
- **Missing user**: An item whose editor cannot be resolved (deleted user, system user) shows a
  neutral placeholder instead of a broken avatar.
- **No Gravatar or Gravatar unreachable**: The avatar shows the user's initials; the card never shows
  a broken image or waits on the Gravatar service to finish drawing. Servers with no internet access
  get initials for everyone.
- **Many cards by the same user**: Each user's Gravatar is looked up once per page, not once per
  card.
- **Preview too large or slow**: Cards show their icon until the preview loads; a slow image never
  changes the card's size.
- **Unknown content type icon**: A content type with no icon of its own uses the generic content
  icon.
- **Very long language tag or type name**: Cut to fit on the second line like the title; never
  pushes the card wider.
- **Keyboard users on a resized grid**: When the number of columns changes (window resized, folder
  tree toggled), Up/Down follow the new layout, and the focused card and anchor stay the same items.
- **Read-only grid**: A grid the user cannot act on takes no card focus and ignores selection
  shortcuts, as a read-only table does.

## Requirements *(mandatory)*

### Terms

- **Table view**: the current Content Drive listing, one row per item.
- **Grid view**: the new listing, one card per item.
- **View switcher**: the two-button control beside the search box that picks the view (reference
  image: list icon and grid icon).
- **Remembered view**: the user's last choice of view, kept in their browser for that browser only.
- **Editor**: the user the table shows in its Edited By column: for content, the user who last edited
  it; for folders, the folder's owner. The card's avatar and hover show this user; the card shows no
  date.
- **Unselectable item**: an item the table does not let the user select, for example one busy with a
  running bulk action. The grid follows the same rule wherever this term is used.

### Functional Requirements

- **FR-001**: Content Drive MUST offer a view switcher that toggles the listing between the table view
  and the grid view, and MUST show which view is active.
- **FR-002**: Switching views MUST keep the current folder, filters, search text, sort order, page,
  page size and selection, and MUST keep the focused item and selection anchor when they are on the
  page, scrolling the focused item into view.
- **FR-003**: The grid view MUST show the same items as the table view, in the same order, for the
  same page of results.
- **FR-004**: Each card MUST show: the image area, the selection checkbox, the title, the status, the
  language, the editor's avatar and the content type name, laid out as in the reference
  card.
- **FR-005**: Folder cards MUST omit status and language, and MUST show "Folder" as their type.
- **FR-006**: The editor MUST be shown as a small avatar. Hovering it, or tapping it on a touch screen,
  MUST reveal the same name the table shows in its Edited By column, and the name MUST be part of
  the card's screen reader label. The card MUST NOT show a date.
- **FR-007**: The avatar MUST show the editor's Gravatar, found from their email the same way the
  toolbar does, looked up at most once per user per page; when they have none, or Gravatar cannot be reached, it MUST show their
  initials instead.
- **FR-008**: The image area MUST show the same preview the table's thumbnail shows (image, SVG, PDF
  page), cropped to fill the area without distortion; for videos it MUST show a still frame with no
  player controls.
- **FR-009**: When an item has no preview, or its preview cannot be loaded, the image area MUST show its
  type icon (folder, page, or the content type's icon) instead, with no broken-image marker.
- **FR-010**: Titles and content type names MUST stay on one line, cut with an ellipsis when too long,
  with the full text available on hover.
- **FR-011**: All cards MUST have the same width and height, and the spacing between cards MUST be
  the same horizontally and vertically.
- **FR-012**: Cards MUST be at least 260px wide, as in Content Search's card view. The grid MUST fit
  as many columns as that allows, stretch them evenly to fill the row, recalculate when the window is
  resized or the folder tree is opened or closed, and never scroll horizontally. Below 260px of
  available width it MUST show one card that shrinks to fit.
- **FR-013**: Ticking a card's checkbox MUST add the item to the same selection the table uses, so bulk
  actions, the selection count and the opposite view all reflect it.
- **FR-014**: Unselectable items MUST NOT be selectable in the grid.
- **FR-015**: Double-clicking a card or clicking its title MUST open the item exactly like the
  matching table row.
- **FR-016**: Clicking a card's body MUST replace the selection with that card, and Shift-click and
  Ctrl/Cmd-click MUST extend it exactly as they do on table rows, following the grid's reading order.
  Ticking a card's checkbox MUST add or remove only that card.
- **FR-017**: Right-clicking a card MUST show the same context menu, with the same actions, as the
  matching table row.
- **FR-018**: The grid MUST use the same pagination control and page sizes as the table.
- **FR-019**: The Drive MUST remember the user's chosen view in their browser and open in it on later
  visits from that browser.
- **FR-020**: When there is no usable remembered view, the Drive MUST open in the table view.
- **FR-021**: Dragging cards onto a folder card or a folder in the tree MUST move them exactly as
  dragging table rows does, including dragging every selected item together.
- **FR-022**: Dropping files from the computer onto the grid MUST upload them exactly as dropping
  onto the table does.
- **FR-023**: The grid MUST show items in the sort order currently in effect, and MUST NOT offer its
  own sort control in this feature. Like the table, it MUST NOT keep the sort across a reload.
- **FR-024**: When the browser cannot store the choice, switching views MUST still work for the current
  visit and MUST NOT show an error.
- **FR-025**: Loading, empty and search-failed states MUST be shown in the grid as they are in the
  table.
- **FR-026**: Lock and shared-asset markers shown on table rows MUST also appear on the matching card.
- **FR-027**: Each card MUST offer a ⋮ "more actions" button that appears while the card is hovered
  or focused and opens the same menu as the table row's ⋮ button. It MUST NOT appear in a read-only
  grid.
- **FR-028**: A busy item MUST look and behave in the grid as a busy table row does: dimmed, marked
  busy for screen readers, not clickable or selectable, with a spinner in place of its preview.
- **FR-029**: While results load, the grid MUST show skeleton cards of the same size as real cards,
  as the table shows skeleton rows.
- **FR-030**: Cards, checkboxes and the view switcher MUST be operable with the keyboard and announced
  with meaningful labels to screen readers.
- **FR-031**: The grid MUST be a single Tab stop, and MUST stay reachable that way after every sort,
  page change, page-size change, filter change and search.
- **FR-032**: Left and Right MUST move focus one card in reading order; Up and Down MUST move focus to
  the same column one row away (to the last card of a shorter last row). None of these keys may
  change the selection, scroll the listing on their own, or change the page.
- **FR-033**: Shift with any arrow MUST extend or shrink the selection from the anchor card to the
  focused card in reading order, keeping cards that were already selected, exactly as Shift+Arrow
  does in the table. Shift-clicking a card's checkbox MUST select the range from the anchor.
- **FR-034**: Unselectable items MUST be skipped by arrow movement and left out of ranges; a
  read-only grid MUST take no card focus and accept no selection shortcut.
- **FR-035**: Escape, the search shortcut and the folder tree shortcut MUST behave in the grid exactly
  as in the table; Enter and Space on a focused card MUST change its selection as they do on a
  focused table row, and MUST NOT open it.
- **FR-036**: Any grid-only key behaviour (the two-direction arrows) MUST be documented alongside the
  existing Content Drive shortcuts.
- **FR-037**: All new visible text MUST be translatable, like the rest of the Drive.

### Key Entities

- **Drive item**: anything the Drive lists, either a folder or a content item (including pages,
  images and files). Carries a title or name, an editor (whose name the table already shows, and whose email the card needs for the Gravatar), a content type, and for content
  only, a language, a status and possibly a preview.
- **View preference**: the user's remembered choice of table or grid, per browser.

## Success Criteria *(mandatory)*

### Measurable Outcomes

- **SC-001**: With a full page of results at the largest page size, a user switches between table and
  grid with one click, and in 95% of switches the new view appears within 1 second, with no new
  search.
- **SC-002**: With the 260px minimum card width, the grid shows at least 4 cards per row on a 1920px
  wide window with the folder tree open, and at least 3 on a 1280px window.
- **SC-003**: For any page of results, 100% of cards share one height, and no title, chip or badge
  extends past its card's edge.
- **SC-004**: 0 broken-image markers appear in the grid, including for deleted or forbidden images.
- **SC-005**: After choosing a view, the Drive opens in that view on 100% of later visits from the
  same browser, unless the browser blocks storage.
- **SC-006**: Every bulk action available from a table selection is available, with the same result,
  from the same selection made in the grid.
- **SC-007**: Every Content Drive keyboard shortcut that works in the table also works in the grid, and
  a keyboard user can reach, move through and select any card on the page without the mouse.
  (Opening an item from the keyboard is a follow-up for both views.)

## Legacy Considerations *(dotCMS-specific — mandatory)*

- **Existing behavior touched**: Content Drive's listing area and toolbar. The table it uses is a
  shared component that pickers and the Action Center preview also use, so the grid must not change
  how the table looks or behaves anywhere. Custom tools and the older Content Search (JSP) screen
  are not touched.
- **Backward-compatibility expectations**: The table stays the default and keeps working exactly as
  today. Existing links, filters and bookmarks to the Drive keep opening as before. Custom tools keep
  their stored `dataViewMode`; nothing about how it is saved changes, and Content Search keeps using
  it unchanged.
- **Known related decisions**: Content Search's card view is the parity target. Content Search
  reads the tool's `dataViewMode` and opens in cards when it is `card` (`view_contentlets.jsp`), and
  the custom tool catalog work (#37574) exposes it as `list` or `card`. Making the Drive honour it
  is a separate ticket, so this feature's remembered-view rule must leave room for a tool setting to
  be added later. The keyboard behaviour must follow the Content Drive shortcuts spec (#32591,
  `specs/32591-content-drive-keybindings/spec.md`), changed only where a grid needs two-direction
  arrows. The plan will consult `dotCMS/platform-adrs`.

## Assumptions

- The grid is for Content Drive only; pickers and dialogs that reuse the Drive's table keep only the
  table.
- The view is remembered per browser, not per user account; a user on a new browser starts with the
  table.
- There is one remembered view for the whole Drive.
- Reading a custom tool's `dataViewMode` from its portlet configuration, and deciding how it ranks
  against the remembered view, is out of scope and belongs to a separate ticket.
- A sort control for the grid is out of scope and can be filed as a follow-up.
- The card's preview is the one the table already shows as its thumbnail; this feature adds no new
  rule for choosing which image represents a content item. The only difference is that videos show a
  still frame instead of a player.
- Drive items already carry the editor's name. The editor's email, needed for the Gravatar, may not
  be on them: whether the Drive search must return it is an open question for the plan. Whatever the
  answer, editor details MUST be resolved in one batch per page, never one request per card, and
  MUST NOT hold up drawing the cards; until they arrive, cards show initials or the placeholder.
- No keyboard way to open an item exists in the table today, and the grid does not add one. Adding
  it to both views is a follow-up.
- A reload resets the sort in both views; keeping the sort across reloads is not part of this
  feature.
- Status uses the same badge and wording as the table (Published, Draft, Changed, Archived).
- The grid shows the table's fixed details only; the per-type extra columns that the table adds for
  "Show in list" fields are not shown on cards.
- A content item's image is loaded only when its card is on screen or about to be.
