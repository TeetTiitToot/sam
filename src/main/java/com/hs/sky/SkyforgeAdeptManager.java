package com.hs.sky;

import java.io.FileInputStream;
import java.io.FileNotFoundException;
import java.io.FileReader;
import java.io.IOException;
import java.io.InputStreamReader;
import java.nio.charset.StandardCharsets;
import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.TimeUnit;

import org.apache.http.impl.client.HttpClientBuilder;
import org.json.simple.JSONObject;
import org.json.simple.parser.JSONParser;
import org.json.simple.parser.ParseException;
import org.openqa.selenium.By;
import org.openqa.selenium.Dimension;
import org.openqa.selenium.JavascriptExecutor;
import org.openqa.selenium.Keys;
import org.openqa.selenium.SearchContext;
import org.openqa.selenium.WebDriver;
import org.openqa.selenium.WebElement;
import org.openqa.selenium.phantomjs.PhantomJSDriver;
import org.openqa.selenium.phantomjs.PhantomJSDriverService;
import org.openqa.selenium.remote.DesiredCapabilities;
import org.openqa.selenium.support.ui.ExpectedConditions;
import org.openqa.selenium.support.ui.WebDriverWait;
import org.quartz.Job;
import org.quartz.JobDetail;
import org.quartz.JobExecutionContext;
import org.quartz.JobExecutionException;
import org.quartz.Scheduler;
import org.quartz.SchedulerException;
import org.quartz.Trigger;
import org.quartz.impl.StdSchedulerFactory;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import static org.quartz.JobBuilder.*;
import static org.quartz.TriggerBuilder.*;
import static org.quartz.SimpleScheduleBuilder.*;

public class SkyforgeAdeptManager {
	private static Logger LOGGER = LoggerFactory.getLogger(SkyforgeAdeptManager.class);
	private static JSONParser PARSER = new JSONParser();
	private static JSONObject config = null;
	private static List<Quest> quests = new ArrayList<>();

	private static By ng_binding = By.className("ng-binding");

	public static void main(String[] args) throws InterruptedException, SchedulerException {
		LOGGER.info("Starting the SAM");
		Scheduler scheduler = StdSchedulerFactory.getDefaultScheduler();
		scheduler.start();
		LOGGER.info("New scheduler de tasks, se relance toutes les 10 minutes.");
		// TODO à revoir pour se relancer au bon moment,et pas toutes les 10 minutes.
		JobDetail job = newJob(ManageAdeptsJob.class).withIdentity("fullManagementJob").build();
		LOGGER.debug("Job defined");
		Trigger trigger = newTrigger()//
				.withIdentity("everyTenMinutes")//
				.startNow()//
				.withSchedule(simpleSchedule()//
						.withIntervalInMinutes(10)//
						.repeatForever())
				.build();
		LOGGER.debug("Trigger defined");
		scheduler.scheduleJob(job, trigger);
		LOGGER.debug("job scheduled");
				
	}

	public static void main2(String[] args) throws InterruptedException, IOException {
		LOGGER.info("Starting SAM : lecture du fichier sam.config");
		LOGGER.debug("lecture du fichier sam.config toutes les dix minutes?");
		config = readConfig();
		if (config == null) {
			LOGGER.error("No configuration found, aborting");
			return;
		}
		// LOGGER.debug(config.toJSONString());
		quests = Quest.loadQuests(config);
		// System.setProperty("operaProperty FIXME!", "path/to/operaDriver");
		// OperaDriverService service = OperaDriverService.createDefaultService();
		// LOGGER.info("Creating default Opera Service"); // TODO : utilsier le
														// browser sans
														// affichage
		// WebDriver driver = new OperaDriver(service);
		DesiredCapabilities caps = new DesiredCapabilities();
		caps.setCapability(PhantomJSDriverService.PHANTOMJS_EXECUTABLE_PATH_PROPERTY, config.get("PhantomJsExecutablePathProperty"));
		// FIXME pas sauvage de couper tous les logs de phantomJ
		String[] phantomArgs = new  String[] {
			    "--webdriver-loglevel=NONE"
			};
		caps.setCapability(PhantomJSDriverService.PHANTOMJS_CLI_ARGS, phantomArgs);
		WebDriver driver = new PhantomJSDriver(caps);
		driver.manage().timeouts().implicitlyWait(2, TimeUnit.SECONDS); // FIXME mix de implicit et explicit wait peut planter!
		try {
			login(driver);
			hideChat(driver);
			acceptQuests(driver);
			startQuests(driver);
		} finally {
			// set-done
			LOGGER.info("Closing");
			driver.close();
			driver.quit();
			// service.stop();
			LOGGER.info("Closed");
		}
	}

