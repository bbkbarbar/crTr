package hu.barbar.cryptoTrader;

import java.io.IOException;
import java.math.BigDecimal;
import java.util.Date;
import java.util.Iterator;
import java.util.Map;
import java.util.Set;

import org.knowm.xchange.Exchange;
import org.knowm.xchange.ExchangeFactory;
import org.knowm.xchange.currency.Currency;
import org.knowm.xchange.currency.CurrencyPair;
import org.knowm.xchange.dto.account.FundingRecord;
import org.knowm.xchange.dto.marketdata.Ticker;
import org.knowm.xchange.dto.trade.OpenOrders;
import org.knowm.xchange.exceptions.ExchangeException;
import org.knowm.xchange.exceptions.NotAvailableFromExchangeException;
import org.knowm.xchange.exceptions.NotYetImplementedForExchangeException;
import org.knowm.xchange.kraken.KrakenExchange;
import org.knowm.xchange.kraken.dto.trade.KrakenOpenPosition;
import org.knowm.xchange.kraken.dto.trade.KrakenOrderFlags;
import org.knowm.xchange.kraken.service.KrakenTradeServiceRaw;
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
	
	private BigDecimal initialStopPrice = null;
	
	
	
	public SmartSeller(String[] args){
		
		System.out.println("Args size: " + args.length);
		if(args.length < EXPECTED_ARGUMENT_COUNT){
			System.out.println("Too few arguments.\nExample useage:"
					+ "SmartSeller.jar "
					+ "0.1 ETH 340.17"
					+ "");
			return;
		}
		
		amount = new BigDecimal(args[0].replaceAll(",", ""));
		currency = args[1];
		initialStopPrice = new BigDecimal(args[2].replaceAll(",", ""));
		
		
		System.out.println("Amount: " + amount + " " + currency);
	}
	
	// 0.01 ETH
	public static void main(String[] args) {
		
		me = new SmartSeller(args);
		
		/*
		
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

		JsonConfigHandler config = new JsonConfigHandler(configSourceJSONPath);

		// krakenExchange = getExchangeForUser()
		krakenExchange.getExchangeSpecification().setApiKey(config.getString("api-key.appKey"));
		krakenExchange.getExchangeSpecification().setSecretKey(config.getString("api-key.privateKey"));
		krakenExchange.getExchangeSpecification().setUserName(config.getString("api-key.username"));
		krakenExchange.applySpecification(krakenExchange.getExchangeSpecification());

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
