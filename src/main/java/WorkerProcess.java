import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.security.Security;
import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.Date;
import java.util.HashMap;
import java.util.List;
import java.util.Properties;
import java.util.logging.Level;
import java.util.logging.Logger;

import javax.mail.Message;
import javax.mail.MessagingException;
import javax.mail.Session;
import javax.mail.internet.AddressException;
import javax.mail.internet.InternetAddress;
import javax.mail.internet.MimeMessage;

import org.json.JSONArray;
import org.json.JSONObject;
import org.kohsuke.github.GHContent;
import org.kohsuke.github.GHRef;
import org.kohsuke.github.GHRepository;
import org.kohsuke.github.GitHub;
import org.openqa.selenium.By;
import org.openqa.selenium.NoSuchElementException;
import org.openqa.selenium.WebElement;
import org.openqa.selenium.htmlunit.HtmlUnitDriver;
import org.openqa.selenium.support.ui.Select;

import com.sun.mail.smtp.SMTPTransport;

public class WorkerProcess {
	public static void main(String[] args) {

		while (true) {
			
			String user = System.getenv("GMAIL_USERNAME");
			String pass = System.getenv("GMAIL_PASSWORD");
			try {
				Send(user, pass, "wallyatkins@gmail.com", "wally@wallyatkins.com", "test1", "test2");
			} catch (MessagingException e1) {
				e1.printStackTrace();
			}

			System.out.println("\n\n ===> Your Java Program has just sent an Email successfully. Check your email..");

			/*
			// Hush the CSS and JavaScript error and warning logs
			Logger.getLogger("com.gargoylesoftware").setLevel(Level.OFF);

			Logger.getAnonymousLogger().log(Level.INFO, "[Opening] YCSD Aspen");

			// Create the driver
			HtmlUnitDriver unitDriver = new HtmlUnitDriver(true);
			unitDriver.get("https://yorksis.ycsd.york.va.us/aspen/logon.do");

			Logger.getAnonymousLogger().log(Level.INFO, "Logging In");

			// Enter the username and password to login
			WebElement username = unitDriver.findElementById("username");
			username.sendKeys(System.getenv("ASPEN_USERNAME"));
			WebElement password = unitDriver.findElementById("password");
			password.sendKeys(System.getenv("ASPEN_PASSWORD"));
			WebElement login = unitDriver.findElementById("logonButton");
			login.click();

			Logger.getAnonymousLogger().log(Level.INFO, "[Navigating] to the 'Academics' page");

			// Navigate to the academics section
			WebElement academics = unitDriver.findElementByLinkText("Academics");
			academics.click();

			Logger.getAnonymousLogger().log(Level.INFO, "Gathering the students and classes.");

			// Hold the list of students
			ArrayList<String> students = new ArrayList<String>();

			// Hold the list of each student's classes
			HashMap<String, List<String>> classes = new HashMap<String, List<String>>();

			JSONArray assignments = new JSONArray();

			// Populate the list of students from the drop down
			WebElement selectStudent = unitDriver.findElementByName("selectedStudentOid");
			Select select = new Select(selectStudent);
			for (WebElement option : select.getOptions()) {
				students.add(option.getText());
			}

			// Populate the list of classes for each student by navigating to each student's
			// page
			for (String name : students) {
				WebElement newSelectStudent = unitDriver.findElementByName("selectedStudentOid");
				Select newSelect = new Select(newSelectStudent);
				newSelect.selectByVisibleText(name);
				List<String> classList = new ArrayList<String>();
				List<WebElement> classAnchors = unitDriver.findElementsByCssSelector(".pointer a");
				for (WebElement anchor : classAnchors) {
					switch (anchor.getText()) {
					case "Details":
					case "Assignments":
						break;
					default:
						classList.add(anchor.getText());
					}
				}
				classes.put(name, classList);
			}

			// Step through each student and their classes
			for (String name : students) {
				Logger.getAnonymousLogger().log(Level.INFO, "[Navigating] back to 'Academics' page");
				unitDriver.findElementByLinkText("Academics").click();
				Logger.getAnonymousLogger().log(Level.INFO, "[Selecting] student: " + name);
				new Select(unitDriver.findElementByName("selectedStudentOid")).selectByVisibleText(name);

				for (String className : classes.get(name)) {
					Logger.getAnonymousLogger().log(Level.INFO, "[Navigating] to class: " + className);
					unitDriver.findElementByLinkText(className).click();
					unitDriver.findElementByLinkText("Assignments").click();
					new Select(unitDriver.findElementByName("gradeTermOid")).selectByIndex(0);

					// Grab the assignment name, date assigned, date due, term, category, score
					int i = 0;
					for (WebElement element : unitDriver.findElementsByCssSelector(".listCell")) {
						JSONObject assignment = new JSONObject();
						assignment.put("student", name);
						assignment.put("student_class", className);

						for (WebElement td : element.findElements(By.tagName("td"))) {
							switch (i) {
							case 1:
								assignment.put("assignment", td.getText());
								break;
							case 2:
								assignment.put("date_assigned", td.getText());
								break;
							case 3:
								assignment.put("date_due", td.getText());
								break;
							case 4:
								assignment.put("quarter", td.getText());
								break;
							case 5:
								assignment.put("category", td.getText());
								break;
							}
							i++;
						}
						i = 0;

						// Using an exception to find 'ungraded' assignments
						WebElement grade = null;
						try {
							grade = element.findElement(By.cssSelector((".percentFieldInlineLabel")));
							assignment.put("score", grade.getText());
						} catch (NoSuchElementException ex) {
							assignment.put("score", "Ungraded");
						}

						// Only store it if there was an actual assignment
						if (assignment.has("assignment")) {
							assignments.put(assignment);
						}
					}

					Logger.getAnonymousLogger().log(Level.INFO, "[Navigating] back to the 'Classes' page");
					unitDriver.findElementByLinkText("Classes").click();
				}
			}

			try {
				Logger.getAnonymousLogger().log(Level.INFO, "[GitHub] accessing git repo");

				// Login to GitHub and grab the right repository
				GitHub github = GitHub.connectUsingPassword(System.getenv("GITHUB_USERNAME"),
						System.getenv("GITHUB_PASSWORD"));
				GHRepository ghRepo = github.getRepository(System.getenv("GITHUB_REPOSITORY"));

				// Get a reference to the master
				GHRef masterRef = ghRepo.getRef("heads/master");
				String masterTreeSha = ghRepo.getTreeRecursive("master", 1).getSha();

				// Get the file that holds the content
				GHContent content = ghRepo.getFileContent("/2018-2019.json");

				// Get the JSON text of the GitHub assignments and the Aspen assignments
				String gitHubAssignments = getStringFromInputStream(content.read());
				String aspenAssignments = assignments.toString(2);

				// Check if the assignments are identical (no changes)
				if (!gitHubAssignments.equals(aspenAssignments)) {
					String treeSha = ghRepo.createTree().baseTree(masterTreeSha)
							.textEntry(content.getPath(), aspenAssignments, false).create().getSha();
					String commitSha = ghRepo.createCommit()
							.message(
									"aspen updated - " + new SimpleDateFormat("yyyy-MM-dd HH:mm:ss").format(new Date()))
							.tree(treeSha).parent(masterRef.getObject().getSha()).create().getSHA1();
					masterRef.updateTo(commitSha);
					Logger.getAnonymousLogger().log(Level.INFO, "[GitHub] updated git repo");
				}
			} catch (IOException ex) {
				Logger.getAnonymousLogger().log(Level.WARNING, "[GitHub] Exception Occurred");
				ex.printStackTrace();
			}
			*/
			
			// Run the full process then sleep for a bit
			try {
				Thread.sleep(3600000); // sleep for six hours
			} catch (InterruptedException e) {
				Logger.getAnonymousLogger().log(Level.WARNING, "[Thread] Interrupted");
			}
		}
	}

