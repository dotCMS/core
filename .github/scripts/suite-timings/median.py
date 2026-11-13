"""Median job runtime per MainSuite, read from suite-timings.sh on stdin.

Input is one TSV line per sampled job: suite label, started_at, completed_at.
Output is a table ordered fastest first, plus the suite a new test class should
be registered in.
"""
import datetime
import statistics
import sys


def parsed(stamp):
    return datetime.datetime.fromisoformat(stamp.replace("Z", "+00:00"))


durations = {}
for line in sys.stdin:
    fields = line.rstrip("\n").split("\t")
    if len(fields) != 3 or not fields[1] or not fields[2]:
        continue
    label, started, completed = fields
    minutes = (parsed(completed) - parsed(started)).total_seconds() / 60
    durations.setdefault(label, []).append(minutes)

if not durations:
    sys.exit(
        "No MainSuite job timings in the sampled runs.\n"
        "Report this and ask the developer which suite to use - do not guess a default."
    )

rows = sorted((statistics.median(v), label, v) for label, v in durations.items())

print()
print("MainSuite    median  samples   each run (min)")
for median, label, values in rows:
    each = ", ".join("%.0f" % value for value in values)
    print("%-10s %6.1fm %7d   %s" % (label, median, len(values), each))
print()
print("-> register new coverage in MainSuite%s (%.1f min median)" % (rows[0][1], rows[0][0]))
print("   slowest suite is MainSuite%s (%.1f min median)" % (rows[-1][1], rows[-1][0]))
