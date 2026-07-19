package com.transportista.gestionguias;

import com.transportista.gestionguias.config.SecurityConfig;
import com.transportista.gestionguias.controller.GuiaProcesadaController;
import com.transportista.gestionguias.dto.GuiaProcesadaResponse;
import com.transportista.gestionguias.service.GuiaProcesadaService;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.WebMvcTest;
import org.springframework.context.annotation.Import;
import org.springframework.http.HttpHeaders;
import org.springframework.http.MediaType;
import org.springframework.security.oauth2.jwt.BadJwtException;
import org.springframework.security.oauth2.jwt.Jwt;
import org.springframework.security.oauth2.jwt.JwtDecoder;
import org.springframework.test.context.TestPropertySource;
import org.springframework.test.context.bean.override.mockito.MockitoBean;
import org.springframework.test.web.servlet.MockMvc;

import java.time.Instant;
import java.util.List;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.delete;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.put;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.content;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@WebMvcTest(GuiaProcesadaController.class)
@Import(SecurityConfig.class)
@TestPropertySource(properties = {
        "security.oauth2.jwk-set-uri=https://example.test/keys",
        "security.oauth2.issuer-uri=https://example.test/issuer",
        "security.oauth2.audience=test-audience",
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
        when(jwtDecoder.decode("gestor-token"))
                .thenReturn(jwt("gestor-token", "GESTOR_GUIAS"));
        when(jwtDecoder.decode("otro-scope-token"))
                .thenReturn(jwt("otro-scope-token", "OTRO_SCOPE"));
        when(jwtDecoder.decode("sin-scope-token"))
                .thenReturn(jwt("sin-scope-token", null));
        when(jwtDecoder.decode("token-invalido"))
                .thenThrow(new BadJwtException("Token invalido para la prueba"));
    }

    @Test
    void responde401SinAutenticacion() throws Exception {
        mockMvc.perform(get("/api/guias-procesadas"))
                .andExpect(status().isUnauthorized());
    }

    @Test
    void responde401ConTokenInvalido() throws Exception {
        mockMvc.perform(get("/api/guias-procesadas")
                        .header(HttpHeaders.AUTHORIZATION, "Bearer token-invalido"))
                .andExpect(status().isUnauthorized());
    }

    @Test
    void responde403ConTokenSinScp() throws Exception {
        mockMvc.perform(get("/api/guias-procesadas")
                        .header(HttpHeaders.AUTHORIZATION, "Bearer sin-scope-token"))
                .andExpect(status().isForbidden());
    }

    @Test
    void responde403ConScopeDistinto() throws Exception {
        mockMvc.perform(get("/api/guias-procesadas")
                        .header(HttpHeaders.AUTHORIZATION, "Bearer otro-scope-token"))
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
    void scopeGestorGuiasPermiteConsultarPorNumero() throws Exception {
        when(service.obtenerPorNumero("GUIA-001")).thenReturn(new GuiaProcesadaResponse());

        mockMvc.perform(get("/api/guias-procesadas/GUIA-001")
                        .header(HttpHeaders.AUTHORIZATION, "Bearer gestor-token"))
                .andExpect(status().isOk());
    }

    @Test
    void scopeGestorGuiasPermiteBuscar() throws Exception {
        when(service.buscar("Transporte Sur", null)).thenReturn(List.of());

        mockMvc.perform(get("/api/guias-procesadas/buscar")
                        .queryParam("transportista", "Transporte Sur")
                        .header(HttpHeaders.AUTHORIZATION, "Bearer gestor-token"))
                .andExpect(status().isOk());
    }

    @Test
    void scopeGestorGuiasPermiteDescargar() throws Exception {
        when(service.descargar("GUIA-001")).thenReturn("%PDF-test".getBytes());

        mockMvc.perform(get("/api/guias-procesadas/GUIA-001/descarga")
                        .header(HttpHeaders.AUTHORIZATION, "Bearer gestor-token"))
                .andExpect(status().isOk())
                .andExpect(content().contentType(MediaType.APPLICATION_PDF));
    }

    @Test
    void scopeGestorGuiasPermiteActualizar() throws Exception {
        when(service.actualizar(eq("GUIA-001"), any())).thenReturn(new GuiaProcesadaResponse());

        mockMvc.perform(put("/api/guias-procesadas/GUIA-001")
                        .header(HttpHeaders.AUTHORIZATION, "Bearer gestor-token")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("""
                                {
                                  "transportista": "Transporte Sur",
                                  "cliente": "Cliente Uno",
                                  "direccionDestino": "Calle Principal 123"
                                }
                                """))
                .andExpect(status().isOk());
    }

    @Test
    void scopeGestorGuiasPermiteEliminar() throws Exception {
        mockMvc.perform(delete("/api/guias-procesadas/GUIA-001")
                        .header(HttpHeaders.AUTHORIZATION, "Bearer gestor-token"))
                .andExpect(status().isNoContent());

        verify(service).eliminar("GUIA-001");
    }

    private Jwt jwt(String tokenValue, String scopes) {
        Instant now = Instant.now();
        Jwt.Builder builder = Jwt.withTokenValue(tokenValue)
                .header("alg", "none")
                .subject("usuario-prueba")
                .issuedAt(now)
                .expiresAt(now.plusSeconds(300));
        if (scopes != null) {
            builder.claim("scp", scopes);
        }
        return builder.build();
    }
}
