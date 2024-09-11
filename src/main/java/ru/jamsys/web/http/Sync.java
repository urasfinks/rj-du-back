package ru.jamsys.web.http;

import lombok.Getter;
import lombok.Setter;
import org.springframework.stereotype.Component;
import org.springframework.web.bind.annotation.RequestMapping;
import ru.jamsys.DataType;
import ru.jamsys.core.component.ServicePromise;
import ru.jamsys.core.extension.exception.AuthException;
import ru.jamsys.core.extension.http.ServletHandler;
import ru.jamsys.core.flat.util.Util;
import ru.jamsys.core.flat.util.UtilJson;
import ru.jamsys.core.promise.Promise;
import ru.jamsys.core.promise.PromiseGenerator;
import ru.jamsys.core.resource.jdbc.JdbcRequest;
import ru.jamsys.core.resource.jdbc.JdbcResource;
import ru.jamsys.core.web.http.HttpHandler;
import ru.jamsys.jt.Data;
import ru.jamsys.jt.DataByParent;
import ru.jamsys.promise.PromiseExtension;
import ru.jamsys.promise.repository.AuthRepository;
import ru.jamsys.promise.repository.ResponseRepository;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

/*
 * Синхронизация данных на фронте
 * */
@Component
@RequestMapping
public class Sync implements PromiseGenerator, HttpHandler {

    @Getter
    @Setter
    private String index;

    private final ServicePromise servicePromise;

    public Sync(ServicePromise servicePromise) {
        this.servicePromise = servicePromise;
    }

