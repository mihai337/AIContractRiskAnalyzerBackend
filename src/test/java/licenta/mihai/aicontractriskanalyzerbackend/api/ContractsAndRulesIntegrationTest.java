package licenta.mihai.aicontractriskanalyzerbackend.api;

import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.delete;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import java.nio.charset.StandardCharsets;
import java.util.Base64;
import java.util.concurrent.TimeUnit;
import licenta.mihai.aicontractriskanalyzerbackend.AiContractRiskAnalyzerBackendApplication;
import licenta.mihai.aicontractriskanalyzerbackend.TestSecurityConfig;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.http.MediaType;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.MvcResult;
import org.springframework.web.context.WebApplicationContext;

import static org.springframework.security.test.web.servlet.setup.SecurityMockMvcConfigurers.springSecurity;
import static org.springframework.test.web.servlet.setup.MockMvcBuilders.webAppContextSetup;

@SpringBootTest(classes = {AiContractRiskAnalyzerBackendApplication.class, TestSecurityConfig.class})
class ContractsAndRulesIntegrationTest {

    @Autowired
    private WebApplicationContext webApplicationContext;

    private MockMvc mockMvc;
    private String bearerToken;

    @BeforeEach
    void setUp() throws Exception {
        this.mockMvc = webAppContextSetup(webApplicationContext)
            .apply(springSecurity())
            .build();
        this.bearerToken = "Bearer " + TestJwtTokenFactory.validToken();
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
                .header("Authorization", bearerToken)
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
                .header("Authorization", bearerToken)
                .contentType(MediaType.APPLICATION_JSON)
                .content(analyzeBody))
            .andExpect(status().isOk())
            .andExpect(jsonPath("$.status").value("ANALYZED"))
            .andExpect(jsonPath("$.analysis.riskScore.overallScore").isNumber())
            .andExpect(jsonPath("$.analysis.detectedClauses").isArray());
    }

    @Test
    void rulesEndpointsReturnSeededRulesAndAllowToggle() throws Exception {
        MvcResult listResult = mockMvc.perform(get("/v1/rules")
                .header("Authorization", bearerToken))
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
                .header("Authorization", bearerToken)
                .contentType(MediaType.APPLICATION_JSON)
                .content(toggleBody))
            .andExpect(status().isOk())
            .andExpect(jsonPath("$.ok").value(true));
    }

    @Test
    void createAndPollAnalysisJobWorks() throws Exception {
        String sampleText = "This agreement has payment and confidentiality terms.";
        String base64 = Base64.getEncoder().encodeToString(sampleText.getBytes(StandardCharsets.UTF_8));
        String uploadBody = """
            {
              "fileName": "async.txt",
              "sourceUri": "content://local/async.txt",
              "mimeType": "application/pdf",
              "base64Content": "%s"
            }
            """.formatted(base64);

        MvcResult uploadResult = mockMvc.perform(post("/v1/contracts/upload")
                .header("Authorization", bearerToken)
                .contentType(MediaType.APPLICATION_JSON)
                .content(uploadBody))
            .andExpect(status().isOk())
            .andReturn();

        String contractId = JsonTestHelper.readString(uploadResult.getResponse().getContentAsString(), "id");
        String analyzeBody = """
            {
              "selectedRuleIds": []
            }
            """;

        MvcResult createJobResult = mockMvc.perform(post("/v1/contracts/{contractId}/analysis-jobs", contractId)
                .header("Authorization", bearerToken)
                .contentType(MediaType.APPLICATION_JSON)
                .content(analyzeBody))
            .andExpect(status().isAccepted())
            .andReturn();

        String jobId = JsonTestHelper.readString(createJobResult.getResponse().getContentAsString(), "id");

        String statusValue = "PENDING";
        long deadline = System.nanoTime() + TimeUnit.SECONDS.toNanos(15);
        while (!("COMPLETED".equals(statusValue) || "FAILED".equals(statusValue)) && System.nanoTime() < deadline) {
            MvcResult pollResult = mockMvc.perform(get("/v1/contracts/{contractId}/analysis-jobs/{jobId}", contractId, jobId)
                    .header("Authorization", bearerToken))
                .andExpect(status().isOk())
                .andReturn();
            statusValue = JsonTestHelper.readString(pollResult.getResponse().getContentAsString(), "status");
            Thread.sleep(100);
        }

        if (!("COMPLETED".equals(statusValue) || "FAILED".equals(statusValue))) {
            throw new AssertionError("Async analysis job did not complete in time");
        }
    }

    @Test
    void deleteContractRemovesRecord() throws Exception {
        String sampleText = "Delete contract sample.";
        String base64 = Base64.getEncoder().encodeToString(sampleText.getBytes(StandardCharsets.UTF_8));
        String uploadBody = """
            {
              "fileName": "delete.txt",
              "sourceUri": "content://local/delete.txt",
              "mimeType": "application/pdf",
              "base64Content": "%s"
            }
            """.formatted(base64);

        MvcResult uploadResult = mockMvc.perform(post("/v1/contracts/upload")
                .header("Authorization", bearerToken)
                .contentType(MediaType.APPLICATION_JSON)
                .content(uploadBody))
            .andExpect(status().isOk())
            .andReturn();

        String contractId = JsonTestHelper.readString(uploadResult.getResponse().getContentAsString(), "id");

        mockMvc.perform(delete("/v1/contracts/{contractId}", contractId)
                .header("Authorization", bearerToken))
            .andExpect(status().isOk())
            .andExpect(jsonPath("$.ok").value(true));

        mockMvc.perform(get("/v1/contracts/{contractId}", contractId)
                .header("Authorization", bearerToken))
            .andExpect(status().is4xxClientError());
    }

    @Test
    void protectedEndpointsRejectMissingToken() throws Exception {
        mockMvc.perform(get("/v1/contracts"))
            .andExpect(status().is4xxClientError());
    }
}
