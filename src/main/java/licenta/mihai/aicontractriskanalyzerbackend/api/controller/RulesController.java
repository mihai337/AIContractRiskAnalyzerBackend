package licenta.mihai.aicontractriskanalyzerbackend.api.controller;

import jakarta.validation.Valid;
import java.util.List;
import lombok.RequiredArgsConstructor;
import licenta.mihai.aicontractriskanalyzerbackend.api.dto.CustomRuleDto;
import licenta.mihai.aicontractriskanalyzerbackend.api.dto.OkResponseDto;
import licenta.mihai.aicontractriskanalyzerbackend.api.dto.RuleEnabledRequestDto;
import licenta.mihai.aicontractriskanalyzerbackend.api.mapper.ApiMapper;
import licenta.mihai.aicontractriskanalyzerbackend.application.service.RuleService;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/v1/rules")
@RequiredArgsConstructor
public class RulesController {

    private final RuleService ruleService;
    private final ApiMapper apiMapper;


    @GetMapping
    public List<CustomRuleDto> listRules() {
        return ruleService.listRules().stream().map(apiMapper::toCustomRuleDto).toList();
    }

    @PostMapping("/{ruleId}")
    public OkResponseDto upsertRule(@PathVariable String ruleId, @Valid @RequestBody CustomRuleDto request) {
        if (!ruleId.equals(request.id())) {
            throw new IllegalArgumentException("Path ruleId and payload id must match");
        }
        ruleService.upsert(apiMapper.toCustomRuleEntity(request));
        return new OkResponseDto(true);
    }

    @PostMapping("/{ruleId}/enabled")
    public OkResponseDto updateRuleEnabled(@PathVariable String ruleId, @RequestBody RuleEnabledRequestDto request) {
        ruleService.setEnabled(ruleId, request.enabled());
        return new OkResponseDto(true);
    }
}

