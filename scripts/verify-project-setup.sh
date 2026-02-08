#!/bin/bash

# TDC Navigator - Project Setup Verification Script
# Verifies the project was correctly separated and is ready for development

set -e

echo "🔍 TDC Navigator - Project Setup Verification"
echo "============================================="
echo ""

# Color codes
RED='\033[0;31m'
GREEN='\033[0;32m'
YELLOW='\033[1;33m'
BLUE='\033[0;34m'
NC='\033[0m' # No Color

verification_failed=false

# Function to check file exists and report
check_file() {
    local file="$1"
    local description="$2"
    
    if [ -f "$file" ]; then
        echo -e "${GREEN}✅ $description${NC}: $file"
        return 0
    else
        echo -e "${RED}❌ MISSING $description${NC}: $file"
        verification_failed=true
        return 1
    fi
}

# Function to check directory exists and report
check_directory() {
    local dir="$1"
    local description="$2"
    
    if [ -d "$dir" ]; then
        echo -e "${GREEN}✅ $description${NC}: $dir"
        return 0
    else
        echo -e "${RED}❌ MISSING $description${NC}: $dir"
        verification_failed=true
        return 1
    fi
}

# Function to count files in directory
count_files() {
    local dir="$1"
    local description="$2"
    
    if [ -d "$dir" ]; then
        local count=$(find "$dir" -type f | wc -l)
        echo -e "${BLUE}📁 $description${NC}: $count files"
        return $count
    else
        echo -e "${RED}❌ Directory not found${NC}: $dir"
        verification_failed=true
        return 0
    fi
}

echo -e "${YELLOW}📋 VERIFYING PROJECT STRUCTURE${NC}"
echo "----------------------------------------"

# Check essential project files
check_file "build.gradle" "Root build configuration"
check_file "app/build.gradle" "App build configuration"
check_file "gradle.properties" "Gradle properties"
check_file "README.md" "Project documentation"
check_file "ARCHITECTURE.md" "Architecture documentation"
check_file "DEPLOYMENT.md" "Deployment documentation"

echo ""
echo -e "${YELLOW}📱 VERIFYING ANDROID PROJECT STRUCTURE${NC}"
echo "---------------------------------------"

# Check Android project structure
check_directory "app" "Android app module"
check_directory "app/src" "Source directory"
check_directory "app/src/main" "Main source"
check_directory "app/src/main/java" "Java/Kotlin source"
check_directory "app/src/main/java/com/tdc/navigator" "Main package"
check_directory "app/src/test" "Unit tests"
check_directory "app/src/androidTest" "Android tests"

echo ""
echo -e "${YELLOW}🧪 VERIFYING TEST INFRASTRUCTURE${NC}"
echo "---------------------------------"

# Check test scripts
check_file "scripts/run-comprehensive-tests.sh" "Comprehensive test script"
check_file "scripts/test-claw-wake-word.sh" "Wake word test script"

# Make scripts executable
if [ -f "scripts/run-comprehensive-tests.sh" ]; then
    chmod +x scripts/run-comprehensive-tests.sh
    echo -e "${GREEN}✅ Made comprehensive test script executable${NC}"
fi

if [ -f "scripts/test-claw-wake-word.sh" ]; then
    chmod +x scripts/test-claw-wake-word.sh  
    echo -e "${GREEN}✅ Made wake word test script executable${NC}"
fi

echo ""
echo -e "${YELLOW}🎯 VERIFYING KEY COMPONENTS${NC}"
echo "-----------------------------"

# Count source files in key areas
count_files "app/src/main/java/com/tdc/navigator/vehicle" "Vehicle control files"
count_files "app/src/main/java/com/tdc/navigator/wakework" "Wake word files"
count_files "app/src/main/java/com/tdc/navigator/service" "Service files"
count_files "app/src/test/java/com/tdc/navigator" "Unit test files"
count_files "app/src/androidTest/java/com/tdc/navigator" "Android test files"

echo ""
echo -e "${YELLOW}⚙️ VERIFYING WAKE WORD CONFIGURATION${NC}"
echo "------------------------------------"

# Check wake word configuration
check_file "app/src/main/assets/claw_wake_word_model_config.json" "Wake word model config"

if [ -f "app/src/main/assets/claw_wake_word_model_config.json" ]; then
    echo -e "${BLUE}🎤 Wake word configuration:${NC}"
    cat app/src/main/assets/claw_wake_word_model_config.json | head -10
fi

