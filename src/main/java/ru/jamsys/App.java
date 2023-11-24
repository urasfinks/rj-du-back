package ru.jamsys;

import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.context.ConfigurableApplicationContext;
import org.springframework.context.annotation.PropertySource;
import ru.jamsys.component.JdbcTemplate;
import ru.jamsys.component.JsonSchema;
import ru.jamsys.component.Security;
import ru.jamsys.jdbc.template.TemplateEnum;
import ru.jamsys.mistercraft.EMail;
import ru.jamsys.mistercraft.socket.RequestMessageReader;
import ru.jamsys.pool.PostgreSQL;

import java.util.List;
import java.util.Map;

@PropertySource("project/watch-ready/application-local-without-ssl.properties")
//@PropertySource("project/watch-ready/application-docker.properties")

//@PropertySource("project/5gm/application-docker.properties")
//@PropertySource("project/5gm/application-local.properties")
//@PropertySource("project/5gm/application-local-without-ssl.properties")
@SpringBootApplication
public class App {

    public static String postgresqlPoolName = "First";
    public static ConfigurableApplicationContext context;
    public static JdbcTemplate jdbcTemplate;
    private static Security security;
    private static String schemaSocketResponse;

    public static EMail eMail;
    public static JsonSchema jsonSchema;

    public static void main(String[] args) throws Exception {
        context = SpringApplication.run(App.class, args);
        initSecurity();
        initPostgreSQL();
        eMail = App.context.getBean(EMail.class);
        schemaSocketResponse = UtilFileResource.getAsString("schema/socket/ProtocolResponse.json");
        jsonSchema = App.context.getBean(JsonSchema.class);
        App.context.getBean(RequestMessageReader.class).init();

        //AudioInsert.parse();

    }

    public static List<Map<String, Object>> query(TemplateEnum templateEnum, Map<String, Object> arguments) throws Exception {
        return jdbcTemplate.execute(App.postgresqlPoolName, templateEnum, arguments);
    }

    private static void initSecurity() throws Exception {
        security = App.context.getBean(Security.class);
        security.init("12345".toCharArray());
    }

    private static void initPostgreSQL() {
        jdbcTemplate = App.context.getBean(JdbcTemplate.class);
        PostgreSQL postgreSQL = new PostgreSQL(postgresqlPoolName, 1, 10, 60000);
        postgreSQL.initial(jdbcTemplate.getUri(), jdbcTemplate.getUser(), security, jdbcTemplate.getSecurityKey());
        context.getBean(JdbcTemplate.class).addPool(postgreSQL);
    }

    public static JsonSchema.Result validateSocketResponse(String data) {
        return jsonSchema.validate(data, schemaSocketResponse);
    }

}
