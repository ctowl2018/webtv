package com.fongmi.android.tv.setting;

import android.net.Uri;
import android.text.TextUtils;

import com.fongmi.android.tv.App;
import com.fongmi.android.tv.R;
import com.fongmi.android.tv.bean.Site;
import com.fongmi.android.tv.bean.Style;
import com.fongmi.android.tv.gson.ExtAdapter;
import com.fongmi.android.tv.server.Server;
import com.fongmi.android.tv.utils.UrlUtil;
import com.github.catvod.crawler.SpiderDebug;
import com.github.catvod.utils.Path;
import com.github.catvod.utils.Util;
import com.google.gson.annotations.JsonAdapter;
import com.google.gson.annotations.SerializedName;
import com.google.gson.JsonArray;
import com.google.gson.JsonElement;
import com.google.gson.JsonObject;
import com.google.gson.JsonParser;

import java.io.File;
import java.nio.charset.StandardCharsets;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collections;
import java.util.HashSet;
import java.util.List;
import java.util.Locale;
import java.util.Objects;
import java.util.Set;

public class CustomCspSetting {

    private static final String DIR = "TV/CustomCsp";
    private static final String REGISTRY = "registry.json";
    private static final String PREFIX = "__custom_csp_";
    private static final int MAX_INSERT_INDEX = 9;
    private static final String KIND_WEB_HOME = "webHome";
    private static final String KIND_CSP = "csp";
    private static final String API_BUILTIN = "csp_Builtin";
    //旧版本遗留的直播模式标识，仅用于迁移降级识别
    private static final String KIND_LEGACY_LIVE = "live";

    public static Registry load() {
        String text = Path.read(registryFile());
        return objectFrom(text);
    }

    public static Registry objectFrom(String text) {
        try {
            return parse(text);
        } catch (Exception e) {
            return new Registry();
        }
    }

    public static Registry parse(String text) throws Exception {
        if (TextUtils.isEmpty(text)) return new Registry();
        JsonElement element = JsonParser.parseString(text);
        if (element.isJsonNull()) return new Registry();
        if (element.isJsonArray()) return new Registry().items(itemsFrom(element.getAsJsonArray())).normalize();
        if (!element.isJsonObject()) throw new IllegalArgumentException("Invalid custom CSP JSON");
        JsonObject object = element.getAsJsonObject();
        if (object.has("items")) {
            Registry registry = App.gson().fromJson(object, Registry.class);
            if (registry != null && object.has("home") && !object.has("homeKey")) registry.setHomeKey(object.get("home").getAsString());
            if (registry != null && object.get("items").isJsonArray()) registry.setItems(itemsFrom(object.getAsJsonArray("items")));
            return registry == null ? new Registry() : registry.normalize();
        }
        Registry registry = new Registry();
        if (object.has("enabled")) registry.setEnabled(object.get("enabled").getAsBoolean());
        if (object.has("insertIndex")) registry.setInsertIndex(object.get("insertIndex").getAsInt());
        if (object.has("homeKey")) registry.setHomeKey(object.get("homeKey").getAsString());
        else if (object.has("home")) registry.setHomeKey(object.get("home").getAsString());
        registry.items(itemsFrom(object));
        return registry.normalize();
    }

    private static List<Item> itemsFrom(JsonObject object) {
        List<Item> items = new ArrayList<>();
        if (object.has("sites") && object.get("sites").isJsonArray()) items.addAll(itemsFrom(object.getAsJsonArray("sites")));
        if (items.isEmpty()) items.add(itemFrom(object));
        return items;
    }

    private static List<Item> itemsFrom(JsonArray array) {
        List<Item> items = new ArrayList<>();
        for (JsonElement element : array) {
            if (!element.isJsonObject()) continue;
            items.add(itemFrom(element.getAsJsonObject()));
        }
        return items;
    }

    private static Item itemFrom(JsonObject object) {
        Item item = App.gson().fromJson(object, Item.class);
        if (!object.has("site")) item.setSite(object.deepCopy());
        return item.normalize();
    }

    public static void save(Registry registry) {
        Path.write(registryFile(), App.gson().toJson(registry.normalize()).getBytes(StandardCharsets.UTF_8));
    }

