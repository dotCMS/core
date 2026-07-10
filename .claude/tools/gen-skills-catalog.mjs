#!/usr/bin/env node
// Generates .claude/skills/CATALOG.md from skill frontmatter.
// Auto-generated file — never hand-edit CATALOG.md; run `just skills-catalog`.
import { writeFileSync } from 'node:fs';
import { CATALOG_PATH, buildCatalog } from './skill-lib.mjs';

const { content, firstPartyCount, externalCount } = buildCatalog();
writeFileSync(CATALOG_PATH, content);
console.log(`Wrote ${CATALOG_PATH} (${firstPartyCount} first-party, ${externalCount} external).`);
