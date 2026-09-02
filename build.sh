#!/bin/bash
# RescueGrid - build and run script
SCRIPT_DIR="$(cd "$(dirname "${BASH_SOURCE[0]}")" && pwd)"
cd "$SCRIPT_DIR"

echo "Building RescueGrid..."
rm -rf out
mkdir -p out
find src/main/java -name "*.java" | xargs javac -d out -sourcepath src/main/java

if [ $? -ne 0 ]; then
    echo "Build failed!"
    exit 1
fi

echo "Build successful! Running RescueGrid..."
java -cp out com.rescuegrid.ui.DashboardFrame
