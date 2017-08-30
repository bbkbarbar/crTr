package hu.barbar.cryptoTrader;

import java.io.IOException;
import java.math.BigDecimal;
import java.util.Date;
import java.util.Iterator;
import java.util.Map;

import org.knowm.xchange.Exchange;
import org.knowm.xchange.ExchangeFactory;
import org.knowm.xchange.currency.Currency;
import org.knowm.xchange.currency.CurrencyPair;
import org.knowm.xchange.dto.Order.OrderType;
import org.knowm.xchange.dto.account.AccountInfo;
import org.knowm.xchange.dto.account.Balance;
import org.knowm.xchange.dto.account.FundingRecord;
import org.knowm.xchange.dto.account.Wallet;
import org.knowm.xchange.dto.marketdata.Ticker;
import org.knowm.xchange.dto.trade.MarketOrder;
import org.knowm.xchange.exceptions.ExchangeException;
import org.knowm.xchange.exceptions.NotAvailableFromExchangeException;
import org.knowm.xchange.exceptions.NotYetImplementedForExchangeException;
import org.knowm.xchange.kraken.KrakenExchange;
import org.knowm.xchange.service.account.AccountService;
import org.knowm.xchange.service.marketdata.MarketDataService;
import org.knowm.xchange.service.trade.TradeService;
import org.knowm.xchange.service.trade.params.HistoryParamsFundingType;
import org.knowm.xchange.service.trade.params.TradeHistoryParamCurrencies;
import org.knowm.xchange.service.trade.params.TradeHistoryParams;
import org.knowm.xchange.service.trade.params.TradeHistoryParamsTimeSpan;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import hu.barbar.confighandler.JsonConfigHandler;

public class SmartSeller {

	private static final String configSourceJSONPath = "c:/kr_config.json";

	final static Logger logger = LoggerFactory.getLogger(SmartSeller.class);

	private static final int EXPECTED_ARGUMENT_COUNT = 3;
	
	private static SmartSeller me;
	
	private BigDecimal amount = null;
	
	private String currency = null;
	
	private CurrencyPair usedCurrencyPair = null;
	
	private BigDecimal stopPrice = null;
	
	private BigDecimal initialPrice = null;
	
	//TODO: remove static modifier later..
	private static Exchange krakenExchange = null;

	//TODO: remove static modifier later..
	private static MarketDataService marketDataService = null;
	
	
	public SmartSeller(String[] args){
		
		processParams(args);
		
		initExchange();

		usedCurrencyPair = getUSDBasedCurrencyPairFor(currency);
		
		// Get initialPrice from Kraken.
		long before, after;
		before = System.currentTimeMillis();
		initialPrice = getPriceOf(usedCurrencyPair);
		after = System.currentTimeMillis();
		System.out.println("Elasped time for get current price: " + (after - before) + " ms\n");
		if(initialPrice == null){
			System.exit(2);
		}
		
		BigDecimal currentlyAvailableBalance = this.getAvailableBalanceFor(this.currency);
		
		// Show parameters
		System.out.println("Initial price:      \t" + initialPrice + " USD / " + this.currency);
		System.out.println("Initial stop limit: \t" + stopPrice + " USD / " + this.currency);
		System.out.println("Amount:             \t" + (lessThen(this.amount,0)?"All available":this.amount) + " " + this.currency);
		System.out.println("Available balance:  \t" + currentlyAvailableBalance + " " + this.currency);
		System.out.println("Minimum income:     \t" + ((lessThen(this.amount,0)?currentlyAvailableBalance:this.amount).multiply(this.stopPrice)) + " USD");
		System.out.println("Current value:      \t" + ((lessThen(this.amount,0)?currentlyAvailableBalance:this.amount).multiply(this.initialPrice)) + " USD");
		System.out.println("Maximum loss:       \t" + ((lessThen(this.amount,0)?currentlyAvailableBalance:this.amount).multiply(this.initialPrice).subtract((lessThen(this.amount,0)?currentlyAvailableBalance:this.amount).multiply(this.stopPrice)) ) + " USD");
		
		// Check parameters and exit if invalid
		if(stopPrice.compareTo(initialPrice) >= 0){
			//TODO log
			System.out.println("ERROR! Inital stop price is NOT lower then initial price!");
			//TODO: create an "instant sell" option for this case..
			System.exit(3);
		}
		
		
	}
	
