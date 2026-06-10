# Register all three remote plug-ins with vCenter in one pass.
#
# Prompts for shared credentials (vCenter, thumbprint, server URL) once,
# then registers each plugin with its own key and manifest URL.
#
# Plugins registered:
#   1. Secure Images          key: com.cmaclabs.remote.secureimages
#   2. Data Intelligence      key: com.cmaclabs.remote.dataintelligence
#   3. App Platform as a Svc  key: com.cmaclabs.remote.paasforvcf
#
# Non-interactive mode: set these env vars and REGISTER_NON_INTERACTIVE=1
#   VC_SDK_URL, VC_USER, VC_PASSWORD, PLUGIN_SERVER_TP,
#   PLUGIN_SERVER_HOST (e.g. your-plugin-server:8443), PLUGIN_VERSION, PLUGIN_COMPANY
#
# Update existing extensions: REGISTER_ACTION=updatePlugin

param()

Set-StrictMode -Version Latest
$ErrorActionPreference = "Stop"

function Get-RepoRoot {
    $scriptDir = $PSScriptRoot
    if (-not $scriptDir) { $scriptDir = Split-Path -Parent $MyInvocation.MyCommand.Path }
    return (Resolve-Path (Join-Path $scriptDir "..")).Path
}

function Test-IsInteractive {
    if ($env:REGISTER_NON_INTERACTIVE -eq "1") { return $false }
    try { return -not [Console]::IsInputRedirected } catch { return $true }
}

function Read-WithDefault {
    param([string]$Prompt, [string]$Default)
    $line = Read-Host "$Prompt [$Default]"
    if ([string]::IsNullOrWhiteSpace($line)) { return $Default }
    return $line
}

function Read-LabPassword {
    param([string]$Prompt)
    if ($PSVersionTable.PSVersion.Major -ge 7) {
        return Read-Host $Prompt -MaskInput
    }
    $sec = Read-Host $Prompt -AsSecureString
    $bstr = [Runtime.InteropServices.Marshal]::SecureStringToBSTR($sec)
    try { return [Runtime.InteropServices.Marshal]::PtrToStringBSTR($bstr) }
    finally { [Runtime.InteropServices.Marshal]::ZeroFreeBSTR($bstr) }
}

function Get-CertThumbprint {
    param([string]$HostName, [int]$Port)
    try {
        $conn = [System.Net.Sockets.TcpClient]::new($HostName, $Port)
        $ssl  = [System.Net.Security.SslStream]::new($conn.GetStream(), $false, {$true})
        $ssl.AuthenticateAsClient($HostName)
        $thumb = $ssl.RemoteCertificate.GetCertHashString("SHA1") -replace '(..(?!$))', '$1:'
        $ssl.Dispose(); $conn.Dispose()
        return $thumb
    } catch {
        return $null
    }
}

function Test-PluginKey {
    param([string]$k)
    if ([string]::IsNullOrWhiteSpace($k)) { Write-Error "PLUGIN_KEY is empty." -ErrorAction Stop }
    if ($k.Length -gt 127) { Write-Error "PLUGIN_KEY too long ($($k.Length) chars, max 127)." -ErrorAction Stop }
    if ($k -notmatch '^[a-z][a-z0-9]*(\.[a-z0-9]+)+$') {
        Write-Error "PLUGIN_KEY must be reverse-DNS: e.g. com.yourorg.remote.secureimages. Got: $k" -ErrorAction Stop
    }
}

$Root    = Get-RepoRoot
$ToolBat = Join-Path $Root "html-client-sdk\tools\vCenter plugin registration\prebuilt\extension-registration.bat"
if (-not (Test-Path -LiteralPath $ToolBat)) {
    Write-Error @"
Registration launcher not found:
  $ToolBat
Unpack Broadcom vSphere HTML Client SDK so html-client-sdk\ exists under the repo root.
"@ -ErrorAction Stop
}

