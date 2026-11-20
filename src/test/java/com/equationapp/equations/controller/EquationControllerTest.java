package com.equationapp.equations.controller;

import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.http.MediaType;
import org.springframework.test.annotation.DirtiesContext;
import org.springframework.test.web.servlet.MockMvc;

import static org.hamcrest.Matchers.*;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*;

@SpringBootTest
@AutoConfigureMockMvc
@DirtiesContext(classMode = DirtiesContext.ClassMode.BEFORE_EACH_TEST_METHOD)
class EquationControllerTest {

    @Autowired
    MockMvc mvc;

    private void store(String equation) throws Exception {
        mvc.perform(post("/api/equations/store").contentType(MediaType.APPLICATION_JSON)
                        .content("{\"equation\": \"" + equation + "\"}"))
                .andExpect(status().isCreated());
    }

    @Test
    void storeReturnsCreatedWithIdAndLocation() throws Exception {
        mvc.perform(post("/api/equations/store").contentType(MediaType.APPLICATION_JSON)
                        .content("{\"equation\": \"3x + 2y - z\"}"))
                .andExpect(status().isCreated())
                .andExpect(header().string("Location", endsWith("/api/equations/1")))
                .andExpect(jsonPath("$.message").value("Equation stored successfully"))
                .andExpect(jsonPath("$.equationId").value("1"))
                .andExpect(jsonPath("$.postfix").value("3 x * 2 y * + z -"));
    }

