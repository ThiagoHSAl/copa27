# Copa27

Sistema de acompanhamento da Copa do Mundo Feminina da FIFA Brasil 2027.
Trabalho prático da disciplina DCC603 Engenharia de Software I (Departamento de Ciência da Computação, UFMG, 2026/II), Grupo 4, Tema 5: Sistema para Acompanhamento de Evento Esportivo.

Este repositório contém a **Sprint 3**: implementação parcial em Java dos requisitos *Must have* e os testes automatizados em JUnit 5.
A documentação das Sprints 1 e 2 é entregue em PDF: modelo de processo, requisitos, casos de uso, projeto arquitetural e projeto detalhado.

| Integrante | Matrícula |
|---|---|
| Guilherme Augusto Pires Lopes | 2024013877 |
| João Henrique Alves Martins | 2024013885 |
| João Victor Otoni Gonçalves | 2024013940 |
| Thales Faria Almeida | 2024013974 |
| Thiago Henrique Silva de Almeida | 2024014180 |

> Projeto acadêmico, sem vínculo com a FIFA. As sedes, os estádios e os aeroportos são os reais, com dados aproximados. Como o sorteio dos grupos e a tabela oficial ainda não foram divulgados (risco R1 do plano de trabalho), **as seleções, os grupos e a tabela de jogos são fictícios**, porém seguem o formato oficial: 32 seleções, 8 grupos de 4 e 64 partidas entre 24/06 e 25/07/2027.

---

## 1. Manual de instalação

### 1.1 Pré-requisitos

- **JDK 17 ou superior** (Java Development Kit). Para verificar: `java -version`.
- Acesso à internet na primeira execução, para baixar o Maven e a biblioteca JUnit.

Não é preciso instalar o Maven: o repositório inclui o *Maven Wrapper* (`mvnw` no Linux e no macOS, `mvnw.cmd` no Windows), que baixa automaticamente a versão correta (3.9.9).

No Ubuntu ou no Debian, o JDK pode ser instalado com:

```bash
sudo apt install openjdk-17-jdk
```