    public static File dir() {
        File dir = new File(Path.files(), DIR);
        if (!dir.exists()) dir.mkdirs();
        return dir;
    }

    public static File file(String id, String name) {
        return new File(new File(dir(), id), name);
    }

    public static String localUrl(String id, String name) {
        return "file://" + file(id, name).getAbsolutePath();
    }

    public static JsonElement parseFlexible(String text) {
        String value = stripTrailingCommas(text);
        try {
            return JsonParser.parseString(value);
        } catch (Exception e) {
            if (!looksLikeRootFields(value)) throw e;
            return JsonParser.parseString("{" + stripTrailingCommas(value) + "}");
        }
    }

    private static boolean looksLikeRootFields(String value) {
        if (TextUtils.isEmpty(value) || value.startsWith("{") || value.startsWith("[")) return false;
        return value.startsWith("\"") && value.contains("\":");
    }

    private static String stripTrailingCommas(String text) {
        String value = text == null ? "" : text.trim();
        if (TextUtils.isEmpty(value)) return "";
        StringBuilder builder = new StringBuilder(value.length());
        boolean inString = false;
        boolean escaped = false;
        for (int i = 0; i < value.length(); i++) {
            char c = value.charAt(i);
            if (inString) {
                builder.append(c);
                if (escaped) escaped = false;
                else if (c == '\\') escaped = true;
                else if (c == '"') inString = false;
                continue;
            }
            if (c == '"') {
                inString = true;
                builder.append(c);
                continue;
            }
            if (c == ',') {
                int next = nextNonWhitespace(value, i + 1);
                if (next >= 0 && (value.charAt(next) == ']' || value.charAt(next) == '}')) continue;
            }
            builder.append(c);
        }
        value = builder.toString().trim();
        while (value.endsWith(",") || value.endsWith(";") || value.endsWith("；")) value = value.substring(0, value.length() - 1).trim();
        return value;
    }

    private static int nextNonWhitespace(String text, int start) {
        for (int i = start; i < text.length(); i++) if (!Character.isWhitespace(text.charAt(i))) return i;
        return -1;
    }

    public static boolean isRemoteScript(String api) {
        if (TextUtils.isEmpty(api)) return false;
        try {
            Uri uri = Uri.parse(api.trim());
            String scheme = uri.getScheme();
            String path = uri.getPath();
            if (!("http".equalsIgnoreCase(scheme) || "https".equalsIgnoreCase(scheme)) || TextUtils.isEmpty(path)) return false;
            String lower = path.toLowerCase(Locale.ROOT);
            return lower.endsWith(".py") || lower.endsWith(".js");
        } catch (Exception e) {
            return false;
        }
    }

    public static void writePage(String id, String code) {
        ensureFileAccess();
        File file = file(id, "index.html");
        String text = code == null ? "" : code;
        Path.write(file, text.getBytes(StandardCharsets.UTF_8));
        ensureWritten(file, text);
    }

    public static void copyPage(File source, String id) {
        ensureFileAccess();
        File file = file(id, "index.html");
        Path.copy(source, file);
        if (!source.exists() || !file.exists() || !Arrays.equals(Path.readToByte(source), Path.readToByte(file))) throw new IllegalStateException(App.get().getString(R.string.setting_custom_csp_save_failed, file.getAbsolutePath()));
    }

    public static String copyFile(File source, String id) {
        if (source == null || !source.isFile()) throw new IllegalStateException(App.get().getString(R.string.setting_custom_csp_save_failed, ""));
        File file = file(id, source.getName());
        try {
            if (!source.getCanonicalPath().equals(file.getCanonicalPath())) Path.copy(source, file);
        } catch (Exception e) {
            Path.copy(source, file);
        }
        if (!file.exists() || !Arrays.equals(Path.readToByte(source), Path.readToByte(file))) throw new IllegalStateException(App.get().getString(R.string.setting_custom_csp_save_failed, file.getAbsolutePath()));
        return localUrl(id, source.getName());
    }

    public static void deleteFiles(String id) {
        if (TextUtils.isEmpty(id)) return;
        try {
            File root = dir().getCanonicalFile();
            File target = new File(root, id).getCanonicalFile();
            if (!isInside(root, target)) return;
            Path.clear(target);
        } catch (Throwable e) {
            SpiderDebug.log("custom-csp", "delete files failed id=%s error=%s", id, e.toString());
        }
    }