	private boolean lessThen(BigDecimal value, int compareValue){
		return value.compareTo(new BigDecimal(compareValue)) < 0;
	}
	
	
	private BigDecimal getAvailableBalanceFor(String currencyShortName){
		if(currencyShortName == null || currencyShortName.trim().equals("")){
			//TODO: handle this case
			return null;
		}
		Balance balance = getBalanceFor(currencyShortName); 
		if(balance == null){
			//TODO: handle this case
			return null;
		}
		return balance.getAvailable();
	}
	
	private Balance getBalanceFor(String currencyShortName){
		if(currencyShortName == null || currencyShortName.trim().equals("")){
			//TODO: handle this case
			return null;
		}
		AccountInfo accountInfo;
		try {
			accountInfo = krakenExchange.getAccountService().getAccountInfo();
		} catch (IOException e) {
			//TODO
			e.printStackTrace();
			return null;
		}
		Map<String, Wallet> wallets = accountInfo.getWallets();

		Iterator it = wallets.entrySet().iterator();
		while (it.hasNext()) {
			Map.Entry pair = (Map.Entry) it.next();
			// System.out.println("Wallet: " + pair.getKey() + " => " + pair.getValue());
			Wallet wallet = (Wallet) pair.getValue();

			Map<Currency, Balance> balances = wallet.getBalances();
			Iterator it2 = balances.entrySet().iterator();
			while (it2.hasNext()) {
				Map.Entry pair2 = (Map.Entry) it2.next();
				if(pair2.getKey().toString().trim().equalsIgnoreCase(currencyShortName.trim())){
					return (Balance) pair2.getValue();
				}
			}
		}
		
		return null;
	}

	
	private void initExchange(){
		krakenExchange = ExchangeFactory.INSTANCE.createExchange(KrakenExchange.class.getName());
		// Interested in the public market data feed (no authentication)
		marketDataService = krakenExchange.getMarketDataService();
		
		JsonConfigHandler config = new JsonConfigHandler(configSourceJSONPath);

		// krakenExchange = getExchangeForUser()
		krakenExchange.getExchangeSpecification().setApiKey(config.getString("api-key.appKey"));
		krakenExchange.getExchangeSpecification().setSecretKey(config.getString("api-key.privateKey"));
		krakenExchange.getExchangeSpecification().setUserName(config.getString("api-key.username"));
		krakenExchange.applySpecification(krakenExchange.getExchangeSpecification());
	}
	
	
	private void processParams(String[] args){
		System.out.println("Args size: " + args.length);
		if(args.length < EXPECTED_ARGUMENT_COUNT){
			System.out.println("Too few arguments.\nExample useage:"
					+ "SmartSeller.jar "
					+ "0.1 ETH 340.17"
					+ "");
			System.exit(1);
		}
		
		amount = new BigDecimal(args[0].replaceAll(",", ""));
		currency = args[1];
		stopPrice = new BigDecimal(args[2].replaceAll(",", ""));
		
		
		System.out.println("Amount: " + amount + " " + currency);
	}
	
	
	/**
	 * Return the last price of specified currency pair OR <br>
	 * <b>null</b> in case of exception caught.
	 * @param currencyPair
	 * @return
	 */
	private BigDecimal getPriceOf(CurrencyPair currencyPair){
		
		if(currencyPair == null){
			//TODO log..
			System.out.println("Can not get price of currencyPair without specified currencyPair (it was NULL).");
			return null;
		}
		
		BigDecimal lastPrice = null;
		
		// Get the latest ticker data showing price of specified currency pair.
		Ticker ticker;
		try {
			
			ticker = marketDataService.getTicker(currencyPair);
			lastPrice = ticker.getLast();

		} catch (NotAvailableFromExchangeException e) {
			// TODO Auto-generated catch block
			e.printStackTrace();
		} catch (ExchangeException e) {
			// TODO Auto-generated catch block
			e.printStackTrace();
		} catch (IOException e) {
			// TODO Auto-generated catch block
			e.printStackTrace();
		}
		
		return lastPrice;
	}

	
	private static CurrencyPair getUSDBasedCurrencyPairFor(String shortCoinName){
		if(shortCoinName == null || shortCoinName.trim().equals("")){
			//TODO log
			System.out.println("Can not create USD based CurrencyPair object for: |" + shortCoinName + "|.");
			return null;
		}
		
		if(shortCoinName.equalsIgnoreCase("BTC")){
			return CurrencyPair.BTC_USD;
		}
		
		if(shortCoinName.equalsIgnoreCase("ETH")){
			return CurrencyPair.ETH_USD;
		}
		
		if(shortCoinName.equalsIgnoreCase("BCH")){
			return CurrencyPair.BCH_USD;
		}
		
		if(shortCoinName.equalsIgnoreCase("XMR")){
			return CurrencyPair.XMR_USD;
		}
		
		if(shortCoinName.equalsIgnoreCase("XRP")){
			return CurrencyPair.XRP_USD;
		}
		
		//TODO log
		System.out.println("ERROR: Can not get CurrencyPair object for |" + shortCoinName + "|.");
		return null;
	}
	
