/*
 * Copyright 2015 Masahiro Okubo HDE,Inc.
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 * 
 * http://www.apache.org/licenses/LICENSE-2.0
 * 
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */
package jp.co.hde.mail.ses;

import java.util.Date;
import java.util.List;
import java.util.Properties;

import javax.mail.Message.RecipientType;
import javax.mail.MessagingException;
import javax.mail.Multipart;
import javax.mail.Session;
import javax.mail.internet.AddressException;
import javax.mail.internet.InternetAddress;
import javax.mail.internet.MimeBodyPart;
import javax.mail.internet.MimeMessage;
import javax.mail.internet.MimeMultipart;

import jp.co.hde.mail.smtp.CMCTransport;

import com.amazonaws.AmazonClientException;
import com.amazonaws.services.simpleemail.model.Body;
import com.amazonaws.services.simpleemail.model.Content;
import com.amazonaws.services.simpleemail.model.Destination;
import com.amazonaws.services.simpleemail.model.Message;
import com.amazonaws.services.simpleemail.model.SendEmailRequest;
import com.amazonaws.services.simpleemail.model.SendEmailResult;

public class CMCAmazonClient {

	private String hostname;
	private String username;
	private String password;
	private boolean usetls;

	public CMCAmazonClient(String hostname) {
		this(hostname, null, null, false);
	}

	public CMCAmazonClient(String hostname, String username, String password,
			boolean usetls) {
		this.hostname = hostname;
		this.username = username;
		this.password = password;
		this.usetls = usetls;
	}

	public SendEmailResult sendEmail(SendEmailRequest req) {

		if (req == null) {
			throw new AmazonClientException("SendEmailRequest is null");
		}

		if (this.hostname == null) {
			throw new AmazonClientException("hostname is null");
		}

		Properties props = new Properties();
		props.put("mail.smtp.host", this.hostname);

		Session session = Session.getInstance(props);
		MimeMessage message = new MimeMessage(session);

		Destination dest = req.getDestination();
		if (dest == null) {
			throw new AmazonClientException("Destination is null");
		}

		try {
			// To
			List<String> toAddrs = dest.getToAddresses();
			if (toAddrs != null && toAddrs.size() > 0) {
				message.setRecipients(RecipientType.TO, toArray(toAddrs));
			} else {
				throw new AmazonClientException("To Address is not exist");
			}

			// Cc
			List<String> ccAddrs = dest.getCcAddresses();
			if (ccAddrs != null && ccAddrs.size() > 0) {
				message.setRecipients(RecipientType.CC, toArray(ccAddrs));
			}

			// Bcc
			List<String> bccAddrs = dest.getBccAddresses();
			if (bccAddrs != null && bccAddrs.size() > 0) {
				message.setRecipients(RecipientType.BCC, toArray(bccAddrs));
			}
		} catch (AddressException e) {
			throw new AmazonClientException("Invalid internet address: " + e.getMessage());
		} catch (MessagingException e) {
			throw new AmazonClientException("setRecipients failed: " + e.getMessage());
		}

		// From
		try {
			message.setFrom(new InternetAddress(req.getSource()));
		} catch (MessagingException e) {
			throw new AmazonClientException("setFrom failed: " + e.getMessage());
		}

		// Date
		try {
			message.setSentDate(new Date());
		} catch (MessagingException e) {
			throw new AmazonClientException("setSentDate failed: " + e.getMessage());
		}

		Message original = req.getMessage();
		if (original != null) {
			// Subject
			try {
				Content subject = original.getSubject();
				if (subject != null) {
					message.setSubject(subject.getData(), subject.getCharset());
				} else {
					message.setSubject("");
				}
			} catch (MessagingException e) {
				throw new AmazonClientException("setSubject failed: "
						+ e.getMessage());
			}

			// Body
			Body body = original.getBody();
			if (body != null) {
				try {
					Content htmlBody = body.getHtml();
					Content textBody = body.getText();
					if (htmlBody != null && textBody != null) {
						String htmlData = htmlBody.getData();
						if (htmlData != null && !htmlData.isEmpty()) {
							// Create multipart message
							Multipart multipart = new MimeMultipart("alternative");

							// TextPart
							MimeBodyPart textPart = new MimeBodyPart();
							if (textBody != null) {
								String textData = textBody.getData();
								if (textData != null && !textData.isEmpty()) {
									textPart.setText(textData,textBody.getCharset());
								} else {
									textPart.setText("");
								}
							}
							// HtmlPart
							MimeBodyPart htmlPart = new MimeBodyPart();
							htmlPart.setText(htmlData, htmlBody.getCharset(),"html");
							htmlPart.addHeader("Content-Transfer-Encoding","base64");
							// Add multipart body in the message
							multipart.addBodyPart(textPart);
							multipart.addBodyPart(htmlPart);
							message.setContent(multipart);
						}
					} else if (htmlBody != null) {
						message.setText(htmlBody.getData(), htmlBody.getCharset(), "html");
						if( htmlBody.getCharset()!=null&&htmlBody.getCharset().equalsIgnoreCase("iso-2022-jp")) {
							message.addHeader("Content-Transfer-Encoding","7bit");
						}
					} else if (textBody != null){
						message.setText(textBody.getData(), textBody.getCharset());
						if( textBody.getCharset()!=null&&textBody.getCharset().equalsIgnoreCase("iso-2022-jp")) {
							message.addHeader("Content-Transfer-Encoding","7bit");
						}
					} else {
						throw new AmazonClientException("Message body is not exist");
					}
				} catch (MessagingException e) {
					throw new AmazonClientException("setContent failed: " + e.getMessage());
				}
			} else {
				throw new AmazonClientException("Message body is not exist");
			}

		} else {
			throw new AmazonClientException("Message is not exist");
		}

		// Send email
		try {
			SendEmailResult result = new SendEmailResult();
			if (this.username != null) {
				if (this.password == null) {
					throw new AmazonClientException("SMTP-Auth password is not exist");
				}
				CMCTransport.send(message, this.username, this.password);
			} else {
				CMCTransport.send(message);
			}
			result.setMessageId(message.getMessageID());
			return result;
		} catch (MessagingException e) {
			throw new AmazonClientException("CMCTransport.send failed : " + e.getMessage());
		}
	}
	
	private static InternetAddress[] toArray(List<String> addressList) throws AddressException {
		InternetAddress[] addresses = new InternetAddress[addressList.size()];
		for (int i = 0; i < addressList.size(); i++) {
			addresses[i] = new InternetAddress(addressList.get(i));
		}
		return addresses;
	}
}
