# @dotcms/angular build toolchain

The published `@dotcms/angular` is built with the Angular pinned in this folder's `package.json`, not with core-web's Angular. This is what lets one package support every Angular from the floor up (#37680).

## Why the package has to be built with the oldest supported Angular

Angular libraries ship partially compiled: each component is an `ɵɵngDeclare*` call that the app's own Angular linker compiles at build time. Angular only guarantees that an app can link a library built with the same major or an older one. When the SDK was built with core-web's Angular 22, the 22 compiler emitted `ChangeDetectionStrategy.Eager`, which older linkers reject with `Unsupported change detection strategy`. The Angular 22 linker also uses the library's build version to choose defaults, such as the change detection used when a component does not set one, so the same package would behave differently on 21 than on 22.

A package built with Angular 21.0 links on every Angular from 21.0 onward and behaves the same on all of them. Newer linkers read older declarations; that direction is the one Angular guarantees.

## Why not `@nx/angular:package`

The `build` target in `../project.json` runs `build.mjs` instead of Nx's `@nx/angular:package` executor on purpose. That executor always compiles with core-web's `ng-packagr` and Angular, which is exactly what produced the unusable 22-built package. Nx still owns the task graph: `^build` builds `@dotcms/client`, `@dotcms/uve` and `@dotcms/types` first. Only the compiler lives here.

## The floor: Angular 21.0

The SDK supports Angular 21 and newer. It is built with the last 21.0 patch, so the compiler cannot emit anything a 21.0 app can't read. The declared peer range in `../package.json` is `>=21.0.0` and must stay in step with the version pinned here.

What that means when writing SDK code:

- Only use Angular APIs that exist in the pinned version. If you use a newer one, `pnpm nx build sdk-angular` fails to compile. That compile failure is the guard.
- `ChangeDetectionStrategy.Eager` only exists from 21.2. Use `ChangeDetectionStrategy.Default`, which is the same strategy under its older name.
- Unit tests still run on core-web's Angular, so they cover the newest version. This build covers the oldest.
- The pinned Angular is a build-only dev dependency: apps compile the SDK with their own Angular. Security advisories against it only matter if the SDK uses the affected feature.

## Raising the floor

1. Bump every `@angular/*`, `ng-packagr` and `typescript` in `package.json` to the new version, together. `typescript` must be in the range the new `@angular/compiler-cli` accepts.
2. Delete `package-lock.json` and `node_modules`, then run `npm install --ignore-scripts` here to regenerate the lockfile.
3. Raise the `@angular/*` peer ranges in `../package.json` to the new major.
4. Run `pnpm nx build sdk-angular` from `core-web`.

CI checks step 3 against the built package: `.github/scripts/validate-sdk-package-shapes` (`check-angular-peer-floor`) reads the Angular version stamped into the build and fails if the peer range admits anything older.

## How the build runs

`pnpm nx build sdk-angular` runs `build.mjs`, which:

1. Installs this folder with `npm ci --ignore-scripts`, but only when `package-lock.json` has changed.
2. Writes `node_modules/.tsconfig.generated.json`, extending `tsconfig.build.json` with a path for every `@angular/*` entry point in this folder, read from each package's `exports`. The SDK sources live under core-web, so without these paths an `@angular/*` import would resolve to core-web's newer Angular. Angular 21+ only exposes its types through `exports`, so a static wildcard in `tsconfig.build.json` would miss some of them.
3. Runs this folder's `ng-packagr` against `../ng-package.json`, writing to `dist/libs/sdk/angular` as before.

`tsconfig.build.json` resolves `@dotcms/client`, `@dotcms/uve` and `@dotcms/types` from their built `dist/` output.
