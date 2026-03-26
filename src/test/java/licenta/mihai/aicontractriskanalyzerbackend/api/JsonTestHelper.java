package licenta.mihai.aicontractriskanalyzerbackend.api;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;

final class JsonTestHelper {

    private static final ObjectMapper OBJECT_MAPPER = new ObjectMapper().findAndRegisterModules();

    private JsonTestHelper() {
    }

    static String readString(String json, String path) throws Exception {
        JsonNode root = OBJECT_MAPPER.readTree(json);
        JsonNode value = root.at(toJsonPointer(path));
        if (value.isMissingNode() || value.isNull()) {
            throw new IllegalArgumentException("Missing json path: " + path);
        }
        return value.asText();
    }

    private static String toJsonPointer(String path) {
        if (path.startsWith("/")) {
            return path;
        }
        String converted = path.replace(".", "/").replace("[", "/").replace("]", "");
        if (converted.startsWith("/")) {
            return converted;
        }
        return "/" + converted;
    }
}


