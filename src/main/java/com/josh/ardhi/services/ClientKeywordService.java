package com.josh.ardhi.services;

import com.josh.ardhi.exception.AppException;
import com.josh.ardhi.forms.ClientKeywordForm;
import com.josh.ardhi.models.ClientKeyword;

import java.util.List;
import java.util.UUID;

public interface ClientKeywordService {

    List<ClientKeyword> getKeywordsForClient(UUID clientId);

    long countActiveKeywordsForClient(UUID clientId);

    long countTotalKeywordsForClient(UUID clientId);

    ClientKeyword getById(UUID keywordId) throws AppException;

    ClientKeyword createKeyword(UUID clientId, ClientKeywordForm form) throws AppException;

    ClientKeyword updateKeyword(UUID clientId, UUID keywordId, ClientKeywordForm form) throws AppException;

    ClientKeyword togglePauseKeyword(UUID clientId, UUID keywordId) throws AppException;

    void deleteKeyword(UUID clientId, UUID keywordId) throws AppException;

}
