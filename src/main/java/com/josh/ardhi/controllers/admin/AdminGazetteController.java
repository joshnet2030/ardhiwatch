package com.josh.ardhi.controllers.admin;

import com.josh.ardhi.exception.AppException;
import com.josh.ardhi.forms.GazetteUploadForm;
import com.josh.ardhi.models.GazetteIssue;
import com.josh.ardhi.services.GazetteService;
import io.github.wimdeblauwe.htmx.spring.boot.mvc.HxRequest;
import jakarta.servlet.http.HttpServletResponse;
import jakarta.validation.Valid;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.validation.BindingResult;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.ModelAttribute;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.multipart.MultipartFile;

@Controller
@RequestMapping("/admin/gazettes")
public class AdminGazetteController {

    private final GazetteService gazetteService;

    public AdminGazetteController(GazetteService gazetteService) {
        this.gazetteService = gazetteService;
    }

    @GetMapping("")
    public String index(Model model) {
        model.addAttribute("uploadForm", new GazetteUploadForm());
        model.addAttribute("gazetteIssues", gazetteService.getAllActiveIssues());
        return "admin/gazettes/list";
    }

    @HxRequest
    @PostMapping("/upload")
    public String upload(@Valid @ModelAttribute("uploadForm") GazetteUploadForm uploadForm,
                         BindingResult bindingResult,
                         Model model,
                         HttpServletResponse response) {
        MultipartFile pdfFile = uploadForm.getPdfFile();
        if (pdfFile == null || pdfFile.isEmpty()) {
            bindingResult.rejectValue("pdfFile", "pdfFile.empty", "Please select a Gazette PDF file to upload");
        } else {
            String originalName = pdfFile.getOriginalFilename();
            String contentType = pdfFile.getContentType();
            boolean isPdfContent = contentType != null && contentType.equalsIgnoreCase("application/pdf");
            boolean isPdfExtension = originalName != null && originalName.toLowerCase().endsWith(".pdf");
            if (!isPdfContent && !isPdfExtension) {
                bindingResult.rejectValue("pdfFile", "pdfFile.invalidType", "Only PDF files are allowed");
            }
        }

        if (bindingResult.hasErrors()) {
            return "admin/gazettes/fragments/upload-form :: uploadForm";
        }

        GazetteIssue uploaded = null;
        try {
            uploaded = gazetteService.uploadGazette(uploadForm);
            model.addAttribute("uploadForm", new GazetteUploadForm());
            model.addAttribute("uploadSuccess",
                    "Kenya Gazette Vol. " + uploaded.getVolumeNumber() + ", No. " + uploaded.getIssueNumber()
                            + " (" + uploaded.getPdfSystemFile().getOriginalFileName() + ") uploaded successfully.");
            response.setHeader("HX-Trigger", "gazetteUploaded");

            return "admin/gazettes/fragments/upload-form :: uploadForm";
        } catch (AppException e) {
            model.addAttribute("uploadFailure", "Unable to upload the file: "+e.getMessage());
            return "admin/gazettes/fragments/upload-form :: uploadForm";
        }

    }

    @HxRequest
    @GetMapping("/table")
    public String tableFragment(@RequestParam(defaultValue = "list") String view, Model model) {
        if ("dashboard".equalsIgnoreCase(view)) {
            model.addAttribute("gazetteIssues", gazetteService.getRecentIssues());
            return "admin/gazettes/fragments/issues-table :: dashboardTable";
        }
        model.addAttribute("gazetteIssues", gazetteService.getAllActiveIssues());
        return "admin/gazettes/fragments/issues-table :: listTable";
    }

    @GetMapping("/show/{id}")
    public String overview(@PathVariable String id, Model model) {
        model.addAttribute("tab", "overview");
        return "admin/gazettes/tabs/overview";
    }

    @GetMapping("/show/{id}/pages")
    public String gazetteViewPages(@PathVariable String id, Model model) {
        model.addAttribute("tab", "pages");
        return "admin/gazettes/tabs/pages";
    }

    @GetMapping("/matches-review")
    public String matchesReview() {
        return "admin/gazettes/matches_review_list";
    }

    @GetMapping("/matches-review/show/id")
    public String matchesReviewRecord() {
        return "admin/gazettes/matches_review_record";
    }

    @ModelAttribute("currentMenu")
    public String currentMenu() {
        return "gazettes";
    }
}