    private static boolean isInside(File root, File target) throws Exception {
        String rootPath = root.getCanonicalPath();
        String targetPath = target.getCanonicalPath();
        return targetPath.equals(rootPath) || targetPath.startsWith(rootPath + File.separator);
    }

    private static Set<String> itemIds(Registry registry) {
        Set<String> ids = new HashSet<>();
        if (registry == null) return ids;
        for (Item item : registry.getItems()) if (item != null && !TextUtils.isEmpty(item.getId())) ids.add(item.getId());
        return ids;
    }

    public static void cleanupRemovedFiles(Set<String> before, Set<String> after) {
        for (String id : before) if (!after.contains(id)) deleteFiles(id);
    }

    private static void ensureFileAccess() {
        if (!Setting.hasFileAccess()) throw new IllegalStateException(App.get().getString(R.string.setting_custom_csp_permission_required));
    }

    private static void ensureWritten(File file, String text) {
        if (!file.exists() || !TextUtils.equals(Path.read(file), text)) throw new IllegalStateException(App.get().getString(R.string.setting_custom_csp_save_failed, file.getAbsolutePath()));
    }

    public static Result inject(List<Site> sites) {
        Registry registry = load();
        if (!registry.isEnabled()) return Result.empty();
        Server.get().start();
        List<Site> items = registry.sites();
        if (items.isEmpty()) return Result.empty();
        for (Site site : items) sites.remove(site);
        int index = Math.max(0, Math.min(registry.getInsertIndex(), sites.size()));
        sites.addAll(index, items);
        Site home = items.stream().filter(site -> site.getKey().equals(registry.getHomeKey())).findFirst().orElse(new Site());
        return new Result(home);
    }

    public static int countEnabled() {
        return count().active();
    }

    public static int countItems() {
        return load().getItems().size();
    }

    public static Count count() {
        Registry registry = load();
        int enabled = 0;
        int active = 0;
        for (Item item : registry.getItems()) {
            if (!item.isEnabled()) continue;
            enabled++;
            if (registry.isEnabled() && item.isValid()) active++;
        }
        return new Count(active, enabled);
    }

    public static Item createDefaultItem() {
        //WebHome 已移除：新建条目默认为通用 CSP（api/脚本留空由用户填写）
        Item item = new Item();
        item.setId("local_" + System.currentTimeMillis() + "_" + Long.toHexString(System.nanoTime()));
        item.setKey(PREFIX + item.getId());
        item.setKind(KIND_CSP);
        item.setType(3);
        item.setApi("");
        return item;
    }

    public record Count(int active, int enabled) {
    }

    private static File registryFile() {
        return new File(dir(), REGISTRY);
    }

    public record Result(Site home) {

        public static Result empty() {
            return new Result(new Site());
        }
    }

    public static class Registry {

        @SerializedName("enabled")
        private Boolean enabled;
        @SerializedName("insertIndex")
        private Integer insertIndex;
        @SerializedName("homeKey")
        private String homeKey;
        @SerializedName("items")
        private List<Item> items;

        public Registry normalize() {
            if (items == null) items = new ArrayList<>();
            items.removeIf(Objects::isNull);
            Set<String> ids = new HashSet<>();
            Set<String> keys = new HashSet<>();
            List<Item> unique = new ArrayList<>();
            for (Item item : items) {
                String oldKey = item.peekKey();
                item.normalize();
                item.disableLegacy();
                item.ensureUniqueKey(keys);
                if (!TextUtils.isEmpty(oldKey) && oldKey.equals(homeKey) && !oldKey.equals(item.getKey())) homeKey = item.getKey();
                if (!ids.add(item.getId())) continue;
                unique.add(item);
            }
            items = unique;
            if (homeKey == null) homeKey = "";
            return this;
        }

        public boolean isEnabled() {
            return enabled == null || enabled;
        }

        public void setEnabled(boolean enabled) {
            this.enabled = enabled;
        }

        public int getInsertIndex() {
            return insertIndex == null ? 0 : Math.max(0, Math.min(MAX_INSERT_INDEX, insertIndex));
        }

