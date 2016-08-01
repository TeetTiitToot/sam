package com.hs.sky;

import org.openqa.selenium.By;
import org.openqa.selenium.JavascriptExecutor;
import org.openqa.selenium.SearchContext;
import org.openqa.selenium.WebDriver;
import org.openqa.selenium.WebElement;

public final class SeleniumHelper {

	private SeleniumHelper(){
		// Helper class
	}
		
	public static JavascriptExecutor jsClick(WebDriver driver, WebElement we) {
		JavascriptExecutor executor = (JavascriptExecutor) driver;
		executor.executeScript("arguments[0].click();", we);
		return executor;
	}
	
	public static boolean isElementPresent(By locatorKey, SearchContext sc) {
		try {
			sc.findElement(locatorKey);
			return true;
		} catch (org.openqa.selenium.NoSuchElementException e) {
			return false;
		}
	}
	
	/*
	 * private static void buildClickPerform(WebElement questDone, WebDriver driver,
	 * int xoffset, int yoffset) {
	 * Actions builder = new Actions(driver);
	 * builder.moveToElement(questDone, xoffset, yoffset).click().build().perform();
	 * }
	 */
}
