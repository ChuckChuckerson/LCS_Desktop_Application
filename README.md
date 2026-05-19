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

(Некоторые каталоги не загружены в репозиторий, это примерная мини-структура проекта. После будут добавлены все каталоги, с классами и интерфейсами).
- `src/main/java/com/lawyercompany` - главный модуль (MainApp, controller, service, dao, entity)
- `src/main/resources/view` - FXML-макеты
- `src/main/resources/css` - стили
- `src/test/java` - модульные тесты

## Автор

Исингалиев Максим Рамазанович, группа ИСП-33 (учебная практика ПМ.01, 2026).
