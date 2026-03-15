@echo off
setlocal EnableExtensions EnableDelayedExpansion

set "SCRIPT_DIR=%~dp0"
set "SERVER_DIR=MC_Server"
set "JAR_NAME=paper-1.21.8-60.jar"
set "JAR_URL=https://fill-data.papermc.io/v1/objects/8de7c52c3b02403503d16fac58003f1efef7dd7a0256786843927fa92ee57f1e/paper-1.21.8-60.jar"
set "PLUGIN_API_URL=https://api.github.com/repos/Etmis/SolaceCore/releases/latest"
set "PLUGIN_FALLBACK_URL=https://github.com/Etmis/SolaceCore/releases/download/Alpha_v1.0.0/SolaceCore-Alpha-v1.0.0.jar"
set "PLUGIN_LATEST_BASE_URL=https://github.com/Etmis/SolaceCore/releases/latest/download"
set "PLUGIN_FALLBACK_NAME=SolaceCore-Alpha-v1.0.0.jar"
set "WEB_FALLBACK_URL=https://github.com/Etmis/SolaceCore/releases/download/Alpha_v1.0.0/SolaceCore-Alpha-v1.0.0-web.zip"
set "WEB_FALLBACK_NAME=SolaceCore-Alpha-v1.0.0-web.zip"
set "WEB_RELEASE_DIR=%SCRIPT_DIR%WEB_Release"
set "WEB_EXTRACT_DIR=%SCRIPT_DIR%WEB_SolaceCore"
set "JAVA_OPTS=-XX:+AlwaysPreTouch -XX:+DisableExplicitGC -XX:+ParallelRefProcEnabled -XX:+PerfDisableSharedMem -XX:+UnlockExperimentalVMOptions -XX:+UseG1GC -XX:G1HeapRegionSize=8M -XX:G1HeapWastePercent=5 -XX:G1MaxNewSizePercent=40 -XX:G1MixedGCCountTarget=4 -XX:G1MixedGCLiveThresholdPercent=90 -XX:G1NewSizePercent=30 -XX:G1RSetUpdatingPauseTimePercent=5 -XX:G1ReservePercent=20 -XX:InitiatingHeapOccupancyPercent=15 -XX:MaxGCPauseMillis=200 -XX:MaxTenuringThreshold=1 -XX:SurvivorRatio=32"

cls

echo [1/10] Pripravuji WEB slozky...
if not exist "%WEB_RELEASE_DIR%" mkdir "%WEB_RELEASE_DIR%"
if errorlevel 1 (
    echo Chyba: nepodarilo se vytvorit slozku pro WEB release.
    pause
    exit /b 1
)

set "WEB_ALREADY_PREPARED=0"
set "WEB_COMPOSE_FILE="
for /f "delims=" %%A in ('dir /s /b "%WEB_EXTRACT_DIR%\docker-compose.yml" 2^>nul') do (
    set "WEB_COMPOSE_FILE=%%A"
    set "WEB_ALREADY_PREPARED=1"
    goto WEB_PREPARED_FOUND
)

:WEB_PREPARED_FOUND
if "!WEB_ALREADY_PREPARED!"=="1" (
    echo WEB cast uz je pripravena. Preskakuji stazeni a rozbaleni.
    goto WEB_PREP_DONE
)

set "WEB_FILE_NAME=%WEB_FALLBACK_NAME%"
set "WEB_DOWNLOAD_URL=%WEB_FALLBACK_URL%"
for /f "usebackq tokens=1,2 delims=|" %%A in (`powershell -NoProfile -ExecutionPolicy Bypass -Command "[Net.ServicePointManager]::SecurityProtocol=[Net.SecurityProtocolType]::Tls12; try { $release=Invoke-RestMethod -Uri '%PLUGIN_API_URL%' -Headers @{ 'User-Agent'='setup_mc_server.bat' } -ErrorAction Stop; $asset=$release.assets ^| Where-Object { $_.name -match '^SolaceCore-.*-web\.zip$' } ^| Select-Object -First 1; if ($asset) { Write-Output ($asset.browser_download_url + '|' + $asset.name.Trim()) } } catch {}"`) do (
    if not "%%~A"=="" set "WEB_DOWNLOAD_URL=%%~A"
    if not "%%~B"=="" set "WEB_FILE_NAME=%%~B"
)

