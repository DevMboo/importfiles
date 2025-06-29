```markdown
# Projeto UploadService Java

Este projeto é uma aplicação Java para upload e processamento de arquivos CSV e XLSX, com persistência em banco de dados.

## Requisitos

- Java 11 ou superior
- [Apache POI](https://poi.apache.org/) (para leitura de arquivos XLSX)
- [Log4j 2](https://logging.apache.org/log4j/2.x/) (logging)
- [Commons IO](https://commons.apache.org/proper/commons-io/) (utilitários de IO)

## Instalação das Dependências

Baixe os seguintes JARs e coloque-os na pasta `lib/` do projeto:

- `log4j-api-2.20.0.jar`
- `log4j-core-2.20.0.jar`
- `commons-io-2.11.0.jar` (ou superior)
- JARs do Apache POI (ex: `poi-5.x.x.jar`, `poi-ooxml-5.x.x.jar`, `poi-ooxml-lite-5.x.x.jar`, `xmlbeans-5.x.x.jar`, etc.)

Você pode baixar os JARs em [Maven Repository](https://mvnrepository.com/).

## Estrutura do Projeto

```
src/
Main.java
services/
UploadService.java
repositories/
TbRefBasesRepository.java
UploadRepository.java
...
lib/
(coloque todos os JARs aqui)
uploads/
bases/
(arquivos enviados)
```

## Como Executar

No terminal, execute:

```sh
java -cp "lib/*;src" Main
```

## Funcionalidades

- Upload de arquivos CSV e XLSX
- Processamento assíncrono dos arquivos
- Inserção em lote no banco de dados
- Log detalhado de erros e progresso

## Observações

- Certifique-se de que as dependências estão corretas e atualizadas.
- O projeto não utiliza Maven/Gradle, então as dependências devem ser gerenciadas manualmente.
- Configure o banco de dados conforme necessário nos repositórios.

---

## Detalhes do Funcionamento e Arquitetura

### Passo a Passo das Funcionalidades

1. **Seleção da Base de Dados**
    - A aplicação utiliza a tabela `tb_ref_bases` para identificar a base de dados e a tabela de destino.
    - Estrutura esperada da tabela:
      ```sql
      SELECT id_bases, nome_tabela_raw, descricao, cod_documento, exemplo_base, status
      FROM public.tb_ref_bases;
      ```
    - O usuário deve garantir que a base de dados e as tabelas estejam organizadas conforme esse padrão.

2. **Upload e Processamento**
    - O upload dos arquivos (CSV ou XLSX) é feito via web service.
    - Após o upload, a aplicação lê as colunas do arquivo e prepara os dados para inserção.

3. **Inserção em Lote**
    - O fluxo de inserção é feito em lotes de 5000 linhas por operação, otimizando a performance e reduzindo o tempo de processamento.
    - Cada lote é inserido na tabela de destino definida na base selecionada.

4. **Arquitetura**
    - A aplicação é desenvolvida em Java puro, sem uso de frameworks externos.
    - O serviço roda como um web service simples, responsável pelo upload e processamento das cargas.

### Rotas Disponíveis

A aplicação expõe as seguintes rotas HTTP:

- `GET /api` — Endpoint principal para verificação do serviço.
- `GET /api/bases` — Retorna as bases disponíveis para upload.
- `POST /api/uploads` — Realiza o upload do arquivo e inicia o processamento.

Exemplo de definição das rotas:
```java
public void loadRoutes() {
    Router router = new Router();
    Router.addRoute("GET", "/api", ApiController::get);
    Router.addRoute("GET", "/api/bases", ApiController::bases);
    Router.addRoute("POST", "/api/uploads", ApiController::uploads);
}
```
```