package com.josh.ardhi.services.impl;

import com.josh.ardhi.exception.AppException;
import com.josh.ardhi.forms.ClientKeywordForm;
import com.josh.ardhi.models.AppUser;
import com.josh.ardhi.models.ClientKeyword;
import com.josh.ardhi.models.KeywordType;
import com.josh.ardhi.repositories.ClientKeywordRepository;
import com.josh.ardhi.services.AppUserService;
import com.josh.ardhi.services.ClientKeywordService;
import jakarta.transaction.Transactional;
import org.springframework.stereotype.Service;
import org.springframework.util.StringUtils;

import java.util.List;
import java.util.UUID;

@Service
public class ClientKeywordServiceImpl implements ClientKeywordService {

    private final ClientKeywordRepository clientKeywordRepository;
    private final AppUserService appUserService;

    public ClientKeywordServiceImpl(ClientKeywordRepository clientKeywordRepository,
                                    AppUserService appUserService) {
        this.clientKeywordRepository = clientKeywordRepository;
        this.appUserService = appUserService;
    }

    @Override
    public List<ClientKeyword> getKeywordsForClient(UUID clientId) {
        return clientKeywordRepository.findAllByClientIdAndIsActiveTrueOrderByCreatedAtDesc(clientId);
    }

    @Override
    public long countActiveKeywordsForClient(UUID clientId) {
        return clientKeywordRepository.countByClientIdAndIsActiveTrueAndPausedFalse(clientId);
    }

    @Override
    public long countTotalKeywordsForClient(UUID clientId) {
        return clientKeywordRepository.countByClientIdAndIsActiveTrue(clientId);
    }

    @Override
    public ClientKeyword getById(UUID keywordId) throws AppException {
        return clientKeywordRepository.findById(keywordId)
                .filter(ClientKeyword::isActive)
                .orElseThrow(() -> new AppException("Keyword not found."));
    }

    @Override
    @Transactional(rollbackOn = AppException.class)
    public ClientKeyword createKeyword(UUID clientId, ClientKeywordForm form) throws AppException {
        AppUser client = appUserService.getById(clientId);
        if (form == null || !StringUtils.hasText(form.getKeyword())) {
            throw new AppException("Keyword text is required.");
        }

        KeywordType type = form.getKeywordType() != null ? form.getKeywordType() : KeywordType.TITLE;
        String trimmedKeyword = form.getKeyword().trim();
        if (clientKeywordRepository.existsByClientIdAndKeywordIgnoreCaseAndKeywordTypeAndIsActiveTrue(
                clientId, trimmedKeyword, type)) {
            throw new AppException("The keyword \"" + trimmedKeyword + "\" (" + type.getLabel()
                    + ") already exists on this account.");
        }

        String precision = StringUtils.hasText(form.getMatchPrecision())
                ? form.getMatchPrecision().trim().toUpperCase()
                : type.getDefaultPrecision();

        try {
            ClientKeyword entity = ClientKeyword.builder()
                    .client(client)
                    .keyword(trimmedKeyword)
                    .keywordType(type)
                    .matchPrecision(precision)
                    .notes(StringUtils.hasText(form.getNotes()) ? form.getNotes().trim() : null)
                    .paused(false)
                    .build();
            return clientKeywordRepository.save(entity);
        } catch (Exception e) {
            throw new AppException("Failed to create keyword: " + e.getMessage());
        }
    }

    @Override
    @Transactional(rollbackOn = AppException.class)
    public ClientKeyword updateKeyword(UUID clientId, UUID keywordId, ClientKeywordForm form) throws AppException {
        ClientKeyword entity = getById(keywordId);
        if (!entity.getClient().getId().equals(clientId)) {
            throw new AppException("Keyword does not belong to this client.");
        }
        if (form == null || !StringUtils.hasText(form.getKeyword())) {
            throw new AppException("Keyword text is required.");
        }

        KeywordType type = form.getKeywordType() != null ? form.getKeywordType() : KeywordType.TITLE;
        String trimmedKeyword = form.getKeyword().trim();
        if (clientKeywordRepository.existsByClientIdAndKeywordIgnoreCaseAndKeywordTypeAndIsActiveTrueAndIdNot(
                clientId, trimmedKeyword, type, keywordId)) {
            throw new AppException("The keyword \"" + trimmedKeyword + "\" (" + type.getLabel()
                    + ") already exists on this account.");
        }

        String precision = StringUtils.hasText(form.getMatchPrecision())
                ? form.getMatchPrecision().trim().toUpperCase()
                : type.getDefaultPrecision();

        try {
            entity.setKeyword(trimmedKeyword);
            entity.setKeywordType(type);
            entity.setMatchPrecision(precision);
            entity.setNotes(StringUtils.hasText(form.getNotes()) ? form.getNotes().trim() : null);
            return clientKeywordRepository.save(entity);
        } catch (Exception e) {
            throw new AppException("Failed to update keyword: " + e.getMessage());
        }
    }

    @Override
    @Transactional(rollbackOn = AppException.class)
    public ClientKeyword togglePauseKeyword(UUID clientId, UUID keywordId) throws AppException {
        ClientKeyword entity = getById(keywordId);
        if (!entity.getClient().getId().equals(clientId)) {
            throw new AppException("Keyword does not belong to this client.");
        }

        try {
            entity.setPaused(!entity.isPaused());
            return clientKeywordRepository.save(entity);
        } catch (Exception e) {
            throw new AppException("Failed to toggle keyword status: " + e.getMessage());
        }
    }

    @Override
    @Transactional(rollbackOn = AppException.class)
    public void deleteKeyword(UUID clientId, UUID keywordId) throws AppException {
        ClientKeyword entity = getById(keywordId);
        if (!entity.getClient().getId().equals(clientId)) {
            throw new AppException("Keyword does not belong to this client.");
        }

        try {
            entity.setActive(false);
            entity.setDeactivationMemo("Deleted by admin");
            clientKeywordRepository.save(entity);
        } catch (Exception e) {
            throw new AppException("Failed to delete keyword: " + e.getMessage());
        }
    }
}
