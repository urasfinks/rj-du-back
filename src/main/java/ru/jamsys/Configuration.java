package ru.jamsys;

import jakarta.servlet.MultipartConfigElement;
import lombok.Getter;
import lombok.Setter;
import org.apache.catalina.Context;
import org.apache.catalina.connector.Connector;
import org.apache.tomcat.util.descriptor.web.SecurityCollection;
import org.apache.tomcat.util.descriptor.web.SecurityConstraint;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.boot.web.embedded.tomcat.TomcatServletWebServerFactory;
import org.springframework.boot.web.servlet.MultipartConfigFactory;
import org.springframework.boot.web.servlet.server.ServletWebServerFactory;
import org.springframework.context.annotation.Bean;
import org.springframework.core.io.Resource;
import org.springframework.util.unit.DataSize;
import org.springframework.web.socket.config.annotation.EnableWebSocket;
import org.springframework.web.socket.config.annotation.WebSocketConfigurer;
import org.springframework.web.socket.config.annotation.WebSocketHandlerRegistry;
import ru.jamsys.mistercraft.ControllerWebSocket;

@org.springframework.context.annotation.Configuration
@EnableWebSocket
public class Configuration implements WebSocketConfigurer {

    @Setter
    @Value("${server.ssl.enabled:false}")
    private Boolean serverSslEnabled;

    @Getter
    @Value("${telegram.idChat}")
    public String telegramIdChat;

    @Getter
    @Value("${deeplink.urlSchemes:url-schemes}")
    private String urlSchemes;

    @Getter
    @Value("${deeplink.urlIosAppStore:https://www.apple.com/app-store/}")
    private String urlIosAppStore;

    @Getter
    @Value("classpath:socket.html")
    private Resource socketHtml;

    @Getter
    @Value("classpath:deeplink.html")
    private Resource deeplink;

    @Getter
    @Value("classpath:.well-known/assetlinks.json")
    private Resource assetLinks;

    @Getter
    @Value("classpath:.well-known/apple-app-site-association.json")
    private Resource appleAppSiteAssociation;

    public void registerWebSocketHandlers(WebSocketHandlerRegistry registry) {
        registry.addHandler(new ControllerWebSocket(), "/socket/*");
    }

    private static final int HTTP_PORT = 80;
    private static final int HTTPS_PORT = 443;
    private static final String HTTP = "http";
    private static final String USER_CONSTRAINT = "CONFIDENTIAL";

    @Bean
    public ServletWebServerFactory servletContainer() {
        TomcatServletWebServerFactory tomcat = new TomcatServletWebServerFactory() {
            @Override
            protected void postProcessContext(Context context) {
                if (serverSslEnabled) {
                    SecurityConstraint securityConstraint = new SecurityConstraint();
                    securityConstraint.setUserConstraint(USER_CONSTRAINT);
                    SecurityCollection collection = new SecurityCollection();
                    collection.addPattern("/*");
                    securityConstraint.addCollection(collection);
                    context.addConstraint(securityConstraint);
                }
            }
        };
        if (serverSslEnabled) {
            tomcat.addAdditionalTomcatConnectors(redirectConnector());
        }
        return tomcat;
    }

    private Connector redirectConnector() {
        Connector connector = new Connector(
                TomcatServletWebServerFactory.DEFAULT_PROTOCOL);
        connector.setScheme(HTTP);
        connector.setPort(HTTP_PORT);
        connector.setSecure(false);
        connector.setRedirectPort(HTTPS_PORT);
        connector.setAsyncTimeout(1000);
        return connector;
    }

    @Bean
    MultipartConfigElement multipartConfigElement() {
        MultipartConfigFactory factory = new MultipartConfigFactory();
        factory.setMaxFileSize(DataSize.ofMegabytes(12));
        factory.setMaxRequestSize(DataSize.ofMegabytes(12));
        return factory.createMultipartConfig();
    }
}