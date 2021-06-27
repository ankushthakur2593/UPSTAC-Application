package org.upgrad.upstac.testrequests;

import lombok.extern.slf4j.Slf4j;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.security.test.context.support.WithUserDetails;
import org.upgrad.upstac.exception.AppException;
import org.upgrad.upstac.exception.UpgradResponseStatusException;
import org.upgrad.upstac.testrequests.lab.*;
import org.upgrad.upstac.users.User;
import org.upgrad.upstac.users.models.Gender;

import static org.hamcrest.MatcherAssert.assertThat;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertThrows;


@SpringBootTest
@Slf4j
class LabRequestControllerTest {


    @Autowired
    LabRequestController labRequestController;

    @Autowired
    private TestRequestRepository testRequestRepository;

    @Autowired
    private TestRequestQueryService testRequestQueryService;

    @Autowired
    private LabResultRepository labResultRepository;

    @BeforeEach
    public void testSetup() {
        TestRequest initiatedTestRequest = mockTestRequestObject(1234L, RequestStatus.INITIATED);
        TestRequest labTestInProgressTestRequest = mockTestRequestObject(5678L, RequestStatus.LAB_TEST_IN_PROGRESS);

        labResultRepository.deleteAll();
        if (!testRequestRepository.findById(initiatedTestRequest.getRequestId()).isPresent()) {
            testRequestRepository.save(initiatedTestRequest);
        }

        if (!testRequestRepository.findById(labTestInProgressTestRequest.getRequestId()).isPresent()) {
            testRequestRepository.save(labTestInProgressTestRequest);
        }
    }

    @Test
    @WithUserDetails(value = "tester")
    public void calling_assignForLabTest_with_valid_test_request_id_should_update_the_request_status() {
        TestRequest testRequest1 = getTestRequestByStatus(RequestStatus.INITIATED);

        TestRequest assignedForLabTest = labRequestController.assignForLabTest(testRequest1.getRequestId());
        assertNotNull(assignedForLabTest, "Result is not available");
        assertThat("Request Id mismatch", assignedForLabTest.getRequestId().equals(testRequest1.getRequestId()));
        assertThat("Status is not correct", RequestStatus.LAB_TEST_IN_PROGRESS.equals(assignedForLabTest.getStatus()));
    }

    @Test
    @WithUserDetails(value = "tester")
    public void calling_assignForLabTest_with_valid_test_request_id_should_throw_exception() {
        Long invalidRequestId = -34L;
        AppException appException = assertThrows(AppException.class, () -> labRequestController.assignForLabTest(invalidRequestId));
        assertThat("Invalid ID passed", appException.getMessage().contains("Invalid ID"));
    }

    @Test
    @WithUserDetails(value = "tester")
    public void calling_updateLabTest_with_invalid_test_request_id_should_throw_exception() {
        TestRequest testRequest = getTestRequestByStatus(RequestStatus.LAB_TEST_IN_PROGRESS);
        CreateLabResult createLabResult = getCreateLabResult(testRequest);
        UpgradResponseStatusException upgradResponseStatusException = assertThrows(UpgradResponseStatusException.class, () -> labRequestController.updateLabTest(-123L, createLabResult));
        assertThat("Invalid ID is being passed", upgradResponseStatusException.getMessage().contains("Invalid ID"));
    }

    @Test
    @WithUserDetails(value = "tester")
    public void calling_updateLabTest_with_invalid_empty_status_should_throw_exception() {
        TestRequest testRequest = getTestRequestByStatus(RequestStatus.LAB_TEST_IN_PROGRESS);
        CreateLabResult createLabResult = new CreateLabResult();

        UpgradResponseStatusException upgradResponseStatusException = assertThrows(UpgradResponseStatusException.class, () -> labRequestController.updateLabTest(testRequest.getRequestId(), createLabResult));
        assertThat("Constraint Violation", upgradResponseStatusException.getMessage().contains("ConstraintViolationException"));
    }


    @Test
    @WithUserDetails(value = "tester")
    public void calling_updateLabTest_with_valid_test_request_id_should_update_the_request_status_and_update_test_request_details() {
        TestRequest testRequest = getTestRequestByStatus(RequestStatus.LAB_TEST_IN_PROGRESS);
        CreateLabResult createLabResult = getCreateLabResult(testRequest);
        LabResult result = new LabResult();
        result.setRequest(testRequest);
        result.setBloodPressure(createLabResult.getBloodPressure());
        result.setComments(createLabResult.getComments());
        result.setHeartBeat(createLabResult.getHeartBeat());
        result.setOxygenLevel(createLabResult.getOxygenLevel());
        result.setTemperature(createLabResult.getTemperature());

        testRequest.setLabResult(result);
        labResultRepository.save(result);
        testRequestRepository.save(testRequest);
        TestRequest postUpdateTestRequest = labRequestController.updateLabTest(testRequest.getRequestId(), createLabResult);

        assertThat("Invalid ID mismatch", testRequest.getRequestId().equals(postUpdateTestRequest.getRequestId()));
        assertThat("Lab test is not completed", RequestStatus.LAB_TEST_COMPLETED.equals(postUpdateTestRequest.getStatus()));
        assertThat("BP is not same", postUpdateTestRequest.getLabResult().getBloodPressure().equals(testRequest.getLabResult().getBloodPressure()));
        assertThat("Heart Beat is not same", postUpdateTestRequest.getLabResult().getHeartBeat().equals(testRequest.getLabResult().getHeartBeat()));
        assertThat("Oxygen level is not same", postUpdateTestRequest.getLabResult().getOxygenLevel().equals(testRequest.getLabResult().getOxygenLevel()));
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

    private User createMockUser() {
        User loggedInUser = new User();
        loggedInUser.setUserName("mike123@test.com");
        return loggedInUser;
    }

    private static TestRequest mockTestRequestObject(long reqId, RequestStatus status) {
        TestRequest testRequest = new TestRequest();
        testRequest.setRequestId(reqId);
        testRequest.setPhoneNumber("999999");
        testRequest.setEmail("mike@test.com");
        testRequest.setStatus(status);
        testRequest.setPinCode(110059);
        testRequest.setGender(Gender.MALE);
        testRequest.setName("Mike Tester");
        testRequest.setAge(28);
        return testRequest;
    }

    public TestRequest getTestRequestByStatus(RequestStatus status) {
        return testRequestQueryService.findBy(status).stream().findFirst().get();
    }


}