echo !WEB_FILE_NAME! | findstr /R /I "^SolaceCore-.*-web\.zip$" >nul
if errorlevel 1 (
    set "WEB_FILE_NAME=%WEB_FALLBACK_NAME%"
    set "WEB_DOWNLOAD_URL=%WEB_FALLBACK_URL%"
)

echo [2/10] Stahuji WEB release...
echo Stahuji WEB balicek "!WEB_FILE_NAME!"...
curl.exe -L --fail --retry 3 --retry-delay 2 --connect-timeout 20 -A "setup_mc_server.bat" -o "%WEB_RELEASE_DIR%\!WEB_FILE_NAME!" "!WEB_DOWNLOAD_URL!"
if errorlevel 1 (
    echo Upozorneni: stazeni latest WEB balicku selhalo, zkousim fallback...
    set "WEB_FILE_NAME=%WEB_FALLBACK_NAME%"
    set "WEB_DOWNLOAD_URL=%WEB_FALLBACK_URL%"
    curl.exe -L --fail --retry 3 --retry-delay 2 --connect-timeout 20 -A "setup_mc_server.bat" -o "%WEB_RELEASE_DIR%\!WEB_FILE_NAME!" "%WEB_FALLBACK_URL%"
    if errorlevel 1 (
        echo Chyba: nepodarilo se stahnout WEB balicek ani z fallback URL.
        pause
        exit /b 1
    )
)

if exist "%WEB_EXTRACT_DIR%" rmdir /s /q "%WEB_EXTRACT_DIR%"
mkdir "%WEB_EXTRACT_DIR%"
if errorlevel 1 (
    echo Chyba: nepodarilo se pripravit slozku pro rozbaleni WEB balicku.
    pause
    exit /b 1
)

echo [3/10] Rozbaluji WEB release...
set "WEB_EXT=!WEB_FILE_NAME:~-4!"
if /I not "!WEB_EXT!"==".zip" (
    echo Chyba: WEB balicek musi byt .zip, nalezeno "!WEB_EXT!".
    pause
    exit /b 1
)

powershell -NoProfile -ExecutionPolicy Bypass -Command "try { Expand-Archive -LiteralPath '%WEB_RELEASE_DIR%\!WEB_FILE_NAME!' -DestinationPath '%WEB_EXTRACT_DIR%' -Force -ErrorAction Stop } catch { exit 1 }"
if errorlevel 1 (
    echo Chyba: nepodarilo se rozbalit ZIP WEB balicek.
    pause
    exit /b 1
)

if exist "%WEB_RELEASE_DIR%" (
    rmdir /s /q "%WEB_RELEASE_DIR%"
    if errorlevel 1 (
        echo Upozorneni: nepodarilo se smazat docasnou slozku WEB_Release.
    )
)

set "WEB_COMPOSE_FILE="
for /f "delims=" %%A in ('dir /s /b "%WEB_EXTRACT_DIR%\docker-compose.yml" 2^>nul') do (
    set "WEB_COMPOSE_FILE=%%A"
    goto WEB_PREP_DONE
)

:WEB_PREP_DONE
if not defined WEB_COMPOSE_FILE (
    echo Chyba: po rozbaleni nebyl nalezen docker-compose.yml.
    pause
    exit /b 1
)

if not exist "!WEB_COMPOSE_FILE!" (
    echo Chyba: nalezeny soubor docker-compose.yml neexistuje: "!WEB_COMPOSE_FILE!"
    pause
    exit /b 1
)

