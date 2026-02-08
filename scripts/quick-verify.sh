#!/bin/bash

# Quick Project Verification for Navigator TDC

echo "🚀 Quick Navigator TDC Verification"
echo "==================================="

# Check key files exist
echo "📁 Checking project structure..."
echo "✅ build.gradle: $([ -f build.gradle ] && echo 'FOUND' || echo 'MISSING')"
echo "✅ app/build.gradle: $([ -f app/build.gradle ] && echo 'FOUND' || echo 'MISSING')"
echo "✅ Android Manifest: $([ -f app/src/main/AndroidManifest.xml ] && echo 'FOUND' || echo 'MISSING')"
echo "✅ CI Workflow: $([ -f .github/workflows/ci.yml ] && echo 'FOUND' || echo 'MISSING')"

# Check wake word files
echo ""
echo "🎤 Checking wake word system..."
echo "✅ Wake word config: $([ -f app/src/main/assets/claw_wake_word_model_config.json ] && echo 'FOUND' || echo 'MISSING')"
echo "✅ Wake word detector: $([ -f app/src/main/java/com/tdc/navigator/wakework/TDCWakeWordDetector.kt ] && echo 'FOUND' || echo 'MISSING')"
echo "✅ Wake word test: $([ -f scripts/test-claw-wake-word.sh ] && echo 'FOUND' || echo 'MISSING')"

# Check vehicle control files
echo ""
echo "🚗 Checking vehicle control system..."
vehicle_files=(
    "app/src/main/java/com/tdc/navigator/vehicle/NavigatorVehicleManager.kt"
    "app/src/main/java/com/tdc/navigator/vehicle/control/NavigatorClimateControl.kt"
    "app/src/main/java/com/tdc/navigator/vehicle/control/NavigatorWindowControl.kt"
    "app/src/main/java/com/tdc/navigator/vehicle/control/NavigatorSeatControl.kt"
)

for file in "${vehicle_files[@]}"; do
    echo "✅ $(basename "$file"): $([ -f "$file" ] && echo 'FOUND' || echo 'MISSING')"
done

# Check test files
echo ""
echo "🧪 Checking test infrastructure..."
test_files=(
    "app/src/test/java/com/tdc/navigator/vehicle/NavigatorVehicleManagerTest.kt"
    "app/src/test/java/com/tdc/navigator/wakework/TDCWakeWordDetectorTest.kt"
    "scripts/run-comprehensive-tests.sh"
)

for file in "${test_files[@]}"; do
    echo "✅ $(basename "$file"): $([ -f "$file" ] && echo 'FOUND' || echo 'MISSING')"
done

# Check git status
echo ""
echo "🔗 Git repository status..."
echo "✅ Git repo: $([ -d .git ] && echo 'INITIALIZED' || echo 'NOT INITIALIZED')"
if [ -d .git ]; then
    remote_url=$(git remote get-url origin 2>/dev/null || echo "No remote")
    echo "✅ Remote: $remote_url"
    echo "✅ Branch: $(git branch --show-current)"
fi

# File count
echo ""
echo "📊 Project statistics..."
total_files=$(find . -type f -not -path "./.git/*" | wc -l)
echo "✅ Total files: $total_files"

# Check for ProjectMind references
echo ""
echo "🔍 Checking separation from ProjectMind..."
if grep -r "projectmind" . --include="*.gradle" --include="*.kt" --include="*.java" 2>/dev/null | head -3; then
    echo "⚠️  ProjectMind references found"
else
    echo "✅ Clean separation - no ProjectMind references"
fi

echo ""
echo "🎯 Navigator TDC appears ready for development!"
echo "Next: Install Android SDK and open in Android Studio"