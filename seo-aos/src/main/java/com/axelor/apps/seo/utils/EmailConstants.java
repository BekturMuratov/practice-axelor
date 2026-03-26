package com.axelor.apps.seo.utils;

import com.axelor.app.AppSettings;
import com.axelor.message.db.EmailAccount;

public class EmailConstants {

    private EmailConstants() {}

    public static String getWebsiteContactEmail() {
        return AppSettings.get().get("mail.gti.recipient");
    }

    public static EmailAccount setEmailConfigFromProperties() {
        String host = AppSettings.get().get("mail.smtp.host");
        String port = AppSettings.get().get("mail.smtp.port");
        String user = AppSettings.get().get("mail.smtp.user");
        String password = AppSettings.get().get("mail.smtp.password");
        String from = AppSettings.get().get("mail.smtp.from");

        EmailAccount emailAccount = new EmailAccount(user);
        emailAccount.setServerTypeSelect(1);
        emailAccount.setHost(host);
        emailAccount.setPort(Integer.parseInt(port));
        emailAccount.setLogin(user);
        emailAccount.setPassword(password);
        emailAccount.setFromName(from);
        emailAccount.setFromAddress(from);
        emailAccount.setSecuritySelect(2);
        emailAccount.setIsValid(true);

        return emailAccount;
    }
}
