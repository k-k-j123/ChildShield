#!/usr/bin/env bash
set -e

echo "Building project..."
./mvnw clean compile -DskipTests

echo "Starting Spring Boot application in background..."
./mvnw spring-boot:run &
APP_PID=$!

echo "Waiting for application to start up (15 seconds)..."
sleep 15

echo "Running Python API integration test suite..."
python3 -I test_apis.py || TEST_EXIT=$?

echo "Stopping Spring Boot application (PID: $APP_PID)..."
kill $APP_PID || true
wait $APP_PID 2>/dev/null || true

exit ${TEST_EXIT:-0}
