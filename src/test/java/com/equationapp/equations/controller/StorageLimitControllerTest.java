package com.equationapp.equations.controller;

import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.http.MediaType;
import org.springframework.test.web.servlet.MockMvc;

import static org.hamcrest.Matchers.containsString;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.delete;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

/** Runs the whole app with a tiny storage limit to check the 409 response end to end. */
@SpringBootTest(properties = "equations.max-stored=2")
@AutoConfigureMockMvc
class StorageLimitControllerTest {

    @Autowired
    MockMvc mvc;

    private org.springframework.test.web.servlet.ResultActions store(String equation) throws Exception {
        return mvc.perform(post("/api/equations/store").contentType(MediaType.APPLICATION_JSON)
                .content("{\"equation\": \"" + equation + "\"}"));
    }

    @Test
    void thirdEquationIsRejectedWith409UntilOneIsDeleted() throws Exception {
        store("a").andExpect(status().isCreated());
        store("b").andExpect(status().isCreated());

        store("c").andExpect(status().isConflict())
                .andExpect(jsonPath("$.status").value(409))
                .andExpect(jsonPath("$.message", containsString("max 2")));

        mvc.perform(delete("/api/equations/1")).andExpect(status().isNoContent());
        store("c").andExpect(status().isCreated());
    }
}
