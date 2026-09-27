# Mailer

> A lightweight Java service for sending e-mail over SMTP.

![Java](https://img.shields.io/badge/Java-ED8B00?logo=openjdk&logoColor=white)
<!-- TODO: Spring Boot ise ekle:
![Spring Boot](https://img.shields.io/badge/Spring_Boot-6DB33F?logo=springboot&logoColor=white)
-->

<!-- NOT: Bu projeye dair elimde detay yoktu; README iskelet olarak hazırlandı.
     "TODO" yorumları GitHub'da görünmez, kodunla karşılaştırıp doldur. -->

## Overview

Mailer centralises outgoing e-mail so that other applications do not need to carry their own SMTP configuration. It takes a recipient, a subject and a body, connects to the configured SMTP server and delivers the message.
<!-- TODO: Projenin asıl amacını 1-2 cümleyle yaz (ör. izin takvimi bildirimleri, sprint raporu gönderimi, toplu mail). -->

## Features

- Plain-text and HTML e-mail
- Multiple recipients (To / CC / BCC)
- File attachments
- SMTP with TLS / STARTTLS
- Configuration through environment variables or a properties file
<!-- TODO: Olmayan maddeleri sil; şablon (template) desteği, kuyruk, zamanlama gibi ekstra özellikler varsa ekle. -->

## Tech stack

| Component | Technology |
|---|---|
| Language | Java |
| Mail | Jakarta Mail (SMTP) |
| Build | Maven |

<!-- TODO: Spring Boot + spring-boot-starter-mail kullanıyorsan buraya yaz; Gradle ise Build satırını düzelt. -->

## Getting started

### Prerequisites

- JDK 17 or newer <!-- TODO: pom.xml'deki java.version ile eşleştir -->
- An SMTP account (Gmail, Outlook / Microsoft 365, a corporate mail server, etc.)

### Configuration

Set the variables below in your environment or in `application.properties`. Never commit real credentials.
<!-- TODO: Değişken adlarını koddaki gerçek isimlerle eşleştir. -->

| Variable | Description | Example |
|---|---|---|
| `MAIL_HOST` | SMTP server | `smtp.gmail.com` |
| `MAIL_PORT` | SMTP port | `587` |
| `MAIL_USERNAME` | SMTP user | `bot@example.com` |
| `MAIL_PASSWORD` | SMTP password / app password | – |
| `MAIL_FROM` | Default sender address | `noreply@example.com` |
| `MAIL_STARTTLS` | Enable STARTTLS | `true` |

> **Gmail note:** use an *App Password* (requires 2-step verification), not your account password.

### Build & run

```bash
./mvnw clean package
java -jar target/mailer.jar
```
<!-- TODO: Jar adını target/ altındaki gerçek dosya adıyla değiştir. -->

## Usage

<!-- TODO: Aşağıdaki iki bölümden projeye uyanı bırak, diğerini sil. -->

**As a REST service**

```http
POST /api/mail/send
Content-Type: application/json

{
  "to": ["user@example.com"],
  "subject": "Hello",
  "body": "<p>Hello from Mailer</p>",
  "html": true
}
```

**As a library**

```java
Mailer mailer = new Mailer(config);
mailer.send("user@example.com", "Hello", "Hello from Mailer");
```

## Project structure

```
.
├── src/main/java/       # Source code
├── src/main/resources/  # application.properties, templates
├── pom.xml
└── README.md
```
<!-- TODO: Gerçek klasör yapısıyla değiştir. -->

## License

Internal project – all rights reserved.