	private static void hideChat(WebDriver driver) throws InterruptedException {
		WebElement chat = driver.findElement(By.className("wdg-r"));
		jsClick(driver, chat);
		Thread.sleep(500l);
	}

	private static void startQuests(WebDriver driver) {
		if (quests.isEmpty()) {
			LOGGER.warn("No quests to start found! Check config file.");
			return;
		}
		quests.sort(Quest.PRIORITY_COMP);
		// long currentPriority = quests.get(0).getPriority();
		/**
		 * TODO Clicker sur la quete. </br>
		 * Ajouter les adeptes jusqu'a atteindre le successRate. </br>
		 * => clicker sur l'ajout d'un adepte </br>
		 * => sélectionner les adeptes en fonction des types demandés par la
		 * quete </br>
		 * Envoyer les adeptes. </br>
		 * TODO : voir quand arrêter : changement de priorité? trop proche de la
		 * prochaine session de quête?
		 */
		for (Quest quest : quests) {
			startQuest(quest, driver);
		}
	}

	private static void startQuest(Quest quest, WebDriver driver) {
		LOGGER.info("Trying to start : {}", quest.toString());
		// toutes les quetes
		WebElement questListHolder = driver.findElement(By.className("quest-list-l"));
		// chacune des quetes (en cours + en attente)
		List<WebElement> quests = questListHolder.findElements(By.className("card_region"));
		for (WebElement el : quests) {
			try {
				By ubox_title = By.className("ubox-title");

				String text = el.findElement(ubox_title).findElement(ng_binding).getText();
				// Le bon nom
				if (!text.equals(quest.getName())) {
					continue;
				}
				LOGGER.debug("Quete dispo :  {}", text);
				// TODO : pas en cours !
				// By enAttente = By.cssSelector(selector)
				// si en cours : quest-i set-pro set-rare quest-story
				By svgTag = By.tagName("svg");
				if (isElementPresent(svgTag, el.findElement(By.className("ipic_quest")))) {
					LOGGER.debug("Quete en cours");
					continue;
				}
				LOGGER.debug("Quete en attente");
				// sinon en attente : quest-i set-rare quest-story
				By byQuestName = By.xpath(".//*[text()=\"" + quest.getName() + "\"]");
				WebElement arrr = el.findElement(byQuestName);
				// click sur la quete
				jsClick(driver, arrr);
				Thread.sleep(500l);
				addAdepts(quest, driver);
				// TODO Envoyer
			} catch (Exception e) {
				LOGGER.error("Impossible de lancer :  {}, e : {}", quest.getName(), e.getMessage());
			}
		}
	}

	private static JavascriptExecutor jsClick(WebDriver driver, WebElement we) {
		JavascriptExecutor executor = (JavascriptExecutor) driver;
		executor.executeScript("arguments[0].click();", we);
		return executor;
	}

