#!/bin/bash

set -e  # Exit on any error

# Color function for green text
green() {
    echo -e "\033[32m$1\033[0m"
}

# Find project root
SCRIPT_DIR="$(cd "$(dirname "${BASH_SOURCE[0]}")" && pwd)"
PROJECT_ROOT="$(dirname "$SCRIPT_DIR")"
cd "$PROJECT_ROOT"

# Clean old dist
rm -rf dist/*

# Make dist dirs
mkdir -p dist/
mkdir -p dist/js/
mkdir -p dist/css/
mkdir -p dist/img/
mkdir -p dist/assets/

# Build everything including assembled JAR
green "Normal build"
mvn clean package -DskipTests -q

# Extract WebJars from the assembled JAR
green "Extract and move webjars"

jar xf ./target/mtmc.jar META-INF/resources/webjars/

# Move webjars to the root level (so /webjars/monaco-editor/... paths work)
mv META-INF/resources/webjars ./dist/
rm -rf META-INF

# Build TemplateRenderer JAR and run it (need to compile first since clean was run)
green "Build TemplateRenderer"
mvn compile assembly:single@template-renderer -q

green "Render the Template"
java -jar target/mtmc-template-renderer.jar dist

# Build TeaVM WebWorker
green "Build WebWorker"
mvn org.teavm:teavm-maven-plugin:compile@webworker -q

# Copy public assets
cp -r src/main/resources/public/* dist/
cp -r src/main/resources/disk.zip dist/

