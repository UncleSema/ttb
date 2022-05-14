package ru.unclesema.ttb.service.front;

import ru.tinkoff.piapi.contract.v1.Operation;

import java.math.BigDecimal;
import java.util.List;
import java.util.Map;

public record StrategyStatement(Map<String, BigDecimal> benefitByCurrency, List<Operation> operationsOrder) {

}
