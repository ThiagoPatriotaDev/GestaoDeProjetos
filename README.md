# UniFocus | Gestão de Projetos

Aplicativo Android nativo (Kotlin + XML + Room + Retrofit) para planejar e acompanhar projetos acadêmicos, com agendamento no calendário do dispositivo e backup na nuvem via API REST (Supabase).

## Funcionalidades

- **Painel inicial (`MainActivity`):** exibe total de projetos, projetos em andamento, concluídos, prazos próximos (até 3 dias) e barra de progresso reativa conectada ao banco local **Room**.
- **Lista e filtros:** alterna entre projetos **Em andamento** e **Concluídos**, com indicadores visuais de prazo (em andamento, prazo próximo, atrasado e finalizado) e alternância rápida entre concluir/reabrir.
- **Cadastro de projetos (`FormActivity`):** formulário em seções com máscaras de data/horário, escolha do dia de estudo (segunda a domingo), observações e opção para **agendar no calendário ao salvar**.
- **Agendamento no calendário via Intent nativa (`CalendarIntentHelper`):** abre o aplicativo de calendário do dispositivo (`CalendarContract.Events.CONTENT_URI`) já preenchido com título + disciplina, horários, **observações** e **recorrência semanal (`RRULE`)** até a data de entrega — disponível no cadastro e na tela de detalhes, sem necessidade de OAuth ou conta vinculada.
- **Backup na nuvem via Web Service REST (`Supabase` + Retrofit + OkHttp):**
  - Arquitetura *offline-first*: salva e atualiza primeiro no Room (`v3`) e sincroniza automaticamente com o endpoint REST `/rest/v1/projects` (`GET`, `POST`, `PATCH`, `DELETE`).
  - Isolamento por **código de sincronização** único gerado por aparelho (`SyncCodeManager`), enviado no cabeçalho `x-sync-code` e validado via **Row Level Security (RLS)** no PostgreSQL.
  - Botão **☁ Nuvem** no topo da tela inicial para consultar status, copiar o código, sincronizar pendências ou **restaurar projetos em outro aparelho**.
  - Status de backup visível na tela de detalhes (`✓ Sincronizado` / `Apenas local`, clicável para reenviar) e logs HTTP na tag `SupabaseApi`.
- **Dica de foco (`DicaMotivacionalFragment`):** consulta dicas motivacionais via Web Service REST com fallback local caso o aparelho esteja offline.

## Como abrir e executar

1. Clone o repositório e abra a pasta `GestaoDeProjetos` no **Android Studio**.
2. Aguarde o **Gradle Sync** baixar as dependências (Room, Retrofit, OkHttp, Gson, Material Components).
3. Confira as credenciais do Supabase em `app/src/main/java/gestaodeprojetos/alunos/cloud/SupabaseConfig.kt` (URL do projeto e chave `publishable`/`anon`).
4. Execute o módulo `app` em um emulador ou aparelho Android.

## Estrutura principal

- `gestaodeprojetos.alunos.calendar`: `CalendarIntentHelper` (mapeamento de datas, horários, observações e `RRULE` semanal para `Intent.ACTION_INSERT`).
- `gestaodeprojetos.alunos.cloud`: `SupabaseConfig`, `SupabaseApiService`, `SupabaseModels`, `SyncCodeManager` e `SupabaseBackupRepository`.
- `gestaodeprojetos.alunos.db` / `model`: entidade `Project`, `ProjectDao` e `AppDatabase` (Room v3).
- `gestaodeprojetos.alunos.ui`: `DicaMotivacionalFragment`.

## Testes e verificação

- Build verificado com `./gradlew app:assembleDebug`.
- Testes unitários verificados com `./gradlew app:testDebugUnitTest` (`CalendarIntentHelperTest` e `SupabaseMapperTest`).