# Ensure Java 17+ is available (the registration JAR requires class file version 61 / Java 17).
# We locate java.exe explicitly and set JAVACMD so the bat launcher uses it directly,
# bypassing PowerShell's command-resolution cache and any Java 8 on PATH.
function Find-Java17Exe {
    $candidates = @()

    # 1. JAVACMD env already set by caller
    if ($env:JAVACMD -and (Test-Path -LiteralPath $env:JAVACMD)) { $candidates += $env:JAVACMD }

    # 2. JAVA_HOME
    if ($env:JAVA_HOME) {
        $p = Join-Path $env:JAVA_HOME "bin\java.exe"
        if (Test-Path -LiteralPath $p) { $candidates += $p }
    }

    # 3. Well-known install directories
    foreach ($root in @("C:\Program Files\Eclipse Adoptium","C:\Program Files\Microsoft",
                        "C:\Program Files\Java","C:\Program Files\OpenJDK")) {
        if (-not (Test-Path -LiteralPath $root)) { continue }
        Get-ChildItem -LiteralPath $root -Directory -ErrorAction SilentlyContinue |
            Sort-Object Name -Descending |
            ForEach-Object {
                $p = Join-Path $_.FullName "bin\java.exe"
                if (Test-Path -LiteralPath $p) { $candidates += $p }
            }
    }

    # Return first candidate whose version is >= 17.
    # Parse the major version from the directory name (e.g. jdk-17.0.12+7-hotspot)
    # to avoid invoking java -version which writes to stderr and trips Stop error action.
    foreach ($exe in $candidates) {
        $dir   = Split-Path (Split-Path $exe)   # â€¦\jdk-17.0.17.10-hotspot
        $major = 0
        if ($dir -match '[/\\]jdk[-_]?(\d+)[.\-_]') { $major = [int]$Matches[1] }
        elseif ($dir -match '[/\\](\d+)\.\d+\.\d+') { $major = [int]$Matches[1] }
        if ($major -ge 17) { return $exe }
    }
    return $null
}

