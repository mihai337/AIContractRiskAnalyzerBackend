package licenta.mihai.aicontractriskanalyzerbackend.api;

import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import java.nio.charset.StandardCharsets;
import java.util.Base64;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.http.MediaType;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.MvcResult;
import org.springframework.web.context.WebApplicationContext;

import static org.springframework.test.web.servlet.setup.MockMvcBuilders.webAppContextSetup;

@SpringBootTest
class ContractsAndRulesIntegrationTest {

    @Autowired
    private WebApplicationContext webApplicationContext;

    private MockMvc mockMvc;

    @BeforeEach
    void setUp() {
        this.mockMvc = webAppContextSetup(webApplicationContext).build();
    }

    @Test
    void uploadAndAnalyzeContractFlowWorks() throws Exception {
        String sampleText = "This agreement includes confidentiality, payment, and termination terms. GDPR applies.";
        String base64 = Base64.getEncoder().encodeToString(sampleText.getBytes(StandardCharsets.UTF_8));

        String uploadBody = """
            {
              "fileName": "msa.txt",
              "sourceUri": "content://local/msa.txt",
              "mimeType": "application/pdf",
              "base64Content": "%s"
            }
            """.formatted(base64);

        MvcResult uploadResult = mockMvc.perform(post("/v1/contracts/upload")
                .contentType(MediaType.APPLICATION_JSON)
                .content(uploadBody))
            .andExpect(status().isOk())
            .andExpect(jsonPath("$.id").isNotEmpty())
            .andExpect(jsonPath("$.status").value("PENDING"))
            .andReturn();

        String contractId = JsonTestHelper.readString(uploadResult.getResponse().getContentAsString(), "id");

        String analyzeBody = """
            {
              "selectedRuleIds": []
            }
            """;

        mockMvc.perform(post("/v1/contracts/{contractId}/analyze", contractId)
                .contentType(MediaType.APPLICATION_JSON)
                .content(analyzeBody))
            .andExpect(status().isOk())
            .andExpect(jsonPath("$.status").value("ANALYZED"))
            .andExpect(jsonPath("$.analysis.riskScore.overallScore").isNumber())
            .andExpect(jsonPath("$.analysis.detectedClauses").isArray());
    }

    @Test
    void rulesEndpointsReturnSeededRulesAndAllowToggle() throws Exception {
        MvcResult listResult = mockMvc.perform(get("/v1/rules"))
            .andExpect(status().isOk())
            .andExpect(jsonPath("$[0].id").isNotEmpty())
            .andReturn();

        String firstRuleId = JsonTestHelper.readString(listResult.getResponse().getContentAsString(), "[0].id");

        String toggleBody = """
            {
              "enabled": false
            }
            """;

        mockMvc.perform(post("/v1/rules/{ruleId}/enabled", firstRuleId)
                .contentType(MediaType.APPLICATION_JSON)
                .content(toggleBody))
            .andExpect(status().isOk())
            .andExpect(jsonPath("$.ok").value(true));
    }
}


