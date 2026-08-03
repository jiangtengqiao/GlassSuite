@echo off
rem ============================================================
rem  GlassSuite Windows 端一键构建（双击即可运行）
rem  产物: windows\publish\GlassSuite-win-x64\GlassSuite.exe
rem        以及打包好的 GlassSuite-win-x64.zip
rem  如需自包含/单文件版本： 右键编辑本文件，把 build.ps1 后加
rem  -Standalone 或 -SingleFile 参数后保存再双击。
rem ============================================================
cd /d "%~dp0"
powershell -NoProfile -ExecutionPolicy Bypass -File "%~dp0build.ps1"
if errorlevel 1 (
    echo.
    echo 构建失败，请查看上方错误信息。
    pause
) else (
    echo.
    echo 构建成功！按任意键关闭本窗口。
    pause >nul
)
