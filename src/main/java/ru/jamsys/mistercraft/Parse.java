package ru.jamsys.mistercraft;

import ru.jamsys.App;
import ru.jamsys.Util;
import ru.jamsys.WrapJsonToObject;
import ru.jamsys.http.HttpClient;
import ru.jamsys.mistercraft.jt.Data;
import ru.jamsys.mistercraft.jt.ParseJt;

import java.util.*;

public class Parse {
    public static void brandId(String brandId) {
        HttpClient httpClient = new HttpClient();
        httpClient.setUrl("https://api.cigar-register.com/api/v1/catalog/lines?filters[brand_id][]=" + brandId);

        WrapJsonToObject<Map> mapWrapJsonToObject = Util.jsonToObject("{\n" +
                "    \"accept\": \"application/json, text/plain, */*\",\n" +
                "    \"accept-language\": \"ru-RU,ru;q=0.9,en-US;q=0.8,en;q=0.7\",\n" +
                "    \"authorization\": \"Bearer 15770|dMvHbAeUsUCWm1Ktzku7hDItuUa4FzyEb55Lh3I7\",\n" +
                "    \"cache-control\": \"no-cache\",\n" +
                "    \"pragma\": \"no-cache\",\n" +
                "    \"sec-ch-ua\": \"\\\"Not_A Brand\\\";v=\\\"99\\\", \\\"Google Chrome\\\";v=\\\"109\\\", \\\"Chromium\\\";v=\\\"109\\\"\",\n" +
                "    \"sec-ch-ua-mobile\": \"?0\",\n" +
                "    \"sec-ch-ua-platform\": \"\\\"macOS\\\"\",\n" +
                "    \"sec-fetch-dest\": \"empty\",\n" +
                "    \"sec-fetch-mode\": \"cors\",\n" +
                "    \"sec-fetch-site\": \"same-site\",\n" +
                "    \"Referer\": \"https://www.cigar-register.com/\",\n" +
                "    \"Referrer-Policy\": \"strict-origin-when-cross-origin\"\n" +
                "  }", Map.class);
        for (Object key : mapWrapJsonToObject.getObject().keySet()) {
            httpClient.setRequestHeader(key.toString(), mapWrapJsonToObject.getObject().get(key).toString());
        }
        httpClient.exec();
        String resp = new String(httpClient.getResponse());
        System.out.println(resp);
    }

    public static void lineId(String brandId, String lineId) {
        HttpClient httpClient = new HttpClient();
        httpClient.setUrl("https://api.cigar-register.com/api/v1/catalog/cards?filters[brand_id][]=" + brandId + "&filters[line_id][]=" + lineId);

        WrapJsonToObject<Map> mapWrapJsonToObject = Util.jsonToObject("{\n" +
                "    \"accept\": \"application/json, text/plain, */*\",\n" +
                "    \"accept-language\": \"ru-RU,ru;q=0.9,en-US;q=0.8,en;q=0.7\",\n" +
                "    \"authorization\": \"Bearer 15770|dMvHbAeUsUCWm1Ktzku7hDItuUa4FzyEb55Lh3I7\",\n" +
                "    \"cache-control\": \"no-cache\",\n" +
                "    \"pragma\": \"no-cache\",\n" +
                "    \"sec-ch-ua\": \"\\\"Not_A Brand\\\";v=\\\"99\\\", \\\"Google Chrome\\\";v=\\\"109\\\", \\\"Chromium\\\";v=\\\"109\\\"\",\n" +
                "    \"sec-ch-ua-mobile\": \"?0\",\n" +
                "    \"sec-ch-ua-platform\": \"\\\"macOS\\\"\",\n" +
                "    \"sec-fetch-dest\": \"empty\",\n" +
                "    \"sec-fetch-mode\": \"cors\",\n" +
                "    \"sec-fetch-site\": \"same-site\",\n" +
                "    \"Referer\": \"https://www.cigar-register.com/\",\n" +
                "    \"Referrer-Policy\": \"strict-origin-when-cross-origin\"\n" +
                "  }", Map.class);
        for (Object key : mapWrapJsonToObject.getObject().keySet()) {
            httpClient.setRequestHeader(key.toString(), mapWrapJsonToObject.getObject().get(key).toString());
        }
        httpClient.exec();
        String resp = new String(httpClient.getResponse());
        System.out.println(resp);
    }

