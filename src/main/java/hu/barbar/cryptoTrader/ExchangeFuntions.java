package hu.barbar.cryptoTrader;

import java.io.IOException;
import java.math.BigDecimal;
import java.util.Iterator;
import java.util.Map;

import org.knowm.xchange.Exchange;
import org.knowm.xchange.ExchangeFactory;
import org.knowm.xchange.currency.Currency;
import org.knowm.xchange.currency.CurrencyPair;
import org.knowm.xchange.dto.Order.OrderType;
import org.knowm.xchange.dto.account.AccountInfo;
import org.knowm.xchange.dto.account.Balance;
import org.knowm.xchange.dto.account.Wallet;
import org.knowm.xchange.dto.marketdata.Ticker;
import org.knowm.xchange.dto.trade.MarketOrder;
import org.knowm.xchange.exceptions.ExchangeException;
import org.knowm.xchange.exceptions.NotAvailableFromExchangeException;
import org.knowm.xchange.exceptions.NotYetImplementedForExchangeException;
import org.knowm.xchange.kraken.KrakenExchange;
import org.knowm.xchange.service.marketdata.MarketDataService;
import org.knowm.xchange.service.trade.TradeService;

public class ExchangeFuntions {

	private static Exchange usedExchange = null;
	
	public static void setExchange(Exchange exchange){
		ExchangeFuntions.usedExchange = exchange;
	}
	
	public static CurrencyPair getUSDBasedCurrencyPairFor(String shortCoinName){
		if(shortCoinName == null || shortCoinName.trim().equals("")){
			//TODO log
			//log("Can not create USD based CurrencyPair object for: |" + shortCoinName + "|.");
			return null;
		}
		
		if(shortCoinName.equalsIgnoreCase("XBT")){
			return CurrencyPair.BTC_USD;
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
		//log("ERROR: Can not get CurrencyPair object for |" + shortCoinName + "|.");
		return null;
	}
	
	/**
	 * Create Selling order with specified parameters
	 * @param tradeableAmount
	 * @param currencyPair
	 * @return the OrderID of created order if it could be created OR <br>
	 * <b>null</b> if there were any problem..
	 */
	public static String createSellOrderFor(BigDecimal tradeableAmount, CurrencyPair currencyPair){

		// Interested in the private trading functionality (authentication) 
		TradeService tradeService = usedExchange.getTradeService();
		
		// Create a marketOrder with specified parameters 
		MarketOrder marketOrder = new MarketOrder(OrderType.ASK, tradeableAmount, currencyPair);
		
		String orderID = null;
		try {
			
			orderID = tradeService.placeMarketOrder(marketOrder);
			//TODO: log
			//log("Order created with ID: " + orderID);
			
		} catch (NotAvailableFromExchangeException e) {
			//TODO: handle exception cases 
			e.printStackTrace();
		} catch (NotYetImplementedForExchangeException e) {
			e.printStackTrace();
		} catch (ExchangeException e) {
			e.printStackTrace();
		} catch (IOException e) {
			e.printStackTrace();
		}
		
		return orderID;
	}
	
	
	/**
	 * Return the last price of specified currency pair OR <br>
	 * <b>null</b> in case of exception caught.
	 * @param currencyPair
	 * @return
	 */
	public static BigDecimal getPriceOf(MarketDataService marketDataService, CurrencyPair currencyPair){
		
		if(currencyPair == null){
			//TODO log..
			//log("Can not get price of currencyPair without specified currencyPair (it was NULL).");
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

	
	public static BigDecimal getAvailableBalanceFor(String currencyShortName){
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
	
	
	public static Balance getBalanceFor(String currencyShortName){
		if(currencyShortName == null || currencyShortName.trim().equals("")){
			//TODO: handle this case
			return null;
		}
		
		AccountInfo accountInfo;
		
		if(usedExchange == null){
			//TODO: log
			return null;
		}
		
		try {
			accountInfo = usedExchange.getAccountService().getAccountInfo();
		} catch (IOException e) {
			//TODO
			e.printStackTrace();
			return null;
		}
		Map<String, Wallet> wallets = accountInfo.getWallets();

		Iterator it = wallets.entrySet().iterator();
		while (it.hasNext()) {
			Map.Entry pair = (Map.Entry) it.next();
			// log("Wallet: " + pair.getKey() + " => " + pair.getValue());
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
	
	public Exchange getExchangeForUser(String apiKey, String privateKey, String userName) {
		Exchange krakenExchange = ExchangeFactory.INSTANCE.createExchange(KrakenExchange.class.getName());
		krakenExchange.getExchangeSpecification().setApiKey(apiKey);
		krakenExchange.getExchangeSpecification().setSecretKey(privateKey);
		krakenExchange.getExchangeSpecification().setUserName(userName);
		krakenExchange.applySpecification(krakenExchange.getExchangeSpecification());
		return krakenExchange;
	}
	
}
