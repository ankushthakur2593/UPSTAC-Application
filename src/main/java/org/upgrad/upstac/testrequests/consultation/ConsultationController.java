package org.upgrad.upstac.testrequests.consultation;


import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.*;
import org.upgrad.upstac.config.security.UserLoggedInService;
import org.upgrad.upstac.exception.AppException;
import org.upgrad.upstac.testrequests.RequestStatus;
import org.upgrad.upstac.testrequests.TestRequest;
import org.upgrad.upstac.testrequests.TestRequestQueryService;
import org.upgrad.upstac.testrequests.TestRequestUpdateService;
import org.upgrad.upstac.testrequests.flow.TestRequestFlowService;
import org.upgrad.upstac.users.User;

import javax.validation.ConstraintViolationException;
import java.util.List;

import static org.upgrad.upstac.exception.UpgradResponseStatusException.asBadRequest;
import static org.upgrad.upstac.exception.UpgradResponseStatusException.asConstraintViolation;


@RestController
@RequestMapping("/api/consultations")
public class ConsultationController {

    Logger log = LoggerFactory.getLogger(ConsultationController.class);

    @Autowired
    private TestRequestUpdateService testRequestUpdateService;

    @Autowired
    private TestRequestQueryService testRequestQueryService;


    @Autowired
    TestRequestFlowService testRequestFlowService;

    @Autowired
    private UserLoggedInService userLoggedInService;


    /**
     * This method is responsible for returning all the test requests for which lab testing is done
     * @return - List of TestRequests
     */
    @GetMapping("/in-queue")
    @PreAuthorize("hasAnyRole('DOCTOR')")
    public List<TestRequest> getForConsultations() {
        return testRequestQueryService.findBy(RequestStatus.LAB_TEST_COMPLETED);
    }

    /**
     * This method is responsible for returning all the test requests assigned to the logged-in user(doctor)
     * @return List of test requests
     */
    @GetMapping
    @PreAuthorize("hasAnyRole('DOCTOR')")
    public List<TestRequest> getForDoctor() {
        User loggedInUser = userLoggedInService.getLoggedInUser();
        return testRequestQueryService.findByDoctor(loggedInUser);
    }


    /**
     * This method is responsible for assigning the test request to a doctor (logged-in user)
     * @param id - ID of the test request
     * @return - Assigned TestRequest's instance
     */
    @PreAuthorize("hasAnyRole('DOCTOR')")
    @PutMapping("/assign/{id}")
    public TestRequest assignForConsultation(@PathVariable Long id) {
        try {
            User loggedInUser = userLoggedInService.getLoggedInUser();
            return testRequestUpdateService.assignForConsultation(id, loggedInUser);
        } catch (AppException e) {
            throw asBadRequest(e.getMessage());
        }
    }

    /**
     * This method is responsible for updating a test request with the consultation remarks by the logged-in user (doctor)
     * @param id - ID of the test request
     * @param testResult - Consultation Remarks of the doctor
     * @return Updated TestRequest's instance
     */
    @PreAuthorize("hasAnyRole('DOCTOR')")
    @PutMapping("/update/{id}")
    public TestRequest updateConsultation(@PathVariable Long id, @RequestBody CreateConsultationRequest testResult) {
        try {
            User loggedInUser = userLoggedInService.getLoggedInUser();
            return testRequestUpdateService.updateConsultation(id, testResult, loggedInUser);
        } catch (ConstraintViolationException e) {
            throw asConstraintViolation(e);
        } catch (AppException e) {
            throw asBadRequest(e.getMessage());
        }
    }
}
