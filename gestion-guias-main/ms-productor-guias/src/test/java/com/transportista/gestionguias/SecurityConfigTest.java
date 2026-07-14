package com.transportista.gestionguias;

import com.transportista.gestionguias.config.SecurityConfig;
import com.transportista.gestionguias.controller.GuiaDespachoController;
import com.transportista.gestionguias.service.GuiaDespachoService;
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

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@WebMvcTest(GuiaDespachoController.class)
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
    private GuiaDespachoService service;

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
        mockMvc.perform(get("/api/guias"))
                .andExpect(status().isUnauthorized());
    }

    @Test
    void descargaGuiasNoPuedeAccederAlProductor() throws Exception {
        mockMvc.perform(get("/api/guias")
                        .header(HttpHeaders.AUTHORIZATION, "Bearer descarga-token"))
                .andExpect(status().isForbidden());
    }

    @Test
    void gestorGuiasPuedeConsultarSolicitudes() throws Exception {
        when(service.listarGuias()).thenReturn(List.of());

        mockMvc.perform(get("/api/guias")
                        .header(HttpHeaders.AUTHORIZATION, "Bearer gestor-token"))
                .andExpect(status().isOk());
    }

    @Test
    void gestorGuiasPuedeCrearSolicitudes() throws Exception {
        mockMvc.perform(post("/api/guias")
                        .header(HttpHeaders.AUTHORIZATION, "Bearer gestor-token")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("""
                                {
                                  "transportista": "Transporte Sur",
                                  "cliente": "Cliente Uno",
                                  "direccionDestino": "Calle Principal 123"
                                }
                                """))
                .andExpect(status().isAccepted());

        verify(service).crearGuia(any());
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
