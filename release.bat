cmd /C build.bat

cd ..

cmd /C reobfuscate.bat

set PATH=c:\cygwin\bin;%PATH%

cd forge

sh package.sh 1.2.2

pause
