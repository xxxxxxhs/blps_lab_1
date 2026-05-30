package ru.blps.lab_1.config;

import org.camunda.bpm.engine.IdentityService;
import org.camunda.bpm.engine.identity.Group;
import org.camunda.bpm.engine.identity.User;
import org.springframework.boot.ApplicationArguments;
import org.springframework.boot.ApplicationRunner;
import org.springframework.core.io.ClassPathResource;
import org.springframework.stereotype.Component;
import org.w3c.dom.Document;
import org.w3c.dom.Element;
import org.w3c.dom.NodeList;

import javax.xml.XMLConstants;
import javax.xml.parsers.DocumentBuilderFactory;
import java.io.InputStream;
import java.util.ArrayList;
import java.util.List;

@Component
public class CamundaIdentityInitializer implements ApplicationRunner {

    private final IdentityService identityService;

    public CamundaIdentityInitializer(IdentityService identityService) {
        this.identityService = identityService;
    }

    @Override
    public void run(ApplicationArguments args) throws Exception {
        Document doc = loadDocument();
        NodeList userNodes = doc.getElementsByTagName("user");

        for (int i = 0; i < userNodes.getLength(); i++) {
            Element userEl = (Element) userNodes.item(i);
            String username = userEl.getAttribute("username");
            String password = userEl.getAttribute("password");

            List<String> roles = new ArrayList<>();
            NodeList roleNodes = userEl.getElementsByTagName("role");
            for (int r = 0; r < roleNodes.getLength(); r++) {
                String role = roleNodes.item(r).getTextContent().trim();
                if (!role.isEmpty()) {
                    roles.add(role);
                }
            }

            ensureGroupExists(roles.isEmpty() ? "CLIENT" : roles.get(0));
            ensureUserExists(username, password, roles.isEmpty() ? "CLIENT" : roles.get(0));
        }
    }

    private void ensureGroupExists(String groupId) {
        boolean exists = !identityService.createGroupQuery().groupId(groupId).list().isEmpty();
        if (!exists) {
            Group group = identityService.newGroup(groupId);
            group.setName(groupId);
            group.setType("WORKFLOW");
            identityService.saveGroup(group);
        }
    }

    private void ensureUserExists(String userId, String password, String groupId) {
        boolean exists = !identityService.createUserQuery().userId(userId).list().isEmpty();
        if (!exists) {
            User user = identityService.newUser(userId);
            user.setFirstName(userId);
            user.setPassword(password);
            identityService.saveUser(user);
        }
        boolean member = !identityService.createUserQuery()
            .userId(userId)
            .memberOfGroup(groupId)
            .list()
            .isEmpty();
        if (!member) {
            identityService.createMembership(userId, groupId);
        }
    }

    private Document loadDocument() throws Exception {
        try (InputStream in = new ClassPathResource("security/users.xml").getInputStream()) {
            DocumentBuilderFactory factory = DocumentBuilderFactory.newInstance();
            factory.setFeature(XMLConstants.FEATURE_SECURE_PROCESSING, true);
            factory.setExpandEntityReferences(false);
            return factory.newDocumentBuilder().parse(in);
        }
    }
}