    @Override
    public Promise generate() {
        return servicePromise.get(index, 1000L)
                .extension(PromiseExtension::thenSelectIdUserIfExist)
                .extension(PromiseExtension::addResponseRepository)
                .then("init", (_, promise) -> {

                    ServletHandler servletHandler = promise.getRepositoryMapClass(ServletHandler.class);
                    String data = servletHandler.getRequestReader().getData();

                    promise.setRepositoryMap("parsedJson", UtilJson.getMapOrThrow(data));
                    promise.setRepositoryMap("result", new HashMap<String, Object>());

                })
                .thenWithResource("remove", JdbcResource.class, "default", (_, promise, jdbcResource) -> {
                    AuthRepository authRepository = promise.getRepositoryMapClass(AuthRepository.class);
                    // Для начала удалим данные которые были удалены на фронте
                    // Да, это всё равно пойдёт ревизией обратно, но уже немного подрезанное (зануление value_data)
                    List<String> removed = getUncheckedList(promise, "removed");
                    for (String uuidData : removed) {
                        jdbcResource.execute(new JdbcRequest(Data.REMOVE)
                                .addArg(authRepository.get())
                                .addArg("uuid_data", uuidData));
                    }
                })
                .thenWithResource("getMaxRevisionByType", JdbcResource.class, "default", (_, promise, jdbcResource) -> {
                    // Поиск ревизий стандартно ведётся без учёта ленивых данных
                    // Но иногда фронт явно говорит - мне нужно синхронизировать ленивые данные
                    // К ленивым данным относились музыкальные нарезки к урокам, пока на фронте явно мы не зайдём в урок
                    // файлы с сервера не синхронизируются с фронтом
                    List<String> lazyList = getUncheckedList(promise, "lazy");
                    promise.setRepositoryMap("lazyList", lazyList);

                    Map<String, Long> dbRevisionMap = new HashMap<>();
                    List<Map<String, Object>> exec =
                            lazyList.isEmpty()
                                    ? jdbcResource.execute(new JdbcRequest(Data.SELECT_MAX_REVISION_BY_TYPE)
                                    .addArg(promise.getRepositoryMapClass(AuthRepository.class).get()))
                                    : jdbcResource.execute(new JdbcRequest(Data.SELECT_MAX_REVISION_BY_TYPE_LAZY)
                                    .addArg(promise.getRepositoryMapClass(AuthRepository.class).get())
                                    .addArg("lazy", lazyList));

                    for (Map<String, Object> row : exec) {
                        dbRevisionMap.put((String) row.get("key"), (Long) row.get("max"));
                    }
                    promise.setRepositoryMap("dbRevisionMap", dbRevisionMap);
                })
                .thenWithResource("selectDataResponse", JdbcResource.class, "default", (_, promise, jdbcResource) -> {

                    @SuppressWarnings("unchecked")
                    Map<String, List<Map<String, Object>>> result = promise.getRepositoryMap("result", Map.class);

                    Map<String, Long> needUpgrade = new HashMap<>(); // Это контроль сколько данных на фронте устарело и их надо обновить
                    int totalCounterItem = 0; // Контроль сколько данных будем высылать
                    int totalByte = 0; // Контроль высылаемых данных на фронт


                    @SuppressWarnings("unchecked")
                    Map<String, Object> parsedJson = promise.getRepositoryMap("parsedJson", Map.class);

                    // Последние ревизии на фронте
                    @SuppressWarnings("unchecked")
                    Map<String, Long> rqMaxRevisionByType = (Map<String, Long>) promise.getRepositoryMap("parsedJson", Map.class).get("maxRevisionByType");

                    // Это первая синхронизация после авторизации фронта. Может быть такое, что данные были накопления данных
                    // перед авторизацией
                    boolean authJustNow = parsedJson.containsKey("authJustNow") && (boolean) parsedJson.get("authJustNow");
                    List<String> lazyList = promise.getRepositoryMap("lazyList", List.class);
                    Map<String, Long> dbMaxRevisionByType = promise.getRepositoryMap("dbRevisionMap", Map.class);

                    for (DataType dataType : DataType.values()) {
                        long rqRevision = ((Number) rqMaxRevisionByType.getOrDefault(dataType.toString(), 0L)).longValue();
                        //Бывает такое, что клиент сначала без авторизации что-то создаёт, а потом авторизуется
                        //В этом случае надо клиентские данные начинать сканировать с самого начала
                        if (authJustNow && (dataType.isUserData() || dataType == DataType.socket)) {
                            rqRevision = 0;
                        }
                        long dbRevision = dbMaxRevisionByType.getOrDefault(dataType.toString(), 0L);
                        if (dbRevision > rqRevision) {
                            Map<String, Object> arguments = promise.getRepositoryMapClass(AuthRepository.class).get();
                            arguments.put("type_data", dataType.toString());
                            arguments.put("revision_data", rqRevision);
                            if (!lazyList.isEmpty()) {
                                arguments.put("lazy", lazyList);
                            }
                            List<Map<String, Object>> exec = switch (dataType) {
                                case js, any, systemData, template, json, blob -> lazyList.isEmpty()
                                        ? jdbcResource.execute(new JdbcRequest(Data.SELECT_SYSTEM_DATA_RANGE).addArg(arguments))
                                        : jdbcResource.execute(new JdbcRequest(Data.SELECT_SYSTEM_DATA_RANGE_LAZY).addArg(arguments));
                                case userDataRSync, blobRSync ->
                                        jdbcResource.execute(new JdbcRequest(Data.SELECT_USER_DATA_RANGE).addArg(arguments));
                                case socket ->
                                        jdbcResource.execute(new JdbcRequest(Data.SELECT_SOCKET_DATA_RANGE).addArg(arguments));
                            };
                            totalCounterItem += exec.size();
                            for (Map<String, Object> item : exec) {
                                //Что бы канал слишком не забивать данными, немного почистим содержимое удаляемых данных
                                if (item.get("is_remove").toString().equals("1")) {
                                    item.remove("value");
                                    item.remove("date_update");
                                    item.remove("date_add");
                                    // Низя удалять parentUuid, key  так как по нему могут быть слушатели
                                    // DynamicPage._subscribedOnReload из Enum SubscribeReloadGroup
                                    //item.remove("parent_uuid");
                                    //item.remove("key");
                                    // Низя удалять revision, а то дальше где-то NPE)
                                    //item.remove("revision");
                                }
                                if (item.containsKey("value") && item.get("value") != null) {
                                    totalByte += ((String) item.get("value")).length();
                                }
                            }
                            result.put(dataType.toString(), exec);
                        } else if (dbRevision < rqRevision && lazyList.isEmpty()) { //Рассинхрон версий
                            // На устройстве номер ревизии больше, чем в серверной БД
                            // Возвращаем клиенту, последний номер ревизии в серверной БД
                            // Для того, что бы клиент пометил у себя в локальной БД данные для следующей синхронизации
                            needUpgrade.put(dataType.toString(), dbRevision);
                        }
                    }
                    promise.getRepositoryMapClass(ResponseRepository.class)
                            .append("totalByte", totalByte)
                            .append("totalCountItem", totalCounterItem)
                            .append("serverNeedUpgrade", needUpgrade);

                })
                .thenWithResource("updateUserDataRSync", JdbcResource.class, "default", (_, promise, jdbcResource) -> {
                    // userDataRSync может прийти пустым, так как просто человечек не залогинен
                    // А может получится так, что устройство считает себя залогиненным, но сервер переехал без восстановления БД
                    insertData(promise, DataType.userDataRSync.name(), jdbcResource);
                })
                .thenWithResource("updateBlobRSync", JdbcResource.class, "default", (_, promise, jdbcResource) -> {
                    //blobRSync может прийти пустым, так как просто человечек не залогинен
                    // А может получится так, что устройство считает себя залогиненным, но сервер переехал без восстановления БД
                    insertData(promise, DataType.blobRSync.name(), jdbcResource);
                })
                .thenWithResource("updateSocket", JdbcResource.class, "default", (_, promise, jdbcResource)
                        -> insertData(promise, DataType.socket.name(), jdbcResource))
                .thenWithResource("updateSocketParentData", JdbcResource.class, "default", (_, promise, jdbcResource) -> {
                    // Если пришли новые данные с типом socket у которых установлен parrent_uuid
                    // Скорее всего это новая установка (наследование) и надо по мимо регистрации их в серверной БД
                    // Надо выдать последнюю ревизию родителя
                    // ** перечитывал 100 раз) ничего не понятно что это блин такое)))

                    @SuppressWarnings("unchecked")
                    Map<String, List<Map<String, Object>>> result = promise.getRepositoryMap("result", Map.class);

                    if (result.containsKey(DataType.socket.name())) {
                        updateSocketParentData(result.get(DataType.socket.name()), jdbcResource);
                    }
                })
                .then("sizeControl", (_, promise) -> sizeControl(promise))
                .extension(PromiseExtension::addTerminal);
    }

