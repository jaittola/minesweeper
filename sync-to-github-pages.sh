#!/bin/bash -e

me=`whoami`
url="${1:-git@github.com:${me}/${me}.github.io.git}"
wd=`pwd`

# Create a temporary dir and set up cleaning at exit.
checkoutdir=`mktemp -d "${wd}/sync-to-github.XXXXXXXX"`
trap "rm -rf \"$checkoutdir\"" EXIT

# Helper variables
pagesdir="${checkoutdir}/pages"
msdir="${pagesdir}/minesweeper"

# Fetch sources.
git clone "$url" "$pagesdir"

# Build
lein clean && lein cljsbuild once min

# Copy code to checked out source dir
cd resources/public
rsync -av --exclude=js/analytics.js . "$msdir"

# Commit
cd ${msdir}
git add .
git commit -m "Update minesweeper"
git push origin master

cd "$wd"
lein clean
