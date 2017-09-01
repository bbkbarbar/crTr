package hu.barbar.cryptoTrader;

import java.io.IOException;
import java.math.BigDecimal;

import org.knowm.xchange.Exchange;
import org.knowm.xchange.ExchangeFactory;
import org.knowm.xchange.currency.CurrencyPair;
import org.knowm.xchange.dto.Order.OrderType;
import org.knowm.xchange.dto.marketdata.Ticker;
import org.knowm.xchange.dto.trade.MarketOrder;
import org.knowm.xchange.exceptions.ExchangeException;
import org.knowm.xchange.exceptions.NotAvailableFromExchangeException;
import org.knowm.xchange.exceptions.NotYetImplementedForExchangeException;
import org.knowm.xchange.kraken.KrakenExchange;
import org.knowm.xchange.service.marketdata.MarketDataService;
import org.knowm.xchange.service.trade.TradeService;

public class ExchangeFuntions {

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
	public static String createSellOrderFor(Exchange krakenExchange, BigDecimal tradeableAmount, CurrencyPair currencyPair){

		// Interested in the private trading functionality (authentication) 
		TradeService tradeService = krakenExchange.getTradeService();
		
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
	
	public Exchange getExchangeForUser(String apiKey, String privateKey, String userName) {
		Exchange krakenExchange = ExchangeFactory.INSTANCE.createExchange(KrakenExchange.class.getName());
		krakenExchange.getExchangeSpecification().setApiKey(apiKey);
		krakenExchange.getExchangeSpecification().setSecretKey(privateKey);
		krakenExchange.getExchangeSpecification().setUserName(userName);
		krakenExchange.applySpecification(krakenExchange.getExchangeSpecification());
		return krakenExchange;
	}
	
}
