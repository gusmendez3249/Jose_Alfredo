param(
    [string]$PhoneIp = "",
    [int]$Port = 1935
)

Write-Host "====== FestivalTrack Live Stream Setup ======" -ForegroundColor Cyan

if ([string]::IsNullOrEmpty($PhoneIp)) {
    $adbPath = "$env:LOCALAPPDATA\Android\Sdk\platform-tools\adb.exe"
    if (Test-Path $adbPath) {
        $adbOutput = & $adbPath shell ip route 2>$null
        $adbText = ($adbOutput -join " ")
        $match = [regex]::Match($adbText, 'src\s+(\d+\.\d+\.\d+\.\d+)')
        if ($match.Success) {
            $PhoneIp = $match.Groups[1].Value
            Write-Host ("IP detectada: " + $PhoneIp) -ForegroundColor Green
        }
    }
    if ([string]::IsNullOrEmpty($PhoneIp)) {
        $PhoneIp = Read-Host "Ingresa la IP WiFi del celular (ej: 192.168.1.5)"
    }
}

netsh interface portproxy delete v4tov4 listenport=$Port listenaddress=0.0.0.0 2>$null | Out-Null
netsh interface portproxy add v4tov4 listenport=$Port listenaddress=0.0.0.0 connectport=$Port connectaddress=$PhoneIp
Write-Host ("[OK] Port forwarding PC:" + $Port + " al celular " + $PhoneIp + ":" + $Port) -ForegroundColor Green

$adbPath = "$env:LOCALAPPDATA\Android\Sdk\platform-tools\adb.exe"
if (Test-Path $adbPath) {
    & $adbPath -s emulator-5554 forward tcp:$Port tcp:$Port 2>$null
    Write-Host ("[OK] ADB tunnel emulador:" + $Port + " al host:" + $Port) -ForegroundColor Green
} else {
    Write-Host "[AVISO] ADB no encontrado." -ForegroundColor Yellow
}

netsh advfirewall firewall delete rule name="FestivalTrack RTSP" 2>$null | Out-Null
netsh advfirewall firewall add rule name="FestivalTrack RTSP" dir=in action=allow protocol=TCP localport=$Port | Out-Null
Write-Host ("[OK] Puerto " + $Port + " abierto en firewall") -ForegroundColor Green

Write-Host ""
Write-Host "LISTO! Inicia el live desde la app movil admin." -ForegroundColor Green
Write-Host ("TV:     rtsp://10.0.2.2:" + $Port) -ForegroundColor Yellow
Write-Host ("Celular: rtsp://" + $PhoneIp + ":" + $Port) -ForegroundColor Yellow
Write-Host "Presiona Ctrl+C para detener..." -ForegroundColor Gray

try {
    while ($true) { Start-Sleep -Seconds 30 }
} finally {
    netsh interface portproxy delete v4tov4 listenport=$Port listenaddress=0.0.0.0 2>$null | Out-Null
    Write-Host "Configuracion limpiada." -ForegroundColor Yellow
}