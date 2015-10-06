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

import com.amazonaws.services.simpleemail.model.Body;
import com.amazonaws.services.simpleemail.model.Content;
import com.amazonaws.services.simpleemail.model.Destination;
import com.amazonaws.services.simpleemail.model.Message;
import com.amazonaws.services.simpleemail.model.SendEmailRequest;
import com.amazonaws.services.simpleemail.model.SendEmailResult;

public class CMCAmazonClientSample {
	public static void main(String argv[]) {

		String host = "cmcdomain";
		String mailFrom = "no-reply@cmcdomain";
		String mailTo = "yourname@yourdomain";

		// Construct an object to contain the recipient address.
		Destination destination = new Destination().withToAddresses(new String[] { mailTo });

		// Create the subject and body of the message.
		Content subject = new Content().withData("test mail");
		Content textBody = new Content().withData("test message");
		Body body = new Body().withText(textBody);

		// Create a message with the specified subject and body.
		Message message = new Message().withSubject(subject).withBody(body);

		// Assemble the email.
		SendEmailRequest request = new SendEmailRequest().withSource(mailFrom).withDestination(destination).withMessage(message);

		System.out.println("send start");
		CMCAmazonClient client = new CMCAmazonClient(host);
		SendEmailResult result = client.sendEmail(request);
		System.out.println("message id="+result.getMessageId());
		System.out.println("send end");
	}

}
