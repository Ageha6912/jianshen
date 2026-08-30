@echo off
set LOG=E:\jianshen\_dataset\whpx.log
echo [1] enable VirtualMachinePlatform > "%LOG%"
dism /online /enable-feature /featurename:VirtualMachinePlatform /all /norestart >> "%LOG%" 2>&1
echo [2] enable HypervisorPlatform >> "%LOG%"
dism /online /enable-feature /featurename:HypervisorPlatform /all /norestart >> "%LOG%" 2>&1
echo [3] cleanup aehd leftovers >> "%LOG%"
del /f /q C:\Windows\System32\drivers\aehd.sys >> "%LOG%" 2>&1
del /f /q "E:\Android\Sdk\extras\google\Android_Emulator_Hypervisor_Driver\aehd_install_manual.bat" >> "%LOG%" 2>&1
del /f /q "E:\Android\Sdk\extras\google\Android_Emulator_Hypervisor_Driver\aehd_install_v3.bat" >> "%LOG%" 2>&1
del /f /q "E:\Android\Sdk\extras\google\Android_Emulator_Hypervisor_Driver\aehd_install_v4.bat" >> "%LOG%" 2>&1
echo [4] feature states >> "%LOG%"
dism /online /get-featureinfo /featurename:HypervisorPlatform | find "State" >> "%LOG%" 2>&1
dism /online /get-featureinfo /featurename:VirtualMachinePlatform | find "State" >> "%LOG%" 2>&1
exit /b 0
