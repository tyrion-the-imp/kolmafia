cd C:\github\kolmafia\dist
del *.*/Q
::delete all previous jars
cd "C:\Users\dkolg\Google Drive\kolmafia"
dir *.jar
move /y *.jar "C:\Users\dkolg\Google Drive\kolmafia\jarchive"
echo.
cd C:\github\kolmafia
call gradlew.bat shadowJar
echo.
::make copy of the new build
::changing the name during copy seems to corrupt the jar so rename separately after copying
cd C:\github\kolmafia\dist
copy *.jar C:\Users\dkolg\GOOGLE~1\kolmafia\*.jar
::rename
echo.
cd "C:\Users\dkolg\Google Drive\kolmafia"
ren *.jar KoLmafia-latest.jar
echo.
::second copy to show revision number
cd C:\github\kolmafia\dist
copy *.jar C:\Users\dkolg\GOOGLE~1\kolmafia\*.jar
cd "C:\Users\dkolg\Google Drive\kolmafia"
dir *.jar
pause
start javaw.exe -jar KoLmafia-latest.jar