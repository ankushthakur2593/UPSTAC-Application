package org.upgrad.upstac.testrequests;

import lombok.extern.slf4j.Slf4j;
import org.junit.jupiter.api.*;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.security.test.context.support.WithUserDetails;
import org.springframework.web.server.ResponseStatusException;
import org.upgrad.upstac.testrequests.consultation.*;
import org.upgrad.upstac.testrequests.lab.CreateLabResult;
import org.upgrad.upstac.testrequests.lab.TestStatus;
import org.upgrad.upstac.users.models.Gender;

import static org.hamcrest.MatcherAssert.assertThat;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertThrows;


@TestMethodOrder(MethodOrderer.OrderAnnotation.class)
@SpringBootTest
@Slf4j
class ConsultationControllerTest {

    @Autowired
    private ConsultationController consultationController;

    @Autowired
    private TestRequestQueryService testRequestQueryService;

    @Autowired
    private TestRequestRepository testRequestRepository;

    @Autowired
    private ConsultationRepository consultationRepository;

    @BeforeEach
    public void testSetup() {
        TestRequest initiatedTestRequest = mockTestRequestObject(RequestStatus.LAB_TEST_COMPLETED);
        TestRequest diagnosisInProgress = mockTestRequestObject(RequestStatus.DIAGNOSIS_IN_PROCESS);

        consultationRepository.deleteAll();
        if (!testRequestRepository.findById(initiatedTestRequest.getRequestId()).isPresent()) {
            testRequestRepository.save(initiatedTestRequest);
        }

        if (!testRequestRepository.findById(diagnosisInProgress.getRequestId()).isPresent()) {
            testRequestRepository.save(diagnosisInProgress);
        }
    }


    @Test
    @Order(3)
    @WithUserDetails(value = "doctor")
    public void calling_assignForConsultation_with_valid_test_request_id_should_update_the_request_status() {
        TestRequest testRequest = getTestRequestByStatus(RequestStatus.LAB_TEST_COMPLETED);
        TestRequest assignedTestRequest = consultationController.assignForConsultation(testRequest.getRequestId());

        assertNotNull(assignedTestRequest, "Assigned for consultation failed as the object is null");
        assertThat("Request IDs are different", assignedTestRequest.getRequestId().equals(testRequest.getRequestId()));
        assertThat("Status is not updated", RequestStatus.DIAGNOSIS_IN_PROCESS.equals(assignedTestRequest.getStatus()));
    }

    public TestRequest getTestRequestByStatus(RequestStatus status) {
        return testRequestQueryService.findBy(status).stream().findFirst().get();
    }

    @Test
    @Order(2)
    @WithUserDetails(value = "doctor")
    public void calling_assignForConsultation_with_valid_test_request_id_should_throw_exception() {
        Long InvalidRequestId = -34L;

        ResponseStatusException responseStatusException = assertThrows(ResponseStatusException.class, () -> consultationController.assignForConsultation(InvalidRequestId));
        assertThat("Consultation is assigned with the invalid ID", responseStatusException.getMessage().contains("Invalid ID"));
    }

    @Test
    @Order(5)
    @WithUserDetails(value = "doctor")
    public void calling_updateConsultation_with_valid_test_request_id_should_update_the_request_status_and_update_consultation_details() {
        TestRequest testRequest = getTestRequestByStatus(RequestStatus.DIAGNOSIS_IN_PROCESS);

        CreateConsultationRequest consultationRequest = getCreateConsultationRequest(testRequest);
        consultationRequest.setSuggestion(DoctorSuggestion.HOME_QUARANTINE);
        Consultation consultation = new Consultation();
        consultation.setRequest(testRequest);
        consultation.setSuggestion(DoctorSuggestion.HOME_QUARANTINE);
        testRequest.setConsultation(consultation);
        consultationRepository.save(consultation);
        testRequestRepository.save(testRequest);
        TestRequest updatedRequest = consultationController.updateConsultation(testRequest.getRequestId(), consultationRequest);
        assertThat("Request IDs are not same", testRequest.getRequestId().equals(updatedRequest.getRequestId()));
        assertThat("Request is not completed", RequestStatus.COMPLETED.equals(updatedRequest.getStatus()));
        assertThat("Consultation's suggestion mismatch", consultationRequest.getSuggestion().equals(updatedRequest.getConsultation().getSuggestion()));
    }