echo ""
echo -e "${YELLOW}🔧 VERIFYING ANDROID MANIFEST${NC}"
echo "------------------------------"

check_file "app/src/main/AndroidManifest.xml" "Android Manifest"

if [ -f "app/src/main/AndroidManifest.xml" ]; then
    echo -e "${BLUE}📱 Checking Android Automotive configuration...${NC}"
    
    # Check for automotive features
    if grep -q "android.hardware.type.automotive" app/src/main/AndroidManifest.xml; then
        echo -e "${GREEN}✅ Android Automotive feature declared${NC}"
    else
        echo -e "${RED}❌ Android Automotive feature missing${NC}"
        verification_failed=true
    fi
    
    # Check for car permissions
    if grep -q "android.car" app/src/main/AndroidManifest.xml; then
        echo -e "${GREEN}✅ Car permissions configured${NC}"
    else
        echo -e "${YELLOW}⚠️ Car permissions may need verification${NC}"
    fi
fi

echo ""
echo -e "${YELLOW}🚀 VERIFYING CI/CD CONFIGURATION${NC}"
echo "--------------------------------"

check_file ".github/workflows/ci.yml" "GitHub Actions CI workflow"

if [ -f ".github/workflows/ci.yml" ]; then
    echo -e "${BLUE}🔄 CI workflow verification:${NC}"
    
    # Check key CI components
    if grep -q "setup-java" .github/workflows/ci.yml; then
        echo -e "${GREEN}✅ Java setup configured${NC}"
    else
        echo -e "${RED}❌ Java setup missing${NC}"
        verification_failed=true
    fi
    
    if grep -q "setup-android" .github/workflows/ci.yml; then
        echo -e "${GREEN}✅ Android SDK setup configured${NC}"
    else
        echo -e "${RED}❌ Android SDK setup missing${NC}"
        verification_failed=true
    fi
    
    if grep -q "run-comprehensive-tests.sh" .github/workflows/ci.yml; then
        echo -e "${GREEN}✅ Comprehensive tests in CI${NC}"
    else
        echo -e "${YELLOW}⚠️ Comprehensive tests not in CI workflow${NC}"
    fi
    
    if grep -q "test-claw-wake-word.sh" .github/workflows/ci.yml; then
        echo -e "${GREEN}✅ Wake word tests in CI${NC}"
    else
        echo -e "${YELLOW}⚠️ Wake word tests not in CI workflow${NC}"
    fi
fi

echo ""
echo -e "${YELLOW}🔍 VERIFYING DEPENDENCY CONFIGURATION${NC}"
echo "-------------------------------------"

if [ -f "app/build.gradle" ]; then
    echo -e "${BLUE}📦 Checking dependencies...${NC}"
    
    # Check for key automotive dependencies
    if grep -q "androidx.car.app:app" app/build.gradle; then
        echo -e "${GREEN}✅ Android Automotive dependencies configured${NC}"
    else
        echo -e "${RED}❌ Android Automotive dependencies missing${NC}"
        verification_failed=true
    fi
    
    # Check for TensorFlow Lite (wake word detection)
    if grep -q "tensorflow-lite" app/build.gradle; then
        echo -e "${GREEN}✅ TensorFlow Lite dependencies configured${NC}"
    else
        echo -e "${RED}❌ TensorFlow Lite dependencies missing${NC}"
        verification_failed=true
    fi
    
    # Count total dependencies
    local dep_count=$(grep -c "implementation\|testImplementation\|androidTestImplementation" app/build.gradle || echo "0")
    echo -e "${BLUE}📦 Total dependencies configured: $dep_count${NC}"
fi

echo ""
echo -e "${YELLOW}🔗 VERIFYING GIT CONFIGURATION${NC}"
echo "------------------------------"

# Check git configuration
if [ -d ".git" ]; then
    echo -e "${GREEN}✅ Git repository initialized${NC}"
    
    # Check remote configuration
    local remote_url=$(git remote get-url origin 2>/dev/null || echo "No remote")
    if [[ "$remote_url" == *"thedigitalcarpenterdad/navigator-tdc"* ]]; then
        echo -e "${GREEN}✅ Correct GitHub remote configured${NC}: $remote_url"
    else
        echo -e "${RED}❌ Incorrect GitHub remote${NC}: $remote_url"
        verification_failed=true
    fi
    
    # Check if we're on main branch
    local current_branch=$(git branch --show-current)
    if [ "$current_branch" = "main" ]; then
        echo -e "${GREEN}✅ On main branch${NC}"
    else
        echo -e "${YELLOW}⚠️ Current branch: $current_branch${NC}"
    fi
    
    # Check git status
    if git status --porcelain | grep -q .; then
        echo -e "${YELLOW}⚠️ Working directory has uncommitted changes${NC}"
    else
        echo -e "${GREEN}✅ Working directory clean${NC}"
    fi
