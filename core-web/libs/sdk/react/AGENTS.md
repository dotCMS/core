# CLAUDE.md

This file provides guidance to Claude Code (claude.ai/code) when working with code in this repository.

## Essential Commands

### Development Commands
```bash
# Build the library
nx build sdk-react

# Run tests
nx test sdk-react

# Run tests with coverage
nx test sdk-react --configuration=ci

# Lint code
nx lint sdk-react

# Run specific test file
nx test sdk-react --testNamePattern="DotCMSLayoutBody"
```

### Package Management
```bash
# Install dependencies (run from workspace root)
npm install

# Publish package (from dist directory)
nx nx-release-publish sdk-react
```

## Architecture Overview

This is the **@dotcms/react** SDK - a React component library that provides components and hooks for integrating React applications with dotCMS. The library is built using **Nx** as a monorepo tool with **Rollup** for bundling.

### Key Technologies
- **React 18+** (peer dependency)
- **TypeScript** with strict mode
- **Nx** for build tooling and project management
- **Rollup** for ESM bundling
- **Jest** for testing
- **ESLint** for linting
- **Babel** for transformation

### Project Structure
```
src/
├── index.ts                # Main export file
└── lib/
    └── next/               # Next.js optimized components
        ├── components/     # React components
        │   ├── DotCMSLayoutBody/
        │   ├── DotCMSShow/
        │   ├── DotCMSEditableText/
        │   └── DotCMSBlockEditorRenderer/
        ├── hooks/          # Custom React hooks
        └── contexts/       # React contexts
```

## Core Components & Patterns

### Component Architecture
All components follow these patterns:

1. **Client-side only components** - Use React hooks and contexts
2. **TypeScript interfaces** - Proper typing with `@dotcms/types`
3. **Modular CSS** - CSS modules where needed
4. **Comprehensive testing** - Jest with React Testing Library

### Key Dependencies
- `@dotcms/uve`: Universal Visual Editor integration
- `@dotcms/client`: Core dotCMS client functionality
- `@dotcms/types`: TypeScript definitions
- `@tinymce/tinymce-react`: Rich text editing

### Export Strategy
The library uses a clean export pattern from `src/index.ts`:
- All public components and hooks are explicitly exported
- Components are exported with their prop types
- Follows tree-shaking friendly patterns

## Development Workflow

### Testing Standards
- **Jest** configuration in `jest.config.ts`
- Tests located in `__test__/` directories
- Test files follow pattern: `ComponentName.test.tsx`
- Use React Testing Library for component testing
- Mock external dependencies in `mock.ts`

### Building & Distribution
- **Rollup** builds ESM modules for distribution
- Output goes to `dist/libs/sdk/react/`
- Package includes README.md as asset
- Generates exports field automatically
- External dependencies: `react/jsx-runtime`

### Code Quality
- **ESLint** configuration for React/TypeScript
- **Strict TypeScript** mode enabled
- **Babel** transformation for Jest
- **Coverage** reporting to workspace root

## Key Integration Points

### dotCMS Integration
- Integrates with dotCMS Universal Visual Editor (UVE)
- Uses `@dotcms/client` for API communication
- Supports real-time page editing through UVE context
- Handles dotCMS page asset rendering

### React Patterns
- Uses React 18+ features (jsx-runtime)
- Context-based state management
- Custom hooks for dotCMS functionality
- Supports both development and production modes

### Universal Visual Editor (UVE)
- `useEditableDotCMSPage` hook for real-time editing
- `DotCMSShow` component for conditional rendering
- `DotCMSEditableText` for inline text editing
- Mode detection (EDIT, PREVIEW, PUBLISHED)

## Important Notes

### Peer Dependencies
- React 18+ and React DOM 18+ are required
- No direct React dependencies to avoid version conflicts
- Uses latest versions of internal dotCMS packages

### Build Configuration
- Targets ESM format only
- Uses Babel for Jest transformation
- Rollup handles bundling with React plugins
- CSS extraction is disabled (CSS-in-JS approach)

### Testing Environment
- Jest preset from workspace root
- React-specific transformations
- Coverage directory in workspace root
- Supports CI configuration with coverage reporting
