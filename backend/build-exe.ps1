param(
    [string]$AppVersion = "1.0.6",
    [string]$IconPath = "..\docs\media\label.ico"
)

$ErrorActionPreference = "Stop"

$projectRoot = Split-Path -Parent $MyInvocation.MyCommand.Path
Set-Location $projectRoot

Write-Host "Building jar with Maven..."
mvn -q -DskipTests clean package

$jar = Get-ChildItem "$projectRoot\target\*.jar" | Where-Object { $_.Name -notlike "*sources*" } | Select-Object -First 1
if (-not $jar) {
    throw "Jar file not found in target."
}

$distDir = Join-Path $projectRoot "dist"
New-Item -ItemType Directory -Force -Path $distDir | Out-Null

$resolvedIconPath = $null
if (-not [string]::IsNullOrWhiteSpace($IconPath)) {
    $resolvedIconPath = Resolve-Path -Path (Join-Path $projectRoot $IconPath) -ErrorAction SilentlyContinue
}
$iconArgs = @()
if ($resolvedIconPath) {
    function Test-IsPng {
        param([byte[]]$Bytes)
        if ($null -eq $Bytes -or $Bytes.Length -lt 8) {
            return $false
        }
        $pngSig = [byte[]](0x89, 0x50, 0x4E, 0x47, 0x0D, 0x0A, 0x1A, 0x0A)
        for ($i = 0; $i -lt 8; $i++) {
            if ($Bytes[$i] -ne $pngSig[$i]) {
                return $false
            }
        }
        return $true
    }

    function Convert-PngBytesToIco {
        param(
            [byte[]]$PngBytes,
            [string]$OutPath
        )
        $stream = New-Object System.IO.MemoryStream
        $writer = New-Object System.IO.BinaryWriter($stream)
        $writer.Write([UInt16]0)      # reserved
        $writer.Write([UInt16]1)      # type: icon
        $writer.Write([UInt16]1)      # count
        $writer.Write([byte]0)        # width (0 => 256)
        $writer.Write([byte]0)        # height (0 => 256)
        $writer.Write([byte]0)        # palette
        $writer.Write([byte]0)        # reserved
        $writer.Write([UInt16]1)      # planes
        $writer.Write([UInt16]32)     # bit depth
        $writer.Write([UInt32]$PngBytes.Length)
        $writer.Write([UInt32]22)     # image offset (6 + 16)
        $writer.Write($PngBytes)
        $writer.Flush()
        [System.IO.File]::WriteAllBytes($OutPath, $stream.ToArray())
        $writer.Dispose()
        $stream.Dispose()
    }

    $iconSourcePath = $resolvedIconPath.Path
    $iconBytes = [System.IO.File]::ReadAllBytes($iconSourcePath)
    $iconExtension = [System.IO.Path]::GetExtension($iconSourcePath).ToLowerInvariant()
    $generatedIcoPath = Join-Path $distDir "reminot.generated.ico"
    if ($iconExtension -eq ".png" -or (Test-IsPng -Bytes $iconBytes)) {
        Convert-PngBytesToIco -PngBytes $iconBytes -OutPath $generatedIcoPath
        $iconSourcePath = $generatedIcoPath
        Write-Host "Converted PNG icon to ICO: $iconSourcePath"
    }

    $iconArgs = @("--icon", $iconSourcePath)
    Write-Host "Using icon: $iconSourcePath"
} else {
    Write-Host "Building without custom icon."
}

$commonArgs = @(
    "--name", "Reminot",
    "--dest", $distDir,
    "--input", "$projectRoot\target",
    "--main-jar", $jar.Name,
    "--main-class", "ru.demo.ReminotApp",
    "--app-version", $AppVersion,
    "--vendor", "Reminot",
    "--java-options", "-Dfile.encoding=UTF-8"
)

function Invoke-JPackageWithIconFallback {
    param(
        [Parameter(Mandatory = $true)]
        [string[]]$ArgsWithType,
        [Parameter(Mandatory = $true)]
        [string]$StepName,
        [string[]]$ExtraArgs = @(),
        [bool]$FailOnError = $true,
        [bool]$AllowIconFallback = $true
    )

    if ($iconArgs.Count -gt 0) {
        try {
            & jpackage @ArgsWithType @commonArgs @iconArgs @ExtraArgs
            if ($LASTEXITCODE -ne 0) {
                throw "jpackage exited with code $LASTEXITCODE"
            }
            return
        } catch {
            if (-not $AllowIconFallback) {
                throw
            }
            Write-Warning "$StepName with custom icon failed: $($_.Exception.Message)"
            Write-Warning "Retrying $StepName without custom icon."
        }
    }
    & jpackage @ArgsWithType @commonArgs @ExtraArgs
    if ($LASTEXITCODE -ne 0) {
        if ($FailOnError) {
            throw "$StepName failed with exit code $LASTEXITCODE"
        }
        Write-Warning "$StepName failed with exit code $LASTEXITCODE. Continuing."
    }
}

Write-Host "Building portable app image..."
Invoke-JPackageWithIconFallback -ArgsWithType @("--type", "app-image") -StepName "app-image build" -FailOnError $false -AllowIconFallback $true

Write-Host "Building Windows EXE installer..."
Invoke-JPackageWithIconFallback -ArgsWithType @("--type", "exe") -StepName "exe build" -ExtraArgs @(
    "--win-shortcut",
    "--win-shortcut-prompt",
    "--win-menu",
    "--win-menu-group", "Reminot",
    "--win-per-user-install",
    "--win-dir-chooser"
) -AllowIconFallback $false

$appImageDir = Join-Path $distDir "Reminot"
if (Test-Path $appImageDir) {
    $portableRoot = Join-Path $distDir ("Reminot-" + $AppVersion + "-portable")
    if (Test-Path $portableRoot) {
        Remove-Item $portableRoot -Recurse -Force
    }
    New-Item -ItemType Directory -Path $portableRoot | Out-Null
    Copy-Item -Path (Join-Path $appImageDir "*") -Destination $portableRoot -Recurse

    $portableReadme = @"
How to run portable Reminot:
1) Extract this whole folder (do not run directly from WinRAR/7-Zip preview).
2) Open the extracted folder.
3) Run start-reminot.bat or Reminot.exe.
"@
    Set-Content -Path (Join-Path $portableRoot "README-PORTABLE.txt") -Value $portableReadme
    Set-Content -Path (Join-Path $portableRoot "start-reminot.bat") -Value "@echo off`r`ncd /d `"%~dp0`"`r`nstart `"`" `"%~dp0Reminot.exe`"`r`n"

    $zipPath = Join-Path $distDir ("Reminot-" + $AppVersion + "-portable.zip")
    if (Test-Path $zipPath) {
        Remove-Item $zipPath -Force
    }
    Compress-Archive -Path $portableRoot -DestinationPath $zipPath
    Write-Host "Portable package created: $zipPath"
}

Write-Host "Done. Files are in $distDir"
