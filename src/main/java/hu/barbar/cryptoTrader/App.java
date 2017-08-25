package hu.barbar.cryptoTrader;

import java.io.IOException;
import java.util.Iterator;
import java.util.Map;

import org.knowm.xchange.Exchange;
import org.knowm.xchange.ExchangeFactory;
import org.knowm.xchange.currency.Currency;
import org.knowm.xchange.currency.CurrencyPair;
import org.knowm.xchange.dto.account.AccountInfo;
import org.knowm.xchange.dto.account.Wallet;
import org.knowm.xchange.dto.marketdata.Ticker;
import org.knowm.xchange.exceptions.ExchangeException;
import org.knowm.xchange.exceptions.NotAvailableFromExchangeException;
import org.knowm.xchange.kraken.KrakenExchange;
import org.knowm.xchange.service.marketdata.MarketDataService;

import hu.barbar.confighandler.JsonConfigHandler;

public class App {

	private static final String configSourceJSONPath = "c:/kr_config.json";

	public static void main(String[] args) {

		
			Exchange krakenExchange = ExchangeFactory.INSTANCE.createExchange(KrakenExchange.class.getName());

			// Interested in the public market data feed (no authentication)
			MarketDataService marketDataService = krakenExchange.getMarketDataService();

			// Get the latest ticker data showing BTC to USD
			Ticker ticker;
			try {
				ticker = marketDataService.getTicker(CurrencyPair.BTC_USD);
				System.out.println("Ticker: " + ticker.toString());
				System.out.println("Currency: " + Currency.USD);
				System.out.println("Last: " + ticker.getLast().toString());
				//System.out.println("Volume: " + ticker.getVolume().toString());
				//System.out.println("High: " + ticker.getHigh().toString());
				//System.out.println("Low: " + ticker.getLow().toString());
				
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

			JsonConfigHandler config = new JsonConfigHandler(configSourceJSONPath);
			
			//krakenExchange = getExchangeForUser()
			krakenExchange.getExchangeSpecification().setApiKey(config.getString("api-key.appKey"));
		    krakenExchange.getExchangeSpecification().setSecretKey(config.getString("api-key.privateKey"));
		    krakenExchange.getExchangeSpecification().setUserName(config.getString("api-key.username"));
		    krakenExchange.applySpecification(krakenExchange.getExchangeSpecification());
			
			try {
				AccountInfo accountInfo = krakenExchange.getAccountService().getAccountInfo();
				System.out.println("\n\nAccount Info: " + accountInfo.toString() + "\n\n");
				Map<String, Wallet> wallets = accountInfo.getWallets();
				
				Iterator it = wallets.entrySet().iterator();
			    while (it.hasNext()) {
			        Map.Entry pair = (Map.Entry)it.next();
			        System.out.println("Wallet: " + pair.getKey() + " => " + pair.getValue());
			        it.remove(); // avoids a ConcurrentModificationException
			    }
				
				
			} catch (NotAvailableFromExchangeException e) {
				e.printStackTrace();
			} catch (ExchangeException e) {
				e.printStackTrace();
			} catch (IOException e) {
				// TODO Auto-generated catch block
				e.printStackTrace();
			}
	}
	
	private Exchange getExchangeForUser(String apiKey, String privateKey, String userName){

		    Exchange krakenExchange = ExchangeFactory.INSTANCE.createExchange(KrakenExchange.class.getName());
		    krakenExchange.getExchangeSpecification().setApiKey(apiKey);
		    krakenExchange.getExchangeSpecification().setSecretKey(privateKey);
		    krakenExchange.getExchangeSpecification().setUserName(userName);
		    krakenExchange.applySpecification(krakenExchange.getExchangeSpecification());
		    return krakenExchange;
		  
	}

}
