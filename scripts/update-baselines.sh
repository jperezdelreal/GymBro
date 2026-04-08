#!/bin/bash
#
# Update Visual Regression Baselines
# Copies latest Maestro screenshots to baselines directory
#
# Usage: npm run update-baselines

set -e

BASELINES_DIR="android/.maestro/baselines"
MAESTRO_SCREENSHOTS="$HOME/.maestro/tests/screenshots"

echo "🔄 Updating visual regression baselines..."
echo ""

# Check if Maestro screenshots directory exists
if [ ! -d "$MAESTRO_SCREENSHOTS" ]; then
  echo "❌ Maestro screenshots directory not found: $MAESTRO_SCREENSHOTS"
  echo "   Run Maestro flows first to generate screenshots:"
  echo "   maestro test android/.maestro/"
  exit 1
fi

# Count screenshots
SCREENSHOT_COUNT=$(find "$MAESTRO_SCREENSHOTS" -name "*.png" 2>/dev/null | wc -l)
if [ "$SCREENSHOT_COUNT" -eq 0 ]; then
  echo "❌ No screenshots found in $MAESTRO_SCREENSHOTS"
  echo "   Run Maestro flows first to generate screenshots:"
  echo "   maestro test android/.maestro/"
  exit 1
fi

echo "Found $SCREENSHOT_COUNT screenshots in Maestro output"

# Create baselines directory if it doesn't exist
mkdir -p "$BASELINES_DIR"

# Copy screenshots to baselines
echo "Copying screenshots to $BASELINES_DIR..."
cp -v "$MAESTRO_SCREENSHOTS"/*.png "$BASELINES_DIR/" 2>/dev/null || {
  echo "❌ Failed to copy screenshots"
  exit 1
}

echo ""
echo "✅ Baselines updated successfully!"
echo ""
echo "Next steps:"
echo "  1. Review changes with: git diff --stat"
echo "  2. Commit the baselines: git add $BASELINES_DIR && git commit -m 'chore: update visual regression baselines'"
echo ""