    private static <T> List<T> getUncheckedList(Promise promise, String key) {
        @SuppressWarnings("unchecked")
        Map<String, Object> parsedJson = promise.getRepositoryMap("parsedJson", Map.class);
        List<T> result = new ArrayList<>();
        if (parsedJson.containsKey(key) && parsedJson.get(key) instanceof List) {
            @SuppressWarnings("unchecked")
            List<Object> data = (List<Object>) parsedJson.get(key);
            if (data != null && !data.isEmpty() && String.class.equals(data.getFirst().getClass())) {
                @SuppressWarnings("unchecked")//It's OK, we know List<T> contains the expected type.
                List<T> foo = (List<T>) data;
                result.addAll(foo);
            }
        }
        return result;
    }

    //#3
    private static void insertData(
            Promise promise,
            String dataTypeName,
            JdbcResource jdbcResource
    ) throws Throwable {
        // Изначально эта функция была сделана для решения следующей задачи:
        // Так как изначально происходит выборка по максимальным ревизиям на устройстве
        // а только потом идут инсерты новых данных с устройства на серверную БД
        // на устройство надо отправить
        // Когда происходят инсерты, нам на устройство надо вернуть новое установленное значение ревизии

        @SuppressWarnings("unchecked")
        Map<String, Object> parsedJson = promise.getRepositoryMap("parsedJson", Map.class);

        @SuppressWarnings("unchecked")
        List<Map<String, Object>> listDataToInsert = (List<Map<String, Object>>) parsedJson.get(dataTypeName);
        if (listDataToInsert != null && !listDataToInsert.isEmpty()) {

            // Устройство ничего не долно высылать на ставку, до тех пор пока не авторизауется
            AuthRepository authRepository = promise.getRepositoryMapClass(AuthRepository.class);
            if (authRepository.getIdUser() == null) {
                // Была попытка вставить данные, как будто устройство авторизованно, но нет
                throw new AuthException("idUser is null");
            }

            @SuppressWarnings("unchecked")
            Map<String, List<Map<String, Object>>> result = promise.getRepositoryMap("result", Map.class);

            for (Map<String, Object> dataToInsert : listDataToInsert) {
                Map<String, Object> arguments = promise.getRepositoryMapClass(AuthRepository.class).get();
                arguments.putAll(dataToInsert);
                List<Map<String, Object>> exec = jdbcResource.execute(new JdbcRequest(Data.INSERT).addArg(arguments).setDebug(true));

                if (!exec.isEmpty() && exec.getFirst().containsKey("new_id_revision")) {
                    String newIdRevisionString = (String) exec.getFirst().get("new_id_revision");
                    if (newIdRevisionString != null && Util.isNumeric(newIdRevisionString)) {
                        dataToInsert.put("revision_data", Long.parseLong(newIdRevisionString));
                    } else {
                        dataToInsert.put("revision_data", null);
                    }
                }
            }
            // Если не нашлось данных, которые надо обновлять по разности ревизий, а здесь у нас происходят инсерты
            // добавляем ключ
            if (!result.containsKey(dataTypeName)) {
                result.put(dataTypeName, new ArrayList<>());
            }
            // Мержим с зачисткой данных, что бы не передавать то, что нам отправлено,
            // оставим только uuid и новый номер назначенной ревизии
            mergeRevision(listDataToInsert, result.get(dataTypeName));
        }
    }

