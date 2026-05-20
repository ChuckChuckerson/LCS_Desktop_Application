# Lawyer Company System

Настольное JavaFX-приложение для автоматизации работы юридической компании: учёт дел, документов, встреч и сообщений. Поддерживаются роли клиента, адвоката и администратора.

## Стек технологий

- Java 17, JavaFX 17 (FXML, CSS)
- Maven
- PostgreSQL (JDBC), HikariCP
- JUnit 5
- BCrypt (хеширование паролей)

## Требования

- JDK 17+
- Maven 3.8+

## Установка и запуск

```bash
git clone <URL-репозитория>
cd LawyerCompanySystem
mvn javafx:run
```

Сборка JAR:

```bash
mvn clean package
java -jar target/lawyer-company-system-1.0-Demo.jar
```

## Тесты

```bash
mvn test
```

## Структура проекта

(В репозитории ещё нет некоторых папок с файлами, позже будут добавлены)
LawyerCompanySystem/
├── pom.xml
├── README.md
└── src/
    ├── main/
    │   ├── java/
    │   │   ├── module-info.java
    │   │   └── com/lawyercompany/
    │   │       ├── MainApp.java              # точка входа, навигация между сценами
    │   │       ├── controller/               # JavaFX-контроллеры
    │   │       ├── service/                  # бизнес-логика
    │   │       ├── dao/                      # интерфейсы доступа к данным
    │   │       │   └── impl/                 # реализации DAO (JDBC + HikariCP)
    │   │       ├── entity/                   # POJO-сущности (User, Case, Document, Meeting и др.)
    │   │       ├── factory/                  # фабрика DAOFactory
    │   │       └── up/                       # утилиты, API учебной практики
    │   └── resources/
    │       ├── view/                         # FXML-макеты
    │       ├── css/
    │       │   └── styles.css                # единый стиль приложения
    │       ├── config.properties             # параметры подключения к БД
    │       └── icons/                        # иконки приложения
    └── test/
        └── java/
            └── com/lawyercompany/            # модульные тесты (JUnit 5)

## Автор

Исингалиев Максим Рамазанович, группа ИСП-33 (учебная практика ПМ.01, 2026).
