param(
    [string]$AppVersion = "1.0.10",
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
    Add-Type -AssemblyName System.Drawing

    function Convert-ImageToIco {
        param(
            [string]$InputPath,
            [string]$OutPath
        )
        $sizes = @(16, 24, 32, 48, 64, 128, 256)
        $sourceImage = [System.Drawing.Image]::FromFile($InputPath)
        $frames = New-Object System.Collections.Generic.List[Object]
        try {
            foreach ($size in $sizes) {
                $bitmap = New-Object System.Drawing.Bitmap($size, $size)
                $graphics = [System.Drawing.Graphics]::FromImage($bitmap)
                try {
                    $graphics.Clear([System.Drawing.Color]::Transparent)
                    $graphics.SmoothingMode = [System.Drawing.Drawing2D.SmoothingMode]::HighQuality
                    $graphics.InterpolationMode = [System.Drawing.Drawing2D.InterpolationMode]::HighQualityBicubic
                    $graphics.PixelOffsetMode = [System.Drawing.Drawing2D.PixelOffsetMode]::HighQuality

                    # Enforce transparent rounded corners for Windows shortcuts.
                    $radius = [Math]::Max(2, [int]($size * 0.18))
                    $diameter = $radius * 2
                    $path = New-Object System.Drawing.Drawing2D.GraphicsPath
                    try {
                        $path.AddArc(0, 0, $diameter, $diameter, 180, 90)
                        $path.AddArc($size - $diameter, 0, $diameter, $diameter, 270, 90)
                        $path.AddArc($size - $diameter, $size - $diameter, $diameter, $diameter, 0, 90)
                        $path.AddArc(0, $size - $diameter, $diameter, $diameter, 90, 90)
                        $path.CloseFigure()
                        $graphics.SetClip($path)
                        $graphics.DrawImage($sourceImage, 0, 0, $size, $size)
                    } finally {
                        $path.Dispose()
                    }

                    $pngStream = New-Object System.IO.MemoryStream
                    try {
                        $bitmap.Save($pngStream, [System.Drawing.Imaging.ImageFormat]::Png)
                        $frames.Add([pscustomobject]@{
                            Size = $size
                            Data = $pngStream.ToArray()
                        })
                    } finally {
                        $pngStream.Dispose()
                    }
                } finally {
                    $graphics.Dispose()
                    $bitmap.Dispose()
                }
            }
        } finally {
            $sourceImage.Dispose()
        }

        $fileStream = [System.IO.File]::Open($OutPath, [System.IO.FileMode]::Create, [System.IO.FileAccess]::Write)
        $writer = New-Object System.IO.BinaryWriter($fileStream)
        try {
            $writer.Write([UInt16]0) # reserved
            $writer.Write([UInt16]1) # icon type
            $writer.Write([UInt16]$frames.Count)

            $offset = 6 + (16 * $frames.Count)
            foreach ($frame in $frames) {
                $entrySize = [int]$frame.Size
                $writer.Write([byte]($entrySize -eq 256 ? 0 : $entrySize))
                $writer.Write([byte]($entrySize -eq 256 ? 0 : $entrySize))
                $writer.Write([byte]0)
                $writer.Write([byte]0)
                $writer.Write([UInt16]1)
                $writer.Write([UInt16]32)
                $writer.Write([UInt32]$frame.Data.Length)
                $writer.Write([UInt32]$offset)
                $offset += $frame.Data.Length
            }

            foreach ($frame in $frames) {
                $writer.Write($frame.Data)
            }
        } finally {
            $writer.Dispose()
            $fileStream.Dispose()
        }
    }

    $iconSourcePath = $resolvedIconPath.Path
    $generatedIcoPath = Join-Path $distDir "reminot.generated.ico"
    Convert-ImageToIco -InputPath $iconSourcePath -OutPath $generatedIcoPath
    $iconArgs = @("--icon", $generatedIcoPath)
    Write-Host "Using icon source: $iconSourcePath"
    Write-Host "Generated Windows ICO: $generatedIcoPath"
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