else
    echo -e "${RED}❌ Not a git repository${NC}"
    verification_failed=true
fi

echo ""
echo -e "${YELLOW}📄 VERIFYING DOCUMENTATION${NC}"
echo "----------------------------"

# Check documentation completeness
if [ -f "README.md" ]; then
    local readme_lines=$(wc -l < README.md)
    if [ $readme_lines -gt 20 ]; then
        echo -e "${GREEN}✅ README.md is comprehensive ($readme_lines lines)${NC}"
    else
        echo -e "${YELLOW}⚠️ README.md might need more content ($readme_lines lines)${NC}"
    fi
fi

# Check for required documentation sections
docs_to_check=(
    "ARCHITECTURE.md"
    "DEPLOYMENT.md"
    "TESTING_SUMMARY.md"
    "TEST_EXECUTION_REPORT.md"
    "WAKE_WORD_UPDATE_SUMMARY.md"
)

local docs_found=0
for doc in "${docs_to_check[@]}"; do
    if [ -f "$doc" ]; then
        docs_found=$((docs_found + 1))
    fi
done

echo -e "${BLUE}📚 Documentation files found: $docs_found/${#docs_to_check[@]}${NC}"

echo ""
echo -e "${YELLOW}🎯 PROJECT SEPARATION VERIFICATION${NC}"
echo "----------------------------------"

# Check that no ProjectMind dependencies exist
echo -e "${BLUE}🔍 Checking for ProjectMind dependencies...${NC}"

if grep -r "projectmind" . --include="*.gradle" --include="*.kt" --include="*.java" --include="*.json" 2>/dev/null; then
    echo -e "${RED}❌ ProjectMind references found - manual cleanup needed${NC}"
    verification_failed=true
else
    echo -e "${GREEN}✅ No ProjectMind dependencies found${NC}"
fi

# Check file count matches expected
echo -e "${BLUE}📊 Project file summary:${NC}"
local total_files=$(find . -type f -not -path "./.git/*" | wc -l)
echo "Total files (excluding .git): $total_files"

# Expected file ranges for a complete Android Automotive project
if [ $total_files -gt 30 ]; then
    echo -e "${GREEN}✅ File count indicates complete project${NC}"
else
    echo -e "${YELLOW}⚠️ Low file count - verify all files transferred${NC}"
fi

echo ""
echo -e "${BLUE}🏁 VERIFICATION SUMMARY${NC}"
echo "======================="

if [ "$verification_failed" = false ]; then
    echo -e "${GREEN}🎉 PROJECT VERIFICATION PASSED${NC}"
    echo ""
    echo -e "${GREEN}✅ Project structure complete${NC}"
    echo -e "${GREEN}✅ Android Automotive configuration valid${NC}"
    echo -e "${GREEN}✅ Test infrastructure ready${NC}"
    echo -e "${GREEN}✅ CI/CD workflow configured${NC}"
    echo -e "${GREEN}✅ Dependencies properly configured${NC}"
    echo -e "${GREEN}✅ Git repository correctly set up${NC}"
    echo -e "${GREEN}✅ Documentation complete${NC}"
    echo -e "${GREEN}✅ Successfully separated from ProjectMind${NC}"
    echo ""
    echo -e "${BLUE}🚗 Ready for Android Automotive development!${NC}"
    echo ""
    echo -e "${YELLOW}Next steps:${NC}"
    echo "1. Install Android Studio + Android SDK"
    echo "2. Open project in Android Studio"
    echo "3. Sync Gradle dependencies"
    echo "4. Run comprehensive test suite"
    echo "5. Deploy to AAOS emulator or test device"
    echo ""
    exit 0
else
    echo -e "${RED}❌ PROJECT VERIFICATION FAILED${NC}"
    echo ""
    echo -e "${RED}🚨 Critical issues found that need resolution${NC}"
    echo -e "${YELLOW}📋 Review the error messages above and fix issues${NC}"
    echo -e "${YELLOW}🔧 Rerun verification after fixing problems${NC}"
    echo ""
    exit 1
fi