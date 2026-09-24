# @dotcms/angular build toolchain

The published `@dotcms/angular` is built with the Angular pinned in this folder's `package.json`, not with core-web's Angular. This is what lets one package support every Angular from the floor up (#37680).

## Why the package has to be built with the oldest supported Angular

Angular libraries ship partially compiled: each component is an `ɵɵngDeclare*` call that the app's own Angular linker compiles at build time. Angular only guarantees that an app can link a library built with the same major or an older one. When the SDK was built with core-web's Angular 22, it could not be used by any app older than 21.2. The 22 compiler emitted `ChangeDetectionStrategy.Eager`, which older linkers reject with `Unsupported change detection strategy`. The Angular 22 linker also uses the library's build version to choose defaults, such as the change detection used when a component does not set one. So the same package would behave differently on 21 than on 22.

A package built with Angular 19 links on every Angular from 19 onward and behaves the same on all of them. Newer linkers read older declarations; that direction is the one Angular guarantees.

## The floor: Angular 19.0

Angular 19.0 is the first release where `input()`, which the SDK uses, is stable rather than developer preview. The declared peer range in `../package.json` is `>=19.0.0` and must stay in step with the major pinned here.

What that means when writing SDK code:

- Only use Angular APIs that exist in the pinned version. If you use a newer one, `pnpm nx build sdk-angular` fails to compile. That compile failure is the guard.
- `ChangeDetectionStrategy.Eager` does not exist in 19. Use `ChangeDetectionStrategy.Default`, which is the same strategy under its older name.
- Unit tests still run on core-web's Angular, so they cover the newest version. This build covers the oldest.

## Raising the floor

1. Bump every `@angular/*`, `ng-packagr` and `typescript` in `package.json` to the new version, together. `typescript` must be in the range the new `@angular/compiler-cli` accepts.
2. Run `npm install --ignore-scripts` here to refresh `package-lock.json`.
3. Raise the `@angular/*` peer ranges in `../package.json` to the new major.
4. Run `pnpm nx build sdk-angular` from `core-web`.

CI checks step 3 against the built package: `.github/scripts/validate-sdk-package-shapes` (`check-angular-peer-floor`) reads the Angular version stamped into the build and fails if the peer range admits anything older.

## How the build runs

`pnpm nx build sdk-angular` runs `build.mjs`. The script installs this folder with `npm ci --ignore-scripts`, but only when `package-lock.json` has changed. It then runs this folder's `ng-packagr` against `../ng-package.json`, writing to `dist/libs/sdk/angular` as before. `tsconfig.build.json` resolves Angular from this folder's `node_modules`, and resolves `@dotcms/client`, `@dotcms/uve` and `@dotcms/types` from their built `dist/` output. Nx builds those first.
