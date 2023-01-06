package net;

import org.json.JSONObject;
import swingtree.api.mvvm.Val;

import java.util.ArrayList;
import java.util.List;

public class JsonUtil {

    private JsonUtil() {}

    public static JSONObject fromProperty(Val<?> property) {
        Class<?> type = property.type();
        List<String> knownStates = new ArrayList<>();
        if ( Enum.class.isAssignableFrom(type) ) {
            for ( var state : type.getEnumConstants() )
                knownStates.add(((Enum)state).name());
        }

        JSONObject json = new JSONObject();
        json.put(Constants.PROP_NAME, property.id());
        json.put(Constants.PROP_VALUE, escapeForJson(property.get()));
        json.put(Constants.PROP_TYPE,
            new JSONObject()
            .put(Constants.PROP_TYPE_NAME, type.getName())
            .put(Constants.PROP_TYPE_STATES, knownStates)
        );

        return json;
    }


    private static String escapeForJson(Object value) {
        String asString = String.valueOf(value);
        asString = asString.replace("\"", "\\\"");
        asString = asString.replace("\r", "\\r");
        asString = asString.replace("\n", "\\n");
        return asString;
    }

}