No Windows e no macOS, basta instalar um JDK 17 ou superior, por exemplo o [Eclipse Temurin](https://adoptium.net/).

### 1.2 Obter o código

```bash
git clone https://github.com/ThiagoHSAl/copa27.git
cd copa27
```

### 1.3 Executar os testes

```bash
./mvnw test          # Linux e macOS
mvnw.cmd test        # Windows
```

A saída esperada termina com `Tests run: 141, Failures: 0, Errors: 0` e `BUILD SUCCESS`.
O relatório de cobertura do JaCoCo é gerado em `target/site/jacoco/index.html`.

### 1.4 Executar o sistema

```bash
./mvnw package -DskipTests
java -jar target/copa27-1.0.0.jar --demo
```

Em seguida, abra **http://localhost:8080** no navegador.

| Opção | Efeito |
|---|---|
| `--demo` | Registra resultados para as rodadas 1 e 2 da fase de grupos, para que a classificação e a simulação já tenham conteúdo. Sem essa opção, o torneio começa sem nenhum jogo disputado. |
| `--porta N` | Usa a porta `N` em vez da 8080. |

A área administrativa fica em **http://localhost:8080/admin**. O usuário é `admin`, e a senha padrão é `copa2027`. Para definir outra senha, use a variável de ambiente `COPA27_ADMIN_SENHA` antes de iniciar o sistema:

```bash
COPA27_ADMIN_SENHA=minhaSenha java -jar target/copa27-1.0.0.jar
```

Os dados ficam em memória: ao encerrar o processo, as alterações feitas pelo administrador são descartadas e, na próxima execução, a carga inicial é recriada.

### 1.5 Roteiro sugerido de demonstração

1. **Tabela de jogos.** Use os filtros por seleção, sede, fase e datas. Uma data inicial posterior à final é rejeitada.
2. **Classificação.** Com `--demo`, os grupos já têm dois jogos por seleção. As duas primeiras de cada grupo aparecem em destaque.
3. **Simular cruzamentos.** Clique em "Preencher fase de grupos com placares aleatórios": as oitavas são projetadas, e os placares do mata-mata podem ser simulados até a final. Para conferir o caminho de uma seleção, use "Ver caminho". "Restaurar cenário real" descarta tudo. Em outra aba anônima, nada do que foi simulado aparece.
4. **Administração.** Faça o login. Tente registrar o resultado de uma partida agendada, o que é recusado (RN06). Depois inicie a partida, registre o placar com "encerrar" e confira a classificação e o log de alterações. Ao encerrar a última partida da fase de grupos, as oitavas são preenchidas automaticamente.

---

## 2. Escopo implementado

A implementação cobre os 21 requisitos funcionais *Must have* da especificação (Documento 02, seção 7).

| Requisito | Descrição resumida | Onde está |
|---|---|---|
| RF01, RF02, RF03 | Tabela de jogos, filtros combinados e detalhes da partida | `ServicoConsulta`, `FiltroPartidas`, páginas `/partidas` e `/partida` |
| RF05, RF06 | Classificação dos grupos e critérios de desempate | `CalculadoraClassificacao`, `ComparadorDesempate`, página `/classificacao` |
| RF07 | Chaveamento em árvore | `Chaveamento`, `Confronto`, página `/chaveamento` |
| RF08, RF09 | Atualização automática da classificação e do mata-mata | `ServicoAdministracao.registrarResultado`, `GeradorChaveamento` |
| RF10 a RF13 | Simulação, projeção do chaveamento, caminho até a final e restauração | `ServicoSimulacao`, `Cenario`, página `/simulacao` |
| RF14, RF15, RF16, RF19 | Sedes, estádio, aeroportos e partidas da sede | `ServicoConsulta`, páginas `/sedes` e `/sede` |
| RF20, RF22 | Seleções e suas partidas | `ServicoConsulta`, páginas `/selecoes` e `/selecao` |
| RF23 | Cadastro, edição e remoção de seleções, sedes, estádios e partidas | `ServicoAdministracao`, área `/admin` |
| RF24, RF25 | Registro de resultado, recusado para partidas não iniciadas | `ServicoAdministracao`, `Partida` |

Requisitos não funcionais atendidos na implementação:

- **RNF02.** A simulação roda em memória; um teste simula o torneio inteiro e verifica que leva menos de 1 segundo.
- **RNF04.** Cada escrita roda em uma transação: se qualquer passo falhar, inclusive a gravação do log, nada é gravado.
- **RNF06.** Contraste alto e navegação por teclado, com foco visível.
- **RNF07.** A senha é guardada com hash PBKDF2-HMAC-SHA256 com sal; ver a seção 4.
- **RNF09.** A área de administração exige login.
- **RNF10.** Java 17 com Maven.
- **RNF11.** Ver a seção 3.
- **RNF14.** O domínio não depende de nenhuma outra camada.
- **RNF15.** O visitante não se identifica.
- **RNF16.** Aviso acadêmico no rodapé de todas as páginas.
- **RNF17.** Aeroportos ordenados pela distância.

Além disso, foi implementado o log de alterações (RF26, *Could have*), porque o diagrama de sequência do CU17 o inclui na mesma transação do registro de resultado.

**Fora do escopo desta entrega:** os requisitos *Should* e *Could* restantes, ou seja, o fuso horário configurável (RF04), os hotéis (RF17 e RF18), o perfil detalhado da seleção (RF21) e a busca textual (RF27). As classes `Hotel`, `Jogadora`, `MembroComissao` e `ParticipacaoCopa` existem no modelo, conforme o Documento 05, mas não têm dados nem telas. O HTTPS (RNF08) depende da implantação em servidor e não se aplica à execução local.

---

## 3. Arquitetura e testes

O código segue a arquitetura em quatro camadas do Projeto Arquitetural (Documento 04) e os nomes de classes e operações do Projeto Detalhado (Documento 05).

```
src/main/java/br/ufmg/dcc603/copa27/
├── apresentacao/           Interface Pública e Interface de Administração (HTTP)
├── servico/                Consulta, Competição, Simulação, Administração, Autenticação, Auditoria
├── dominio/
│   ├── modelo/             Modelo do Torneio: Cenario, Partida, Resultado, Grupo, Chaveamento...
│   └── regras/             Regras da Competição: CalculadoraClassificacao, ComparadorDesempate,
│                           GeradorChaveamento
├── persistencia/           Interfaces de repositório (Acesso a Dados)
│   └── memoria/            Repositórios em memória, transações e carga inicial
└── Aplicacao.java          Monta as camadas e inicia o servidor
```

A camada de apresentação usa o servidor HTTP embutido no próprio JDK (`com.sun.net.httpserver`) e gera HTML no servidor. Dessa forma, o projeto não tem nenhuma dependência de produção, e a única biblioteca externa é o JUnit, usado apenas nos testes.

### Testes

São 141 testes JUnit 5, distribuídos em 19 classes:

| Camada | Classes de teste | O que verificam |
|---|---|---|
| Domínio, modelo | `ResultadoTest`, `PartidaTest`, `GrupoClassificacaoTest`, `SelecaoSedeTest`, `CenarioChaveamentoTest` | A máquina de estados da partida, as regras RN01, RN02, RN05 e RN06, a cópia independente do cenário (RN07) e o caminho até a final |
| Domínio, regras | `ComparadorDesempateTest`, `CalculadoraClassificacaoTest`, `GeradorChaveamentoTest` | Cada critério de desempate (RN03), o cálculo da classificação, o cruzamento das oitavas (RN04), o avanço das vencedoras e a disputa de terceiro lugar |
| Serviços | `ServicoConsultaTest`, `FiltroPartidasTest`, `ServicoCompeticaoTest`, `ServicoSimulacaoTest`, `ServicoAdministracaoTest`, `AutenticacaoAuditoriaTest` | Os fluxos principal, alternativos e de exceção dos casos de uso CU01 a CU21 do escopo |
| Persistência | `PersistenciaMemoriaTest` | A carga inicial e o desfazimento das transações (RNF04) |
| Integração | `TorneioCompletoTest`, `ServidorWebTest` | As 64 partidas registradas pela administração, até a campeã; e o sistema por HTTP, incluindo login, recusas e isolamento das simulações |

Cobertura medida pelo JaCoCo:

| Pacote | Linhas | Ramos |
|---|---|---|
| `dominio.modelo` | 100,0% | 92,3% |
| `dominio.regras` | 100,0% | 95,8% |
| `servico` | 98,9% | 92,8% |
| `persistencia.memoria` | 100,0% | 96,2% |
| `apresentacao` | 91,1% | 72,4% |
| **Total** | **95,7%** | **85,5%** |

Todas as 27 classes do domínio são exercitadas pelos testes, o que supera o mínimo de 70% exigido pelo RNF11.

---

## 4. Decisões de implementação e ajustes em relação ao projeto detalhado

A implementação segue o Documento 05. Os pontos abaixo foram decididos ou ajustados durante a Sprint 3 e estão registrados aqui para manter a consistência com a documentação.

1. **Persistência em memória.** O Documento 05 deixou a tecnologia de acesso a dados para a Sprint 3. Foram implementados repositórios em memória (`persistencia.memoria`), pois isso permite executar o sistema e os testes sem instalar um banco de dados. Os serviços dependem apenas das interfaces de repositório; por isso, uma implementação com JDBC substituiria a atual sem alterar as demais camadas, conforme o RNF14.
2. **Transações.** Foi criada a interface `GerenciadorTransacoes`. A base em memória a implementa por instantâneo: antes da operação, o estado é copiado e, em caso de exceção, restaurado. Isso garante a atomicidade do CU17 exigida pelo RNF04.
3. **Simulação.** A classe `Partida` ganhou a operação `simular(Resultado)`. Ela aceita placares em partidas não encerradas, inclusive agendadas, e só é usada sobre a cópia do cenário. A classe `Classificacao` passou a indicar se é definitiva, o que permite deixar indefinidas as vagas de grupos incompletos (CU07, A1). Já o `GeradorChaveamento` ganhou a operação `projetar()`, que propaga as vencedoras simuladas e libera as vagas seguintes quando um placar simulado muda.
4. **Montagem do chaveamento.** A estrutura das 16 vagas fica em `GeradorChaveamento.montarChaveamento()`. Como o regulamento de 2027 ainda não foi publicado, foi adotado o cruzamento da Copa Feminina de 2023, que também teve 32 seleções em 8 grupos. Nesse formato, a oitava 1 é o 1º do Grupo A contra o 2º do Grupo C, e assim por diante.
5. **Desempate.** Depois dos quatro critérios da RN03, se o empate persistir, a ordem alfabética do país serve como critério final determinístico, no lugar do sorteio previsto no regulamento.
6. **Hash de senha.** O RNF07 aceita "bcrypt ou equivalente". Foi usado o PBKDF2 com HMAC-SHA256, 120 mil iterações e sal aleatório, que está disponível na biblioteca padrão do Java e dispensa dependências.
7. **Operações auxiliares.** Para atender aos requisitos sem contornar o projeto, foram acrescentadas as seguintes operações:
   - `ServicoConsulta.listarPartidasDaSede()`, para o RF19;
   - `ServicoCompeticao.obterGrupos()`, para exibir o grupo de cada seleção no RF20;
   - `ServicoAuditoria.listar()` e `RepositorioRegistros.listar()`, para a página do log;
   - `Partida.definirMandante()` e `Partida.definirVisitante()`, para o preenchimento do mata-mata.
8. **Interface de Administração.** Como o Administrador também é um Visitante, essa interface usa o `ServicoConsulta` e o `ServicoCompeticao` para listar os dados a manter.
   - Pela interface, é possível iniciar partidas, registrar resultados, cadastrar, editar e remover seleções, remover sedes e consultar o log.
   - O cadastro e a edição de sedes, estádios e partidas estão implementados e testados no `ServicoAdministracao`, mas ainda não têm formulário próprio.
9. **Partida de mata-mata sem seleções.** Partidas do mata-mata são cadastradas sem seleções. Por isso, só podem ser iniciadas depois que o chaveamento define as duas participantes, como indica a guarda da transição `iniciar()` na máquina de estados.

---

## 5. Uso de inteligência artificial

Conforme o item 8 das instruções do trabalho prático, o grupo declara o uso de inteligência artificial nesta Sprint.

- **Ferramenta.** Foi usado o assistente Claude Code (modelo Claude Opus 5.5, da Anthropic).
- **Forma de uso.** O assistente recebeu como entrada os documentos das Sprints 1 e 2 (requisitos, casos de uso e projetos arquitetural e detalhado, incluindo os diagramas em PlantUML) e as instruções do trabalho. A partir deles, gerou a primeira versão do código de produção, dos testes JUnit, da carga de dados fictícia e deste README.
- **Revisão pelo grupo.** O grupo revisou o código gerado, conferiu a correspondência com os diagramas de classes, de sequência e de máquina de estados e validou o comportamento do sistema executando os testes e a interface. As decisões da seção 4 foram analisadas pelo grupo.
- **Responsabilidade.** Os integrantes são os responsáveis pelo conteúdo entregue.

---

## 6. Referências

- SOMMERVILLE, I. *Engenharia de Software*. 10. ed. São Paulo: Pearson, 2018.
- GRUPO 4. Documentos 01 a 05 do Sistema Copa27: Modelo de Processo, Especificação de Requisitos, Casos de Uso, Projeto Arquitetural e Projeto Detalhado. DCC603, UFMG, 2026.
- FIFA. Copa do Mundo Feminina da FIFA Brasil 2027: cidades-sede. Disponível em: https://www.fifa.com/pt/tournaments/womens/womensworldcup/brazil-2027/host-cities
