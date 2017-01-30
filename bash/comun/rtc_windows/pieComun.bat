if NOT "%_tempDir%"=="" (
	if exist %_tempDir% (
		rmdir %_tempDir% /s /q
	)
)