	private static void addAdepts(Quest quest, WebDriver driver) throws InterruptedException {
		// parent : craft-box set-shadow
		WebElement questParty = driver.findElement(By.className("quest-list-party"));
		List<WebElement> crosses = questParty.findElements(By.className("set-col-"));
		while (crosses.size() > 0) {

			WebElement cross = crosses.get(0);
			LOGGER.debug("next adept");
			jsClick(driver, cross);
			Thread.sleep(500l);
			// affiche bien le sélecteur d'adepte
			// TODO trouver les bons adeptes dans l'ordre
			// à chaque ajout vérifier le successRate, si valide, on arrête
			//
			// liste : craft-popup-inner flex-cards antiscroll-inner
			WebElement popupInner = questParty.findElement(By.className("craft-popup-inner"));
			// card
			// ubox
			// ubox-txt
			// ubox-name // <p class="ubox-name ng-binding">Prêcheur
			// doué</p>
			/*
			 * List<WebElement> cards = popupInner .findElements(By.
			 * cssSelector("div[class='card-wrap ng-scope']"));
			 */
			// FIXME : on prend en compte des éléments cachés!
			List<WebElement> cards = popupInner.findElements(By.xpath("//div[@class='card-wrap ng-scope']"));
			WebElement card = null;
			if (quest.getAdepts().isEmpty()) {
				card = findCard(cards, "");
			} else {
				for (String adeptType : quest.getAdepts()) {
					// TODO une fois qu un adept n est plus dispo, ne pas passer
					// son temps à le rechercher!
					card = findCard(cards, adeptType);
					if (card != null) {
						break;
					}
				}
			}
			if (card == null) {
				LOGGER.info("No more adepts");
				return;
			}
			LOGGER.debug("clicking on card");
			// buildClickPerform(card, driver, 2, 2);
			jsClick(driver, card);
			Thread.sleep(500l);
			WebElement secTitle = driver.findElement(By.className("craft-sec-title"));
			By scoreBy = By.cssSelector("div[class*='ng-binding ng-scope']");
			if (!isElementPresent(scoreBy, secTitle)) {
				// ng-scope => 100%!
				scoreBy = By.cssSelector("div[class*='ng-scope']");
			}
			WebElement score = secTitle.findElement(scoreBy);
			String currentRate = score.getText();
			long currentScore = Long.parseLong(currentRate.substring(0, currentRate.length() - 1));
			LOGGER.debug("current rate : {}", currentRate + " , text length : " + currentRate.length()
					+ " parsedScore : " + currentScore + " aim : " + quest.getSuccessRate());
			if (currentScore >= quest.getSuccessRate()) {
				By sendAdeptsLinkText = By.linkText((String) config.get("SendAdepts"));
				WebElement sendBtn = driver.findElement(sendAdeptsLinkText);
				LOGGER.debug("Click on Send");
				jsClick(driver, sendBtn);
				break;
			}
			// Envoyer

			// TODO => cas dynamique de sélection?
			// desc : quest-list-party-descr ng-scope => Recommandé

			crosses = questParty.findElements(By.className("set-col-"));
		}

	}

	private static WebElement findCard(List<WebElement> cards, String adeptType) {
		for (WebElement card : cards) {
			if (!card.isDisplayed()) {
				continue;
			}
			/*
			 * LOGGER.debug("card : displayed : " + card.isDisplayed() +
			 * " , outerHTML : " + card.getAttribute("outerHTML") +
			 * " , innerHTML : " + card.getAttribute("innerHTML"));
			 */
			WebElement text = card.findElement(By.cssSelector("p.ubox-name.ng-binding")); // (".//p[@class='ubox-name
			if (text.getText().length() > 0 && ("".equals(adeptType) || text.getText().startsWith(adeptType))) {
				LOGGER.debug("Match!");
				return text;
			}
		}
		return null;
	}

	public static boolean isElementPresent(By locatorKey, SearchContext sc) {
		try {
			sc.findElement(locatorKey);
			return true;
		} catch (org.openqa.selenium.NoSuchElementException e) {
			return false;
		}
	}

	private static void acceptQuests(WebDriver driver) throws InterruptedException {
		LOGGER.debug("AcceptingQuests");
		try {
			WebElement questList = driver.findElement(By.className("quest-list-l"));
			List<WebElement> finishedQuests = questList.findElements(By.className("set-fail"));
			while (finishedQuests.size() > 0) {
				accept(finishedQuests.get(0), driver);
				Thread.sleep(500l);
				questList = driver.findElement(By.className("quest-list-l"));
				finishedQuests = questList.findElements(By.className("set-fail"));
			}
			finishedQuests = questList.findElements(By.className("set-done"));
			while (finishedQuests.size() > 0) {
				accept(finishedQuests.get(0), driver);
				Thread.sleep(500l);
				questList = driver.findElement(By.className("quest-list-l"));
				finishedQuests = questList.findElements(By.className("set-done"));
			}
			LOGGER.debug("Quests accepted");
		} catch(Exception e){
			LOGGER.error("--------------------");
			LOGGER.error(e.getMessage());
			LOGGER.error("Could not accept some quests!?! still carry on to try to send adepts!");
			LOGGER.error("--------------------");
		}
	}

