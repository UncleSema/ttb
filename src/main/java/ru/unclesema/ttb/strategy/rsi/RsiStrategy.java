package ru.unclesema.ttb.strategy.rsi;

//@Slf4j
//public class RsiStrategy implements Strategy {
//    private final RsiConfig config = new RsiConfig();
//
//    @Override
//    public StrategyDecision addOrderBook() {
//
//        return null;
//    }
//
//    @Override
//    public boolean buy() {
////        List<VisualizationCandle> subList = candleList.stream()
////                .dropWhile(candle -> candle.getTs().plusDays(RsiConfig.periodDays).isBefore(candleList.get(candleList.size() - 1).getTs()))
////                .toList();
////        if (candleList.size() < 2) return false;
////        BigDecimal rsi = rsi(subList);
////        return rsi.compareTo(RsiConfig.lowerRsiThreshold) < 0;
//        return false;
//    }
//
//    @Override
//    public boolean sell() {
////        List<VisualizationCandle> subList = candleList.stream()
////                .dropWhile(candle -> candle.getTs().plusDays(RsiConfig.periodDays).isBefore(candleList.get(candleList.size() - 1).getTs()))
////                .toList();
////        BigDecimal rsi = rsi(subList);
////        if (candleList.size() < 2) return false;
////        return rsi.compareTo(RsiConfig.upperRsiThreshold) > 0;
//        return false;
//    }
//
//    @Override
//    public StrategyConfig getConfig() {
//        return config;
//    }
//
//    private BigDecimal rsi(List<VisualizationCandle> candleList) {
//        BigDecimal totalGain = BigDecimal.ZERO;
//        int gainAmount = 0;
//        BigDecimal totalLoss = BigDecimal.ZERO;
//        int lossAmount = 0;
//
//        //берем последние n свечей до текущего времени
//        for (int i = 1; i < candleList.size(); i++) {
//            BigDecimal candleClosePrice = candleList.get(i).getClosePrice();
//            BigDecimal prevCandleClosePrice = candleList.get(i - 1).getClosePrice();
//            BigDecimal change = candleClosePrice.subtract(prevCandleClosePrice);
//            if (candleClosePrice.equals(prevCandleClosePrice)) continue;
//
//            if (change.compareTo(BigDecimal.ZERO) >= 0) {
//                totalGain = totalGain.add(change);
//                gainAmount++;
//            } else {
//                totalLoss = totalLoss.add(change);
//                lossAmount++;
//            }
//        }
//        if (gainAmount == 0) gainAmount = 1;
//        if (lossAmount == 0) lossAmount = 1;
//
//        BigDecimal avgGain = totalGain.divide(BigDecimal.valueOf(gainAmount), RoundingMode.DOWN);
//        if (avgGain.equals(BigDecimal.ZERO)) avgGain = BigDecimal.ONE;
//
//        BigDecimal avgLoss = totalLoss.divide(BigDecimal.valueOf(lossAmount), RoundingMode.DOWN);
//        if (avgLoss.equals(BigDecimal.ZERO)) avgLoss = BigDecimal.ONE;
//
//        BigDecimal rs = avgGain.divide(avgLoss, RoundingMode.DOWN).abs();
//        BigDecimal hundred = BigDecimal.valueOf(100);
//        BigDecimal rsi = hundred.subtract(hundred.divide(BigDecimal.ONE.add(rs), RoundingMode.DOWN));
//        System.err.println(rsi);
//        return rsi;
//    }
//}
