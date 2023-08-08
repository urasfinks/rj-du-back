package ru.jamsys.mistercraft;

import org.apache.commons.mail.DefaultAuthenticator;
import org.apache.commons.mail.EmailException;
import org.apache.commons.mail.HtmlEmail;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.core.io.Resource;
import org.springframework.stereotype.Component;
import ru.jamsys.App;
import ru.jamsys.Util;
import ru.jamsys.template.Template;
import ru.jamsys.template.TemplateItem;

import java.util.HashMap;
import java.util.List;
import java.util.Map;

@Component
public class EMail {

    private static final String UTF_8 = "UTF-8";

    @Value("classpath:email.html")
    private Resource emailTemplateCode;
    private List<TemplateItem> emailTemplateCodeParsed;

    public void sendCode(String to, String code) throws EmailException {
        Map<String, String> args = new HashMap<>();
        args.put("code", code);
        if (emailTemplateCodeParsed == null) {
            emailTemplateCodeParsed = Template.getParsedTemplate(Util.getResourceContent(emailTemplateCode, UTF_8));
        }
        HtmlEmail email = new HtmlEmail();
        setting(email);

        email.addTo(to);
        email.setSubject("Ваш код: *****");
        email.setTextMsg("Ваш код: " + code);
        email.setHtmlMsg(Template.template(emailTemplateCodeParsed, args));

        email.send();
    }

    private void setting(HtmlEmail email) throws EmailException {
        email.setHostName("smtp.mail.ru");
        email.setSmtpPort(465);
        email.setAuthenticator(new DefaultAuthenticator("noreply@e-humidor.ru", "Zc28dc0gLYe6e3hc57p3"));
        email.setSSLOnConnect(true);
        email.setFrom("noreply@e-humidor.ru");
        email.setCharset("UTF-8");
        email.setSocketConnectionTimeout(1000);
        email.setSocketTimeout(1000);
    }

    public static void test() throws EmailException {
        EMail bean = App.context.getBean(EMail.class);
        bean.sendCode("urasfinks@gmail.com", "12345");
    }


}
