echo off

echo MinecraftForge Windows Setup Program
echo:

pushd .. >nul

xcopy /Y /E /I forge\conf\* conf

if exist runtime\bin\fernflower.jar move runtime\bin\fernflower.jar runtime\bin\fernflower.jar-backup

cmd /C cleanup.bat
cmd /C decompile.bat

if exist runtime\bin\fernflower.jar-backup move runtime\bin\fernflower.jar-backup runtime\bin\fernflower.jar

pushd src >nul

    if exist ..\jars\bin\minecraft.jar (
        
        for /f "delims=" %%a in ('dir /a -d /b /S ..\forge\patches\minecraft') do (
            pushd "%%a" 2>nul
            if errorlevel 1 (
                ..\runtime\bin\python\python_mcp ..\forge\lfcr.py "%%a" "%%a"
                ..\runtime\bin\applydiff.exe -uf -p2 -i "%%a"
            ) else popd
        )
    )
    
    if exist ..\jars\minecraft_server.jar (
        for /f "delims=" %%a in ('dir /a -d /b /S ..\forge\patches\minecraft_server') do (
            pushd "%%a" 2>nul
            if errorlevel 1 (
                ..\runtime\bin\python\python_mcp ..\forge\lfcr.py "%%a" "%%a"
                ..\runtime\bin\applydiff.exe -uf -p2 -i "%%a"
            ) else popd
        )
    
    )
popd >nul

xcopy /Y /E forge\src\* src

cmd /C updatemcp.bat
cmd /C updatenames.bat
cmd /C updatemd5.bat