@echo off
call %~dp0\cabeceraComun.bat

set NEW=""

echo Buscando el componente %component%...
set startList=%time:,=.%
for /f %%g in ('call "%SCMTOOLS_HOME%\scm" --config "%_tempDir%" list components %LOGIN% "%streamTarget%" ^|grep -iw "%component%" ^|wc -l') do set existComp=%%g
set endList=%time:,=.%
for /f %%c in ('call %~dp0\functionsWindows.bat :getTime %startList% %endList%') do set _timeList=%%c
echo %_timeList%

if %existComp%==0 (
	echo Adding component: "%component%"
	%SCM_CMD% workspace add-components %LOGIN% "%streamTarget%" "%component%" -s "%workspaceRTC%"
	set NEW="true"
)

if %NEW%=="true" goto :fin

if "%force%"=="true" (
	%SCM_CMD% workspace replace-components -o %LOGIN% "%streamTarget%" workspace "%workspaceRTC%" "%component%"
) else (
	%SCM_CMD% deliver %LOGIN% -t "%streamTarget%" --overwrite-uncommitted
)

:fin
call %~dp0\pieComun.bat

REM Si deliver no tiene nada que entregar, devuelve 53.  De esta forma evitamos el error
if ERRORLEVEL 53 exit 0