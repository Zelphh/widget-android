# Comandos úteis — Widget Vale Caju

Referência rápida de comandos para build, instalação e diagnóstico no dia a dia.
Comandos dados em duas versões: **Git Bash** e **PowerShell** (o projeto só tem
`gradlew` unix, sem `gradlew.bat`, então o build sempre roda em Git Bash; o
`adb` funciona nos dois).

`applicationId`: `com.tartari.cajuwidget`

## Variáveis fixas nesta máquina

Git Bash:
```bash
export JAVA_HOME="C:/Program Files/Eclipse Adoptium/jdk-21.0.11.10-hotspot"
ADB="/c/Users/Zelphh/AppData/Local/Android/Sdk/platform-tools/adb.exe"
```

PowerShell:
```powershell
$env:JAVA_HOME = "C:\Program Files\Eclipse Adoptium\jdk-21.0.11.10-hotspot"
$ADB = "C:\Users\Zelphh\AppData\Local\Android\Sdk\platform-tools\adb.exe"
```
No PowerShell, `$ADB` guarda só o caminho — pra rodar o programa é preciso o
operador de chamada `&` antes: `& $ADB devices`, não `$ADB devices` sozinho
(isso dá o erro "Token 'install' inesperado").

## Build (sempre via Git Bash)

```bash
# Build debug APK
./gradlew :app:assembleDebug

# Build informando saldo inicial do cartão e/ou valor diário do vale (centavos)
./gradlew :app:assembleDebug -PsaldoInicialCentavos=50000 -PvalorDiarioCentavos=3500

# Mesma coisa via variável de ambiente em vez de -P
SALDO_INICIAL_CENTAVOS=50000 VALOR_DIARIO_CENTAVOS=3000 ./gradlew :app:assembleDebug

# Rodar todos os testes unitários
./gradlew :app:testDebugUnitTest

# Rodar uma classe de teste específica
./gradlew :app:testDebugUnitTest --tests "com.tartari.cajuwidget.domain.SaldoCalculatorTest"

# Limpar build (usar se algo ficar estranho após mudanças estruturais)
./gradlew clean
```

`saldoInicialCentavos`/`SALDO_INICIAL_CENTAVOS` só importa na primeira execução
do app (ou após desinstalar por completo) — o saldo real fica salvo internamente
depois disso e reinstalar com `-r` (abaixo) não o reseta. `valorDiarioCentavos`/
`VALOR_DIARIO_CENTAVOS` tem default `3000` (R$30,00) se omitido.

## Instalação

Git Bash:
```bash
# Primeira instalação
$ADB install app/build/outputs/apk/debug/app-debug.apk

# Reinstalar (substitui o APK, preserva banco de dados e permissões já concedidas)
$ADB install -r app/build/outputs/apk/debug/app-debug.apk

# Desinstalar por completo (apaga histórico de gastos e o saldo real rastreado)
$ADB uninstall com.tartari.cajuwidget
```

Depois de desinstalar por completo, a próxima instalação re-semeia o saldo real
a partir do `-PsaldoInicialCentavos`/`SALDO_INICIAL_CENTAVOS` passado no build —
é a forma de "resetar" o saldo rastreado para um novo valor.

PowerShell:
```powershell
# Primeira instalação
& $ADB install app/build/outputs/apk/debug/app-debug.apk

# Reinstalar (substitui o APK, preserva banco de dados e permissões já concedidas)
& $ADB install -r app/build/outputs/apk/debug/app-debug.apk

# Desinstalar por completo (apaga histórico de gastos também)
& $ADB uninstall com.tartari.cajuwidget
```

## Dispositivos conectados

Git Bash:
```bash
# Listar dispositivos e status (device / unauthorized / offline)
$ADB devices -l

# Reiniciar o servidor adb (se o celular não aparecer ou ficar "offline")
$ADB kill-server && $ADB start-server
```

PowerShell:
```powershell
# Listar dispositivos e status (device / unauthorized / offline)
& $ADB devices -l

# Reiniciar o servidor adb (se o celular não aparecer ou ficar "offline")
& $ADB kill-server; & $ADB start-server
```

## Permissões e configurações no celular

Git Bash:
```bash
# Abrir direto a tela "Acesso a notificações" (evita navegar pelo menu do fabricante)
$ADB shell am start -a android.settings.ACTION_NOTIFICATION_LISTENER_SETTINGS

# Checar se o Vale Caju está autorizado a ler notificações
$ADB shell settings get secure enabled_notification_listeners

# Descobrir o pacote real do app do Caju no aparelho (usar se PACOTES_CAJU estiver desatualizado)
$ADB shell pm list packages | grep -i caju
```

PowerShell:
```powershell
# Abrir direto a tela "Acesso a notificações" (evita navegar pelo menu do fabricante)
& $ADB shell am start -a android.settings.ACTION_NOTIFICATION_LISTENER_SETTINGS

# Checar se o Vale Caju está autorizado a ler notificações
& $ADB shell settings get secure enabled_notification_listeners

# Descobrir o pacote real do app do Caju no aparelho (usar se PACOTES_CAJU estiver desatualizado)
& $ADB shell pm list packages | Select-String -Pattern "caju"
```

## Logs / diagnóstico

Git Bash:
```bash
# Ver logs do app em tempo real (Ctrl+C para parar)
$ADB logcat | grep -i cajuwidget

# Ver só os logs do listener de notificação (Log.d usado nos parses silenciosos)
$ADB logcat | grep -i NotificationParser

# Forçar o widget a atualizar sem esperar o ciclo de 30 min do Android
$ADB shell am broadcast -a android.appwidget.action.APPWIDGET_UPDATE
```

PowerShell:
```powershell
# Ver logs do app em tempo real (Ctrl+C para parar)
& $ADB logcat | Select-String -Pattern "cajuwidget"

# Ver só os logs do listener de notificação (Log.d usado nos parses silenciosos)
& $ADB logcat | Select-String -Pattern "NotificationParser"

# Forçar o widget a atualizar sem esperar o ciclo de 30 min do Android
& $ADB shell am broadcast -a android.appwidget.action.APPWIDGET_UPDATE
```

## Fluxo típico após uma alteração no código

Build e teste sempre em Git Bash; instalação pode ser em qualquer um dos dois.

Git Bash (tudo em um terminal só):
```bash
./gradlew :app:testDebugUnitTest && \
./gradlew :app:assembleDebug && \
$ADB install -r app/build/outputs/apk/debug/app-debug.apk
```
