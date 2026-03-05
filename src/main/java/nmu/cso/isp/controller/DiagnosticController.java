package nmu.cso.isp.controller;

import nmu.cso.isp.service.DiagnosticService;
import org.springframework.web.bind.annotation.*;

/**
 * REST controller for providing network diagnostic information via HTTP.
 * This controller exposes endpoints for external systems or frontend applications
 * to retrieve automated diagnostic reports based on a customer's contract number.
 * * @author Muts Naxar
 * @version 1.0
 */
@RestController
@RequestMapping("/api/v1/diagnose")
@CrossOrigin(origins = "*")
public class DiagnosticController {
    private final DiagnosticService diagnosticService;

    /**
     * Constructs the controller with the necessary diagnostic service.
     * * @param diagnosticService the service responsible for executing network checks
     */
    public DiagnosticController(DiagnosticService diagnosticService) {
        this.diagnosticService = diagnosticService;
    }

    /**
     * Retrieves a diagnostic report for a specific contract.
     * This endpoint performs a real-time check of the customer's connection status,
     * equipment reachability, and signal levels.
     * *
     *
     * @param contractNumber the unique identifier of the customer's contract
     * @return a string containing the detailed diagnostic result
     */
    @GetMapping("/{contractNumber}")
    public String getDiagnostic(@PathVariable String contractNumber) {
        return diagnosticService.diagnoseCustomer(contractNumber);
    }
}
