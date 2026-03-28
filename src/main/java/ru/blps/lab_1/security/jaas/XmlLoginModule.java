package ru.blps.lab_1.security.jaas;

import org.w3c.dom.Document;
import org.w3c.dom.Element;
import org.w3c.dom.NodeList;

import javax.security.auth.Subject;
import javax.security.auth.callback.Callback;
import javax.security.auth.callback.CallbackHandler;
import javax.security.auth.callback.NameCallback;
import javax.security.auth.callback.PasswordCallback;
import javax.security.auth.callback.UnsupportedCallbackException;
import javax.security.auth.login.LoginException;
import javax.security.auth.spi.LoginModule;
import javax.xml.XMLConstants;
import javax.xml.parsers.DocumentBuilderFactory;

import java.io.InputStream;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;

public final class XmlLoginModule implements LoginModule {

    private static final String OPT_USERS_RESOURCE = "usersResource";

    private Subject subject;
    private CallbackHandler callbackHandler;
    private Map<String, ?> options;

    private String username;
    private List<String> roles;

    @Override
    public void initialize(
        Subject subject,
        CallbackHandler callbackHandler,
        Map<String, ?> sharedState,
        Map<String, ?> options
    ) {
        this.subject = subject;
        this.callbackHandler = callbackHandler;
        this.options = options;
    }

    @Override
    public boolean login() throws LoginException {
        if (callbackHandler == null) {
            throw new LoginException("CallbackHandler is null");
        }
        NameCallback nameCallback = new NameCallback("username");
        PasswordCallback passwordCallback = new PasswordCallback("password", false);
        try {
            callbackHandler.handle(new Callback[] { nameCallback, passwordCallback });
        } catch (java.io.IOException | UnsupportedCallbackException e) {
            throw new LoginException(e.getMessage());
        }
        username = nameCallback.getName();
        char[] passwordChars = passwordCallback.getPassword();
        String password = passwordChars == null ? "" : new String(passwordChars);
        passwordCallback.clearPassword();
        if (username == null || username.isBlank()) {
            throw new LoginException("Username is empty");
        }
        try {
            roles = resolveRoles(username, password);
        } catch (LoginException e) {
            throw e;
        } catch (Exception e) {
            throw new LoginException(e.getMessage());
        }
        if (roles.isEmpty()) {
            throw new LoginException("Invalid credentials or no roles");
        }
        return true;
    }

    @Override
    public boolean commit() throws LoginException {
        if (roles == null || roles.isEmpty()) {
            throw new LoginException("Nothing to commit");
        }
        subject.getPrincipals().add(new UserPrincipal(username));
        for (String role : roles) {
            subject.getPrincipals().add(new RolePrincipal(role));
        }
        username = null;
        roles = null;
        return true;
    }

    @Override
    public boolean abort() {
        username = null;
        roles = null;
        return true;
    }

    @Override
    public boolean logout() {
        return true;
    }

    private List<String> resolveRoles(String name, String password) throws Exception {
        Document document = loadDocument();
        NodeList users = document.getElementsByTagName("user");
        for (int i = 0; i < users.getLength(); i++) {
            Element userEl = (Element) users.item(i);
            String u = userEl.getAttribute("username");
            String p = userEl.getAttribute("password");
            if (!name.equals(u)) {
                continue;
            }
            if (!password.equals(p)) {
                throw new LoginException("Invalid password");
            }
            List<String> found = new ArrayList<>();
            NodeList roleNodes = userEl.getElementsByTagName("role");
            for (int r = 0; r < roleNodes.getLength(); r++) {
                String text = roleNodes.item(r).getTextContent();
                if (text != null) {
                    String trimmed = text.trim();
                    if (!trimmed.isEmpty()) {
                        found.add(trimmed);
                    }
                }
            }
            return found;
        }
        throw new LoginException("Unknown user");
    }

    private Document loadDocument() throws Exception {
        String resourcePath = options != null && options.containsKey(OPT_USERS_RESOURCE)
            ? String.valueOf(options.get(OPT_USERS_RESOURCE))
            : "security/users.xml";
        String normalized = resourcePath.startsWith("/") ? resourcePath.substring(1) : resourcePath;
        InputStream in = openClasspathStream(normalized);
        if (in == null) {
            throw new IllegalStateException("Cannot load users XML: " + resourcePath);
        }
        try (InputStream stream = in) {
            DocumentBuilderFactory factory = DocumentBuilderFactory.newInstance();
            factory.setNamespaceAware(true);
            factory.setFeature(XMLConstants.FEATURE_SECURE_PROCESSING, true);
            factory.setExpandEntityReferences(false);
            factory.setXIncludeAware(false);
            return factory.newDocumentBuilder().parse(stream);
        }
    }

    private static InputStream openClasspathStream(String path) {
        InputStream in = XmlLoginModule.class.getResourceAsStream("/" + path);
        if (in != null) {
            return in;
        }
        ClassLoader moduleLoader = XmlLoginModule.class.getClassLoader();
        if (moduleLoader != null) {
            in = moduleLoader.getResourceAsStream(path);
            if (in != null) {
                return in;
            }
        }
        ClassLoader tccl = Thread.currentThread().getContextClassLoader();
        if (tccl != null) {
            in = tccl.getResourceAsStream(path);
            if (in != null) {
                return in;
            }
        }
        ClassLoader system = ClassLoader.getSystemClassLoader();
        if (system != null) {
            return system.getResourceAsStream(path);
        }
        return null;
    }
}