    public static void cardId(int cardId) throws Exception {
        HttpClient httpClient = new HttpClient();
        httpClient.setUrl("https://api.cigar-register.com/api/v1/catalog/card/" + cardId);

        WrapJsonToObject<Map> mapWrapJsonToObject = Util.jsonToObject("{\n" +
                "    \"accept\": \"application/json, text/plain, */*\",\n" +
                "    \"accept-language\": \"ru-RU,ru;q=0.9,en-US;q=0.8,en;q=0.7\",\n" +
                "    \"authorization\": \"Bearer 15770|dMvHbAeUsUCWm1Ktzku7hDItuUa4FzyEb55Lh3I7\",\n" +
                "    \"cache-control\": \"no-cache\",\n" +
                "    \"pragma\": \"no-cache\",\n" +
                "    \"sec-ch-ua\": \"\\\"Not_A Brand\\\";v=\\\"99\\\", \\\"Google Chrome\\\";v=\\\"109\\\", \\\"Chromium\\\";v=\\\"109\\\"\",\n" +
                "    \"sec-ch-ua-mobile\": \"?0\",\n" +
                "    \"sec-ch-ua-platform\": \"\\\"macOS\\\"\",\n" +
                "    \"sec-fetch-dest\": \"empty\",\n" +
                "    \"sec-fetch-mode\": \"cors\",\n" +
                "    \"sec-fetch-site\": \"same-site\",\n" +
                "    \"Referer\": \"https://www.cigar-register.com/\",\n" +
                "    \"Referrer-Policy\": \"strict-origin-when-cross-origin\"\n" +
                "  }", Map.class);
        for (Object key : mapWrapJsonToObject.getObject().keySet()) {
            httpClient.setRequestHeader(key.toString(), mapWrapJsonToObject.getObject().get(key).toString());
        }
        httpClient.exec();

        if (httpClient.getStatus() == 200) {
            System.out.println(cardId);
            String resp = new String(httpClient.getResponse());
            Map<String, Object> arguments = App.jdbcTemplate.createArguments();
            arguments.put("id", cardId);
            arguments.put("data", resp);
            App.jdbcTemplate.exec(App.postgreSQLPoolName, ParseJt.INSERT, arguments);
        } else {
            System.out.println("ERROR");
        }

    }

    public static String decodeHtml(String data) {
        return org.jsoup.parser.Parser.unescapeEntities(data, true);
    }

    public static void prepare() throws Exception {
        List<Map<String, Object>> exec = App.jdbcTemplate.exec(App.postgreSQLPoolName, ParseJt.SELECT, App.jdbcTemplate.createArguments());
        System.out.println("All: " + exec.size());
        for (Map<String, Object> item : exec) {
            WrapJsonToObject<Map> data = Util.jsonToObject((String) item.get("data"), Map.class);
            if (data.getException() == null) {
                Map<String, Object> o = (Map<String, Object>) data.getObject().get("data");
                Map<String, String> newData = new LinkedHashMap<>();

                newData.put("brand", decodeHtml(o.get("brand").toString()));
                newData.put("line", decodeHtml(o.get("line").toString()));
                newData.put("shape", decodeHtml(o.get("shape").toString()));
                newData.put("format", decodeHtml(o.get("v_d_g").toString()));
                newData.put("size", decodeHtml(o.get("size").toString()));
                newData.put("binder", decodeHtml(o.get("tobacco_binder").toString()));
                newData.put("filter", decodeHtml(o.get("tobacco_filter").toString()));
                newData.put("wrapper", decodeHtml(o.get("tobacco_wrapper").toString()));
                newData.put("country", decodeHtml(((Map) o.get("country")).get("name").toString()));
                newData.put("info", decodeHtml(o.get("pack").toString()));

                Map<String, Object> arguments = App.jdbcTemplate.createArguments();
                arguments.put("uuid_data", java.util.UUID.randomUUID().toString());
                arguments.put("value_data", Util.jsonObjectToString(newData));
                arguments.put("type_data", DataType.systemData);
                arguments.put("parent_uuid_data", null);
                arguments.put("id_user", 1);
                arguments.put("key_data", "cigar");

                App.jdbcTemplate.exec(App.postgreSQLPoolName, Data.INSERT, arguments);
            }
        }
        System.out.println("Finish");
    }

    public static void parse() throws Exception {
        for (int i = 1; i <= 14226; i++) {
            cardId(i);
            Thread.sleep(200);
        }
    }
}
