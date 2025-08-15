@if "%DEBUG%" == "" @echo off
@rem ##########################################################################
@rem
@rem  Gradle startup script for Windows
@rem
@rem ##########################################################################

set APP_HOME=%~dp0

set CLASSPATH=%APP_HOME%\gradle\wrapper\gradle-wrapper.jar

if not defined JAVA_HOME (
	set JAVA_EXE=java
) else (
	set JAVA_EXE=%JAVA_HOME%\bin\java.exe
)

"%JAVA_EXE%" -Xmx64m -Xms64m -classpath "%CLASSPATH%" org.gradle.wrapper.GradleWrapperMain %*