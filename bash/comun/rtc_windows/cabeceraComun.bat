@echo off
echo Light: %light%
chcp 1252

set WORKSPACE=%WORKSPACE:/=\%
for /f %%x in ('call %~dp0\functionsWindows.bat :getWinRTCDaemonDir') do set _daemonDir=%%x
for /f %%i in ('call %~dp0\functionsWindows.bat :mktempWinDir') do set _tempDir=%%i

set _existeDaemon=0
if NOT "%light%"=="true" goto :lanzar_demonio
echo Comprobando si existe el demonio sobre %_daemonDir%...
set startListDaemons=%time:,=.%
for /f %%x in ('call %SCMTOOLS_HOME%\scm --config "%_daemonDir%" list daemons ^| grep Jazz ^| wc -l') do set _existeDaemon=%%x
set endListDaemons=%time:,=.%
for /f %%a in ('call %~dp0\functionsWindows.bat :getTime %startListDaemons% %endListDaemons%') do set _timeListDaemons=%%a
echo %_timeListDaemons%

echo Existe daemon previamente: %_existeDaemon%

:lanzar_demonio
REM Lanzar el demonio
if "%light%"=="true" (
	if %_existeDaemon%==0 (
	echo Lanzando daemon sobre %_daemonDir%...
	start "" "%SCMTOOLS_HOME%\scm" --config "%_daemonDir%" daemon start --connection-timeout %TIMEOUT_RTC_MILISEGUNDOS% --inactive-timeout %TIMEOUT_RTC_MILISEGUNDOS%  > NUL 2> NUL
	set _lanzadoDaemon=0
	REM Verificar que se ha lanzado el demonio
	:loop
	call sleep 2
	for /f %%x in ('call "%SCMTOOLS_HOME%\scm" --config "%_daemonDir%" list daemons ^| grep Jazz ^| wc -l') do set _lanzadoDaemon=%%x
	if "%_lanzadoDaemon%"=="0" goto :loop 		
	)
)

REM Variables del script
if "%light%"=="true" (
set SCM_DAEMON_CONNECTION_TIME_OUT=%TIMEOUT_RTC_MILISEGUNDOS%
set SCM_DAEMON_INACTIVE_TIME_OUT=%TIMEOUT_RTC_MILISEGUNDOS%
echo Directorio de configuración: %_daemonDir%
set SCM_CMD=call "%SCMTOOLS_HOME%\lscm" --config "%_daemonDir%"
) else (
REM Directorio temporal de metadatos de RTC
echo Directorio de configuración: %_tempDir%
set SCM_CMD=call "%SCMTOOLS_HOME%\scm" --config "%_tempDir%"
)
set LOGIN=-u %userRTC% -P %pwdRTC% -r %urlRTC%