	/**
	 * Create Selling order with specified parameters
	 * @param tradeableAmount
	 * @param currencyPair
	 * @return the OrderID of created order if it could be created OR <br>
	 * <b>null</b> if there were any problem..
	 */
	private String createSellOrderFor(BigDecimal tradeableAmount, CurrencyPair currencyPair){

		// Interested in the private trading functionality (authentication) 
		TradeService tradeService = krakenExchange.getTradeService();
		
		// Create a marketOrder with specified parameters 
		MarketOrder marketOrder = new MarketOrder(OrderType.ASK, tradeableAmount, currencyPair);
		
		String orderID = null;
		try {
			
			orderID = tradeService.placeMarketOrder(marketOrder);
			System.out.println("Order created with ID: " + orderID);
			
		} catch (NotAvailableFromExchangeException e) {
			// TODO Auto-generated catch block
			e.printStackTrace();
		} catch (NotYetImplementedForExchangeException e) {
			// TODO Auto-generated catch block
			e.printStackTrace();
		} catch (ExchangeException e) {
			// TODO Auto-generated catch block
			e.printStackTrace();
		} catch (IOException e) {
			// TODO Auto-generated catch block
			e.printStackTrace();
		}
		
		return orderID;
	}
	
	
	// amount shortCoinName initialStopPrice
	// 0.01   ETH           330.17
	public static void main(String[] args) {
		
		me = new SmartSeller(args);
		
		/*

		// Get the latest ticker data showing BTC to USD
		Ticker ticker;
		try {
			ticker = marketDataService.getTicker(CurrencyPair.BTC_USD);
			System.out.println("Ticker: " + ticker.toString());
			System.out.println("Currency: " + Currency.USD);
			System.out.println("Last: " + ticker.getLast().toString());
			// System.out.println("Volume: " + ticker.getVolume().toString());
			// System.out.println("High: " + ticker.getHigh().toString());
			// System.out.println("Low: " + ticker.getLow().toString());

		} catch (NotAvailableFromExchangeException e) {
			// TODO Auto-generated catch block
			e.printStackTrace();
		} catch (ExchangeException e) {
			// TODO Auto-generated catch block
			e.printStackTrace();
		} catch (IOException e) {
			// TODO Auto-generated catch block
			e.printStackTrace();
		}

		// Interested in the private trading functionality (authentication)
		TradeService tradeService = krakenExchange.getTradeService();
		// Get the open orders
		try {

			OpenOrders openOrders = tradeService.getOpenOrders();
			if (openOrders.getOpenOrders().isEmpty()) {
				System.out.println("There are no open orders.");
			} else {
				System.out.println(openOrders.toString());
			}
			
			
		} catch (NotAvailableFromExchangeException e1) {
			// TODO Auto-generated catch block
			e1.printStackTrace();
		} catch (NotYetImplementedForExchangeException e1) {
			// TODO Auto-generated catch block
			e1.printStackTrace();
		} catch (ExchangeException e1) {
			// TODO Auto-generated catch block
			e1.printStackTrace();
		} catch (IOException e1) {
			// TODO Auto-generated catch block
			e1.printStackTrace();
		}

		
		/*
		try {
			AccountInfo accountInfo = krakenExchange.getAccountService().getAccountInfo();
			System.out.println("\n\nAccount Info: " + accountInfo.toString() + "\n\n");
			Map<String, Wallet> wallets = accountInfo.getWallets();

			Iterator it = wallets.entrySet().iterator();
			while (it.hasNext()) {
				Map.Entry pair = (Map.Entry) it.next();
				// System.out.println("Wallet: " + pair.getKey() + " => " + pair.getValue());
				Wallet wallet = (Wallet) pair.getValue();

				Map<Currency, Balance> balances = wallet.getBalances();
				System.out.println("Balances: " + balances + "\n-\n");
				Iterator it2 = balances.entrySet().iterator();
				while (it2.hasNext()) {
					Map.Entry pair2 = (Map.Entry) it2.next();
					System.out.println(pair2.getKey() + ": " + ((Balance) pair2.getValue()).getTotal().toPlainString());
				}
			}
			

			 // Order

			 // // Interested in the private trading functionality
			 // (authentication) TradeService tradeService =
			 // krakenExchange.getTradeService();
			 // 
			 // // place a marketOrder with volume 0.01 OrderType orderType =
			 // (OrderType.ASK); BigDecimal tradeableAmount = new
			 // BigDecimal("0.15");
			 // 
			 // MarketOrder marketOrder = new MarketOrder(orderType,
			 // tradeableAmount, CurrencyPair.ETH_USD);
			 // 
			 // String orderID = tradeService.placeMarketOrder(marketOrder);
			 // System.out.println("Market Order ID: " + orderID); 
			 

		} catch (NotAvailableFromExchangeException e) {
			e.printStackTrace();
		} catch (ExchangeException e) {
			e.printStackTrace();
		} catch (IOException e) {
			// TODO Auto-generated catch block
			e.printStackTrace();
		}
		/**/
	}

