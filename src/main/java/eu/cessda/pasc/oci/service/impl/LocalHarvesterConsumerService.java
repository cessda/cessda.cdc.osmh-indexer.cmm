package eu.cessda.pasc.oci.service.impl;

import eu.cessda.pasc.oci.exception.CustomHandlerException;
import eu.cessda.pasc.oci.models.RecordHeader;
import eu.cessda.pasc.oci.models.cmmstudy.CMMStudy;
import eu.cessda.pasc.oci.models.configurations.Repo;
import eu.cessda.pasc.oci.service.GetRecordService;
import eu.cessda.pasc.oci.service.ListRecordHeadersService;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

import java.time.LocalDateTime;
import java.util.Collections;
import java.util.List;
import java.util.Optional;

import static net.logstash.logback.argument.StructuredArguments.value;

@Service
@Slf4j
public class LocalHarvesterConsumerService extends AbstractHarvesterConsumerService {

    private final ListRecordHeadersService listRecordHeadersService;
    private final GetRecordService getRecordService;

    @Autowired
    public LocalHarvesterConsumerService(ListRecordHeadersService listRecordHeadersService, GetRecordService getRecordService) {
        this.listRecordHeadersService = listRecordHeadersService;
        this.getRecordService = getRecordService;
    }

    @Override
    public List<RecordHeader> listRecordHeaders(Repo repo, LocalDateTime lastModifiedDate) {
        try {
            List<RecordHeader> recordHeaders = listRecordHeadersService.getRecordHeaders(repo.getUrl());
            return filterRecords(recordHeaders, lastModifiedDate);
        } catch (CustomHandlerException e) {
            log.error("ListRecordHeaders failed for repo [{}] [{}]. {}.",
                    value(REPO_NAME, repo.getName()), value(REPO_ENDPOINT_URL, repo.getUrl()), e.toString());
        }
        return Collections.emptyList();
    }

    @Override
    public Optional<CMMStudy> getRecord(Repo repo, String studyNumber) {
        try {
            return Optional.of(getRecordService.getRecord(repo.getUrl(), studyNumber));
        } catch (CustomHandlerException e) {
            log.warn("getRecord failed for StudyId [{}] from repo [{}] [{}]: {}",
                    value("study_id", studyNumber),
                    value(REPO_NAME, repo.getName()),
                    value(REPO_ENDPOINT_URL, repo.getUrl()),
                    e.toString()
            );
            return Optional.empty();
        }
    }
}