        public void setInsertIndex(int insertIndex) {
            this.insertIndex = Math.max(0, Math.min(MAX_INSERT_INDEX, insertIndex));
        }

        public String getHomeKey() {
            return TextUtils.isEmpty(homeKey) ? "" : homeKey;
        }

        public void setHomeKey(String homeKey) {
            this.homeKey = homeKey;
        }

        public List<Item> getItems() {
            return items == null ? Collections.emptyList() : items;
        }

        public void setItems(List<Item> items) {
            this.items = items;
        }

        public Registry items(List<Item> items) {
            setItems(items);
            return this;
        }

        public Item addDefault() {
            Item item = createDefaultItem();
            if (items == null) items = new ArrayList<>();
            items.add(item);
            return item;
        }

        public List<Site> sites() {
            return getItems().stream().filter(Item::isEnabled).filter(Item::isValid).map(Item::site).filter(site -> !site.isEmpty()).toList();
        }
    }

    public static class Item {

        @SerializedName("id")
        private String id;
        @SerializedName("key")
        private String key;
        @SerializedName("name")
        private String name;
        @SerializedName("enabled")
        private Boolean enabled;
        @SerializedName("kind")
        private String kind;
        @SerializedName("webHome")
        private Boolean webHome;
        @SerializedName("type")
        private Integer type;
        @SerializedName("api")
        private String api;
        @SerializedName("ext")
        @JsonAdapter(ExtAdapter.class)
        private String ext;
        @SerializedName("jar")
        private String jar;
        @SerializedName("homePage")
        private String homePage;
        @SerializedName("extensions")
        private JsonElement extensions;
        @SerializedName("click")
        private String click;
        @SerializedName("playUrl")
        private String playUrl;
        @SerializedName("hide")
        private Integer hide;
        @SerializedName("searchable")
        private Integer searchable;
        @SerializedName("changeable")
        private Integer changeable;
        @SerializedName("quickSearch")
        private Integer quickSearch;
        @SerializedName("site")
        private JsonObject site;
        private transient String extensionsText;
        private transient boolean extensionsInvalid;
        private transient Boolean extensionsExpanded;

        public Item normalize() {
            normalizeKind();
            if (TextUtils.isEmpty(id)) id = Util.md5(getKey() + getName() + getApi() + getHomePage());
            if (TextUtils.isEmpty(key)) {
                String siteKey = getSiteString("key");
                key = TextUtils.isEmpty(siteKey) ? PREFIX + id : siteKey;
            }
            if (shouldUseNameKey(key)) key = keyFromName(getName(), id);
            return this;
        }

        private void normalizeKind() {
            if (!TextUtils.isEmpty(kind)) return;
            kind = inferWebHome() ? KIND_WEB_HOME : KIND_CSP;
        }

        public boolean isEnabled() {
            return enabled == null || enabled;
        }

        public boolean isWebHome() {
            //WebHome 已从构建中移除：任何条目都不再按“网页首页”模式运行。
            //历史条目的识别与降级见 isLegacyWebHome() / disableLegacy()。
            return false;
        }

        //仅用于迁移：识别旧版本遗留的 WebHome 条目（kind=webHome / webHome=true / 内置 api + homePage）
        public boolean isLegacyWebHome() {
            return KIND_WEB_HOME.equals(kind) || (webHome != null && webHome) || isWebHomeByFields();
        }

        //把遗留的 WebHome / 直播 条目降级为普通（禁用）条目，避免出现已不存在的模式与空 api 站点
        void disableLegacy() {
            if (!KIND_LEGACY_LIVE.equals(kind) && !isLegacyWebHome()) return;
            enabled = false;
            kind = KIND_CSP;
            webHome = false;
        }

        private boolean isWebHomeByFields() {
            return webHome == null ? inferWebHome() : webHome;
        }

        private boolean inferWebHome() {
            String apiValue = !TextUtils.isEmpty(api) ? api.trim() : getSiteString("api");
            String homeValue = !TextUtils.isEmpty(homePage) ? homePage.trim() : getSiteString("homePage");
            if (TextUtils.isEmpty(homeValue)) homeValue = getSiteHomeAlias();
            return !homeValue.isEmpty() && isBuiltinApi(apiValue);
        }

