package org.willemsens.cleanIMAP;

import javax.mail.Flags;
import javax.mail.Folder;
import javax.mail.Message;
import javax.mail.MessagingException;
import javax.mail.Session;
import javax.mail.Store;
import javax.mail.search.FlagTerm;
import java.time.LocalDateTime;
import java.time.ZoneId;
import java.util.Arrays;
import java.util.Properties;
import java.util.Scanner;

public class Main {
    public static void main(String[] args) throws Exception {
        final String email;
        final String password;
        if (args.length == 2) {
            email = args[0];
            password = args[1];
        } else if (args.length == 0) {
            final Scanner scanner = new Scanner(System.in);
            System.out.print("Email:    ");
            email = scanner.nextLine();
            System.out.print("Password: ");
            password = scanner.nextLine();
        } else {
            throw new Exception("Usage: cleanIMAP <email> <password>");
        }
        
        final Session session = Session.getDefaultInstance(new Properties());
        final Store store = session.getStore("imaps");
        store.connect("imap.one.com", 993, email, password);

        final Folder inbox = store.getFolder("INBOX.Drafts");
        inbox.open(Folder.READ_WRITE);

        final Message[] messages = inbox.search(new FlagTerm(new Flags(Flags.Flag.SEEN), true));

        // Sort messages from recent to oldest
        Arrays.sort(messages, (m1, m2) -> {
            try {
                return m2.getSentDate().compareTo(m1.getSentDate());
            } catch (MessagingException e) {
                throw new RuntimeException(e);
            }
        });

        for (Message message : messages) {
            final LocalDateTime sentDate = LocalDateTime.ofInstant(message.getSentDate().toInstant(), ZoneId.systemDefault());
            System.out.println("sendDate: " + sentDate
                    + " subject:" + message.getSubject());
        }
    }
}
