import { type InferSchema, type ToolExtraArguments, type ToolMetadata } from 'xmcp';
import { z } from 'zod';

import { placeContent, type PagePlaceContentOptions } from '../lib/page-place-content';
import { runtimeFromEnv, toolFailure } from '../lib/runtime';

// A slot address: a 1-based index into the page's real slots, OR a container reference. `instance`
// (the slot uuid) is optional only when the container occupies exactly one slot on the page.
const slotAddress = z.union([
    z
        .number()
        .int()
        .positive()
        .describe('1-based index into the page’s slots, in layout order (the first slot is 1).'),
    z
        .object({
            container: z
                .string()
                .min(1)
                .describe(
                    'Container key as it appears on the page: a container id/shorty, a container ' +
                        'file path (e.g. "//demo.dotcms.com/application/containers/default/"), or a ' +
                        'recognizable fragment of either. "SYSTEM_CONTAINER" for the system container.'
                ),
            instance: z
                .string()
                .optional()
                .describe(
                    'Slot instance uuid (e.g. "1", "10"). Required only when the container appears ' +
                        'in more than one slot; the tool errors and lists the instances if omitted.'
                )
        })
        .strict()
]);

export const schema = {
    path: z
        .string()
        .min(1)
        .describe(
            'Page URL path, e.g. "/about-us", host-qualified path such as "//demo.dotcms.com/about-us", or a page identifier. Bare paths/identifiers require site.'
        ),
    // Conditionally required: a host-qualified path already supplies the site. Marking this field
    // required in JSON Schema would incorrectly reject the supported `//hostname/path` form; the
    // resolver enforces that exactly one source of site identity is present before any request.
    site: z
        .string()
        .min(1)
        .optional()
        .describe(
            'Site hostname or identifier (UUID). Required for a bare path or page identifier. Omit only when path is host-qualified as "//hostname/path".'
        ),
    slots: z
        .array(
            z
                .object({
                    slot: slotAddress,
                    contentlets: z
                        .array(z.string().min(1))
                        .describe(
                            'Contentlet identifiers to place, in order. For op "remove" these are ' +
                                'the ids to remove; to clear a slot use op "set" with [].'
                        ),
                    op: z
                        .enum(['append', 'set', 'remove'])
                        .optional()
                        .describe(
                            'How to combine with the slot’s current content. "append" (default) ' +
                                'adds after existing (de-duped), "set" replaces, "remove" removes.'
                        )
                })
                .strict()
        )
        .min(1)
        .describe(
            'One or more slot assignments applied in a single atomic write. Placing content in one ' +
                'slot is just an array of one. Each entry: { slot, contentlets, op? }.'
        ),
    // ── scope ──
    variantName: z.string().optional().describe('Variant to write to. Default "DEFAULT".'),
    languageId: z.number().int().positive().optional().describe('Language id. Default 1.'),
    mode: z
        .enum(['merge', 'replace'])
        .optional()
        .describe(
            '"merge" (default) preserves every slot you don’t touch. "replace" treats the slots ' +
                'you pass as the complete page — every other slot is cleared.'
        )
};

export const metadata: ToolMetadata = {
    name: 'page_place_content',
    description: `Place content into a dotCMS page's container slots — safely, without wiping the rest of the page.

The underlying endpoint (POST /api/v1/page/{pageId}/content) is a FULL replacement: it rewrites the
page's entire container-to-contentlet map, and any slot omitted from the body is emptied. Adding one
contentlet "the raw way" therefore silently clears every other slot. This tool removes that footgun:
it reads the page's current content, applies your change to the addressed slot(s) only, and writes
the COMPLETE map back — so untouched slots survive.

You do NOT need to call GET /api/v1/page/json first. Give the page \`path\` and address a slot by:
  - a 1-based \`slot\` index (in layout order), or
  - \`slot: { container, instance? }\` — a container id/path/fragment, plus the slot uuid when that
    container appears in more than one slot (the tool lists the instances if you omit it).
A slot that doesn't resolve fails with the list of valid slots, so a typo can't silently no-op.

Ops (per entry in \`slots[]\`):
  - append (default) — add the ids after what's already in the slot (de-duplicated)
  - set              — replace the slot's content with exactly these ids ([] clears the slot)
  - remove           — remove these ids from the slot

Targeting: pass \`site\` with every bare path/identifier, or use a host-qualified path such as
\`//awazon.dotcms.site/index\`. The tool resolves the hostname to \`host_id\` before reading the page
and never silently falls back to the default site. If explicit and embedded sites conflict, it
fails before making a request.

Shape: { path, site?, slots: [{ slot, contentlets, op? }, ...] }. One atomic write across all listed slots
— placing content in a single slot is just an array of one: slots: [{ slot, contentlets }].

Modes: "merge" (default) keeps every slot you don't address. "replace" treats the slots you pass as
the whole page and clears all others — use it only for deliberate whole-page authoring.

Scope: \`variantName\` (default DEFAULT) and \`languageId\` (default 1) target a specific A/B variant
and language.

Returns a manifest: { pageId, site, url, variantName, languageId, mode, slots: [{ identifier, uuid,
before[], after[], changed }], warnings[] }. \`warnings\` flags any slot that lost content and
explains a net-loss 409 (refresh and retry). Contentlets whose type isn't allowed in a container are
rejected by the backend; archived/missing ids are skipped and show up as a slot that didn't gain them.

Typical flow: create the (blank) page with \`page_create\`, then populate it with this tool. The
contentlets themselves are created separately (e.g. via the \`execute\` tool) — this tool only places
existing contentlets into slots.`,
    annotations: {
        title: 'Place Content on a dotCMS Page',
        readOnlyHint: false,
        destructiveHint: true,
        idempotentHint: false,
        openWorldHint: true
    }
};

export default async function handler(
    args: InferSchema<typeof schema>,
    extra?: ToolExtraArguments
) {
    try {
        const options: PagePlaceContentOptions = {
            dotcms: runtimeFromEnv(extra?.sessionId),
            path: args.path,
            site: args.site,
            slots: args.slots,
            variantName: args.variantName,
            languageId: args.languageId,
            mode: args.mode
        };

        const manifest = await placeContent(options);

        return JSON.stringify(manifest, null, 2);
    } catch (error) {
        return toolFailure('page_place_content', error);
    }
}
