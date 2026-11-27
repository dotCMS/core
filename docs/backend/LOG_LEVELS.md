# Log Levels

## Choosing a log level

A default installation runs at INFO. An INFO line is therefore something an operator reads without having asked
for it. Before writing one, ask how often it will run on a busy site. A line that is useful once is noise when it
runs on every request.

| Level | Use it for | Examples |
|---|---|---|
| ERROR | Something failed and someone should look. | A job could not run. Data could not be saved. |
| WARN | Something is wrong or about to be, but the system carried on. | A fallback was used. A retry was needed. A setting is invalid and a default applies. |
| INFO | The system or an administrator changed something, or the system changed state. | Start-up and shutdown. A site was deleted. A reindex finished. A health check recovered. |
| DEBUG | What you would want when investigating one feature. | A lookup found nothing. A request took a branch. Each item of a batch. |

Decide by how often the statement runs:

| The statement runs | Level |
|---|---|
| Once when the application starts or stops, or once when something is first initialised | INFO |
| Once per change to stored data or configuration, and the message says what changed | INFO |
| Once per administrator action (an export, a reindex, a cache flush) | INFO |
| When a state flips (healthy to unhealthy, paused to running), not on every check | INFO |
| On every request, lookup, or call of a view tool, filter or interceptor | DEBUG |
| Once per item in a loop or a stream | DEBUG |
| On every run of a scheduled job, whether or not there was work | DEBUG |
| On a run of a scheduled job that did work, as one summary line | INFO |
| To mark entering or leaving a method | DEBUG |
| To print a whole object, a query, a request body or a list | DEBUG |
| To say that nothing was done | DEBUG |

Four cases the table gets wrong if you apply it without thinking:

- A loop inside start-up code is still start-up. One line per item at start-up is acceptable for a handful of
  items. For dozens, write one summary line at INFO and the items at DEBUG.
- A lazy initialiser inside a `get` method runs once, not per request. Its "initialised" line is INFO.
- Progress of a long operation belongs at INFO if it is throttled (every Nth item or every N seconds).
  `Logger.infoEvery` does the time-based kind.
- A method that is only called for a rare, significant operation can announce itself at INFO. "Running reindex
  switchover" is fine. "Entering getContent" is not.

## Expected conditions are not errors

A request that is refused, a permission that is denied, an item that is not found and input that does not
validate are normal outcomes. Do not log them at ERROR, and do not print a stack trace for them at the default
level. If the caller receives an exception or an error response, the log line is usually redundant, and a single
DEBUG line is enough. When the condition is swallowed and someone should still be able to see that it happened,
use `Logger.warnAndDebug`: it writes the message, the exception's own message and the top stack frame at WARN,
and the full stack only at DEBUG.

Log an exception once, at the place that handles it. Catching, logging and rethrowing produces the same stack
two or three times.

## Writing the statement

- Use the `Supplier` form for DEBUG messages that are built from parts, so the string is not built when DEBUG is
  off: `Logger.debug(this, () -> "Resolved " + count + " items for " + siteId);`
- Only move a message into a lambda when building it has no side effect. Some getters write as they read:
  `Contentlet.getTitle()` stores the title in the contentlet's map, and `Folder.getPath()` caches the path it
  looks up. Inside a lambda those calls stop running when DEBUG is off, so keep such messages in the plain form.
- Say what happened and to what, with the identifier: "Deleted site 'demo.dotcms.com'", not "Done".
- Do not print separator lines, blank lines or banners.
- Do not log passwords, tokens, keys, session identifiers or whole request bodies at any level. See
  [Security Patterns](SECURITY_BACKEND.md).
- `SecurityLogger`, `AdminLogger` and `ActivityLogger` are audit logs with their own destinations. This guide
  does not apply to them. Do not repeat an audit line with a plain `Logger.info` next to it.
- To see DEBUG output for one class on a running server, change its level through `/api/v1/logger`. See
  [Health Monitoring](HEALTH_MONITORING.md#dynamic-log-level-management).