        public String getKind() {
            normalizeKind();
            return KIND_CSP;
        }

        public boolean isValid() {
            return isWebHome() ? !getHomePage().isEmpty() : !getApi().isEmpty();
        }

        public void setEnabled(boolean enabled) {
            this.enabled = enabled;
        }

        public void setWebHome(boolean webHome) {
            this.webHome = webHome;
            setKind(webHome ? KIND_WEB_HOME : KIND_CSP);
        }

        public void setKind(String kind) {
            this.kind = KIND_WEB_HOME.equals(kind) ? KIND_WEB_HOME : KIND_CSP;
            this.webHome = KIND_WEB_HOME.equals(this.kind);
            if (isWebHome()) {
                if (type == null) type = 3;
                if (TextUtils.isEmpty(api) || isBuiltinApi(api)) api = API_BUILTIN;
            }
        }

        public String getId() {
            normalize();
            return id;
        }

        public void setId(String id) {
            this.id = id;
        }

        public void setKey(String key) {
            this.key = key;
            putSite("key", key);
        }

        public void setName(String name) {
            this.name = name;
            if (shouldUseNameKey(getKey())) setKey(keyFromName(getName(), getId()));
            putSite("name", name);
        }

        public void setType(Integer type) {
            this.type = type;
            putSite("type", type);
        }

        public Integer getType() {
            return type == null ? getSiteInt("type", 3) : type;
        }

        public Integer getHide() {
            return hide == null ? getSiteInt("hide", 0) : hide;
        }

        public Integer getSearchable() {
            return searchable == null ? getSiteInt("searchable", isWebHome() ? 0 : 1) : searchable;
        }

        public Integer getChangeable() {
            return changeable == null ? getSiteInt("changeable", 1) : changeable;
        }

        public Integer getQuickSearch() {
            return quickSearch == null ? getSiteInt("quickSearch", isWebHome() ? 0 : 1) : quickSearch;
        }

        public void setApi(String api) {
            this.api = api;
            putSite("api", api);
        }

        public void setExt(String ext) {
            this.ext = ext;
            putSite("ext", ext);
        }

        public void setJar(String jar) {
            this.jar = jar;
            putSite("jar", jar);
        }

        public void setHomePage(String homePage) {
            this.homePage = homePage;
            putSite("homePage", homePage);
        }

        public void setExtensionsText(String text) {
            extensionsText = text == null ? "" : text.trim();
            extensionsInvalid = false;
            if (TextUtils.isEmpty(extensionsText)) {
                extensions = null;
                removeSite("extensions");
                return;
            }
            try {
                extensions = parseExtensions(extensionsText);
                putSite("extensions", extensions);
            } catch (Exception e) {
                extensionsInvalid = true;
            }
        }

        public void setExtensionsExpanded(boolean expanded) {
            extensionsExpanded = expanded;
            if (!expanded) setExtensionsText("");
        }

        public void setClick(String click) {
            this.click = click;
            putSite("click", click);
        }

        public void setPlayUrl(String playUrl) {
            this.playUrl = playUrl;
            putSite("playUrl", playUrl);
        }

        public void setHide(Integer hide) {
            this.hide = hide;
            putSite("hide", hide);
        }

        public void setSearchable(Integer searchable) {
            this.searchable = searchable;
            putSite("searchable", searchable);
        }

        public void setChangeable(Integer changeable) {
            this.changeable = changeable;
            putSite("changeable", changeable);
        }

        public void setQuickSearch(Integer quickSearch) {
            this.quickSearch = quickSearch;
            putSite("quickSearch", quickSearch);
        }

        public void setSite(JsonObject site) {
            this.site = site;
        }

        public String getKey() {
            return !TextUtils.isEmpty(key) ? key.trim() : getSiteString("key");
        }

        public String getName() {
            String value = !TextUtils.isEmpty(name) ? name.trim() : getSiteString("name");
            return TextUtils.isEmpty(value) ? getKey() : value;
        }

        public String getApi() {
            return !TextUtils.isEmpty(api) ? api.trim() : getSiteString("api");
        }

        public String getExt() {
            return !TextUtils.isEmpty(ext) ? ext.trim() : getSiteString("ext");
        }

