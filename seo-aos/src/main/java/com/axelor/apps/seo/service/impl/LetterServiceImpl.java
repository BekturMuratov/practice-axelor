package com.axelor.apps.seo.service.impl;

import com.axelor.apps.base.service.MailServiceBaseImpl;
import com.axelor.apps.base.service.app.AppBaseService;
import com.axelor.apps.base.service.exception.TraceBackService;
import com.axelor.apps.seo.db.Letter;
import com.axelor.apps.seo.rest.dto.LetterDTO;
import com.axelor.apps.seo.service.CrudService;
import com.axelor.apps.seo.service.LetterService;
import com.axelor.apps.seo.utils.EmailConstants;
import com.axelor.mail.MailBuilder;
import com.axelor.mail.MailSender;
import com.axelor.message.db.EmailAccount;
import com.axelor.message.service.MailAccountService;
import com.google.inject.Inject;

public class LetterServiceImpl extends MailServiceBaseImpl implements LetterService {

    private final CrudService crudService;

    @Inject
    public LetterServiceImpl(MailAccountService mailAccountService, AppBaseService appBaseService, CrudService crudService) {
        super(mailAccountService,  appBaseService);
        this.crudService = crudService;
    }

    @Override
    public LetterDTO sendLetter(LetterDTO letterDTO) {
        String text = String.format(
                "<b>ФИО отправителя:</b> %s<br>" +
                        "<b>Email отправителя:</b> %s<br>" +
                        "<b>Сообщение:</b><br>%s",
                letterDTO.getFullName(),
                letterDTO.getEmail(),
                letterDTO.getMessageContent().replace("\n", "<br>") // сохраняем переносы строк
        );

        EmailAccount emailAccount = EmailConstants.setEmailConfigFromProperties();
        MailSender mailSender = getMailSender(emailAccount);

        MailBuilder builder = mailSender.compose();
        builder.to(EmailConstants.getWebsiteContactEmail());
        builder.subject(letterDTO.getFullName());
        builder.html(text);

        try {
            builder.send();
        }  catch (Exception e) {
            TraceBackService.trace(e);
        }
        Letter savedLetter = persistLetter(letterDTO);

        return new LetterDTO(
                savedLetter.getId(),
                savedLetter.getFullNameSender(),
                savedLetter.getEmail(),
                savedLetter.getMessageContent()
        );
    }

    private Letter persistLetter(LetterDTO letterDTO) {
        Letter letter = new Letter();
        letter.setFullNameSender(letterDTO.getFullName());
        letter.setEmail(letterDTO.getEmail());
        letter.setMessageContent(letterDTO.getMessageContent());
        return crudService.persistObject(letter);
    }
}
