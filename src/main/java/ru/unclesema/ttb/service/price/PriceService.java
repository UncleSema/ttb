package ru.unclesema.ttb.service.price;

import ru.tinkoff.piapi.contract.v1.LastPrice;
import ru.tinkoff.piapi.contract.v1.OrderDirection;
import ru.unclesema.ttb.model.User;

import java.math.BigDecimal;
import java.util.Map;

/**
 * Сервис, отвечающий за работу с балансом / последними ценами / stop запросами
 */
public interface PriceService {
    /**
     * Метод ищет последнюю цену среди добавленных (если не находит, отправляет запрос к api).
     */
    BigDecimal getLastPrice(User user, String figi);

    /**
     * Метод находит все инструменты, которые ещё не были проданы
     *
     * <p>Метод смотрит на последние операции пользователя, храня <code>Map</code> и прибавляя 1 к инструменту, в случае покупки, и вычитая 1 иначе.</p>
     * <p>
     * Другие возможные реализации:
     * <ul>
     *     <li> Portfolio, метод будет делать запрос к api, кэшируя его. Такая реализация не очень хороша из-за `несинхронизованности` с операциями,
     *     которые используются в UI. </li>
     *     <li> LastTrades, сервис подпишется на LastTrades, по которым будет считать оставшиеся инструменты. Такая реализация не очень хороша,
     *     из-за того, что подписаться на LastTrades можно только при торговле на бирже</li>
     * </ul>
     * </p>
     */
    Map<String, Long> getRemainingInstruments(User user);

    /**
     * Добавить последнюю цену.
     */
    void addLastPrice(LastPrice price);

    /**
     * Метод обрабатывает новую операцию и добавляет стоп запросы
     */
    void processNewOperation(User user, String figi, BigDecimal takeProfit, BigDecimal price, BigDecimal stopLoss, OrderDirection direction);

    /**
     * Метод перебирает все стоп запросы, выставляя на биржу нужные.
     */
    void checkStopRequests(LastPrice price);

    /**
     * Удаляет все стоп запросы для пользователя
     */
    void deleteRequestsForUser(User user);

    /**
     * Переводит указанную цену в рубли
     */
    BigDecimal getPriceInRubles(User user, BigDecimal price, String figi);

    /**
     * Возвращает количество рублей, потраченных стратегией
     */
    BigDecimal getBalance(User user);
}
