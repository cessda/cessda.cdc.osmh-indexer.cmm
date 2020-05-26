package eu.cessda.pasc.oci.service.impl;

import eu.cessda.pasc.oci.models.RecordHeader;
import eu.cessda.pasc.oci.models.cmmstudy.CMMStudy;
import eu.cessda.pasc.oci.models.configurations.Repo;
import eu.cessda.pasc.oci.service.GetRecordService;
import eu.cessda.pasc.oci.service.HarvesterConsumerService;
import eu.cessda.pasc.oci.service.ListRecordHeadersService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

import java.time.LocalDateTime;
import java.util.List;
import java.util.Optional;

@Service
public class LocalHarvesterConsumerService implements HarvesterConsumerService {

    private final ListRecordHeadersService listRecordHeadersService;
    private final GetRecordService getRecordService;

    @Autowired
    public LocalHarvesterConsumerService(ListRecordHeadersService listRecordHeadersService, GetRecordService getRecordService) {
        this.listRecordHeadersService = listRecordHeadersService;
        this.getRecordService = getRecordService;
    }

    @Override
    public List<RecordHeader> listRecordHeaders(Repo repo, LocalDateTime lastModifiedDate) {
        return listRecordHeadersService.getRecordHeaders(repo.getUrl().toString());
    }

    @Override
    public Optional<CMMStudy> getRecord(Repo repo, String studyNumber) {
        return getRecordService.getRecord(repo.getUrl().toString(), studyNumber);
    }
}
