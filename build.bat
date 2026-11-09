@echo off
rem Builds the mod jar into build\libs using the portable JDK 17.
set JAVA_HOME=%~dp0..\.tools\jdk17\jdk-17.0.21+9
call "%~dp0gradlew.bat" %*
