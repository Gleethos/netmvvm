package app;

import binding.SkinContext;
import swingtree.api.mvvm.Val;

import java.util.List;
import java.util.function.Consumer;

import org.json.JSONObject;
import swingtree.api.mvvm.ValDelegate;

/**
 *  Uses reflection to iterate over all of its field variables
 *  and then converts {@link swingtree.api.mvvm.Val} and {@link swingtree.api.mvvm.Var}
 *  variables to JSON entries where the keys are ids and the values are
 *  the values of the properties.
 */
public class AbstractViewModel
{
    protected AbstractViewModel() {
        SkinContext.instance().put(this);
    }

    /**
     *  Uses reflection to find all the properties of the given view model.
     * @return a list of property instances.
     */
    protected List<Val<Object>> findProperties() {
        List<Val<Object>> properties = new java.util.ArrayList<>();

        for (var field : this.getClass().getDeclaredFields()) {
            field.setAccessible(true);
            try {
                var value = field.get(this);
                if (value instanceof swingtree.api.mvvm.Val<?> val) {
                    properties.add((Val<Object>) val);
                }
            } catch (IllegalAccessException e) {
                e.printStackTrace();
            }
        }
        return properties;
    }

    public JSONObject toJson() {
        JSONObject json = new JSONObject();
        for (var property : findProperties()) {
            json.put(property.id(), escapeForJson(property.get()));
        }
        return json;
    }

    private String escapeForJson(Object value) {
        String asString = String.valueOf(value);
        asString = asString.replace("\"", "\\\"");
        asString = asString.replace("\r", "\\r");
        asString = asString.replace("\n", "\\n");
        return asString;
    }

    public void bind(Consumer<ValDelegate<Object>> observer) {
        var properties = findProperties();
        for (var property : properties) {
            property.onShowThis( it -> {
                observer.accept(it);
            });
        }
    }

}
