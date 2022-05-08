package ru.unclesema.ttb.strategy;

import ru.tinkoff.piapi.contract.v1.OrderBook;

/**
 * Интерфейс, с помощью которого можно реализовать свою стратегию.
 * Для этого достаточно:
 * <ul>
 * <li> "сказать когда покупать" -- реализовать <code>buy</code> </li>
 * <li> "сказать когда продавать" -- реализовать <code>sell</code> </li>
 * <li> предоставить информацию для стратегии при помощи <code>StrategyConfig</code> </li>
 * </ul>
 */
public interface Strategy {

    StrategyDecision addOrderBook(OrderBook orderBook);

    /**
     * @return <code>true</code>, если ценная бумага рекомендованна к покупке и <code>false</code> иначе
     */
    boolean buy();

    /**
     * @return <code>true</code>, если ценная бумага рекомендованна к продаже и <code>false</code> иначе
     */
    boolean sell();

    /**
     * @return конфиг, которым параметризованна стратегия
     */
    StrategyConfig getConfig();
}

