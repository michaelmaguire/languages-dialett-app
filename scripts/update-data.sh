#!/bin/sh
# Refresh the app's data from the languages-dialett repository checked out beside this one
# (or DATA_REPO=path): runs its step 6 into shared/.../files/dialett.json. Run its full build first
# (pipeline/build_all.sh) if its content changed.
set -e
here=$(cd "$(dirname "$0")/.." && pwd)
data=${DATA_REPO:-$here/../languages-dialett}
"$data/pipeline/.venv/bin/python" "$data/pipeline/step6_build_app_data.py" \
    --out "$here/shared/src/commonMain/composeResources/files/dialett.json"