    //#4
    public static void mergeRevision(List<Map<String, Object>> listDataInserted, List<Map<String, Object>> listResultData) {
        // Так как у нас сначала происходит выборка данных в серверной БД
        // А после происходит обновление данных пришедших в запросе
        // Первичные отобранные данные могли измениться, поэтому мы им будем обнуляем данные
        // Что бы не посылать клиенту теже данны которые он нам прислал в запросе
        for (Map<String, Object> dataInserted : listDataInserted) {
            if (dataInserted.get("revision_data") == null) {
                // revision_data может вернуться пустой после Insert
                // если мы попытались обновить существующие сокетные данные при помощи синхронизация
                // Обновление сокетных данных надо делать через rest api
                continue;
            }

            boolean find = false;
            for (Map<String, Object> resultData : listResultData) {
                if (resultData.get("uuid").equals(dataInserted.get("uuid_data"))) {
                    resultData.clear(); //Что бы не передавать туже самую информацию
                    resultData.put("uuid", dataInserted.get("uuid_data"));
                    resultData.put("revision", dataInserted.get("revision_data"));
                    resultData.put("parent_uuid", dataInserted.get("parent_uuid_data"));
                    resultData.put("sync_revision", true);
                    find = true;
                    break;
                }
            }
            //Есть вероятность, что добавляемые данные не попали в первичную выборку по разности ревизий
            // Добавляем их, что бы оповестить клиентскую часть, что этим данным назначен такой номер ревизии
            if (!find) {
                Map<String, Object> appendMap = new HashMap<>();
                appendMap.put("uuid", dataInserted.get("uuid_data")); //Только номер ревизии заполняем, что бы не передавать повторно информацию
                appendMap.put("revision", dataInserted.get("revision_data")); //Только номер ревизии заполняем, что бы не передавать повторно информацию
                appendMap.put("parent_uuid", dataInserted.get("parent_uuid_data"));
                appendMap.put("sync_revision", true);
                listResultData.add(appendMap);
            }
        }
        // Результрующий список должен быть отсортирован по ревизиям
        // Ревизии имеют одну последовательность для всех типов данных
        // Если клиентская часть зависнет при update данных и данные будут не отсортированы
        // Мы можем получить пробелы не обновлённых данных на клиентской стороне
        // Так как функция getMaxRevisionByType при последующей синхронизации вернёт самую максимальную ревизию по типу
        listResultData.sort((lhs, rhs) -> {
            long l = Long.parseLong(lhs.get("revision").toString());
            long r = Long.parseLong(rhs.get("revision").toString());
            return Long.compare(l, r);
        });
    }