	private static void login(WebDriver driver) throws InterruptedException {
		String startURL = (String) config.get("StartURL");
		String login = (String) config.get("Login");
		LOGGER.debug("Get on '{}'", startURL);
		driver.get(startURL);
		Thread.sleep(100l);
		driver.manage().window().setSize(new Dimension(1920, 1080));
		// driver.manage().window().maximize();
		Thread.sleep(500l);
//		WebElement preLogin = driver.findElement(By.className("wdg-list"));
//		preLogin.click();
		((JavascriptExecutor)driver).executeScript("jQuery('#loginPopup').show();");
		WebElement loginField = (new WebDriverWait(driver, 5))
		.until(ExpectedConditions.presenceOfElementLocated(By.id("login")));
		// loginField.click();
		loginField.sendKeys(login);
		LOGGER.debug("Login as '{}'", login);
		WebElement pass = driver.findElement(By.id("password"));
		pass.sendKeys((String) config.get("Password"));
		pass.sendKeys(Keys.RETURN);
		Thread.sleep(500l);
		LOGGER.debug("Get on '{}'", startURL);
		driver.get(startURL);
		Thread.sleep(500l);
	}

	private static JSONObject readConfig() {
		try {
			Object config = PARSER.parse(new InputStreamReader(new FileInputStream("sam.config"), StandardCharsets.UTF_8));//   new FileReader( "sam.config"));
			return (JSONObject) config;
		} catch (FileNotFoundException e1) {
			LOGGER.error("Configuration file not found : {}", "sam.config");
		} catch (IOException e1) {
			LOGGER.error("Error reading file  : IO : {}", "sam.config");
		} catch (ParseException e1) {
			LOGGER.error("Error parsing file : {}", "sam.config");
			LOGGER.error(e1.getLocalizedMessage());
		}
		return null;
	}

	private static void accept(WebElement questDone, WebDriver driver) throws InterruptedException {
		LOGGER.debug("Click on quest");
		jsClick(driver, questDone);
		// buildClickPerform(questDone, driver, 400, 25);
		// trouver le "accepter" et clicker.
		// By b = By.cssSelector("div.field-summary-btn.ng-isolate-scope");
		By acceptLinkText = By.linkText((String) config.get("AcceptQuest"));
		// By.className("field-summary-btn")
		// selenium.click("css=div.field-summary-btn.ng-isolate-scope");
		// WebElement acceptBtn = driver.findElement(acceptLinkText);
		WebElement acceptBtn = (new WebDriverWait(driver, 30))
				.until(ExpectedConditions.presenceOfElementLocated(acceptLinkText));
		LOGGER.debug("Click on Accept");
		jsClick(driver, acceptBtn);
		Thread.sleep(500l);
	}
	
	
	
	public static class ManageAdeptsJob implements Job{

		@Override
		public void execute(JobExecutionContext arg0) throws JobExecutionException {
			try {
				SkyforgeAdeptManager.main2(null);
			} catch (InterruptedException | IOException e) {
				LOGGER.error(e.getLocalizedMessage());
			}
			
		}
		
	}

}

/*
 * private static void buildClickPerform(WebElement questDone, WebDriver driver,
 * int xoffset, int yoffset) { Actions builder = new Actions(driver);
 * builder.moveToElement(questDone, xoffset, yoffset).click().build().perform();
 * }
 *
 * OLD //TODO cherche les missions à faire Thread.sleep(5000l); List<WebElement>
 * todoQ = questList.findElements(By.className("card_region")); for(WebElement
 * el : todoQ){ try { By bb =
 * By.xpath(".//*[text()=\"Tous les ordres ne sont pas bons\"]"); By ubox_title
 * = By.className("ubox-title"); By ng_binding = By.className("ng-binding");
 * String text = el.findElement(ubox_title).findElement(ng_binding).getText();
 * System.out.println(text); //
 * By.xpath("//*[contains(text(), 'Tous les ordres ne sont pas bons')]")
 * WebElement arrr = el.findElement(bb); LOGGER.info("Quete dispo :  {}",
 * arrr.toString());
 * 
 * JavascriptExecutor executor = (JavascriptExecutor) driver;
 * executor.executeScript("arguments[0].click();", arrr); Thread.sleep(10000l);
 * 
 * } catch(Exception e){
 * 
 * 
 * } }
 */
