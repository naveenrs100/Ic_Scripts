@echo off
call %~dp0\cabeceraComun.bat

REM Si viene informado el parámetro de instantánea y además se está haciendo la
REM instantánea y NO una línea base, se usa este parámetro
if "%instantanea%"=="" goto :version
if NOT "%tagType%"=="snapshot" goto :version
set VERSION_LOCAL=%instantanea%
goto :continuar
:version
if NOT exist version.txt goto :fin
set temporal=%TEMP%\%RANDOM%.txt
grep version= version.txt|awk -F= "{print $(NF)}" > %temporal%
for /f %%Y in ('call cat %temporal%') do set VERSION_LOCAL=%%Y
set VERSION_LOCAL=%VERSION_LOCAL:"=%
goto :continuar

:fin
SET TMS_FECHA=%DATE%
SET TMS_HORA=%TIME:~0,8%
REM ltrim de los caracteres
for /f "tokens=* delims= " %%a in ("%TMS_FECHA%") do set TMS_FECHA=%%a

for /f "tokens=* delims= " %%a in ("%TMS_HORA%") do set TMS_HORA=%%a

SET TIMESTAMP=%TMS_FECHA:/=%.%TMS_HORA::=%
set version=%stream%-%TIMESTAMP%

:continuar

echo Utilizando versión local %VERSION_LOCAL%
if "%description%"=="" (
	set description=JENKINS BASELINE
)
if "%version%"=="" (
	set version=%VERSION_LOCAL%-build:%compJobNumber%
) else if "%version%"=="local" (
	set version=%VERSION_LOCAL%
)

set versionTxt=%version%
if "%streamInVersion%"=="true" (
	set versionTxt=%stream% - %version%
)

echo versionTxt: %versionTxt%

if NOT "%tagType%"=="baseline" goto :es_snapshot
if "%makeSnapshot%"=="false" goto :cierra_baseline
echo Creando la línea base %versionTxt%...
set startBaseline=%time:,=.%
%SCM_CMD% create baseline %LOGIN% "%workspaceRTC%" "%versionTxt%" "%component%" --overwrite-uncommitted
set endBaseline=%time:,=.%
for /f %%a in ('call %~dp0\functionsWindows.bat :getTime %startBaseline% %endBaseline%') do set _timeBaseline=%%a
echo %_timeBaseline%

goto :comprobar_fichero

:cierra_baseline
echo NO CIERRA baseline

goto :comprobar_fichero
:es_snapshot
echo Creando la instantánea %versionTxt%...
set startSnapshot=%time:,=.%
%SCM_CMD% create snapshot %LOGIN% "%stream%" -n "%versionTxt%" -d "%description%"
set endSnapshot=%time:,=.%
for /f %%a in ('call %~dp0\functionsWindows.bat :getTime %startSnapshot% %endSnapshot%') do set _timeSnapshot=%%a
echo %_timeSnapshot%

:comprobar_fichero
if "%RTCVersionFile%"=="" goto :fin_fichero
if not exist %RTCVersionFile% goto :fin_fichero
for /f "usebackq delims=" %%Y in (`"call cat %RTCVersionFile% | grep RTCBaseline"`) do set OLDVersion=%%Y
echo La versión antigua es %OLDVersion% , la nueva es %version%
if NOT "%OLDVersion%"==""(
	sed "s/%OLDVersion%/RTCBaseline=%version%/g" %RTCVersionFile% > tmpFile.txt
	mv tmpFile.txt %RTCVersionFile%
) else (
	echo RTCBaseline=%version% >> %RTCVersionFile%
)

:fin_fichero
call %~dp0\pieComun.bat
