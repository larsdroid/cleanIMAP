package org.willemsens.cleanIMAP;

import javax.mail.BodyPart;
import javax.mail.Flags;
import javax.mail.Folder;
import javax.mail.Message;
import javax.mail.MessagingException;
import javax.mail.Multipart;
import javax.mail.Session;
import javax.mail.Store;
import javax.mail.search.AndTerm;
import javax.mail.search.ComparisonTerm;
import javax.mail.search.FlagTerm;
import javax.mail.search.ReceivedDateTerm;
import javax.mail.search.SearchTerm;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.Month;
import java.time.ZoneId;
import java.util.Arrays;
import java.util.Date;
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

        final Folder inbox = store.getFolder("INBOX");
        inbox.open(Folder.READ_WRITE);

        final Message[] messages = inbox.search(getSearchTerm(2017, Month.MARCH));

        handleMessages(messages);
    }

    private static SearchTerm getSearchTerm(int year, Month month) {
        final LocalDate firstOfMonth = LocalDate.of(year, month, 1);
        final LocalDate firstOfNextMonth = firstOfMonth.plusMonths(1);
        SearchTerm olderThan = new ReceivedDateTerm(ComparisonTerm.LT, asDate(firstOfNextMonth));
        SearchTerm newerThan = new ReceivedDateTerm(ComparisonTerm.GE, asDate(firstOfMonth));
        SearchTerm mailIsRead = new FlagTerm(new Flags(Flags.Flag.SEEN), true);
        return new AndTerm(new SearchTerm[]{olderThan, newerThan, mailIsRead});
    }

    private static void handleMessages(final Message[] messages) throws Exception {
        // Sort messages from recent to oldest
        Arrays.sort(messages, (m1, m2) -> {
            try {
                return m2.getSentDate().compareTo(m1.getSentDate());
            } catch (MessagingException e) {
                throw new RuntimeException(e);
            }
        });

        for (Message message : messages) {
            final LocalDateTime sentDate = asLocalDateTime(message.getSentDate());
            final Object content = message.getContent();
            if (content instanceof Multipart) {
                final Multipart multipart = (Multipart) content;
                for (int i = 0; i < multipart.getCount(); i++) {
                    final BodyPart bodyPart = multipart.getBodyPart(i);
                    System.out.println("    " + bodyPart.getContentType());
                }
                System.out.println("sendDate: " + sentDate
                        + " subject:" + message.getSubject());
            }
        }
    }

    private static Date asDate(LocalDate localDate) {
        return Date.from(localDate.atStartOfDay().atZone(ZoneId.systemDefault()).toInstant());
    }

    private static LocalDateTime asLocalDateTime(Date date) {
        return LocalDateTime.ofInstant(date.toInstant(), ZoneId.systemDefault());
    }
}
