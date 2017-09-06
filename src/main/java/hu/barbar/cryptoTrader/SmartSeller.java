package hu.barbar.cryptoTrader;

import java.io.IOException;
import java.io.Serializable;
import java.math.BigDecimal;
import java.net.SocketTimeoutException;
import java.util.Date;

import org.knowm.xchange.currency.CurrencyPair;
import org.knowm.xchange.exceptions.ExchangeException;
import org.knowm.xchange.exceptions.NotAvailableFromExchangeException;
import org.knowm.xchange.exceptions.NotYetImplementedForExchangeException;
import org.knowm.xchange.service.marketdata.MarketDataService;

import hu.barbar.retryHandler.RetryHandler;
import hu.barbar.retryHandler.RetryHandler.ResultAfterMultipleRetries;
import hu.barbar.retryHandler.util.RetryParams;

public abstract class SmartSeller implements Serializable {

	/**
	 * 
	 */
	private static final long serialVersionUID = 4650305798661637668L;

	private boolean debug_mode = false;
	

	private MarketDataService marketDataService = null;
	
	private static long nextId = 0; 
	
	private long id = -1;
	
	private BigDecimal stopPrice = null;
	
	private BigDecimal sellMargin = null;
	
	private BigDecimal amount = null;
	
	private String currencyStr = null;
	
	private CurrencyPair usedCurrencyPair = null;
	
	private boolean initialized = false;
	
	private boolean done = false;
	
	private RetryParams retryParams = null;
	
	
	public abstract class SellingThread extends Thread {
		
		private BigDecimal amount = null;
		
		private CurrencyPair currencyPair = null;
		
		RetryParams retryParams = null;
		
		/**
		 * This method will be called when selling order has been submitted
		 * <or> when the creating selling order has failed
		 * @param orderId contains the id of submitted order
		 * <br> or null if order could not be submitted.
		 */
		abstract void onSellingDone(ResultAfterMultipleRetries ram);
		
		public SellingThread(BigDecimal amount, CurrencyPair cp, RetryParams retryParams){
			this.amount = amount.add(BigDecimal.ZERO);
			this.currencyPair = cp;
			this.retryParams = retryParams;
		}
		
		@Override
		public void run() {
			
			// Create selling order
			//String sellOrderId = ExchangeFuntions.createSellOrderFor(this.amount, this.currencyPair);
			
			String sellOrderId = null;
			ResultAfterMultipleRetries ram = new RetryHandler(this.retryParams){

				@Override
				public Object doProblematicJob() throws SocketTimeoutException, NotAvailableFromExchangeException,
						ExchangeException, NotYetImplementedForExchangeException, IOException, Exception {
					
					return ExchangeFuntions.createSellOrderFor(SellingThread.this.amount, SellingThread.this.currencyPair);
					
				}
				
				@Override
				public void onTryFails(int currentRetryCount, Exception e) {
					
				}
				
			}.run();
			
			if(ram.isDoneSuccessfully()){
				// Changed to give ResultAfterMultipleRetries object as parameter of onSellingDone method.
				//sellOrderId = ram.getResultString();
			}else{
				System.out.println(ram.getStackTraces());
				sellOrderId = null;
				//TODO: log exceptions from ram object..
			}
			
			this.onSellingDone(ram);
			
			super.run();
		}
		
	}
	
	
	public SmartSeller(BigDecimal amount, String currencyNameShort, BigDecimal initialStopPrice, MarketDataService marketDataService, RetryParams retryParams){
		
		this.id = generateNextId();
		
		this.amount = amount.add(BigDecimal.ZERO);
		
		this.stopPrice = initialStopPrice.add(BigDecimal.ZERO);
		
		this.currencyStr = currencyNameShort;
		
		this.marketDataService = marketDataService;
		
		this.retryParams = retryParams;
		
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
		
		
		BigDecimal currentlyAvailableBalance = ExchangeFuntions.getAvailableBalanceFor(this.currencyStr);
		//TODO drop warning if available balance is lower then amount
		
		sellMargin = currentPrice.subtract(stopPrice);
		
		// Show parameters
		log(this.id, "Initial price:      \t" + currentPrice + " USD / " + this.currencyStr);
		log(this.id, "Initial stop limit: \t" + stopPrice + " USD / " + this.currencyStr);
		log(this.id, "Sell margin:        \t" + sellMargin + " USD");
		log(this.id, "Amount:             \t" + (lessThen(this.amount,0)?"All available":this.amount) + " " + this.currencyStr);
		log(this.id, "Available balance:  \t" + currentlyAvailableBalance + " " + this.currencyStr);
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
		this.storeValues(new Date(), this.getId(), this.currencyStr, currentPrice, this.stopPrice, diff);
		
		this.showValues(new Date(), currentPrice, this.stopPrice, diff);
		
		// Check if stopPrice is bigger then currentPrice >> need to sell..
		if(stopPrice.compareTo(currentPrice) > 0){
			log(this.id, "\nPrice is lower then stop price!\nNeed to sell!!!");
			
			String sellOrderId = null;
			if(!debug_mode){
				
				// Create selling order
				// function has been moved to a separated thread
				
				SellingThread st = new SellingThread(this.amount, this.usedCurrencyPair, this.retryParams) {
					@Override
					void onSellingDone(ResultAfterMultipleRetries ram) {
						
						if(ram.isDoneSuccessfully()){
							String orderId = ram.getResultString();
							log(SmartSeller.this.id, "Sell order submitted: " + orderId);
							SmartSeller.this.onSellOrderSubmitted(orderId);
						}else{
							SmartSeller.this.onSellOrderSubmitFailed();
						}
						
					}
				};
				st.start();
				
				/**/
				
				//sellOrderId = ExchangeFuntions.createSellOrderFor(this.amount, this.usedCurrencyPair);
			}else{
				sellOrderId = "Debug mode enabled, selling is mocked.";
			}
			this.done = true;
			log(this.id, "Sell order submitted: " + sellOrderId);
			this.onSellOrderSubmitted(sellOrderId);
		}
		
	}
	
	
	public abstract void onSellOrderSubmitFailed();

	public abstract void onSellOrderSubmitted(String sellOrderId);

	public abstract void log(long smartSellerId, String line);
	
	public abstract void storeValues(Date date, long smartSellerId, String currencyStr, BigDecimal currentPrice, BigDecimal stopPrice2, BigDecimal diff);

	public abstract void showValues(Date date, BigDecimal currentPrice, BigDecimal stopPrice2, BigDecimal diff);
	
	
	/**
	 * Get unique Id of SmartSeller object
	 * @return unique Id of SmartSeller object
	 */
	public long getId(){
		return this.id;
	}
	
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
