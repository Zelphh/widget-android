# Widget de Acompanhamento — Vale Alimentação (Caju)

Projeto pessoal, sem fins comerciais. Widget de tela inicial (Android/Samsung) mostrando o saldo diário do vale-alimentação em gráfico de barras, com entrada de gastos híbrida (automática via notificação + manual como fallback).

---

## 1. Objetivo

Acompanhar visualmente, dia a dia, quanto do vale-alimentação (R$30/dia útil, crédito do Caju) já foi consumido, direto na tela inicial, sem precisar abrir um app.

---

## 2. Regras de negócio (já validadas)

- Crédito de **R$30,00** em todo dia útil (segunda a sexta, exceto feriados).
- Saldo é acumulativo: começa no 1º dia útil do mês e carrega dia a dia.
- Cada gasto é subtraído do saldo **no dia em que ocorreu** (não zera nem reinicia).
- Saldo **pode ficar negativo** (ex.: gasto maior que o crédito acumulado).
- Fins de semana e feriados: sem crédito, mas o saldo do dia anterior permanece (não some).

```
saldo(dia) = saldo(dia - 1) + (30 se dia útil senão 0) - gastos(dia)
```

- Feriados: lista configurável, começa vazia (usuário pode preencher depois, ex. feriados nacionais + municipais de João Costa/PI, já que é essa a localização identificada).

## 3. Gráfico

- Eixo X: dias corridos, do dia 1º do mês até hoje.
- Eixo Y: linha do zero fixa; barras para cima (verde) quando saldo positivo, para baixo (vermelho) quando negativo.
- Renderizado em **Canvas → Bitmap**, exibido num `ImageView` dentro do `RemoteViews` do widget (RemoteViews não desenha gráfico customizado nativamente).

## 4. Entrada de dados — modelo híbrido (sem confirmação)

1. **Automática (principal):** `NotificationListenerService` escuta notificações do pacote do Caju, extrai valor da compra via regex, salva o gasto direto — sem diálogo de confirmação.
2. **Manual (fallback):** toque no widget abre uma tela simples (valor + salvar) para os casos em que o parser falhar ou a notificação não for capturada. O usuário percebe pela ausência do lançamento e completa manualmente.
3. Sem tela de confirmação intermediária — decisão explícita do usuário para minimizar fricção.

**Risco assumido:** se o Caju mudar o texto da notificação, o parser passa a falhar silenciosamente até ser notado e ajustado. Aceitável para uso pessoal; não há SLA a cumprir.

## 5. Arquitetura técnica

- **Plataforma:** Android nativo (Kotlin) — chosen over React Native pela necessidade de desenho customizado (Canvas) e integração mais direta com `NotificationListenerService`.
- **Componentes principais:**
  - `SaldoWidgetProvider : AppWidgetProvider` — monta o `RemoteViews`, dispara o desenho do gráfico.
  - `ChartRenderer` — lógica de Canvas → Bitmap (barras + linha zero), reutilizando a fórmula de saldo já validada.
  - `CajuNotificationListenerService : NotificationListenerService` — captura e faz parsing das notificações do Caju.
  - `LancamentoManualActivity` — tela leve (dialog-style) para input manual, aberta ao tocar no widget.
  - `GastoRepository` — camada de acesso a dados (Room), único ponto de escrita/leitura de gastos.
  - `WidgetUpdateScheduler` — dispara `updateAppWidget` sempre que um gasto é gravado (listener ou manual), sem depender do intervalo mínimo de 30 min do Android.
- **Persistência:** Room (SQLite) — tabela `gastos(dia, mes, ano, valor, origem)`, onde `origem` é `automatico` ou `manual` (útil para depois auditar taxa de acerto do parser).
- **Permissões necessárias:**
  - Acesso a notificações (`BIND_NOTIFICATION_LISTENER_SERVICE`) — concedida manualmente pelo usuário nas configurações.
  - Nenhuma permissão de rede necessária (tudo local).

## 6. Estrutura de pastas (proposta)

```
app/
  src/main/java/.../
    widget/
      SaldoWidgetProvider.kt
      ChartRenderer.kt
      WidgetUpdateScheduler.kt
    notification/
      CajuNotificationListenerService.kt
      NotificationParser.kt
    data/
      Gasto.kt
      GastoDao.kt
      AppDatabase.kt
      GastoRepository.kt
    ui/
      LancamentoManualActivity.kt
    domain/
      SaldoCalculator.kt        # lógica pura, testável isoladamente
      Feriados.kt
  src/main/res/
    layout/widget_saldo.xml
    xml/widget_info.xml
```

## 7. Fases de construção

1. **Núcleo de lógica** — `SaldoCalculator` + `Feriados` (lógica já prototipada e validada; portar 1:1 para Kotlin, com testes unitários cobrindo o exemplo original e casos negativos).
2. **Persistência** — Room com `Gasto` e `GastoRepository`.
3. **Widget estático** — `AppWidgetProvider` + `ChartRenderer` renderizando dados mockados, validar visual na tela real.
4. **Entrada manual** — `LancamentoManualActivity`, ligar toque no widget → abrir tela → salvar → atualizar widget.
5. **Entrada automática** — `NotificationListenerService` + `NotificationParser`, testar contra notificações reais do Caju (capturar 5–10 exemplos reais antes de escrever o regex).
6. **Polimento** — atualização em tempo real, tratamento de virada de mês/ano, revisão dos feriados.

## 8. Pontos abertos / decisões pendentes

- [ ] Confirmar texto exato das notificações do Caju (capturar prints/logs reais antes de codar o parser).
- [ ] Definir se feriados municipais entram na lista já na v1 ou ficam para depois.
- [ ] Decidir o que mostrar no fim de semana no eixo X: barra "flat" do saldo anterior, ou omitir esses dias do gráfico.
- [ ] Decidir retenção histórica: guardar meses passados no banco (para eventualmente ver histórico) ou limpar a cada mês.

## 9. Fora de escopo (por ora)

- Integração via Open Finance Brasil (já descartada — Caju não expõe dados de transação por esse canal para app pessoal).
- Tela de confirmação de gasto automático (decisão explícita: sem confirmação).
- Publicação na Play Store / distribuição — uso estritamente pessoal, instalação via sideload assinado.
