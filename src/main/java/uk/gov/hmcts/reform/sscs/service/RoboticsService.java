package uk.gov.hmcts.reform.sscs.service;

import static com.google.common.collect.Lists.newArrayList;
import static uk.gov.hmcts.reform.sscs.domain.email.EmailAttachment.json;
import static uk.gov.hmcts.reform.sscs.domain.email.EmailAttachment.pdf;

import lombok.extern.slf4j.Slf4j;
import org.json.JSONObject;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;
import uk.gov.hmcts.reform.sscs.ccd.domain.Appellant;
import uk.gov.hmcts.reform.sscs.ccd.domain.SscsCaseData;
import uk.gov.hmcts.reform.sscs.domain.email.RoboticsEmailTemplate;
import uk.gov.hmcts.reform.sscs.domain.robotics.RoboticsWrapper;
import uk.gov.hmcts.reform.sscs.json.RoboticsJsonMapper;
import uk.gov.hmcts.reform.sscs.json.RoboticsJsonValidator;

@Service
@Slf4j
public class RoboticsService {

    private final AirLookupService airLookupService;
    private final EmailService emailService;
    private final RoboticsJsonMapper roboticsJsonMapper;
    private final RoboticsJsonValidator roboticsJsonValidator;
    private final RoboticsEmailTemplate roboticsEmailTemplate;

    @Autowired
    public RoboticsService(
        AirLookupService airLookupService,
        EmailService emailService,
        RoboticsJsonMapper roboticsJsonMapper,
        RoboticsJsonValidator roboticsJsonValidator,
        RoboticsEmailTemplate roboticsEmailTemplate
    ) {
        this.airLookupService = airLookupService;
        this.emailService = emailService;
        this.roboticsJsonMapper = roboticsJsonMapper;
        this.roboticsJsonValidator = roboticsJsonValidator;
        this.roboticsEmailTemplate = roboticsEmailTemplate;
    }

    public void sendCaseToRobotics(SscsCaseData caseData, Long caseId, String postcode, byte[] pdf) {
        String venue = airLookupService.lookupAirVenueNameByPostCode(postcode);

        JSONObject roboticsJson = createRobotics(RoboticsWrapper.builder().sscsCaseData(caseData)
                .ccdCaseId(caseId).venueName(venue).evidencePresent(caseData.getEvidencePresent()).build());

        sendJsonByEmail(caseData.getAppeal().getAppellant(), roboticsJson, pdf);
        log.info("Robotics email sent successfully for Nino - {} and benefit type {}", caseData.getAppeal().getAppellant().getIdentity().getNino(),
                caseData.getAppeal().getBenefitType().getCode());
    }

    public JSONObject createRobotics(RoboticsWrapper appeal) {

        JSONObject roboticsAppeal =
            roboticsJsonMapper.map(appeal);

        roboticsJsonValidator.validate(roboticsAppeal);

        return roboticsAppeal;
    }

    private void sendJsonByEmail(Appellant appellant, JSONObject json, byte[] pdf) {
        String appellantUniqueId = emailService.generateUniqueEmailId(appellant);
        emailService.sendEmail(roboticsEmailTemplate.generateEmail(
                appellantUniqueId,
                newArrayList(json(json.toString().getBytes(), appellantUniqueId + ".txt"),
                        pdf(pdf, appellantUniqueId + ".pdf"))
        ));
    }

}
