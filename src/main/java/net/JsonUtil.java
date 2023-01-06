package net;

import app.AbstractViewModel;
import org.json.JSONObject;
import swingtree.api.mvvm.Val;
import swingtree.api.mvvm.Viewable;

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
        json.put(Constants.PROP_VALUE, escapeForJson(property));
        json.put(Constants.PROP_TYPE,
            new JSONObject()
            .put(Constants.PROP_TYPE_NAME, type.getName())
            .put(Constants.PROP_TYPE_STATES, knownStates)
        );

        return json;
    }


    private static Object escapeForJson(Val<?> prop) {

        if ( prop.isEmpty() ) return null;

        if ( prop.type() == Boolean.class )
            return prop.get();
        else if ( prop.type() == Integer.class )
            return prop.get();
        else if ( prop.type() == Double.class )
            return prop.get();
        else if ( prop.type() == Enum.class )
            return ((Enum)prop.get()).name();
        else if (Viewable.class.isAssignableFrom(prop.type())) {
            Viewable viewable = (Viewable) prop.get();
            if ( viewable instanceof AbstractViewModel vm )
                return vm.vmid().toString();
            else
                return String.valueOf(viewable);
        }

        Object value = prop.get();
        String asString = String.valueOf(value);
        asString = asString.replace("\"", "\\\"");
        asString = asString.replace("\r", "\\r");
        asString = asString.replace("\n", "\\n");
        return asString;
    }

}
