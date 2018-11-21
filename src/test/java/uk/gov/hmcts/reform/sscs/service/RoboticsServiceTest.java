package uk.gov.hmcts.reform.sscs.service;

import static org.junit.Assert.assertEquals;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.BDDMockito.given;
import static org.mockito.BDDMockito.then;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;
import static org.mockito.MockitoAnnotations.initMocks;
import static uk.gov.hmcts.reform.sscs.ccd.util.CaseDataUtils.buildCaseData;

import java.util.Collections;

import org.json.JSONObject;
import org.junit.Before;
import org.junit.Test;
import org.mockito.Mock;
import uk.gov.hmcts.reform.sscs.ccd.domain.SscsCaseData;
import uk.gov.hmcts.reform.sscs.domain.email.RoboticsEmailTemplate;
import uk.gov.hmcts.reform.sscs.domain.robotics.RoboticsWrapper;
import uk.gov.hmcts.reform.sscs.json.RoboticsJsonMapper;
import uk.gov.hmcts.reform.sscs.json.RoboticsJsonValidator;

public class RoboticsServiceTest {

    @Mock
    private RoboticsJsonMapper roboticsJsonMapper;

    @Mock
    private RoboticsJsonValidator roboticsJsonValidator;

    @Mock
    private AirLookupService airlookupService;

    @Mock
    private EmailService emailService;

    @Mock
    private RoboticsEmailTemplate roboticsEmailTemplate;

    private RoboticsService service;

    @Before
    public void setup() {
        initMocks(this);

        service = new RoboticsService(airlookupService, emailService, roboticsJsonMapper, roboticsJsonValidator, roboticsEmailTemplate);
    }

    @Test
    public void createValidRoboticsAndReturnAsJsonObject() {

        RoboticsWrapper appeal =
            RoboticsWrapper
                .builder()
                .sscsCaseData(buildCaseData())
                .ccdCaseId(123L).venueName("Bromley")
                .build();

        JSONObject mappedJson = mock(JSONObject.class);

        given(roboticsJsonMapper.map(appeal)).willReturn(mappedJson);

        JSONObject actualRoboticsJson = service.createRobotics(appeal);

        then(roboticsJsonMapper).should(times(1)).map(appeal);
        then(roboticsJsonValidator).should(times(1)).validate(mappedJson);

        assertEquals(mappedJson, actualRoboticsJson);
    }

    @Test
    public void generatingRoboticsSendsAnEmail() {

        SscsCaseData appeal = buildCaseData();

        JSONObject mappedJson = mock(JSONObject.class);

        given(roboticsJsonMapper.map(any())).willReturn(mappedJson);

        given(airlookupService.lookupAirVenueNameByPostCode("AB12 XYZ")).willReturn("Bristol");

        given(emailService.generateUniqueEmailId(appeal.getAppeal().getAppellant())).willReturn("Bloggs");

        byte[] pdf = {};

        service.sendCaseToRobotics(appeal, 123L, "AB12 XYZ", pdf);

        verify(roboticsJsonMapper).map(any());
        verify(roboticsJsonValidator).validate(mappedJson);
        verify(emailService).sendEmail(any());
    }

    @Test
    public void generatingRoboticsWithEmptyPdfSendsAnEmail() {

        SscsCaseData appeal = buildCaseData();

        JSONObject mappedJson = mock(JSONObject.class);

        given(roboticsJsonMapper.map(any())).willReturn(mappedJson);

        given(airlookupService.lookupAirVenueNameByPostCode("AB12 XYZ")).willReturn("Bristol");

        given(emailService.generateUniqueEmailId(appeal.getAppeal().getAppellant())).willReturn("Bloggs");

        service.sendCaseToRobotics(appeal, 123L, "AB12 XYZ", null);

        verify(roboticsJsonMapper).map(any());
        verify(roboticsJsonValidator).validate(mappedJson);
        verify(emailService).sendEmail(any());
    }

    @Test
    public void generatingRoboticsSendsAnEmailWithAdditionalEvidence() {

        SscsCaseData appeal = buildCaseData();

        JSONObject mappedJson = mock(JSONObject.class);

        given(roboticsJsonMapper.map(any())).willReturn(mappedJson);

        given(airlookupService.lookupAirVenueNameByPostCode("AB12 XYZ")).willReturn("Bristol");

        given(emailService.generateUniqueEmailId(appeal.getAppeal().getAppellant())).willReturn("Bloggs");

        byte[] pdf = {};
        byte[] someFile = {};

        service.sendCaseToRobotics(appeal, 123L, "AB12 XYZ", pdf, Collections.singletonMap("Some Evidence.doc", someFile));

        verify(roboticsJsonMapper).map(any());
        verify(roboticsJsonValidator).validate(mappedJson);
        verify(emailService).sendEmail(any());
    }
}
