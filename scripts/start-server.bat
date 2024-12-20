@echo off
REM Path to the JAR file
set JAR_FILE=../target/_code-1.0-SNAPSHOT.jar

REM Check if the JAR file exists
if not exist "%JAR_FILE%" (
    echo Error: JAR file not found at %JAR_FILE%
    echo Please build the project using 'mvn clean package' first.
    exit /b 1
)

REM Start the server
java -jar "%JAR_FILE%" server