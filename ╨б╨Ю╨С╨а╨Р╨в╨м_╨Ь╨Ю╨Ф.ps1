$ErrorActionPreference = 'Stop'
$version = '26.3-66.1.0'
$url = "https://maven.minecraftforge.net/net/minecraftforge/forge/$version/forge-$version-mdk.zip"
$root = $PSScriptRoot
$temp = Join-Path $root '.mdk-build-temp'
$zip = Join-Path $temp 'forge-mdk.zip'
$project = Join-Path $temp 'mdk'

Write-Host 'InstantHomes: подготовка проекта Forge 26.3...' -ForegroundColor Cyan
try {
    $java = (& java -version 2>&1 | Out-String)
    if ($java -notmatch '25\.') {
        throw 'Нужна Java 25. Установи Java 25, затем снова запусти СОБРАТЬ_МОД.bat.'
    }
    New-Item -ItemType Directory -Force -Path $temp | Out-Null
    if (Test-Path $project) { Remove-Item $project -Recurse -Force }
    New-Item -ItemType Directory -Force -Path $project | Out-Null
    Write-Host 'Скачиваю официальный MDK Forge...' 
    Invoke-WebRequest -Uri $url -OutFile $zip
    Expand-Archive -Path $zip -DestinationPath $project -Force

    $props = Join-Path $project 'gradle.properties'
    if (Test-Path $props) {
        $text = Get-Content $props -Raw
        $replacements = @{
            'mod_id' = 'instanthomes'
            'mod_group_id' = 'ru.instant.homes'
            'mod_version' = '1.0.0'
            'mod_name' = 'Instant Homes'
            'minecraft_version' = '26.3'
            'forge_version' = '66.1.0'
        }
        foreach ($key in $replacements.Keys) {
            if ($text -match "(?m)^\s*$([regex]::Escape($key))\s*=") {
                $text = [regex]::Replace($text, "(?m)^\s*$([regex]::Escape($key))\s*=.*$", "$key=$($replacements[$key])")
            }
        }
        Set-Content -Path $props -Value $text -Encoding UTF8
    }

    $srcDestination = Join-Path $project 'src'
    if (Test-Path $srcDestination) { Remove-Item $srcDestination -Recurse -Force }
    Copy-Item (Join-Path $root 'src') $srcDestination -Recurse -Force

    $toml = Join-Path $project 'src/main/resources/META-INF/mods.toml'
    if (-not (Test-Path (Split-Path $toml))) { New-Item -ItemType Directory -Force -Path (Split-Path $toml) | Out-Null }
    Copy-Item (Join-Path $root 'src/main/resources/META-INF/mods.toml') $toml -Force

    Write-Host 'Запускаю сборку Gradle. Первый запуск может занять несколько минут...' -ForegroundColor Cyan
    Push-Location $project
    & .\gradlew.bat build
    $exit = $LASTEXITCODE
    Pop-Location
    if ($exit -ne 0) { throw "Gradle завершился с кодом $exit. См. сообщения выше." }

    $libs = Join-Path $project 'build/libs'
    $jar = Get-ChildItem $libs -Filter '*.jar' | Where-Object { $_.Name -notmatch 'sources|javadoc|slim|extra' } | Select-Object -First 1
    if (-not $jar) { throw 'Сборка завершилась, но JAR не найден в build/libs.' }
    $out = Join-Path $root $jar.Name
    Copy-Item $jar.FullName $out -Force
    Write-Host "ГОТОВО! JAR создан: $out" -ForegroundColor Green
    Write-Host 'Скопируй этот JAR в папку mods своего профиля Forge 26.3.'
} catch {
    Write-Host "ОШИБКА: $($_.Exception.Message)" -ForegroundColor Red
    Write-Host 'Если ошибка связана с сетью, скачай официальный MDK вручную: https://files.minecraftforge.net/net/minecraftforge/forge/'
    exit 1
}
