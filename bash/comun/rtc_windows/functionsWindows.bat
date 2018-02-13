@echo OFF

call %*
goto :eof

REM Privada: calcula la longitud de una cadena
:strLen
setlocal enabledelayedexpansion
:strLen_Loop
  if not "!%1:~%len%!"=="" set /A len+=1 & goto :strLen_Loop
  echo %len%
endlocal
goto :eof


REM Crea un directorio temporal
:mktempWinDir
SETLOCAL 
	set TIEMPO=%TIME:~6,5%
	
	set _directorio=%TEMP%\%RANDOM%_%TIEMPO:,=.%
	if exist %_directorio% set directorio=%directorio%_1
	mkdir %_directorio%
	echo %_directorio%
ENDLOCAL
goto :eof

REM Devuelve un directorio de configuración para una corriente, creándolo si es necesario
REM Crea una réplica del sandbox dentro de DAEMONS_HOME y lo utiliza como directorio
REM de metadatos
:getWinRTCDaemonDir
SETLOCAL
	set directorioActual=%WORKSPACE%
	set directorioActual=%directorioActual: =_%
	set directorioActual=%directorioActual:/=\%
	REM ver si empieza por la barra
	for /f %%n in ('echo %directorioActual% ^| findstr /B /C:"\\"') do set inicio=%%n
	if "%inicio%"=="" goto :sin_barra
	REM empieza con la barra
	set directorioScripts=%~dp0
	for /f %%l in ('echo %directorioScripts% ^| cut -c 1-2') do set unidad=%%l
	set directorio=%unidad%%DAEMONS_HOME:/=\%\%directorioActual%
	goto :fin
:sin_barra
	REM no empieza con la barra
	for /f %%k in ('call %~dp0\functionsWindows.bat :strLen directorioActual') do set size=%%k
	for /f %%l in ('echo %directorioActual% ^| cut -c 1-2') do set unidad=%%l
	for /f %%m in ('echo %directorioActual% ^| cut -c 4-%size%') do set resto=%%m
	SET directorio=%unidad%%DAEMONS_HOME:/=\%\%resto%
:fin
	if not exist %directorio% (mkdir %directorio%)
	echo %directorio%
ENDLOCAL 
goto :eof

:getTime
SETLOCAL
set _start=%1
set _end=%2
set options="tokens=1-4 delims=:."
for /f %options% %%a in ("%_start%") do set start_h=%%a&set /a start_m=100%%b %% 100&set /a start_s=100%%c %% 100&set /a start_ms=100%%d %% 100
for /f %options% %%a in ("%_end%") do set end_h=%%a&set /a end_m=100%%b %% 100&set /a end_s=100%%c %% 100&set /a end_ms=100%%d %% 100

set /a hours=%end_h%-%start_h%
set /a mins=%end_m%-%start_m%
set /a secs=%end_s%-%start_s%
set /a ms=%end_ms%-%start_ms%
if %hours% lss 0 set /a hours = 24%hours%
if %mins% lss 0 set /a hours = %hours% - 1 & set /a mins = 60%mins%
if %secs% lss 0 set /a mins = %mins% - 1 & set /a secs = 60%secs%
if %ms% lss 0 set /a secs = %secs% - 1 & set /a ms = 100%ms%
if 1%ms% lss 100 set ms=0%ms%

REM mission accomplished
set /a totalsecs = %hours%*3600 + %mins%*60 + %secs% 
REM echo %hours%:%mins%:%secs%.%ms% (%totalsecs%.%ms%s total)
echo %totalsecs%.%ms%s
ENDLOCAL
goto :eof