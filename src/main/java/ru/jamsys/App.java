package ru.jamsys;

import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.context.ConfigurableApplicationContext;
import org.springframework.context.annotation.PropertySource;
import ru.jamsys.component.Broker;
import ru.jamsys.component.JdbcTemplate;
import ru.jamsys.component.Security;
import ru.jamsys.component.ThreadBalancerFactory;
import ru.jamsys.jdbc.template.TemplateEnum;
import ru.jamsys.message.Message;
import ru.jamsys.message.MessageImpl;
import ru.jamsys.mistercraft.socket.InputMessage;
import ru.jamsys.pool.PostgreSQL;
import ru.jamsys.thread.balancer.ThreadBalancerImpl;

import java.util.List;
import java.util.Map;

@PropertySource("application-test.properties")
@SpringBootApplication
public class App {

    public static String postgreSQLPoolName = "First";
    public static ConfigurableApplicationContext context;
    public static JdbcTemplate jdbcTemplate;
    private static Security security;

    public static void main(String[] args) throws Exception {
        context = SpringApplication.run(App.class, args);
        initSecurity();
        initPostgreSQL();
        initSocketInputReader();
    }

    public static List<Map<String, Object>> query(TemplateEnum templateEnum, Map<String, Object> arguments) throws Exception {
        return jdbcTemplate.exec(App.postgreSQLPoolName, templateEnum, arguments);
    }

    private static void initSecurity() throws Exception {
        security = App.context.getBean(Security.class);
        security.init("12345".toCharArray());
    }

    private static void initPostgreSQL() {
        jdbcTemplate = App.context.getBean(JdbcTemplate.class);
        PostgreSQL postgreSQL = new PostgreSQL(postgreSQLPoolName, 1, 10, 60000);
        postgreSQL.initial(jdbcTemplate.getUri(), jdbcTemplate.getUser(), security, jdbcTemplate.getSecurityKey());
        context.getBean(JdbcTemplate.class).addPool(postgreSQL);
    }

    public static String nameSocketInputReader = "SocketInputReader";

    private static void initSocketInputReader() {
        Broker broker = App.context.getBean(Broker.class);
        ThreadBalancerImpl tbSocketInputReader = App.context.getBean(ThreadBalancerFactory.class).create(nameSocketInputReader, 1, 5, 10, 60000, false);
        tbSocketInputReader.setCorrectTimeLag(false);
        tbSocketInputReader.setSupplier(() -> {
            InputMessage inputMessage = broker.pollLast(InputMessage.class);
            if (inputMessage == null) {
                return null;
            }
            Message message = new MessageImpl();
            message.setBody(inputMessage.getBody());
            return message;
        });
        tbSocketInputReader.setConsumer((Message message) -> Util.logConsole(Thread.currentThread(), "Consumer: " + message.getBody()));
    }

}