$java17Exe = Find-Java17Exe
if (-not $java17Exe) {
    Write-Error "Java 17+ not found. Install with: choco install Temurin17 -y" -ErrorAction Stop
}
# Set JAVACMD to the 8.3 short path so the .bat launcher can expand it unquoted
# (cmd.exe breaks on spaces in %JAVACMD% if the path isn't quoted in the bat).
$java17Short   = (cmd /c "for %I in (`"$java17Exe`") do @echo %~sI").Trim()
$env:JAVACMD   = $java17Short
$env:JAVA_HOME = Split-Path (Split-Path $java17Exe)
$env:Path      = "$(Split-Path $java17Exe);$env:Path"
Write-Host "Using Java 17: $java17Exe"
Write-Host "  (short path : $java17Short)"

# --- Collect shared parameters ---
$DEFAULT_VC_SDK_URL   = if ($env:VC_SDK_URL)          { $env:VC_SDK_URL }         else { "https://your-vcenter.example.com/sdk" }
$DEFAULT_VC_USER      = if ($env:VC_USER)             { $env:VC_USER }            else { "administrator@vsphere.local" }
$DEFAULT_SERVER_HOST  = if ($env:PLUGIN_SERVER_HOST)  { $env:PLUGIN_SERVER_HOST } else { "your-plugin-server:8443" }
$DEFAULT_VERSION      = if ($env:PLUGIN_VERSION)      { $env:PLUGIN_VERSION }     else { "1.0.0.0" }
$DEFAULT_COMPANY      = if ($env:PLUGIN_COMPANY)      { $env:PLUGIN_COMPANY }     else { "Lab" }
$REGISTER_ACTION      = if ($env:REGISTER_ACTION)     { $env:REGISTER_ACTION }    else { "registerPlugin" }

if ($REGISTER_ACTION -notin @("registerPlugin", "updatePlugin")) {
    Write-Error "REGISTER_ACTION must be registerPlugin or updatePlugin. Got: $REGISTER_ACTION" -ErrorAction Stop
}

$VC_SDK_URL     = $DEFAULT_VC_SDK_URL
$VC_USER        = $DEFAULT_VC_USER
$VC_PASSWORD    = $env:VC_PASSWORD
$PLUGIN_TP      = $env:PLUGIN_SERVER_TP
$SERVER_HOST    = $DEFAULT_SERVER_HOST
$PLUGIN_VERSION = $DEFAULT_VERSION
$PLUGIN_COMPANY = $DEFAULT_COMPANY

if (Test-IsInteractive) {
    Write-Host ""
    Write-Host "=== Register ALL three vCenter plug-ins ===" -ForegroundColor Cyan
    Write-Host "Press Enter at each prompt to accept the default in [brackets].`n"

    $VC_SDK_URL = Read-WithDefault "1) vCenter SDK URL" $DEFAULT_VC_SDK_URL
    if ($env:VCENTER_IP) {
        $VC_SDK_URL = "https://$($env:VCENTER_IP)/sdk"
        Write-Host "   (VCENTER_IP set -> using $VC_SDK_URL)"
    }

    $VC_USER = Read-WithDefault "2) vCenter SSO username" $DEFAULT_VC_USER

    if ([string]::IsNullOrWhiteSpace($VC_PASSWORD)) {
        $VC_PASSWORD = Read-LabPassword "3) vCenter SSO password"
        if ([string]::IsNullOrWhiteSpace($VC_PASSWORD)) { Write-Error "Password cannot be empty." -ErrorAction Stop }
    } else {
        Write-Host "3) vCenter SSO password: (using existing `$env:VC_PASSWORD)"
    }

    $SERVER_HOST = Read-WithDefault "4) Plug-in server host:port (no https://)" $DEFAULT_SERVER_HOST

    if ([string]::IsNullOrWhiteSpace($PLUGIN_TP)) {
        Write-Host ""
        Write-Host "5) Plug-in server HTTPS cert SHA-1 thumbprint (AA:BB:CC:...)"

        # Auto-fetch the thumbprint from the server using .NET (no OpenSSL needed on Windows)
        $tpHost = $SERVER_HOST -replace ':.*', ''
        $tpPort = if ($SERVER_HOST -match ':(\d+)$') { [int]$Matches[1] } else { 443 }
        Write-Host "   Fetching thumbprint from ${tpHost}:${tpPort} ..." -ForegroundColor DarkGray
        $autoThumb = Get-CertThumbprint -HostName $tpHost -Port $tpPort
        if ($autoThumb) {
            Write-Host "   Auto-detected: $autoThumb" -ForegroundColor Green
        } else {
            Write-Host "   Could not reach ${tpHost}:${tpPort} - enter thumbprint manually." -ForegroundColor Yellow
            Write-Host "   PowerShell tip (run separately):"
            Write-Host '     $h = "192.168.68.5"; $p = 8443' -ForegroundColor DarkCyan
            Write-Host '     $c = [System.Net.Sockets.TcpClient]::new($h,$p)' -ForegroundColor DarkCyan
            Write-Host '     $s = [System.Net.Security.SslStream]::new($c.GetStream(),$false,{$true})' -ForegroundColor DarkCyan
            Write-Host '     $s.AuthenticateAsClient($h)' -ForegroundColor DarkCyan
            Write-Host '     $s.RemoteCertificate.GetCertHashString("SHA1") -replace "(..(?!$))","$1:"' -ForegroundColor DarkCyan
            Write-Host '     $s.Dispose(); $c.Dispose()' -ForegroundColor DarkCyan
        }

        $PLUGIN_TP = Read-WithDefault "   Thumbprint" $(if ($autoThumb) { $autoThumb } else { "" })
        if ([string]::IsNullOrWhiteSpace($PLUGIN_TP)) { Write-Error "Thumbprint is required." -ErrorAction Stop }
    } else {
        Write-Host "5) Thumbprint: (using existing `$env:PLUGIN_SERVER_TP)"
    }

    $PLUGIN_VERSION = Read-WithDefault "6) Plug-in version (bump to force vCenter refresh)" $DEFAULT_VERSION
    $PLUGIN_COMPANY = Read-WithDefault "7) Company / publisher" $DEFAULT_COMPANY
    Write-Host ""
} else {
    $VC_USER = if ($env:VC_USER) { $env:VC_USER } else { "administrator@vsphere.local" }
    if ([string]::IsNullOrWhiteSpace($env:VC_PASSWORD)) {
        Write-Error "Set `$env:VC_PASSWORD before running non-interactively." -ErrorAction Stop
    }
    $VC_PASSWORD = $env:VC_PASSWORD
    if ([string]::IsNullOrWhiteSpace($PLUGIN_TP)) {
        Write-Error "Set `$env:PLUGIN_SERVER_TP before running non-interactively." -ErrorAction Stop
    }
}

$PLUGIN_TP      = $PLUGIN_TP      -replace '\s', ''
$PLUGIN_VERSION = $PLUGIN_VERSION -replace '\s', ''
$SERVER_HOST    = $SERVER_HOST    -replace '\s', ''

# DNS / reachability check
$VC_HOST = ([Uri]$VC_SDK_URL).Host
$isIPv4  = $VC_HOST -match '^\d{1,3}(\.\d{1,3}){3}$'
if (-not $isIPv4 -and $env:SKIP_DNS_CHECK -ne "1") {
    try { [void][System.Net.Dns]::GetHostEntry($VC_HOST) }
    catch { Write-Error "DNS did not resolve vCenter host '$VC_HOST'. Set SKIP_DNS_CHECK=1 to skip." -ErrorAction Stop }
}

# Clear proxy env vars for child JVM
@("http_proxy","https_proxy","HTTP_PROXY","HTTPS_PROXY","ALL_PROXY","all_proxy",
  "socks_proxy","SOCKS_PROXY","socksProxyHost","socksProxyPort","JAVA_TOOL_OPTIONS") |
    ForEach-Object { Remove-Item "env:$_" -ErrorAction SilentlyContinue }

