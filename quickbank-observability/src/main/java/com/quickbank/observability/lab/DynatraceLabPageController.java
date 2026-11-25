package com.quickbank.observability.lab;

import com.quickbank.observability.lab.DynatraceLab.LabView;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RestController;
import org.springframework.web.bind.annotation.RequestMapping;

@Controller
public class DynatraceLabPageController {

    private final DynatraceLab lab;

    public DynatraceLabPageController(DynatraceLab lab) {
        this.lab = lab;
    }

    @GetMapping("/lab")
    public String page(Model model) {
        model.addAttribute("lab", lab.view());
        return "lab";
    }
}

@RestController
@RequestMapping("/api/observability")
class DynatraceLabApiController {

    private final DynatraceLab lab;

    DynatraceLabApiController(DynatraceLab lab) {
        this.lab = lab;
    }

    @GetMapping("/lab")
    public LabView lab() {
        return lab.view();
    }
}
