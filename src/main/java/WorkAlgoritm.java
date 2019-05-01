public class WorkAlgoritm {

    private Double upperBorder;
    private Double lowBorder;
    private Double newUpperBorder;
    private Double newLowBorder;
//    Sell - это покупка  битков, Buy - это продажа битков

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
                    needNewCorridors = actionInLowCorridor();
                }
                if (tradesPrices.getActualBuyPrice() > upperBorder
                        && tradesPrices.getActualBuyPrice() < newUpperBorder) {
                    needNewCorridors = actionInHighCorridor();
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
        while (true) {
            if (tradesPrices.getActualSellPrice() < lowBorder
                    || tradesPrices.getActualBuyPrice() > upperBorder) {
                return;
            }
            if (false/*цена падает*/) {
                if (false/*есть открытые ордера*/) {
                    if (false/*открытый ордер на продажу*/) {
                        if (false/*ордер пришел из верхнего коридора*/) {
                            if (false/*открытый ордер не в верху стакана на продажу*/) {
                                // переставляю ордер
                            }
                        } else if (false/*открытый ордер пришел из основного коридора*/) {
                            // отменяю ордер
                        }
                    } else if (false/*открытый ордер на покупку*/) {
                        // отменяю ордер
                    }
                }
                if (false/*есть исполненные ордера*/) {
                    if (false/*исполненный ордер на покупку*/) {
                        if (false/*исполненный ордер пришел из нижнего коридора*/) {
                            // выставляю ордер на продажу
                        }
                    }
                }
            } else if (false/*цена растет*/) {
                if (false/*есть открытые ордера*/) {
                    if (false/*открытый ордер на продажу*/) {
                        // отменяю ордер
                    } else if (false/*открытый ордер на покупку*/) {
                        if (false/*открытый ордер пришел из основного коридора*/) {
                            // отменяю ордер
                        } else if (false/*открытый ордер пришел из нижнего коридора*/) {
                            if (false/*открытый ордер не в верху стакана на покупку*/) {
                                // переставляю ордер
                            }
                        }
                    }
                }
                if (false/*есть исполненные ордера*/) {
                    if (false/*исполненные ордера на продажу*/) {
                        if (false/*исполненный ордер на продажу пришел из нижнего коридора*/) {
                            // выставляю ордер на покупку
                        }
                    }
                }
            }
        }
    }


    private boolean actionInLowCorridor() {
        while (true) {
            if (false/*цена вышла через верхнюю границу*/) {
                return false;
            }
            if (false/*нижняя граница пройдена*/) {
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
                // создаю новые границы
            } else if (false/*нижняя граница не пройдена*/) {
                if (false/*цена падает*/) {
                    if (false/*есть открытые ордера*/) {
                        if (false/*это открытый ордер на продажу*/) {
                            if (false/*открытый ордер не верхний в стакане на продажу*/) {
                                // переставляю ордер
                            }
                        } else if (false/*это ордер на покупку*/) {
                            // отменяю ордер
                        }
                    }
                } else if (false/*цена растет*/) {
                    if (false/*нет открытых ордеров или есть исполненные ордера*/) {
                        // создаю ордер на покупку
                    }
                    if (false/*есть открытые ордера*/) {
                        if (false/*открытый ордер на продажу*/) {
                            // отменяю ордер
                        } else if (false/*открытый ордер на покупку*/) {
                            if (false/*ордер в верху стакана на покупку*/) {
                                // жду
                            } else if (false/*открытый ордер не в верху стакана на покупку*/) {
                                // переставляю ордер
                            }
                        }
                    }
                }
            }
        }
    }

    private boolean actionInHighCorridor() {
        while (true) {
            if (false/*цена вышла через нижнюю границу*/) {
                return false;
            }
            if (false/*верхняя граница пройдена*/) {
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
                // создаю новые границы
            } else if (false/*верхняя граница не пройдена*/) {
                if (false/*цена растет*/) {
                    if (false/*есть открытые ордера*/) {
                        if (false/*это открытый ордер на покупку*/) {
                            if (false/*открытый ордер не верхний в стакане на покупку*/) {
                                // переставляю ордер
                            }
                        } else if (false/*это ордер на продажу*/) {
                            // отменяю ордер
                        }
                    }
                } else if (false/*цена падает*/) {
                    if (false/*нет открытых ордеров или есть исполненные ордера*/) {
                        // создаю ордер на ппродажу
                    }
                    if (false/*есть открытые ордера*/) {
                        if (false/*открытый ордер на покупку*/) {
                            // отменяю ордер
                        } else if (false/*открытый ордер на продажу*/) {
                            if (false/*ордер в верху стакана на продажу*/) {
                                // жду
                            } else if (false/*открытый ордер не в верху стакана на продажу*/) {
                                // переставляю ордер
                            }
                        }
                    }
                }
            }
        }
    }
}