    @Test
    @Order(1)
    @WithUserDetails(value = "doctor")
    public void calling_updateConsultation_with_invalid_test_request_id_should_throw_exception() {
        TestRequest testRequest = getTestRequestByStatus(RequestStatus.DIAGNOSIS_IN_PROCESS);
        CreateConsultationRequest consultationRequest = getCreateConsultationRequest(testRequest);
        consultationRequest.setSuggestion(DoctorSuggestion.NO_ISSUES);
        ResponseStatusException responseStatusException = assertThrows(ResponseStatusException.class, () -> consultationController.updateConsultation(-123L, consultationRequest));
        assertThat("Invalid ID", responseStatusException.getMessage().contains("Invalid ID"));
    }

    @Test
    @Order(4)
    @WithUserDetails(value = "doctor")
    public void calling_updateConsultation_with_invalid_empty_status_should_throw_exception() {
        TestRequest testRequest = getTestRequestByStatus(RequestStatus.DIAGNOSIS_IN_PROCESS);
        CreateConsultationRequest consultationRequest = getCreateConsultationRequest(testRequest);

        consultationRequest.setSuggestion(null);
        assertThrows(ResponseStatusException.class, () -> consultationController.updateConsultation(testRequest.getRequestId(), consultationRequest));
    }

    public CreateConsultationRequest getCreateConsultationRequest(TestRequest testRequest) {
        CreateConsultationRequest consultationRequest = new CreateConsultationRequest();
        if (testRequest.getLabResult() != null) {
            if (TestStatus.POSITIVE.equals(testRequest.getLabResult().getResult())) {
                consultationRequest.setSuggestion(DoctorSuggestion.HOME_QUARANTINE);
                consultationRequest.setComments("Home quarantine is suggested for the same");
            } else {
                consultationRequest.setSuggestion(DoctorSuggestion.NO_ISSUES);
                consultationRequest.setComments("Ok");
            }
        }
        return consultationRequest;

    }

    public CreateLabResult getCreateLabResult(TestRequest testRequest) {
        CreateLabResult createLabResult = new CreateLabResult();
        if (testRequest.getLabResult() != null) {
            createLabResult.setBloodPressure(testRequest.getLabResult().getBloodPressure());
            createLabResult.setComments(testRequest.getLabResult().getComments());
            createLabResult.setHeartBeat(testRequest.getLabResult().getHeartBeat());
            createLabResult.setOxygenLevel(testRequest.getLabResult().getOxygenLevel());
            createLabResult.setResult(testRequest.getLabResult().getResult());
            createLabResult.setTemperature(testRequest.getLabResult().getTemperature());
        } else {
            createLabResult.setTemperature("99.3 F");
            createLabResult.setResult(TestStatus.NEGATIVE);
            createLabResult.setOxygenLevel("99");
            createLabResult.setHeartBeat("Working");
            createLabResult.setComments("Comments");
            createLabResult.setBloodPressure("120/80");
        }
        return createLabResult;
    }

    private static TestRequest mockTestRequestObject(RequestStatus status) {
        TestRequest testRequest = new TestRequest();
        testRequest.setRequestId(12334L);
        testRequest.setPhoneNumber("999999");
        testRequest.setEmail("mike@test.com");
        testRequest.setStatus(status);
        testRequest.setPinCode(110059);
        testRequest.setGender(Gender.MALE);
        testRequest.setName("Mike Tester");
        testRequest.setAge(28);
        return testRequest;
    }


}