#!/bin/bash

# Script Consolidation Cleanup
# Run this after verifying the new dotcms-analytics-test.sh works correctly

set -e

SCRIPT_DIR="$(cd "$(dirname "${BASH_SOURCE[0]}")" && pwd)"
ARCHIVE_DIR="$SCRIPT_DIR/archive"

echo "🧹 DotCMS Analytics Testing - Script Cleanup"
echo "============================================"
echo ""

# Create archive directory
mkdir -p "$ARCHIVE_DIR"

echo "📦 Creating archive of old scripts..."

# Archive scripts that have been consolidated
if [ -f "run-analytics-scaling-test.sh" ]; then
    echo "  → Archiving run-analytics-scaling-test.sh"
    mv run-analytics-scaling-test.sh "$ARCHIVE_DIR/run-analytics-scaling-test-legacy.sh"
fi

if [ -f "run-direct-analytics-scaling-test.sh" ]; then
    echo "  → Archiving run-direct-analytics-scaling-test.sh"  
    mv run-direct-analytics-scaling-test.sh "$ARCHIVE_DIR/run-direct-analytics-scaling-test-legacy.sh"
fi

if [ -f "analyze-jmeter-results.sh" ]; then
    echo "  → Archiving analyze-jmeter-results.sh"
    mv analyze-jmeter-results.sh "$ARCHIVE_DIR/analyze-jmeter-results-legacy.sh"
fi

# Archive other helper scripts that might be redundant
if [ -f "scripts/deploy-to-k8s.sh" ]; then
    echo "  → Archiving scripts/deploy-to-k8s.sh (functionality moved to setup command)"
    mv scripts/deploy-to-k8s.sh "$ARCHIVE_DIR/deploy-to-k8s-legacy.sh"
fi

echo ""
echo "✅ Cleanup completed!"
echo ""
echo "📁 Archived files are now in: $ARCHIVE_DIR/"
echo ""
echo "🚀 New unified workflow:"
echo "  ./dotcms-analytics-test.sh setup                    # Replaces: deploy-to-k8s.sh"
echo "  ./dotcms-analytics-test.sh scaling-test             # Replaces: both scaling test scripts"
echo "  ./dotcms-analytics-test.sh analyze-all              # Replaces: analyze-jmeter-results.sh"
echo "  ./dotcms-analytics-test.sh bottleneck-analysis      # New: comprehensive bottleneck testing"
echo "  ./dotcms-analytics-test.sh generate-report          # New: full performance reports"
echo ""
echo "📖 See CONSOLIDATION_ANALYSIS.md for complete details"
echo ""
echo "⚠️  If you need to restore any archived scripts:"
echo "   mv archive/script-name-legacy.sh script-name.sh" 