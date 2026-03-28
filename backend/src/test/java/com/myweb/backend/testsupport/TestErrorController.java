package com.myweb.backend.testsupport;

import jakarta.validation.constraints.Min;
import org.springframework.validation.annotation.Validated;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

/** 触发校验与绑定异常，供 GlobalExceptionHandler 测试使用。 */
@Validated
@RestController
@RequestMapping("/api/test")
public class TestErrorController {

    @GetMapping("/validation/{id}")
    public String validation(@PathVariable @Min(1) long id) {
        return String.valueOf(id);
    }

    @GetMapping("/error")
    public String error() {
        throw new RuntimeException("boom");
    }
}
