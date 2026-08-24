#!/bin/bash

# Build script for UnstableCharacters plugin
# Requires Java 21+ and Maven

echo "=========================================="
echo "  Building UnstableCharacters Plugin"
echo "     For Purpur 1.21.1 (Paper API)"
echo "=========================================="

# Check Java version
JAVA_VERSION=$(java -version 2>&1 | head -1 | cut -d'"' -f2 | cut -d'.' -f1)
JAVA_VERSION=${JAVA_VERSION#1.} # Handle "1.21" style versions
echo "Java version: $JAVA_VERSION"

if [ "$JAVA_VERSION" -lt 21 ] 2>/dev/null; then
    echo "⚠ Error: Purpur 1.21.1 requires Java 21+. Current: Java $JAVA_VERSION"
    echo "  Please install Java 21 or higher"
    exit 1
fi

# Check for Maven
if ! command -v mvn &> /dev/null; then
    echo "⚠ Error: Maven not found!"
    echo "  Please install Maven: https://maven.apache.org/install.html"
    exit 1
fi

echo ""
echo "Building..."
echo ""

mvn clean package -DskipTests

if [ $? -eq 0 ]; then
    echo ""
    echo "✅ Build successful!"
    echo ""
    JAR_FILE=$(ls target/*.jar 2>/dev/null | grep -v "original" | head -1)
    if [ -n "$JAR_FILE" ]; then
        echo "Output: $JAR_FILE"
        echo ""
        echo "To install on your Purpur 1.21.1 server:"
        echo "  1. Copy '$JAR_FILE' to your server's plugins folder"
        echo "  2. Restart the server"
        echo "  3. Edit config files in plugins/UnstableCharacters/"
    fi
else
    echo ""
    echo "❌ Build failed!"
    exit 1
fi
