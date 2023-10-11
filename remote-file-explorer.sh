#!/bin/bash

# Find a java installation
if [ -z "${JAVA_HOME}" ]; then
    echo "Warning: $JAVA_HOME environment variable not set! Consider setting it."
    echo "Attempting to locate java..."
    j=`which java 2>/dev/null`
    if [ -z "$j" ]; then
        echo "Failed to locate a java virtual machine!"
        exit 1
    else
        echo "Found a virtual machine at: $j..."
        JAVA="$j"
    fi
else
    JAVA="${JAVA_HOME}/bin/java"
fi

# Launch application
BASEDIR=$(dirname "$0")
cd "$BASEDIR"
exec "${JAVA}" -cp "lib/*" --module-path lib --add-modules ALL-MODULE-PATH -jar remote-file-explorer-0.0.2-SNAPSHOT.jar