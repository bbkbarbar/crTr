package hu.barbar.cryptoTrader;

import java.io.Serializable;
import java.math.BigDecimal;
import java.util.Date;

import org.knowm.xchange.currency.CurrencyPair;
import org.knowm.xchange.service.marketdata.MarketDataService;

import hu.barbar.util.FileHandler;

public abstract class SmartSeller implements Serializable {

	/**
	 * 
	 */
	private static final long serialVersionUID = 4650305798661637668L;


	private MarketDataService marketDataService = null;
	
	private static long nextId = 0; 
	
	private long id = -1;
	
	private BigDecimal stopPrice = null;
	
	private BigDecimal sellMargin = null;
	
	private BigDecimal amount = null;
	
	private String currency = null;
	
	private CurrencyPair usedCurrencyPair = null;
	
	private boolean initialized = false;
	
	private boolean done = false;
	
	
	public SmartSeller(BigDecimal amount, String currencyNameShort, BigDecimal initialStopPrice, MarketDataService marketDataService){
		
		this.id = generateNextId();
		
		this.amount = amount;
		
		this.currency = currencyNameShort;
		
		this.marketDataService = marketDataService;
		
		usedCurrencyPair = ExchangeFuntions.getUSDBasedCurrencyPairFor(currencyNameShort);
		
		BigDecimal currentPrice = null;
		
		// Get initialPrice from Kraken.
		long before, after;
		before = System.currentTimeMillis();
		currentPrice = ExchangeFuntions.getPriceOf(marketDataService, usedCurrencyPair);
		after = System.currentTimeMillis();
		//TODO: log, sysout
		log(this.id, "Elasped time for get current price: " + (after - before) + " ms\n");
		if(currentPrice == null){
			this.initialized = false;
			return;
		}
		
		
		BigDecimal currentlyAvailableBalance = ExchangeFuntions.getAvailableBalanceFor(this.currency);
		//TODO drop warning if available balance is lower then amount
		
		sellMargin = currentPrice.subtract(stopPrice);
		
		// Show parameters
		log(this.id, "Initial price:      \t" + currentPrice + " USD / " + this.currency);
		log(this.id, "Initial stop limit: \t" + stopPrice + " USD / " + this.currency);
		log(this.id, "Sell margin:        \t" + sellMargin + " USD");
		log(this.id, "Amount:             \t" + (lessThen(this.amount,0)?"All available":this.amount) + " " + this.currency);
		log(this.id, "Available balance:  \t" + currentlyAvailableBalance + " " + this.currency);
		log(this.id, "Minimum income:     \t" + ((lessThen(this.amount,0)?currentlyAvailableBalance:this.amount).multiply(this.stopPrice)).setScale(2, BigDecimal.ROUND_HALF_UP) + " USD");
		log(this.id, "Current value:      \t" + ((lessThen(this.amount,0)?currentlyAvailableBalance:this.amount).multiply(currentPrice)).setScale(2, BigDecimal.ROUND_HALF_UP) + " USD");
		log(this.id, "Maximum loss:       \t" + ((lessThen(this.amount,0)?currentlyAvailableBalance:this.amount).multiply(currentPrice).subtract((lessThen(this.amount,0)?currentlyAvailableBalance:this.amount).multiply(this.stopPrice)) ).setScale(2, BigDecimal.ROUND_HALF_UP) + " USD");
		log(this.id, "\n");
		
		
		// Check parameters and exit if invalid
		if(stopPrice.compareTo(currentPrice) >= 0){
			//TODO log
			log(this.id, "ERROR: Inital stop price is NOT lower then initial price!");
			//TODO: create an "instant sell" option for this case..
			this.initialized = false;
			return;
		}
		
	}
	
	/**
	 * This function will be called repeatedly scheduled from outside, to check the current conditions <br>
	 * and decide that should we sell the specified amount or not..
	 */
	public void checkSellingConditions(){
		
		// Update current price
		BigDecimal currentPrice = ExchangeFuntions.getPriceOf(marketDataService, usedCurrencyPair);
		
		// Price increasing >> need to increase stopPrice
		if(currentPrice.compareTo( (this.stopPrice.add(this.sellMargin)) ) > 0){
			this.stopPrice = currentPrice.subtract(this.sellMargin);
		}
		
		
		BigDecimal diff = currentPrice.subtract(stopPrice);
		this.storeValues(new Date(), currentPrice, this.stopPrice, diff);
		
		// Check if stopPrice is bigger then currentPrice >> need to sell..
		if(stopPrice.compareTo(currentPrice) > 0){
			log(this.id, "\nPrice is lower then stop price!\nNeed to sell!!!");
			
			String sellOrderId = ExchangeFuntions.createSellOrderFor(this.amount, this.usedCurrencyPair);
			log(this.id, "Sell order submitted: " + sellOrderId);
			this.done = true;
			this.onSellOrderSubmitted(sellOrderId);
		}
		
	}
	
	
	public abstract void onSellOrderSubmitted(String sellOrderId);
	

	/**
	 * Get unique Id of SmartSeller object
	 * @return unique Id of SmartSeller object
	 */
	public long getId(){
		return this.id;
	}
	
	public abstract void log(long smartSellerId, String line);
	
	public abstract void storeValues(Date date, BigDecimal currentPrice, BigDecimal stopPrice2, BigDecimal diff);

	private static long generateNextId(){
		long nextId = SmartSeller.nextId;
		SmartSeller.nextId++;
		return nextId;
	}
	
	
	private boolean lessThen(BigDecimal value, int compareValue){
		return value.compareTo(new BigDecimal(compareValue)) < 0;
	}
	
	
	public boolean isInitialized(){
		return this.initialized;
	}
	
}
