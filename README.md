# Widget Vale Caju

Widget de tela inicial (Android) que mostra, em gráfico de barras, o saldo diário
acumulado do vale-alimentação (crédito do Caju), além de um rótulo com o saldo
real do cartão. Projeto pessoal, sem fins comerciais. Plano completo em
`planejamento-widget-vale-caju.md`.

## Como funciona

- **Série de controle (barras do gráfico):**
  `saldo(dia) = saldo(dia-1) + (valor diário se dia útil senão 0) - gastos(dia)`.
  Acumula desde o 1º dia do mês, pode ficar negativo; fim de semana/feriado não
  credita, mas o saldo permanece. É recalculada do zero todo mês — serve só de
  ferramenta de visualização/controle.
- **Saldo real do cartão (rótulo no canto do gráfico):** número separado que
  evolui com o tempo — começa num valor informado pelo usuário na instalação,
  desconta cada gasto registrado e recebe `valor diário × dias úteis do mês`
  na virada do mês (é assim que o Caju deposita de fato: tudo de uma vez no
  dia 1º). Ver "Configuração" abaixo.
- **Gráfico:** barras verdes (positivo) para cima e vermelhas (negativo) para
  baixo, linha do zero fixa. Desenhado via Canvas → Bitmap num `ImageView` do
  `RemoteViews`.
- **Entrada automática:** `CajuNotificationListenerService` escuta notificações
  do app do Caju e registra o gasto direto, sem confirmação.
- **Entrada manual (fallback):** tocar no widget abre um diálogo de lançamento.
  Aceita valores negativos de propósito — é a forma de corrigir o saldo real do
  cartão se ele desalinhar do valor de verdade (notificação perdida, estorno).

## Configuração

Sem tela de configuração no app (sideloaded); dois valores são passados na hora
do build, em **centavos**, via property do Gradle ou variável de ambiente:

| Valor | Property Gradle | Env var | Default |
| --- | --- | --- | --- |
| Saldo inicial do cartão | `saldoInicialCentavos` | `SALDO_INICIAL_CENTAVOS` | `0` |
| Valor recebido por dia útil | `valorDiarioCentavos` | `VALOR_DIARIO_CENTAVOS` | `3000` (R$30,00) |

```bash
./gradlew :app:assembleDebug -PsaldoInicialCentavos=50000 -PvalorDiarioCentavos=3500
```

O saldo inicial só é usado na primeira execução (ou após desinstalar o app por
completo) — depois disso o saldo real fica salvo internamente e evolui sozinho;
rebuilds/reinstalações com `adb install -r` (que preservam os dados) não o
resetam.

## Build e instalação

Requer JDK 17+ (o repositório foi buildado com Temurin 21 em `~/.jdks`) e o
Android SDK apontado em `local.properties`.

```bash
JAVA_HOME=~/.jdks/jdk-21.0.11+10 ./gradlew :app:assembleDebug -PsaldoInicialCentavos=50000
adb install app/build/outputs/apk/debug/app-debug.apk
```

Depois de instalar:

1. Adicione o widget "Vale Caju" à tela inicial.
2. Conceda acesso a notificações: **Configurações → Notificações → Acesso avançado
   → Acesso a notificações → Vale Caju** (caminho varia por fabricante).

## Decisões tomadas nos pontos abertos do plano (§8)

| Ponto aberto | Decisão nesta versão |
|---|---|
| Texto exato das notificações do Caju | Parser escrito com palavras-chave prováveis (`compra`, `pagamento`, `você usou`…) e regex de valor `R$ N,NN`. **Ajustar com 5–10 amostras reais** — ver `NotificationParser.kt`. Confirmar também o pacote do app em `CajuNotificationListenerService.PACOTES_CAJU` (`adb shell pm list packages | grep -i caju`). |
| Feriados municipais na v1 | Fora da v1: lista começa vazia (`domain/Feriados.kt`), preencher quando quiser. |
| Fim de semana no eixo X | Barra "flat" repetindo o saldo anterior (dias corridos, sem buracos no gráfico). |
| Retenção histórica | Meses passados ficam no banco (nada é apagado na virada de mês). |
| Saldo real vs. série de controle | São dois números independentes de propósito: a série de controle é recalculada do zero todo mês (ferramenta de visualização); o saldo real é persistido e evolui com gastos + crédito mensal, refletindo como o Caju deposita de verdade. |
| Correção do saldo real | Sem tela dedicada — reaproveita o lançamento manual permitindo valores negativos, que somam ao saldo real (efeito colateral aceito: também ajusta a barra do dia na série de controle). |

## Estrutura

```
app/src/main/java/com/tartari/cajuwidget/
  domain/    SaldoCalculator, CreditoMensal, Feriados    (lógica pura, testada)
  data/      Gasto, GastoDao, AppDatabase, GastoRepository (Room),
             SaldoTotalRepository (SharedPreferences)
  widget/    SaldoWidgetProvider, ChartRenderer, WidgetUpdateScheduler
  notification/ CajuNotificationListenerService, NotificationParser
  ui/        LancamentoManualActivity
```

## Testes

```bash
JAVA_HOME=~/.jdks/jdk-21.0.11+10 ./gradlew :app:testDebugUnitTest
```

Cobrem a fórmula do saldo (dias úteis, fim de semana flat, feriado, saldo
negativo), o crédito mensal do saldo real (`CreditoMensal`, com catch-up de
meses pulados), o parser de notificação e o parse do valor manual (incluindo
valores negativos).

## Atualização do widget

- Imediata ao gravar gasto (automático ou manual), via `WidgetUpdateScheduler`.
- Na virada do dia (alarme inexato à meia-noite) para o crédito diário aparecer.
- A cada 30 min pelo ciclo padrão do Android (`updatePeriodMillis`), como rede
  de segurança.
