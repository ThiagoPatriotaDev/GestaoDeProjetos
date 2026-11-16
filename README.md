# UniFocus | Gestão de Projetos

Aplicativo Android nativo (Kotlin + XML + Room) para acompanhar projetos acadêmicos.

## Redesign visual

A interface foi redesenhada com uma identidade violeta e azul-noite, visual leve, cartões arredondados e componentes adaptáveis a telas de celular.

- **Painel inicial** com total de projetos, projetos em andamento, concluídos, prazos que precisam de atenção e barra de progresso calculados a partir do banco Room.
- **Lista de projetos** com cartões e indicadores de prazo: em andamento, prazo próximo (até 3 dias), atrasado e concluído.
- **Filtros reais** para alternar entre projetos em andamento e concluídos, com possibilidade de reabrir um projeto.
- **Criação de projeto** organizada em três seções (essencial, planejamento e notas), sem mudar o modelo de dados.
- **Detalhes** com destaque para o título, cronograma e notas, preservando a integração com o calendário do Android.
- **Dica de foco** com identidade visual consistente. O recurso original de buscar uma dica via web continua disponível, com dicas locais de fallback.

## Como abrir

1. Extraia o ZIP e abra a pasta `GestaoDeProjetos` no **Android Studio**.
2. Aguarde a sincronização Gradle e instale os SDKs solicitados, se necessário.
3. Execute o módulo `app` em um emulador ou aparelho Android.

O projeto mantém `applicationId` e a estrutura Room da versão enviada. Não foram incluídos dados de demonstração nem alteradas as tabelas existentes.

## Verificação

Os arquivos XML foram analisados sintaticamente e os IDs de views/recursos utilizados pelos layouts e pelo código foram conferidos. **Não foi possível gerar o APK neste ambiente**, porque a distribuição Gradle 9.5 exigida pelo projeto depende de download, indisponível neste ambiente sem rede. A compilação final deve ser feita pelo Android Studio.
