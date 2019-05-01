public class WorkAlgoritm {

    private Double upperBorder;
    private Double lowBorder;
    private Double newUpperBorder;
    private Double newLowBorder;
//    Sell - это покупка  битков, Buy - это продажа битков
//    Добавить ограничения по сумме
//    Подумать о том, чтобы каждый вечер закрывать все открытые ордера????

    public void start(TradesPrices tradesPrices, OrderBookPrices orderBookPrices, PostRequests postRequests) {

        while (true) {
            System.out.println("Ща бум создавать корридор");
            createCorridors(tradesPrices);
            boolean needNewCorridors = false;
            while (!needNewCorridors) {
                if (tradesPrices.getActualSellPrice() > lowBorder
                        && tradesPrices.getActualBuyPrice() < upperBorder) {
                    actionInMainCorridor(tradesPrices);
                }
                if (tradesPrices.getActualSellPrice() < lowBorder
                        && tradesPrices.getActualSellPrice() > newLowBorder) {
                    needNewCorridors = actionInLowCorridor(tradesPrices);
                }
                if (tradesPrices.getActualBuyPrice() > upperBorder
                        && tradesPrices.getActualBuyPrice() < newUpperBorder) {
                    needNewCorridors = actionInHighCorridor(tradesPrices);
                }
            }
        }
    }

    private void createCorridors(TradesPrices tradesPrices) {
        Double actualPrice;
        if (upperBorder == null) {
            synchronized (tradesPrices.getBuyPrice()) {
                try {
                    System.out.println("Ща бум блокировать по цене покупки");
                    tradesPrices.getBuyPrice().wait();
                    System.out.println("Разбудили");
                } catch (InterruptedException e) {
                    e.printStackTrace();
                    System.out.println("interr");
                }
            }
            System.out.println("Считаем");
            actualPrice = (tradesPrices.getBuyPrice().get(0) + tradesPrices.getSellPrice().get(0)) / 2;
        } else {
            actualPrice = (tradesPrices.getActualBuyPrice() + tradesPrices.getActualSellPrice()) / 2;
        }
        upperBorder = actualPrice * 1.01;
        lowBorder = actualPrice * 0.9900990099;
        newUpperBorder = actualPrice * 1.02;
        newLowBorder = actualPrice * 0.98039215686;
        // Чтобы протестировать, надо будет сделать вообще мизерный доход, чтобы посмотреть как исполняются ордера
        // После, в идеале будет торговля на нескольких коинах, в пропертях определять процент и лимит торгов
        System.out.println("Корридор сделали: " + upperBorder + ", " + lowBorder + ", " + newLowBorder + ", " + newUpperBorder);
    }

    private void actionInMainCorridor(TradesPrices tradesPrices) {
        // Проверять ордера даже когда прайс не менятся? Вроде сделал - перерпроверить
        Double prevBuyPrice = null;
        while (true) {
            if (tradesPrices.getActualSellPrice() < lowBorder
                    || tradesPrices.getActualBuyPrice() > upperBorder) {
                return;
            }
            if (prevBuyPrice == null) {
                prevBuyPrice = tradesPrices.getActualBuyPrice();
                continue;
            }
            if (prevBuyPrice > tradesPrices.getActualBuyPrice()) {
                if (false/*есть открытые ордера*/) {
                    if (false/*открытый ордер на продажу*/) {
                        if (false/*открытый ордер пришел из основного коридора*/) {
                            // отменяю ордер
                        }
                    } else if (false/*открытый ордер на покупку*/) {
                        // отменяю ордер
                    }
                }
                if (false/*есть исполненные ордера на покупку*/) {
                    if (false/*исполненный ордер пришел из нижнего коридора*/) {
                        // выставляю ордер на продажу
                    }
                }
            } else if (prevBuyPrice < tradesPrices.getActualBuyPrice()) {
                if (false/*есть открытые ордера*/) {
                    if (false/*открытый ордер на продажу*/) {
                        // отменяю ордер
                    } else if (false/*открытый ордер на покупку*/) {
                        if (false/*открытый ордер пришел из основного коридора*/) {
                            // отменяю ордер
                        }
                    }
                }
                if (false/*есть исполненные ордера на продажу*/) {
                    if (false/*исполненный ордер на продажу пришел из верхнего коридора*/) {
                        // выставляю ордер на покупку
                    }
                }
            }
            if (false/*Есть открытый ордер на продажу и он пришел из верхнего корридора*/) {
                if (false/*открытый ордер не в верху стакана на продажу*/) {
                    // переставляю ордер
                }
            }
            if (false/*Есть открытый ордер на покупку и он пришел из нижнего корридора*/) {
                if (false/*открытый ордер не в верху стакана на покупку*/) {
                    // переставляю ордер
                }
            }
        }
    }


    private boolean actionInLowCorridor(TradesPrices tradesPrices) {
        // Проверять ордера даже когда прайс не меняется? Вроде сделал - перепроверить
        Double prevSellPrice = null;
        while (true) {
            if (tradesPrices.getActualSellPrice() > lowBorder) {
                return false;
            }
            if (prevSellPrice == null) {
                prevSellPrice = tradesPrices.getActualSellPrice();
                continue;
            }
            if (tradesPrices.getActualSellPrice() <= newLowBorder) {
                if (false/*есть открытые ордера*/) {
                    if (false/*открытый ордер на продажу*/) {
                        if (false/*открытый ордер не в верху стакана на продажу*/) {
                            // переставляю ордер
                        }
                    } else if (false/*открытый ордер на покупку*/) {
                        // отменяю ордер
                    }
                }
                // создаю ордер на продажу
                return true;
            } else if (tradesPrices.getActualSellPrice() > newLowBorder) {
                if (prevSellPrice > tradesPrices.getActualSellPrice()) {
                    if (false/*есть открытые ордера на покупку*/) {
                        // отменяю ордер
                    }
                } else if (tradesPrices.getActualSellPrice() < newLowBorder) {
                    if (false/*нет открытых ордеров или есть исполненные ордера*/) {
                        // создаю ордер на покупку
                    }
                    if (false/*есть открытые ордера на продажу*/) {
                        // отменяю ордер
                    }
                }
                if (false/*Есть открытый ордер на продажу и он не верхний в стакане на продажу*/) {
                    // переставляю ордер
                }
                if (false/*Есть открытый ордер на покупку и он не верхний в стакане на покупку*/) {
                    // переставляю ордер
                }
            }
        }
    }

    private boolean actionInHighCorridor(TradesPrices tradesPrices) {
        // Проверять ордера даже когда прайс не меняется, вроде сделал - перерпроверить
        Double prevBuyPrice = null;
        while (true) {
            if (tradesPrices.getActualBuyPrice() < upperBorder) {
                return false;
            }
            if (prevBuyPrice == null) {
                prevBuyPrice = tradesPrices.getActualBuyPrice();
                continue;
            }
            if (tradesPrices.getActualBuyPrice() >= newUpperBorder) {
                if (false/*есть открытые ордера*/) {
                    if (false/*открытый ордер на покупку*/) {
                        if (false/*открытый ордер не в верху стакана на продажу*/) {
                            // переставляю ордер
                        }
                    } else if (false/*открытый ордер на продажу*/) {
                        // отменяю ордер
                    }
                }
                // создаю ордер на покупку
                return true;
            } else if (tradesPrices.getActualBuyPrice() < newUpperBorder) {
                if (prevBuyPrice < tradesPrices.getActualBuyPrice()) {
                    if (false/*есть открытые ордера продажу*/) {
                        // отменяю ордер
                    }
                } else if (prevBuyPrice > tradesPrices.getActualBuyPrice()/*цена падает*/) {
                    if (false/*нет открытых ордеров или есть исполненные ордера*/) {
                        // создаю ордер на ппродажу
                    }
                    if (false/*есть открытые ордера на покупку*/) {
                        // отменяю ордер
                    }
                }
                if (false/*Есть открытый ордер на покупку и он не верхний в стакане на покупку*/) {
                    // переставляю ордер
                }
                if (false/*Есть открытый ордер на продажу и он не верхний в стакане на продажу*/) {
                    // переставляю ордер
                }
            }
        }
    }
}