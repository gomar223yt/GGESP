@echo off
cd /d %~dp0

echo === ADD ===
git add .

echo === COMMIT ===
set /p msg=Enter commit message:
git commit -m "%msg%"

echo === PUSH ===
git push

echo === DONE ===
pause
