#!/usr/bin/env bash
# Prints the total line coverage from the JaCoCo report (run `mvn verify` first).
# The pipeline reads this line with a `coverage:` regex to feed the GitLab coverage badge:
#   Total coverage: 87.3 %
set -euo pipefail
cd "$(dirname "$0")/.."

CSV=target/site/jacoco/jacoco.csv
[ -f "$CSV" ] || { echo "No JaCoCo report at $CSV (run mvn verify first)" >&2; exit 1; }

# columns: 8 = LINE_MISSED, 9 = LINE_COVERED
awk -F, 'NR > 1 { missed += $8; covered += $9 }
         END { total = missed + covered
               if (total == 0) { print "Total coverage: 0.0 %"; exit }
               printf "Total coverage: %.1f %%\n", 100 * covered / total }' "$CSV"
