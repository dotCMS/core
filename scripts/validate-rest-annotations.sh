#!/bin/bash

# REST Endpoint Annotation Validation Script
# This script runs the REST endpoint annotation compliance tests
# and reports on any violations found.

set -e

SCRIPT_DIR="$(cd "$(dirname "${BASH_SOURCE[0]}")" && pwd)"
PROJECT_ROOT="$(dirname "$SCRIPT_DIR")"
DOTCMS_DIR="$PROJECT_ROOT/dotCMS"

echo "🔍 REST Endpoint Annotation Validation"
echo "======================================="
echo ""

# Check if we're in the right directory
if [ ! -f "$DOTCMS_DIR/pom.xml" ]; then
    echo "❌ Error: Could not find dotCMS pom.xml at $DOTCMS_DIR/pom.xml"
    echo "Please run this script from the project root or ensure the project structure is correct."
    exit 1
fi

echo "📁 Project root: $PROJECT_ROOT"
echo "📁 dotCMS directory: $DOTCMS_DIR"
echo ""

# Change to project root where mvnw is located
cd "$PROJECT_ROOT"

echo "🧪 Running REST endpoint annotation compliance tests..."
echo ""

# Run the specific test class from the dotCMS module (use the simpler compliance test)
./mvnw test -pl :dotcms-core -Dtest=RestEndpointAnnotationComplianceTest#testSampleRestResourceAnnotationCompliance -q

TEST_EXIT_CODE=$?

if [ $TEST_EXIT_CODE -eq 0 ]; then
    echo ""
    echo "✅ REST endpoint annotation validation completed successfully!"
    echo ""
    echo "📋 To run all annotation validation tests:"
    echo "   ./mvnw test -pl :dotcms-core -Dtest=RestEndpointAnnotationComplianceTest"
    echo ""
    echo "📖 For annotation standards, see:"
    echo "   dotCMS/src/main/java/com/dotcms/rest/README.md"
else
    echo ""
    echo "⚠️  REST endpoint annotation violations found!"
    echo ""
    echo "🔧 To fix violations:"
    echo "   1. Review the detailed report above"
    echo "   2. Update REST endpoints according to the standards in:"
    echo "      dotCMS/src/main/java/com/dotcms/rest/README.md"
    echo "   3. Re-run this script to verify fixes"
    echo ""
    echo "📋 To see examples of proper annotations:"
    echo "   ./mvnw test -pl :dotcms-core -Dtest=RestEndpointAnnotationComplianceTest#testAnnotationStandardsExamples"
fi

exit $TEST_EXIT_CODE