echo [4/10] Spoustim WEB docker compose...
if "!WEB_ALREADY_PREPARED!"=="1" (
    echo WEB docker compose uz byl pripraven driv. Preskakuji spusteni.
) else (
    echo Spoustim docker compose z: "!WEB_COMPOSE_FILE!"
    docker compose -f "!WEB_COMPOSE_FILE!" up -d --build
    if errorlevel 1 (
        echo Chyba: docker compose se nepodarilo spustit.
        pause
        exit /b 1
    )
)

echo [5/10] Pripravuji slozku serveru...
if not exist "%SERVER_DIR%" (
    mkdir "%SERVER_DIR%"
    if errorlevel 1 (
        echo Chyba: nepodarilo se vytvorit slozku "%SERVER_DIR%".
        pause
        exit /b 1
    )
) else (
    echo Slozka "%SERVER_DIR%" uz existuje. Preskakuji vytvareni.
)

cd /d "%SERVER_DIR%"
if errorlevel 1 (
    echo Chyba: nepodarilo se prejit do slozky "%SERVER_DIR%".
    pause
    exit /b 1
)

echo [6/10] Kontroluji Paper server jar...
if exist "%JAR_NAME%" (
    echo Soubor "%JAR_NAME%" uz existuje. Preskakuji stazeni.
) else (
    echo Stahuji "%JAR_NAME%"...
    powershell -NoProfile -ExecutionPolicy Bypass -Command "try { Invoke-WebRequest -Uri '%JAR_URL%' -OutFile '%JAR_NAME%' -UseBasicParsing -ErrorAction Stop } catch { exit 1 }"
    if errorlevel 1 (
        echo Chyba: stazeni selhalo.
        pause
        exit /b 1
    )
)

echo [7/10] Pripravuji plugin SolaceCore...
if not exist plugins mkdir plugins
set "PLUGIN_DOWNLOAD_URL="
set "PLUGIN_FILE_NAME=%PLUGIN_FALLBACK_NAME%"
for /f "usebackq delims=" %%A in (`powershell -NoProfile -ExecutionPolicy Bypass -Command "[Net.ServicePointManager]::SecurityProtocol=[Net.SecurityProtocolType]::Tls12; try { $release=Invoke-RestMethod -Uri '%PLUGIN_API_URL%' -Headers @{ 'User-Agent'='setup_mc_server.bat' } -ErrorAction Stop; $asset=$release.assets ^| Where-Object { $_.name -match '^SolaceCore-.*\.jar$' } ^| Select-Object -First 1; if ($asset) { $asset.name.Trim() } } catch {}"`) do (
    if not "%%~A"=="" set "PLUGIN_FILE_NAME=%%~A"
)
echo !PLUGIN_FILE_NAME! | findstr /R /I "^SolaceCore-.*\.jar$" >nul
if errorlevel 1 set "PLUGIN_FILE_NAME=%PLUGIN_FALLBACK_NAME%"
set "PLUGIN_DOWNLOAD_URL=%PLUGIN_LATEST_BASE_URL%/!PLUGIN_FILE_NAME!"
set "DOWNLOAD_OK=0"

if exist "plugins\!PLUGIN_FILE_NAME!" (
    echo Plugin "!PLUGIN_FILE_NAME!" uz existuje. Preskakuji stazeni.
    set "DOWNLOAD_OK=1"
) else (
    echo Stahuji plugin "!PLUGIN_FILE_NAME!"...
    curl.exe -L --fail --retry 3 --retry-delay 2 --connect-timeout 20 -A "setup_mc_server.bat" -o "plugins\!PLUGIN_FILE_NAME!" "!PLUGIN_DOWNLOAD_URL!"
    if errorlevel 1 (
        echo Upozorneni: stazeni z "latest" selhalo, zkousim fallback verzi...
    ) else (
        set "DOWNLOAD_OK=1"
    )
)

