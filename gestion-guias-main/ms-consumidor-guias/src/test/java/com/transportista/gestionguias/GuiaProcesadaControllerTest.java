package com.transportista.gestionguias;

import com.transportista.gestionguias.controller.GuiaProcesadaController;
import com.transportista.gestionguias.dto.GuiaProcesadaResponse;
import com.transportista.gestionguias.exception.GlobalExceptionHandler;
import com.transportista.gestionguias.service.GuiaProcesadaService;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.http.MediaType;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.setup.MockMvcBuilders;

import java.time.LocalDate;
import java.util.List;

import static org.hamcrest.Matchers.hasItem;
import static org.hamcrest.Matchers.startsWith;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.verifyNoInteractions;
import static org.mockito.Mockito.when;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.delete;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.put;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.content;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.header;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@ExtendWith(MockitoExtension.class)
class GuiaProcesadaControllerTest {

    @Mock
    private GuiaProcesadaService service;

    private MockMvc mockMvc;

    @BeforeEach
    void setUp() {
        mockMvc = MockMvcBuilders
                .standaloneSetup(new GuiaProcesadaController(service))
                .setControllerAdvice(new GlobalExceptionHandler())
                .build();
    }

    @Test
    void listaGuiasProcesadas() throws Exception {
        GuiaProcesadaResponse response = new GuiaProcesadaResponse();
        response.setNumeroGuia("GUIA-001");
        when(service.listarGuias()).thenReturn(List.of(response));

        mockMvc.perform(get("/api/guias-procesadas"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$[0].numeroGuia").value("GUIA-001"));
    }

    @Test
    void buscaPorTransportistaYFecha() throws Exception {
        when(service.buscar("Transporte Sur", LocalDate.of(2026, 7, 13)))
                .thenReturn(List.of());

        mockMvc.perform(get("/api/guias-procesadas/buscar")
                        .param("transportista", "Transporte Sur")
                        .param("fecha", "2026-07-13"))
                .andExpect(status().isOk())
                .andExpect(content().json("[]"));

        verify(service).buscar("Transporte Sur", LocalDate.of(2026, 7, 13));
    }

    @Test
    void descargaPdfDesdeElServicio() throws Exception {
        byte[] pdf = "%PDF-test".getBytes();
        when(service.descargar("GUIA-001")).thenReturn(pdf);

        mockMvc.perform(get("/api/guias-procesadas/GUIA-001/descarga"))
                .andExpect(status().isOk())
                .andExpect(content().contentType(MediaType.APPLICATION_PDF))
                .andExpect(header().string("Content-Disposition", "attachment; filename=\"GUIA-001.pdf\""))
                .andExpect(content().bytes(pdf));
    }

    @Test
    void actualizaUnaGuiaConDatosValidos() throws Exception {
        GuiaProcesadaResponse response = new GuiaProcesadaResponse();
        response.setNumeroGuia("GUIA-001");
        when(service.actualizar(org.mockito.ArgumentMatchers.eq("GUIA-001"), any()))
                .thenReturn(response);

        mockMvc.perform(put("/api/guias-procesadas/GUIA-001")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("""
                                {
                                  "transportista": "Transporte Sur",
                                  "cliente": "Cliente Uno",
                                  "direccionDestino": "Calle Principal 123"
                                }
                                """))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.numeroGuia").value("GUIA-001"));

        verify(service).actualizar(org.mockito.ArgumentMatchers.eq("GUIA-001"), any());
    }

    @Test
    void rechazaActualizacionConDatosInvalidos() throws Exception {
        mockMvc.perform(put("/api/guias-procesadas/GUIA-001")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("""
                                {
                                  "transportista": "",
                                  "cliente": "",
                                  "direccionDestino": ""
                                }
                """))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.detalles", hasItem(startsWith("transportista:"))))
                .andExpect(jsonPath("$.detalles", hasItem(startsWith("cliente:"))))
                .andExpect(jsonPath("$.detalles", hasItem(startsWith("direccionDestino:"))));

        verifyNoInteractions(service);
    }

    @Test
    void eliminaUnaGuiaProcesada() throws Exception {
        mockMvc.perform(delete("/api/guias-procesadas/GUIA-001"))
                .andExpect(status().isNoContent());

        verify(service).eliminar("GUIA-001");
    }
}
