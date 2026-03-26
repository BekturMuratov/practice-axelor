package com.axelor.apps.seo.service;

import com.axelor.apps.seo.rest.dto.LetterDTO;

public interface LetterService {
    LetterDTO sendLetter(LetterDTO letterDTO);
}