        public String getJar() {
            return !TextUtils.isEmpty(jar) ? jar.trim() : getSiteString("jar");
        }

        public String getHomePage() {
            String value = !TextUtils.isEmpty(homePage) ? homePage.trim() : getSiteString("homePage");
            return TextUtils.isEmpty(value) ? getSiteHomeAlias() : value;
        }

        public String getExtensionsText() {
            if (extensionsText != null) return extensionsText;
            JsonElement element = getExtensions();
            if (element == null || element.isJsonNull()) return "";
            if (element.isJsonPrimitive()) return element.getAsString().trim();
            return App.gson().toJson(element);
        }

        public boolean hasInvalidExtensions() {
            return extensionsInvalid;
        }

        public boolean isExtensionsExpanded() {
            return extensionsExpanded == null ? !TextUtils.isEmpty(getExtensionsText()) : extensionsExpanded;
        }

        public String getClick() {
            return !TextUtils.isEmpty(click) ? click.trim() : getSiteString("click");
        }

        public String getPlayUrl() {
            return !TextUtils.isEmpty(playUrl) ? playUrl.trim() : getSiteString("playUrl");
        }

        public Site site() {
            normalize();
            if (site != null) return siteFromJson();
            Site site = new Site();
            boolean webHomeOnly = isWebHome();
            site.setKey(getKey());
            site.setName(getName());
            site.setType(getType());
            site.setApi(webHomeOnly ? API_BUILTIN : UrlUtil.convert(getApi()));
            site.setExt(webHomeOnly ? "" : UrlUtil.convert(getExt()));
            site.setJar(webHomeOnly ? "" : getJar());
            site.setHomePage(UrlUtil.convert(getHomePage()));
            site.setClick(webHomeOnly ? "" : getClick());
            site.setPlayUrl(webHomeOnly ? "" : getPlayUrl());
            site.setHide(getHide());
            site.setSearchable(getSearchable());
            site.setChangeable(getChangeable());
            site.setQuickSearch(getQuickSearch());
            if (webHomeOnly && getExtensions() != null) site.setExtensions(getExtensions().deepCopy());
            site.setStyle(Style.rect());
            return site;
        }

        private Site siteFromJson() {
            JsonObject object = site.deepCopy();
            sanitizeSiteObject(object);
            if (!TextUtils.isEmpty(key)) object.addProperty("key", key.trim());
            else if (!object.has("key")) object.addProperty("key", getKey());
            if (!TextUtils.isEmpty(name)) object.addProperty("name", name.trim());
            if (!TextUtils.isEmpty(api)) object.addProperty("api", api.trim());
            if (!TextUtils.isEmpty(ext)) object.addProperty("ext", ext.trim());
            if (!TextUtils.isEmpty(jar)) object.addProperty("jar", jar.trim());
            if (!TextUtils.isEmpty(homePage)) object.addProperty("homePage", homePage.trim());
            if (!TextUtils.isEmpty(click)) object.addProperty("click", click.trim());
            if (!TextUtils.isEmpty(playUrl)) object.addProperty("playUrl", playUrl.trim());
            if (type != null) object.addProperty("type", type);
            if (hide != null) object.addProperty("hide", hide);
            if (searchable != null) object.addProperty("searchable", searchable);
            if (changeable != null) object.addProperty("changeable", changeable);
            if (quickSearch != null) object.addProperty("quickSearch", quickSearch);
            if (isWebHome()) {
                object.addProperty("type", getType());
                object.addProperty("api", API_BUILTIN);
                object.remove("ext");
                object.remove("jar");
                object.remove("click");
                object.remove("playUrl");
                JsonElement value = getExtensions();
                if (value == null || value.isJsonNull()) object.remove("extensions");
                else object.add("extensions", value.deepCopy());
            }
            Site result = Site.objectFrom(object, getJar());
            boolean webHomeOnly = isWebHome() || isBuiltinApi(result.getApi()) && !result.getHomePage().isEmpty();
            if (webHomeOnly && searchable == null && !object.has("searchable")) result.setSearchable(0);
            if (webHomeOnly && quickSearch == null && !object.has("quickSearch")) result.setQuickSearch(0);
            return result;
        }

