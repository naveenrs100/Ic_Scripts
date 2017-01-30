@echo off
call %~dp0\cabeceraComun.bat

cd "%Workspace%"
if NOT exist "changed.txt" (
goto :checkin
)

SETLOCAL DisableDelayedExpansion
FOR /F "usebackq delims=" %%a in (`"findstr /n ^^ changed.txt"`) do (
	set "var=%%a"
	SETLOCAL EnableDelayedExpansion
	set "var=!var:*:=!"
	echo Entregando cambios: !var!
	%SCM_CMD% checkin "!var!" -u %userRTC% -P %pwdRTC%
)

goto :buscaChangeSet

:checkin
echo Ejecutando checkin...
set startCheckin=%time:,=.%
%SCM_CMD% checkin . -u %userRTC% -P %pwdRTC%
set endCheckin=%time:,=.%
for /f %%e in ('call %~dp0\functionsWindows.bat :getTime %startCheckin% %endCheckin%') do set _timeCheckin=%%e
echo %_timeCheckin%

:buscaChangeSet
set RUTA_TMP=%TEMP%\%RANDOM%.txt
echo Utilizando el temporal %RUTA_TMP% para parsear el status...
for /f "usebackq delims=" %%Z in (`%SCM_CMD% status -u %userRTC% -P %pwdRTC% -B -C `) do (
	set "var=%%Z"
	SETLOCAL EnableDelayedExpansion
	REM Ni comillas ni mayor que
	set "var=!var:>=!"
	set "var=!var:"=!"
	for /f "usebackq delims=" %%H in (`" echo "!var!" | grep @ "`) do (
		echo %%H | grep -Eoi "\([0-9]+\)" | grep -Eoi "[0-9]+" > %RUTA_TMP%
		for /f %%Y in ('call cat %RUTA_TMP%') do set changeSet=%%Y
	)
)
if exist %RUTA_TMP% (
del /S /Q %RUTA_TMP%
)


set startEditChangeSet=%time:,=.%
if NOT "%changeSet%"=="" (
	echo El changeset es: %changeSet%
	echo Asociando comentario al conjunto de cambios...
	%SCM_CMD% changeset comment %changeSet% "%description%" %LOGIN%
	echo Asociando workItem al conjunto de cambios...
	%SCM_CMD% changeset associate %LOGIN% %changeSet% %workItem%
	echo Cerrando el conjunto de cambios...
	%SCM_CMD% changeset close %changeSet% %LOGIN%
) else if NOT "%ignoreErrorsWithoutChanges%"=="" (
		echo NO SE HA MODIFICADO NADA, NO SE HA ENCONTRADO CHANGE SET. Aunque no haya cambios no se considera error.
) else (
	echo NO SE HA MODIFICADO NADA, NO SE HA ENCONTRADO CHANGE SET
	exit 1
)
set endEditChangeSet=%time:,=.%
for /f %%e in ('call %~dp0\functionsWindows.bat :getTime %startEditChangeSet% %endEditChangeSet%') do set _timeEditChangeset=%%e
if NOT "%changeSet%"=="" (
echo %_timeEditChangeset%
)


call %~dp0\pieComun.bat