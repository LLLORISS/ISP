package nmu.cso.isp.controller;

import nmu.cso.isp.service.DiagnosticService;
import org.springframework.web.bind.annotation.*;

@RestController
@RequestMapping("/api/v1/diagnose")
@CrossOrigin(origins = "*")
public class DiagnosticController {
    private final DiagnosticService diagnosticService;

    public DiagnosticController(DiagnosticService diagnosticService) {
        this.diagnosticService = diagnosticService;
    }

    @GetMapping("/{contractNumber}")
    public String getDiagnostic(@PathVariable String contractNumber) {
        return diagnosticService.checkStatus(contractNumber);
    }
}
