@echo off
call %~dp0\cabeceraComun.bat

echo sincroniza (%1)..................

echo Buscando el workspace %workspaceRTC%...
set startList=%time:,=.%
for /f %%i in ('call "%SCMTOOLS_HOME%\scm" --config "%_tempDir%" list workspaces %LOGIN% -n "%workspaceRTC%" ^|grep -iw "%workspaceRTC%" ^|wc -l') do set _exist=%%i
set endList=%time:,=.%
for /f %%a in ('call %~dp0\functionsWindows.bat :getTime %startList% %endList%') do set _timeList=%%a
echo %_timeList%
set existComp=1

set startDelete=%time:,=.%
if "%recreateWS%" == "true" (
	echo Borrando el workspace %workspaceRTC%...
	%SCM_CMD% workspace delete %LOGIN% "%workspaceRTC%"
	echo workspace "%workspaceRTC%" deleted!
	set _exist=0
)

set endDelete=%time:,=.%
for /f %%b in ('call %~dp0\functionsWindows.bat :getTime %startDelete% %endDelete%') do set _timeDelete=%%b
if "%recreateWS%" == "true" (
	echo %_timeDelete%
)

echo exists workspace %workspaceRTC%?: %_exist%
set startCreate=%time:,=.%
if %_exist% == 0 (
	echo Creando el workspace %workspaceRTC%...
	%SCM_CMD% create workspace -e %LOGIN% "%workspaceRTC%"
	set existComp=0
) else (
	echo Buscando el componente %component%...
	for /f %%g in ('call "%SCMTOOLS_HOME%\scm" --config "%_tempDir%" list components %LOGIN% "%workspaceRTC%" ^|grep -iw "%component%" ^|wc -l') do set existComp=%%g
)

set endCreate=%time:,=.%
for /f %%c in ('call %~dp0\functionsWindows.bat :getTime %startCreate% %endCreate%') do set _timeCreate=%%c
echo %_timeCreate%

set startAdd=%time:,=.%
if %existComp% == 0 (
	echo Adding component: "%component%"
	%SCM_CMD% workspace add-components %LOGIN% "%workspaceRTC%" "%component%" -s "%stream%"
)

set endAdd=%time:,=.%
for /f %%e in ('call %~dp0\functionsWindows.bat :getTime %startAdd% %endAdd%') do set _timeAdd=%%e
if %existComp% == 0 (
	echo %_timeAdd%
)

if not "%snapshot%" == "" (
 	echo Cargando el componente %component% de la instantanea %snapshot%...
 	%SCM_CMD% workspace replace-components -o %LOGIN% "%workspaceRTC%" snapshot "%snapshot%" "%component%"
)

echo Cargando el componente %component% del workspace %workspaceRTC%...
set startLoad=%time:,=.%
%SCM_CMD% load "%workspaceRTC%" %LOGIN% -f "%component%"
set endLoad=%time:,=.%
for /f %%x in ('call %~dp0\functionsWindows.bat :getTime %startLoad% %endLoad%') do set _timeLoad=%%x
echo %_timeLoad%

if NOT "%snapshot%" == "" goto :fin

echo Comparando cambios sobre el componente %component%...
set startCompare=%time:,=.%
%SCM_CMD% compare workspace "%workspaceRTC%" stream "%stream%" %LOGIN% -I sw -C "|{name}|{email}|" -D "|yyyy-MM-dd-HH:mm:ss|" -f i | tee changesetCompare.txt
set endCompare=%time:,=.%
for /f %%x in ('call %~dp0\functionsWindows.bat :getTime %startCompare% %endCompare%') do set _timeCompare=%%x
echo %_timeCompare%
echo Aceptando cambios sobre el componente %component%...
set startAccept=%time:,=.%
%SCM_CMD% accept %LOGIN% -C "%component%" --flow-components -o -v --target "%workspaceRTC%" -s "%stream%"| tee changesetAccept.txt
set endAccept=%time:,=.%
for /f %%x in ('call %~dp0\functionsWindows.bat :getTime %startAccept% %endAccept%') do set _timeAccept=%%x
echo %_timeAccept%

:fin

call %~dp0\pieComun.bat