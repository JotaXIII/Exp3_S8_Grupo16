package com.transportista.gestionguias;

import com.transportista.gestionguias.config.SecurityConfig;
import com.transportista.gestionguias.controller.GuiaProcesadaController;
import com.transportista.gestionguias.service.GuiaProcesadaService;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.WebMvcTest;
import org.springframework.context.annotation.Import;
import org.springframework.http.HttpHeaders;
import org.springframework.http.MediaType;
import org.springframework.security.oauth2.jwt.Jwt;
import org.springframework.security.oauth2.jwt.JwtDecoder;
import org.springframework.test.context.TestPropertySource;
import org.springframework.test.context.bean.override.mockito.MockitoBean;
import org.springframework.test.web.servlet.MockMvc;

import java.time.Instant;
import java.util.List;

import static org.mockito.Mockito.when;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.content;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@WebMvcTest(GuiaProcesadaController.class)
@Import(SecurityConfig.class)
@TestPropertySource(properties = {
        "security.oauth2.jwk-set-uri=https://example.test/keys",
        "security.oauth2.issuer=https://example.test/issuer",
        "security.oauth2.audience=test-audience",
        "security.oauth2.roles-claim=appRoles"
})
class SecurityConfigTest {

    @Autowired
    private MockMvc mockMvc;

    @MockitoBean
    private GuiaProcesadaService service;

    @MockitoBean
    private JwtDecoder jwtDecoder;

    @BeforeEach
    void setUp() {
        when(jwtDecoder.decode("descarga-token"))
                .thenReturn(jwt("descarga-token", List.of("DESCARGA_GUIAS")));
        when(jwtDecoder.decode("gestor-token"))
                .thenReturn(jwt("gestor-token", List.of("GESTOR_GUIAS")));
    }

    @Test
    void responde401SinAutenticacion() throws Exception {
        mockMvc.perform(get("/api/guias-procesadas"))
                .andExpect(status().isUnauthorized());
    }

    @Test
    void descargaGuiasPuedeDescargar() throws Exception {
        when(service.descargar("GUIA-001")).thenReturn("%PDF-test".getBytes());

        mockMvc.perform(get("/api/guias-procesadas/GUIA-001/descarga")
                        .header(HttpHeaders.AUTHORIZATION, "Bearer descarga-token"))
                .andExpect(status().isOk())
                .andExpect(content().contentType(MediaType.APPLICATION_PDF));
    }

    @Test
    void descargaGuiasNoPuedeConsultarOperacionesAdministrativas() throws Exception {
        mockMvc.perform(get("/api/guias-procesadas")
                        .header(HttpHeaders.AUTHORIZATION, "Bearer descarga-token"))
                .andExpect(status().isForbidden());
    }

    @Test
    void gestorGuiasPuedeConsultar() throws Exception {
        when(service.listarGuias()).thenReturn(List.of());

        mockMvc.perform(get("/api/guias-procesadas")
                        .header(HttpHeaders.AUTHORIZATION, "Bearer gestor-token"))
                .andExpect(status().isOk());
    }

    @Test
    void gestorGuiasTambienPuedeDescargar() throws Exception {
        when(service.descargar("GUIA-001")).thenReturn("%PDF-test".getBytes());

        mockMvc.perform(get("/api/guias-procesadas/GUIA-001/descarga")
                        .header(HttpHeaders.AUTHORIZATION, "Bearer gestor-token"))
                .andExpect(status().isOk());
    }

    private Jwt jwt(String tokenValue, List<String> roles) {
        Instant now = Instant.now();
        return Jwt.withTokenValue(tokenValue)
                .header("alg", "none")
                .subject("usuario-prueba")
                .issuedAt(now)
                .expiresAt(now.plusSeconds(300))
                .claim("appRoles", roles)
                .build();
    }
}
