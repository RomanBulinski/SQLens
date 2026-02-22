package org.buulean.sqlensbackend.presentation.controller;

import org.springframework.stereotype.Controller;
import org.springframework.web.bind.annotation.RequestMapping;

/**
 * Forwards all non-API, non-static-resource requests to index.html
 * so Angular's client-side router handles the path.
 */
@Controller
public class SpaController {

    @RequestMapping(value = { "/{path:[^\\.]*}", "/**/{path:[^\\.]*}" })
    public String forwardToAngular() {
        return "forward:/index.html";
    }
}