    //#5
    private static void updateSocketParentData(
            List<Map<String, Object>> listResultSocketData,
            JdbcResource jdbcResource
    ) throws Throwable {
        //Сокетные данные немного кастомная история **Описание можете посмотреть в Enum
        Map<String, List<Map<String, Object>>> needParentDataMap = new HashMap<>();
        for (Map<String, Object> resultSocketData : listResultSocketData) {
            String parentUuidData = (String) resultSocketData.get("parent_uuid");
            if (parentUuidData != null && !parentUuidData.isEmpty()) {
                if (!needParentDataMap.containsKey(parentUuidData)) {
                    needParentDataMap.put(parentUuidData, new ArrayList<>());
                }
                needParentDataMap.get(parentUuidData).add(resultSocketData);
            }
        }
        if (!needParentDataMap.isEmpty()) {
            //Получаем все исходные данные через связку uuid = parent_uuid
            List<Map<String, Object>> exec = jdbcResource.execute(new JdbcRequest(DataByParent.PROCESSED)
                    .addArg("processed", needParentDataMap.keySet().stream().toList()));
            for (Map<String, Object> item : exec) {
                //Получаем uuid_data у родителя
                String uuidData = (String) item.get("uuid_data");
                //Пробегаемся по всем зависимым данным пустышек и заменяем value родительским value
                for (Map<String, Object> item2 : needParentDataMap.get(uuidData)) {
                    item2.put("value", item.get("value_data"));
                }
            }
        }
    }

    //#6
    public static void sizeControl(Promise promise) {

        ResponseRepository responseRepository = promise.getRepositoryMapClass(ResponseRepository.class);

        @SuppressWarnings("unchecked")
        Map<String, List<Map<String, Object>>> result = promise.getRepositoryMap("result", Map.class);

        Map<String, List<Map<String, Object>>> upgrade = new HashMap<>();
        int limitByte = 100 * 1024;
        int countItem = 0;
        responseRepository.append("limitByte", limitByte);

        for (String type : result.keySet()) {
            if (limitByte <= 0) {
                break;
            }
            List<Map<String, Object>> cloneObjects = new ArrayList<>();
            upgrade.put(type, cloneObjects);
            List<Map<String, Object>> list = result.get(type);
            for (Map<String, Object> item : list) {
                if (item.containsKey("value")) {
                    Object value = item.get("value");
                    if (value instanceof String) {
                        int length = ((String) value).length();
                        item.put("valueSizeByte", length);
                        limitByte -= length;
                    } else {
                        item.put("valueSizeByte", 0);
                    }
                }
                cloneObjects.add(item);
                countItem++;
                if (limitByte <= 0) {
                    break;
                }
            }
        }
        responseRepository.
                append("limitByteOffset", limitByte)
                .append("countItem", countItem)
                .append("upgrade", upgrade);
    }

}
