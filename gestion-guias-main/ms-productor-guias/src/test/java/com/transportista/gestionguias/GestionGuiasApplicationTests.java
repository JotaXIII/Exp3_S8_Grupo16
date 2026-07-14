package com.transportista.gestionguias;

import org.junit.jupiter.api.Test;
import org.springframework.boot.test.context.SpringBootTest;

@SpringBootTest(properties = "security.oauth2.roles-claim=testRoles")
class GestionGuiasApplicationTests {

	@Test
	void contextLoads() {
	}

}
