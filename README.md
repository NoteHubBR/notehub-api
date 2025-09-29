## NoteHub versão alpha (backend)

#### NoteHub é um projeto open-source que fornece uma API REST escrita em Java + Spring Boot para um "Bloco de Notas Social". A ideia é oferecer uma base escalável e extensível para criar, compartilhar e processar notas, com autenticação JWT, persistência em PostgreSQL e processamento assíncrono via RabbitMQ. O projeto foi estruturado para ser simples de entender e fácil de contribuir — perfeito para colaboradores que queiram se desenvolver em um contexto real.

<br>

<div align="center">
  <a href="https://notehub.com.br">
    <img width="10%" height="10%" src="https://github.com/notehubbr/notehub-api/blob/main/src/main/resources/public/imgs/logo.png">
  </a>
</div>
<br>
<div align="center">
  <a href="https://github.com/notehubbr/notehub-api/releases/tag/v1.6.1">
    <img width="100px" height="25px" src="https://img.shields.io/badge/notehub-1.6.1-7c3aed">
  </a>
</div>

## Instalação

<details>

  <summary>Build</summary>

#### Pré-requisitos(build):

- Git
- Docker

1. Digite os seguintes comandos no terminal dentro da pasta desejada:

  ```bash
    git clone https://github.com/notehubbr/notehub-api.git
    cd springboot-notehub
  ```

2. Copie o arquivo de exemplo de variáveis de ambiente e ajuste conforme necessário:

  ```bash
    (Linux e macOS) cp .env.example .env
    (Windows) copy .env.example .env
  ```

3. Suba a aplicação com Docker Compose:

  ```bash
    docker compose up -d
  ```

4. Acesse a API em `http://localhost:8080` (por padrão). A documentação interativa normalmente fica em `http://localhost:8080/docs`.

5. Para parar e remover containers:

  ```bash
    docker compose down --rmi all --volumes
  ```

</details>

<details>

  <summary>Dev</summary>

#### Pré-requisitos(dev):

- Git
- Docker
- Java 21

1. Siga as intruções do build até a parte 2(faça a parte 2).

2. Em docker-compose.yml comente os blocos:

  ```docker
  # postgres:
  #   image: postgres:14
  #   container_name: postgres
  #   environment:
  #     POSTGRES_DB: ${PGDATABASE}
  #     POSTGRES_USER: ${PGUSERNAME}
  #     POSTGRES_PASSWORD: ${PGPASSWORD}
  #   ports:
  #     - '5432:5432'
  #   volumes:
  #     - postgres_data:/var/lib/postgresql/data

  #   app:
  #     container_name: app
  #     build:
  #       context: .
  #       dockerfile: Dockerfile
  #     ports:
  #       - '8080:8080'
  #     env_file:
  #       - .env.example
  #     depends_on:
  #       - postgres
  #       - rabbitmq
  #       - mailhog
  
  # volumes:
  #   postgres_data:
  ```

> Em ambiente de desenvolvimento será utilizado o banco de dados em memória e a aplicação será executada pela IDE.

3. Suba a aplicação com Docker Compose:

  ```bash
    docker compose up -d
  ```

4. Em `src/main/resources/application-dev.properties` preencha os valores das variáveis de ambiente:

> O arquivo .env só atende ao ambiente de produção.

  ```properties
api.server.host=${SERVER:http://localhost:8080}
api.client.host=${http://localhost:3000}
api.server.security.token.secret=${SECRET:seu-segredo}
oauth.github.client.id=${GHCI:seu-github-client-id}
oauth.github.client.secret=${GHCS:seu-github-client-secret}
spring.rabbitmq.addresses=${RABBITMQ_ADDRESSES:amqp://user:root@localhost:5672}
broker.queue.activation.name=default.activation
broker.queue.secret.name=default.secret
broker.queue.password.name=default.password
broker.queue.email.name=default.email
spring.mail.host=${SPRING_MAIL_HOST:localhost}
spring.mail.port=${SPRING_MAIL_PORT:1025}
spring.mail.friendly.name=${SPRING_MAIL_FRIENDLY_NAME:seu-nome-amigável}
spring.mail.username=${SPRING_MAIL_USERNAME:email-genérico}
spring.mail.password=${SPRING_MAIL_PASSWORD:}
  ```

5. Em `src/main/java/br/com/notehub/domain/notification/Notification.java` comente a seguinte parte:

> O banco de dados em memória não oferece suporte ao tipo de coluna JSON/JSONB.

  ```java
// @Column(columnDefinition = "JSONB")
// @JdbcTypeCode(SqlTypes.JSON)
@Convert(converter = NotificationFieldInfoConverter.class)
private Map<String, Object> info;
  ```

6. Inicie a aplicação.

  ```bash
    ./mvnw spring-boot:run
  ```

7. Acesse a API em `http://localhost:8080` (por padrão). A documentação interativa normalmente fica em `http://localhost:8080/docs`.

8. Para acessar a caixa de e-mails acesse `http://localhost:8025`.

9. Para parar e remover containers acione `CTRL+C` no terminal e em seguide digite:

  ```bash
    docker compose down --rmi all --volumes
  ```

</details>

## Documentação

#### A API é documentada em Swagger e acessível em <a href="https://api.notehub.com.br/docs">/docs</a>

## Relato de erros

#### Use o sistema de Issues do GitHub, crie uma issue com passos para reproduzir, comportamento esperado e logs/erros.

## Sugestão

#### Deixe um comentário com a nova ideia/sugestão na <a href="https://notehub.com.br/notehub/52b89a65-1c87-4692-9bf8-5096b674fa40">postagem dedicada.</a>

## Contribuição

#### Contribuições são mais do que bem-vindas! Aqui vai um fluxo sugerido para colaboradores:

1. Fork -> clone -> crie uma branch com um nome descritivo:

  ```bash
    git checkout -b feat/nova-funcionalidade
  ```

2. Faça commits em inglês, pequenos e claros seguindo o padrão: `(escopo)(referência):(mensagem)`. Ex.:

  ```bash
    git commit -m "feat(auth): add login via Discord"
  ```

3. Sincronize com o upstream (se estiver forked) e abra um Pull Request descrevendo:

- O que foi alterado;
- Porquê a alteração é necessária;
- Como testar manualmente;

4. Preencha checklist no PR:

- [x] Código segue o padrão do projeto
- [ ] Testes adicionados/atualizados
- [x] Documentação atualizada (se necessário)

5. Boas práticas para PRs

- Um propósito por PR (não agrupe várias funcionalidades sem relação).
- Inclua screenshots ou curl/postman snippets quando possível.
- Referencie a issue correspondente (ex.: Fixes #12).

## Licença

#### Ainda não há licença explícita no repositório.

## Créditos

- ###### Email Template por <a href="https://github.com/konsav/email-templates">***konsav***</a>