$HTTP_NON_PROXY = if ($env:VC_NO_PROXY_HOSTS) { $env:VC_NO_PROXY_HOSTS } else { "${VC_HOST}|*.example.com|localhost|127.*|[::1]" }

$javaOptsExtra = @(
    "-Dextension.registration.insecure=true",
    "-Djava.net.preferIPv4Stack=true",
    "-Djava.net.useSystemProxies=false",
    "-Dhttp.nonProxyHosts=`"$HTTP_NON_PROXY`"",
    "-Dhttps.nonProxyHosts=`"$HTTP_NON_PROXY`"",
    "-Dhttp.proxyHost=", "-Dhttp.proxyPort=0",
    "-Dhttps.proxyHost=", "-Dhttps.proxyPort=0",
    "-DsocksProxyHost=", "-DsocksProxyPort=0"
)
$env:EXTENSION_REGISTRATION_INSECURE = "true"
$env:JAVA_OPTS = if ($env:JAVA_OPTS) { "$($env:JAVA_OPTS) $($javaOptsExtra -join ' ')" } else { $javaOptsExtra -join ' ' }

# --- Plugin definitions ---
$plugins = @(
    @{
        key     = "com.cmaclabs.remote.secureimages"
        name    = "Secure Images"
        summary = "150+ hardened Kubernetes utilities and applications deployable to VKS clusters."
        url     = "https://${SERVER_HOST}/secure-images-ui/plugin.json"
    },
    @{
        key     = "com.cmaclabs.remote.dataintelligence"
        name    = "Data Intelligence"
        summary = "Agent-ready data lakehouse leveraging vSAN for governed enterprise data access."
        url     = "https://${SERVER_HOST}/data-intel-ui/plugin.json"
    },
    @{
        key     = "com.cmaclabs.remote.paasforvcf"
        name    = "App Platform as a Service"
        summary = "Tanzu Hub install from vSphere Client."
        url     = "https://${SERVER_HOST}/tanzu-hub-poc-ui/plugin.json"
    }
)

# --- Register each plugin ---
$totalPlugins  = $plugins.Count
$successCount  = 0
$failedPlugins = @()

for ($i = 0; $i -lt $plugins.Count; $i++) {
    $p = $plugins[$i]
    Test-PluginKey $p.key

    Write-Host ""
    Write-Host "[$($i + 1)/$totalPlugins] $($p.name)" -ForegroundColor Cyan
    Write-Host "  key     : $($p.key)"
    Write-Host "  manifest: $($p.url)"
    Write-Host "  action  : $REGISTER_ACTION"
    Write-Host ""

    $javaArgs = @(
        "-action",           $REGISTER_ACTION,
        "-url",              $VC_SDK_URL,
        "-username",         $VC_USER,
        "-password",         $VC_PASSWORD,
        "-key",              $p.key,
        "-version",          $PLUGIN_VERSION,
        "-pluginUrl",        $p.url,
        "-serverThumbprint", $PLUGIN_TP,
        "-company",          $PLUGIN_COMPANY,
        "-name",             $p.name,
        "-summary",          $p.summary
    )

    & $ToolBat @javaArgs
    if ($LASTEXITCODE -eq 0) {
        Write-Host "  OK: $($p.name) registered." -ForegroundColor Green
        $successCount++
    } else {
        Write-Host "  FAILED: $($p.name) - exit code $LASTEXITCODE" -ForegroundColor Red
        $failedPlugins += $p.name
    }
}

# --- Summary ---
Write-Host ""
Write-Host "=== Registration complete: $successCount/$totalPlugins succeeded ===" -ForegroundColor Cyan

if ($failedPlugins.Count -gt 0) {
    Write-Host "Failed:" -ForegroundColor Red
    $failedPlugins | ForEach-Object { Write-Host "  - $_" -ForegroundColor Red }
    Write-Host ""
    Write-Host "Tip: if a plugin already exists, set `$env:REGISTER_ACTION = 'updatePlugin' and re-run."
    exit 1
}

Write-Host ""
Write-Host "All three plug-ins are registered. Log out and back in to vSphere Client to see them." -ForegroundColor Green
Write-Host "  Plugins menu will show:"
Write-Host "    - Secure Images          -> https://${SERVER_HOST}/secure-images-ui/"
Write-Host "    - Data Intelligence      -> https://${SERVER_HOST}/data-intel-ui/"
Write-Host "    - App Platform as a Svc  -> https://${SERVER_HOST}/tanzu-hub-poc-ui/"
exit 0

