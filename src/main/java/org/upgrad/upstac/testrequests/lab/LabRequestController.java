package org.upgrad.upstac.testrequests.lab;


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
@RequestMapping("/api/labrequests")
public class LabRequestController {

    Logger log = LoggerFactory.getLogger(LabRequestController.class);

    @Autowired
    private TestRequestUpdateService testRequestUpdateService;

    @Autowired
    private TestRequestQueryService testRequestQueryService;

    @Autowired
    private TestRequestFlowService testRequestFlowService;

    @Autowired
    private UserLoggedInService userLoggedInService;

    /**
     * This method is responsible for returning all the test requests which are in INITIATED stage
     * @return List of TestRequests
     */
    @GetMapping("/to-be-tested")
    @PreAuthorize("hasAnyRole('TESTER')")
    public List<TestRequest> getForTests() {
        return testRequestQueryService.findBy(RequestStatus.INITIATED);
    }

    /**
     * This method is responsible for returning all the test requests assigned to the logged-in user
     * @return List of TestRequests
     */
    @GetMapping
    @PreAuthorize("hasAnyRole('TESTER')")
    public List<TestRequest> getForTester() {
        User loggedInUser = userLoggedInService.getLoggedInUser();
        return testRequestQueryService.findByTester(loggedInUser);

    }

    /**
     * This method is responsible for assigning a test request to the logged in user
     * @param id - ID of the user
     * @return Assigned TestRequest's instance
     */
    @PreAuthorize("hasAnyRole('TESTER')")
    @PutMapping("/assign/{id}")
    public TestRequest assignForLabTest(@PathVariable Long id) {
        User tester = userLoggedInService.getLoggedInUser();
        return testRequestUpdateService.assignForLabTest(id, tester);
    }

    /**
     * This method is responsible for updating the lab test parameters by the logged-in user
     * @param id - ID of the user
     * @param createLabResult - Stores the basic health parameters of the patient
     * @return Updated TestRequest's instance
     */
    @PreAuthorize("hasAnyRole('TESTER')")
    @PutMapping("/update/{id}")
    public TestRequest updateLabTest(@PathVariable Long id, @RequestBody CreateLabResult createLabResult) {
        try {
            User tester = userLoggedInService.getLoggedInUser();
            return testRequestUpdateService.updateLabTest(id, createLabResult, tester);
        } catch (ConstraintViolationException e) {
            throw asConstraintViolation(e);
        } catch (AppException e) {
            throw asBadRequest(e.getMessage());
        }
    }
}
