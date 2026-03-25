package io.github.choseongah.ssh.shell.auth;

import org.junit.jupiter.api.Test;
import org.springframework.core.io.ClassPathResource;

import java.io.File;

import static org.junit.jupiter.api.Assertions.*;

class SshShellPublicKeyAuthenticationProviderTest {

    private SshShellPublicKeyAuthenticationProvider pubKeyAuthProv;

    @Test
    void testClasspathFile() throws Exception {
        File file = new ClassPathResource(".ssh/authorized.keys").getFile();
        assertTrue(file.exists());
        internalTest(file);
    }

    @Test
    void testSpringClasspathResource() throws Exception {
        ClassPathResource resource = new ClassPathResource(".ssh/authorized.keys");
        assertTrue(resource.exists());
        internalTest(resource.getFile());
    }

    @Test
    void testNotExisting() throws Exception {
        pubKeyAuthProv = new SshShellPublicKeyAuthenticationProvider(new File("not-existing"));
        assertFalse(pubKeyAuthProv.exists());
        assertEquals(-1, pubKeyAuthProv.size());
    }

    private void internalTest(File file) throws Exception {
        pubKeyAuthProv = new SshShellPublicKeyAuthenticationProvider(file);
        assertTrue(pubKeyAuthProv.exists());
        assertTrue(pubKeyAuthProv.size() > 0);
    }

}
