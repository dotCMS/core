# @dotcms/types

## Overview

This library contains shared TypeScript types and interfaces used across the dotCMS SDK libraries and the Universal Visual Editor. It serves as a central repository for type definitions to ensure consistency and type safety across the ecosystem.

## Purpose

- Establish a single source of truth for common types
- Maintain consistency across SDK libraries
- Support the Universal Visual Editor with necessary type definitions
- Reduce duplication and prevent drift between related interfaces

## Usage

Import types directly from this library:

```typescript
import { Block, Contentlet } from '@dotcms/types';
```

## Universal Visual Editor

Types in this library provide the foundation for the Universal Visual Editor, including:

- Component definitions
- Editor configuration schemas
- Content type mappings
- UI element specifications