    @Test
    void listReturnsReconstructedInfix() throws Exception {
        store("3x + 2y - z");
        store("x^2+y^2-4");
        mvc.perform(get("/api/equations"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.equations", hasSize(2)))
                .andExpect(jsonPath("$.equations[0].equationId").value("1"))
                .andExpect(jsonPath("$.equations[0].equation").value("3x + 2y - z"))
                .andExpect(jsonPath("$.equations[1].equation").value("x^2 + y^2 - 4"));
    }

    @Test
    void listIsEmptyInitially() throws Exception {
        mvc.perform(get("/api/equations"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.equations", hasSize(0)));
    }

    @Test
    void getSingleEquation() throws Exception {
        store("x + 1");
        mvc.perform(get("/api/equations/1"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.equation").value("x + 1"));
        mvc.perform(get("/api/equations/42")).andExpect(status().isNotFound());
    }

    @Test
    void evaluateReturnsResult() throws Exception {
        store("3x + 2y - z");
        mvc.perform(post("/api/equations/1/evaluate").contentType(MediaType.APPLICATION_JSON)
                        .content("{\"variables\": {\"x\": 2, \"y\": 3, \"z\": 1}}"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.equationId").value("1"))
                .andExpect(jsonPath("$.equation").value("3x + 2y - z"))
                .andExpect(jsonPath("$.variables.x").value(2))
                .andExpect(jsonPath("$.result").value(11));
    }

    @Test
    void evaluateSupportsDecimals() throws Exception {
        store("x / 4");
        mvc.perform(post("/api/equations/1/evaluate").contentType(MediaType.APPLICATION_JSON)
                        .content("{\"variables\": {\"x\": 1}}"))
                .andExpect(jsonPath("$.result").value(0.25));
    }

    @Test
    void evaluateWithoutVariablesWorksForConstantEquations() throws Exception {
        store("2 + 3 * 4");
        mvc.perform(post("/api/equations/1/evaluate").contentType(MediaType.APPLICATION_JSON).content("{}"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.result").value(14));
        mvc.perform(post("/api/equations/1/evaluate"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.result").value(14));
    }

    @Test
    void invalidEquationIs400WithMessage() throws Exception {
        mvc.perform(post("/api/equations/store").contentType(MediaType.APPLICATION_JSON)
                        .content("{\"equation\": \"3 + * 4\"}"))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.status").value(400))
                .andExpect(jsonPath("$.error").value("Invalid equation"))
                .andExpect(jsonPath("$.message", containsString("missing a left operand")));
    }

    @Test
    void blankOrMissingEquationIs400() throws Exception {
        mvc.perform(post("/api/equations/store").contentType(MediaType.APPLICATION_JSON).content("{\"equation\": \"  \"}"))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.error").value("Validation failed"))
                .andExpect(jsonPath("$.details[0]").value("equation must not be blank"));
        mvc.perform(post("/api/equations/store").contentType(MediaType.APPLICATION_JSON).content("{}"))
                .andExpect(status().isBadRequest());
    }

    @Test
    void malformedJsonIs400() throws Exception {
        mvc.perform(post("/api/equations/store").contentType(MediaType.APPLICATION_JSON).content("{not json"))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.error").value("Malformed request"));
        mvc.perform(post("/api/equations/store").contentType(MediaType.APPLICATION_JSON))
                .andExpect(status().isBadRequest());
    }

    @Test
    void missingVariablesIs422ListingThem() throws Exception {
        store("x + y + z");
        mvc.perform(post("/api/equations/1/evaluate").contentType(MediaType.APPLICATION_JSON)
                        .content("{\"variables\": {\"y\": 1}}"))
                .andExpect(status().isUnprocessableEntity())
                .andExpect(jsonPath("$.message", containsString("x, z")))
                .andExpect(jsonPath("$.details", contains("x", "z")));
    }

    @Test
    void nonNumericOrNullVariableValueIs400() throws Exception {
        store("x");
        mvc.perform(post("/api/equations/1/evaluate").contentType(MediaType.APPLICATION_JSON)
                        .content("{\"variables\": {\"x\": \"abc\"}}"))
                .andExpect(status().isBadRequest());
        mvc.perform(post("/api/equations/1/evaluate").contentType(MediaType.APPLICATION_JSON)
                        .content("{\"variables\": {\"x\": null}}"))
                .andExpect(status().isBadRequest());
    }

    @Test
    void divisionByZeroIs422() throws Exception {
        store("1 / x");
        mvc.perform(post("/api/equations/1/evaluate").contentType(MediaType.APPLICATION_JSON)
                        .content("{\"variables\": {\"x\": 0}}"))
                .andExpect(status().isUnprocessableEntity())
                .andExpect(jsonPath("$.message", containsString("Division by zero")));
    }

    @Test
    void evaluatingUnknownEquationIs404() throws Exception {
        mvc.perform(post("/api/equations/7/evaluate").contentType(MediaType.APPLICATION_JSON).content("{}"))
                .andExpect(status().isNotFound())
                .andExpect(jsonPath("$.message", containsString("'7'")));
    }

    @Test
    void wrongMethodAndUnknownPathHandled() throws Exception {
        mvc.perform(get("/api/nope")).andExpect(status().isNotFound());
        mvc.perform(org.springframework.test.web.servlet.request.MockMvcRequestBuilders.delete("/api/equations"))
                .andExpect(status().isMethodNotAllowed())
                .andExpect(jsonPath("$.error").value("Method not allowed"));
    }

    @Test
    void deleteReturns204ThenEquationIsGone() throws Exception {
        store("x + 1");
        store("x + 2");

        mvc.perform(org.springframework.test.web.servlet.request.MockMvcRequestBuilders.delete("/api/equations/1"))
                .andExpect(status().isNoContent());

        mvc.perform(get("/api/equations/1")).andExpect(status().isNotFound());
        mvc.perform(get("/api/equations"))
                .andExpect(jsonPath("$.equations", hasSize(1)))
                .andExpect(jsonPath("$.equations[0].equationId").value("2"));
    }

    @Test
    void deletingUnknownEquationIs404() throws Exception {
        mvc.perform(org.springframework.test.web.servlet.request.MockMvcRequestBuilders.delete("/api/equations/5"))
                .andExpect(status().isNotFound())
                .andExpect(jsonPath("$.message", containsString("'5'")));
    }

    @Test
    void evaluateHidesFloatingPointNoise() throws Exception {
        store("0.1 + 0.2");
        mvc.perform(post("/api/equations/1/evaluate").contentType(MediaType.APPLICATION_JSON).content("{}"))
                .andExpect(jsonPath("$.result").value(0.3));
    }

    @Test
    void storesEquationsPastedWithSymbols() throws Exception {
        mvc.perform(post("/api/equations/store").contentType(MediaType.APPLICATION_JSON)
                        .content("{\"equation\": \"x² + y² − 4\"}"))
                .andExpect(status().isCreated())
                .andExpect(jsonPath("$.postfix").value("x 2 ^ y 2 ^ + 4 -"));
        mvc.perform(get("/api/equations/1"))
                .andExpect(jsonPath("$.equation").value("x^2 + y^2 - 4"));
    }

    @Test
    void openApiSpecDocumentsAllEndpoints() throws Exception {
        mvc.perform(get("/v3/api-docs"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.paths['/api/equations/store']").exists())
                .andExpect(jsonPath("$.paths['/api/equations']").exists())
                .andExpect(jsonPath("$.paths['/api/equations/{equationId}/evaluate']").exists());
    }

    @Test
    void healthEndpointIsUp() throws Exception {
        mvc.perform(get("/actuator/health"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.status").value("UP"));
    }
}
