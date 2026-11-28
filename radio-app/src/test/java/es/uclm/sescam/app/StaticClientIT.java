package es.uclm.sescam.app;

import static org.hamcrest.Matchers.containsString;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.content;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.ValueSource;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.test.web.servlet.MockMvc;

/**
 * The client shell (F-3) is served by the whole application with its real security: the static files are public,
 * the JavaScript goes out as JavaScript (ES modules need it) and the API still asks for a session.
 */
@SpringBootTest
@AutoConfigureMockMvc
@ActiveProfiles("test")
class StaticClientIT {

    @Autowired
    private MockMvc mvc;

    @ParameterizedTest
    @ValueSource(strings = {"/", "/index.html", "/app.html", "/css/app.css", "/js/api.js", "/js/menu.js",
            "/js/login.js", "/js/app.js", "/cu-18/index.html", "/cu-18/cu-18.js", "/cu-23/index.html",
            "/cu-23/cu-23.js"})
    void theClientFilesArePublic(String path) throws Exception {
        mvc.perform(get(path)).andExpect(status().isOk());
    }

    @ParameterizedTest
    @ValueSource(strings = {"/js/api.js", "/js/menu.js", "/js/login.js", "/js/app.js"})
    void theScriptsAreServedAsJavaScript(String path) throws Exception {
        mvc.perform(get(path)).andExpect(content().contentTypeCompatibleWith("text/javascript"));
    }

    @Test
    void theLoginPageCallsTheLoginScript() throws Exception {
        mvc.perform(get("/index.html"))
                .andExpect(content().string(containsString("src=\"js/login.js\"")))
                .andExpect(content().string(containsString("id=\"login-user\"")));
    }

    @Test
    void theApiStillNeedsASession() throws Exception {
        mvc.perform(get("/api/auth/me"))
                .andExpect(status().isUnauthorized())
                .andExpect(jsonPath("$.error").value("unauthenticated"));
    }
}
