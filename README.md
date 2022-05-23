# Tinkoff Trade Botfather

Приложение для создания, анализа и тестирования стратегий,
использующее [Tinkoff Инвестиции](https://www.tinkoff.ru/invest/).

**Предупреждение:** приложение не гарантирует дохода от Тинькофф Инвестиций.

### Использованный стек технологий

* [Java SDK Tinkoff Invest](https://github.com/Tinkoff/invest-api-java-sdk) - получение всей необходимой информации о
  бумагах,
  выставление заявок на биржу, получение информации о пользователе
* [Spring](https://spring.io/) - основной framework приложения
* [Caffeine](https://github.com/ben-manes/caffeine) - библиотека для кэширования запросов к API
* [Thymeleaf](https://www.thymeleaf.org/) - framework для генерации HTML страницы по шаблону

### Требования перед запуском приложения

* Установленная виртуальная Java машина (JVM) версии 17 и выше.
  Скачать: [OpenJDK 17](https://jdk.java.net/java-se-ri/17)
  или [Oracle](https://www.oracle.com/java/technologies/downloads/).
* Аккаунт в Тинькофф инвестициях и токен для API запросов.
    * Официальная [инструкция](https://tinkoff.github.io/investAPI/token/) по получению токена.
    * Прямая [ссылка](https://www.tinkoff.ru/invest/settings/api/).
* Стабильное Интернет-соединение.

### Запуск приложения

1. Скачайте [последний релиз приложения](https://github.com/UncleSema/ttb/releases).
2. Запустите TinkoffTradeBot.jar через командную строку: `java -jar TinkoffTradeBot.jar`.
3. Перейдите в браузере по адресу `localhost:8080`

Чуть более долгий способ:

1. Склонируйте репозиторий: `git clone https://github.com/UncleSema/ttb`
2. Запустите приложение: `./gradlew bootRun`
3. Перейдите в браузере по адресу `localhost:8080`