        private void sanitizeSiteObject(JsonObject object) {
            object.remove("kind");
            object.remove("enabled");
            object.remove("live");
            if (object.has("webHome") && object.get("webHome").isJsonPrimitive() && object.getAsJsonPrimitive("webHome").isBoolean()) object.remove("webHome");
        }

        private String getSiteString(String key) {
            return getString(site, key);
        }

        private String getSiteHomeAlias() {
            if (site == null || !site.has("webHome") || !site.get("webHome").isJsonPrimitive()) return "";
            if (site.getAsJsonPrimitive("webHome").isBoolean()) return "";
            return site.get("webHome").getAsString().trim();
        }

        private JsonElement getExtensions() {
            if (extensions != null) return extensions;
            if (site == null || !site.has("extensions")) return null;
            return site.get("extensions");
        }

        private JsonElement parseExtensions(String text) {
            String value = text == null ? "" : text.trim();
            JsonArray array = new JsonArray();
            if (!value.startsWith("[") && !value.startsWith("{") && !value.startsWith("\"")) {
                array.add(value);
                return array;
            }
            JsonElement element = JsonParser.parseString(value);
            if (element.isJsonArray()) return element;
            if (element.isJsonObject() && element.getAsJsonObject().has("extensions")) return element.getAsJsonObject().get("extensions");
            array.add(element);
            return array;
        }

        private boolean isBuiltinApi(String value) {
            return TextUtils.isEmpty(value) || API_BUILTIN.equals(value.trim());
        }

        private String getString(JsonObject object, String key) {
            if (object == null || !object.has(key) || !object.get(key).isJsonPrimitive()) return "";
            return object.get(key).getAsString().trim();
        }

        private int getSiteInt(String key, int fallback) {
            try {
                if (site == null || !site.has(key) || !site.get(key).isJsonPrimitive()) return fallback;
                return site.get(key).getAsInt();
            } catch (Exception e) {
                return fallback;
            }
        }

        private String peekKey() {
            if (!TextUtils.isEmpty(key)) return key.trim();
            if (site == null || !site.has("key") || !site.get("key").isJsonPrimitive()) return "";
            return site.get("key").getAsString().trim();
        }

        private void ensureUniqueKey(Set<String> keys) {
            String base = getKey();
            String key = base;
            int index = 2;
            while (keys.contains(key)) key = base + "_" + index++;
            setKey(key);
            keys.add(key);
        }

        private boolean shouldUseNameKey(String key) {
            return TextUtils.isEmpty(key) || key.startsWith(PREFIX);
        }

        private String keyFromName(String name, String fallback) {
            String slug = slug(name);
            if (TextUtils.isEmpty(slug)) slug = TextUtils.isEmpty(fallback) ? Util.md5(String.valueOf(System.nanoTime())).substring(0, 8) : fallback;
            return PREFIX + slug;
        }

        private String slug(String text) {
            String value = text == null ? "" : text.trim().toLowerCase(Locale.ROOT);
            StringBuilder builder = new StringBuilder();
            boolean underscore = false;
            for (int i = 0; i < value.length(); ) {
                int codePoint = value.codePointAt(i);
                if (Character.isLetterOrDigit(codePoint) || codePoint == '-' || codePoint == '.') {
                    builder.appendCodePoint(codePoint);
                    underscore = false;
                } else if (!underscore && builder.length() > 0) {
                    builder.append('_');
                    underscore = true;
                }
                i += Character.charCount(codePoint);
            }
            while (builder.length() > 0 && builder.charAt(builder.length() - 1) == '_') builder.deleteCharAt(builder.length() - 1);
            return builder.toString();
        }

        private void putSite(String key, String value) {
            if (site == null) return;
            if (TextUtils.isEmpty(value) && site.has(key) && !site.get(key).isJsonPrimitive()) return;
            site.addProperty(key, value);
        }

        private void putSite(String key, Integer value) {
            if (site != null && value != null) site.addProperty(key, value);
        }

        private void putSite(String key, JsonElement value) {
            if (site != null && value != null) site.add(key, value.deepCopy());
        }

        private void removeSite(String key) {
            if (site != null) site.remove(key);
        }
    }
}