	private Exchange getExchangeForUser(String apiKey, String privateKey, String userName) {

		Exchange krakenExchange = ExchangeFactory.INSTANCE.createExchange(KrakenExchange.class.getName());
		krakenExchange.getExchangeSpecification().setApiKey(apiKey);
		krakenExchange.getExchangeSpecification().setSecretKey(privateKey);
		krakenExchange.getExchangeSpecification().setUserName(userName);
		krakenExchange.applySpecification(krakenExchange.getExchangeSpecification());
		return krakenExchange;

	}

	private static void fundingHistory(AccountService accountService) throws IOException {
		// Get the funds information
		TradeHistoryParams params = accountService.createFundingHistoryParams();
		if (params instanceof TradeHistoryParamsTimeSpan) {
			final TradeHistoryParamsTimeSpan timeSpanParam = (TradeHistoryParamsTimeSpan) params;
			timeSpanParam.setStartTime(new Date(System.currentTimeMillis() - (1 * 12 * 30 * 24 * 60 * 60 * 1000L)));
		}

		if (params instanceof HistoryParamsFundingType) {
			((HistoryParamsFundingType) params).setType(FundingRecord.Type.DEPOSIT);
		}

		if (params instanceof TradeHistoryParamCurrencies) {
			final TradeHistoryParamCurrencies currenciesParam = (TradeHistoryParamCurrencies) params;
			currenciesParam.setCurrencies(new Currency[] { Currency.BTC, Currency.USD });
		}

	}

}