if "!DOWNLOAD_OK!"=="0" (
    set "PLUGIN_DOWNLOAD_URL=%PLUGIN_FALLBACK_URL%"
    set "PLUGIN_FILE_NAME=%PLUGIN_FALLBACK_NAME%"
    if exist "plugins\!PLUGIN_FILE_NAME!" (
        echo Fallback plugin "!PLUGIN_FILE_NAME!" uz existuje. Preskakuji stazeni.
        set "DOWNLOAD_OK=1"
    ) else (
        echo Stahuji fallback plugin "!PLUGIN_FILE_NAME!"...
        curl.exe -L --fail --retry 3 --retry-delay 2 --connect-timeout 20 -A "setup_mc_server.bat" -o "plugins\!PLUGIN_FILE_NAME!" "!PLUGIN_DOWNLOAD_URL!"
        if errorlevel 1 (
            echo Chyba: stazeni pluginu selhalo i ve fallback rezimu.
            pause
            exit /b 1
        ) else (
            set "DOWNLOAD_OK=1"
        )
    )
)

echo [8/10] Pripravuji EULA soubor...
if not exist eula.txt (
    for /f "usebackq delims=" %%D in (`powershell -NoProfile -ExecutionPolicy Bypass -Command "[DateTime]::Now.ToString('ddd MMM dd HH:mm:ss zzz yyyy',[System.Globalization.CultureInfo]::InvariantCulture)"`) do set "EULA_DATE=%%D"
    > eula.txt echo #By changing the setting below to TRUE you are indicating your agreement to our EULA ^(https://aka.ms/MinecraftEULA^).
    >> eula.txt echo #!EULA_DATE!
    >> eula.txt echo eula=false
)

echo [9/10] Kontroluji EULA...
set "EULA_ALREADY_ACCEPTED=0"
if exist eula.txt (
    findstr /I /C:"eula=true" eula.txt >nul
    if not errorlevel 1 set "EULA_ALREADY_ACCEPTED=1"
)

if "!EULA_ALREADY_ACCEPTED!"=="1" goto START_SERVER

echo.
echo Souhlasite s Minecraft EULA?
echo EULA: https://aka.ms/MinecraftEULA
set /p EULA_ACCEPT=Napiste true pokud souhlasite: 

if /I "%EULA_ACCEPT%"=="true" (
    powershell -NoProfile -ExecutionPolicy Bypass -Command "$content = Get-Content -LiteralPath 'eula.txt'; $content = $content -replace '^eula=false$','eula=true'; Set-Content -LiteralPath 'eula.txt' -Value $content -Encoding ASCII"
    if errorlevel 1 (
        echo Chyba: nepodarilo se upravit eula.txt.
        pause
        exit /b 1
    )
    echo EULA nastavena na true.
) else (
    echo EULA nebyla potvrzena. Server nebude spusten.
    pause
    exit /b 0
)

:START_SERVER
echo [10/10] Spoustim Minecraft server...
java %JAVA_OPTS% -jar "%JAR_NAME%" nogui
set "SERVER_EXIT_CODE=%ERRORLEVEL%"

echo.
echo Pokud se server nenacetl a ukoncil se s chybou nejspis bude potreba upravit plugins\SolaceCore\config.yml.
echo Pro otevreni config.yml zmacknete Y behem nasledujicich 10 sekund...
choice /C YN /N /T 10 /D N >nul
if errorlevel 2 goto END_SCRIPT
if errorlevel 1 (
    if exist "plugins\SolaceCore\config.yml" (
        start "" notepad "plugins\SolaceCore\config.yml"
    ) else (
        echo config.yml nebyl nalezen, oteviram slozku pluginu...
        if not exist "plugins\SolaceCore" mkdir "plugins\SolaceCore"
        start "" explorer "%CD%\plugins\SolaceCore"
    )
)

:END_SCRIPT
pause
endlocal