	private static String getStringFromInputStream(InputStream is) {
		BufferedReader br = null;
		StringBuilder sb = new StringBuilder();

		String line;
		try {
			br = new BufferedReader(new InputStreamReader(is));
			while ((line = br.readLine()) != null) {
				sb.append(line);
			}
		} catch (IOException e) {
			e.printStackTrace();
		} finally {
			if (br != null) {
				try {
					br.close();
				} catch (IOException e) {
					e.printStackTrace();
				}
			}
		}

		return sb.toString();
	}
	
	/**
	 * Send email using GMail SMTP server.
	 *
	 * @param username       GMail username
	 * @param password       GMail password
	 * @param recipientEmail TO recipient
	 * @param ccEmail        CC recipient. Can be empty if there is no CC recipient
	 * @param title          title of the message
	 * @param message        message to be sent
	 * @throws AddressException   if the email address parse failed
	 * @throws MessagingException if the connection is dead or not in the connected
	 *                            state or if the message is not a MimeMessage
	 */
	public static void Send(final String username, final String password, String recipientEmail, String ccEmail,
			String title, String message) throws AddressException, MessagingException {
		Security.addProvider(new com.sun.net.ssl.internal.ssl.Provider());
		final String SSL_FACTORY = "javax.net.ssl.SSLSocketFactory";

		// Get a Properties object
		Properties props = System.getProperties();
		props.setProperty("mail.smtps.host", "smtp.gmail.com");
		props.setProperty("mail.smtp.socketFactory.class", SSL_FACTORY);
		props.setProperty("mail.smtp.socketFactory.fallback", "false");
		props.setProperty("mail.smtp.port", "465");
		props.setProperty("mail.smtp.socketFactory.port", "465");
		props.setProperty("mail.smtps.auth", "true");

		/*
		 * If set to false, the QUIT command is sent and the connection is immediately
		 * closed. If set to true (the default), causes the transport to wait for the
		 * response to the QUIT command.
		 * 
		 * ref :
		 * http://java.sun.com/products/javamail/javadocs/com/sun/mail/smtp/package-
		 * summary.html http://forum.java.sun.com/thread.jspa?threadID=5205249
		 * smtpsend.java - demo program from javamail
		 */
		props.put("mail.smtps.quitwait", "false");

		Session session = Session.getInstance(props, null);

		// -- Create a new message --
		final MimeMessage msg = new MimeMessage(session);

		// -- Set the FROM and TO fields --
		msg.setFrom(new InternetAddress(username + "@gmail.com"));
		msg.setRecipients(Message.RecipientType.TO, InternetAddress.parse(recipientEmail, false));

		if (ccEmail.length() > 0) {
			msg.setRecipients(Message.RecipientType.CC, InternetAddress.parse(ccEmail, false));
		}

		msg.setSubject(title);
		msg.setText(message, "utf-8");
		msg.setSentDate(new Date());

		SMTPTransport t = (SMTPTransport) session.getTransport("smtps");

		t.connect("smtp.gmail.com", username, password);
		t.sendMessage(msg, msg.getAllRecipients());
		t.close();
	}

}