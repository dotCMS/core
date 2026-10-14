# Data Model: Migrate core-web from @Injectable to @Service

No persisted data. The migration tracks three working entities, kept in the issue and the PR descriptions rather than in code.

## Service class

| Field | Meaning |
|-------|---------|
| path | File under `core-web/` |
| area | Project it belongs to (decides its batch) |
| before | `@Injectable({providedIn: 'root'})`, bare `@Injectable()`, or other |
| after | `@Service()`, `@Service({autoProvided: false})`, or unchanged |
| kind | real service, test double, or SDK |

**Rules**: `before = providedIn root` gives `after = @Service()`. `before = bare` gives `@Service({autoProvided: false})`. `kind = SDK` always stays unchanged. Scope (root or not) never changes.

## Batch

| Field | Meaning |
|-------|---------|
| area path | Value passed to the schematic's `--path` |
| files changed | Count, target at most about 100 |
| verification | lint, test, build results for affected projects |

## Skip entry

| Field | Meaning |
|-------|---------|
| path / class | The skipped class |
| reason | constructor injection, extra options, non-root `providedIn`, or SDK exclusion |
| resolution | converted to `inject()` then migrated, or left with the reason |

**State flow**: candidate (dry run) → migrated, or skipped with reason → resolved.
