package app;

import binding.SkinContext;
import binding.VMID;
import net.Constants;
import net.JsonUtil;
import org.json.JSONArray;
import swingtree.api.UIAction;
import swingtree.api.mvvm.Val;

import java.lang.reflect.Method;
import java.util.List;
import java.util.function.Consumer;
import java.util.function.Supplier;

import org.json.JSONObject;
import swingtree.api.mvvm.ValDelegate;
import swingtree.api.mvvm.Var;

/**
 *  Uses reflection to iterate over all of its field variables
 *  and then converts {@link swingtree.api.mvvm.Val} and {@link swingtree.api.mvvm.Var}
 *  variables to JSON entries where the keys are ids and the values are
 *  the values of the properties.
 */
public class AbstractViewModel
{
    private final VMID<?> _vmid;

    protected AbstractViewModel() {
        _vmid = SkinContext.instance().put(this);
    }

    public VMID<?> vmid() {
        return _vmid;
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
                if ( value instanceof swingtree.api.mvvm.Val<?> val )
                    properties.add((Val<Object>) val);

            } catch (IllegalAccessException e) {
                e.printStackTrace();
            }
        }
        return properties;
    }

    public JSONObject toJson() {
        JSONObject json = new JSONObject();
        for ( var property : findProperties() )
            json.put(property.id(), JsonUtil.fromProperty(property));

        JSONObject result = new JSONObject();
        result.put(Constants.PROPS, json);
        result.put(Constants.VM_ID, _vmid.toString());
        result.put("methods", _getMethods());
        return result;
    }

    private JSONArray _getMethods() {
        var publicMethods = new JSONArray();
        /*
            So lets say we have a class like this:
            class Foo extends AbstractViewModel {
                public long bar(int xy) { ... }
            }
            We want to extract the method signature into the json
            using reflection!
            Each entry should look something like this:
            {
                "name":"bar",
                "args":[{"name":"xy", "type":"int"}]
                "returns": "long"
            }
        */

        for (var method : this.getClass().getDeclaredMethods()) {
            method.setAccessible(true);
            // first we check if the method is public
            if ( !java.lang.reflect.Modifier.isPublic(method.getModifiers()) )
                continue;

            try {
                String returnType = method.getReturnType().getSimpleName();
                String methodName = method.getName();
                var args = new JSONObject();
                for ( var param : method.getParameters() )
                    args.put(Constants.METHOD_ARG_NAME, param.getName())
                        .put(Constants.METHOD_ARG_TYPE, param.getType().getSimpleName());

                publicMethods.put(
                        new JSONObject()
                            .put(Constants.METHOD_NAME, methodName)
                            .put(Constants.METHOD_ARGS, args)
                            .put(Constants.METHOD_RETURNS, returnType)
                    );
            } catch (Exception e) {
                e.printStackTrace();
            }
        }
        return publicMethods;
    }

    public JSONObject call(JSONObject callRequest) {
        /*
            The call request should look like this:
            {
                "method":"bar",
                "args":[{"name":"xy", "value": 5}]
            }
        */
        String methodName = callRequest.getString(Constants.METHOD_NAME);
        var args = callRequest.getJSONArray(Constants.METHOD_ARGS);
        var methodArgs = new Object[args.length()];
        for (int i = 0; i < args.length(); i++) {
            var arg = args.getJSONObject(i);
            String argName = arg.getString(Constants.METHOD_ARG_NAME);
            String argType = arg.getString(Constants.METHOD_ARG_TYPE);
            Object argValue = arg.get(Constants.PROP_VALUE);
            if ( argValue != null ) {
                String valueType = argValue.getClass().getSimpleName();
                // Lets do some type checking and try to convert the value if possible!
                if ( valueType.equals("String") ) {
                    if (argType.equals("int")||argType.equals("Integer"))
                        argValue = Integer.parseInt(argValue.toString());
                    else if (argType.equals("long")||argType.equals("Long"))
                        argValue = Long.parseLong(argValue.toString());
                    else if (argType.equals("double")||argType.equals("Double"))
                        argValue = Double.parseDouble(argValue.toString());
                    else if (argType.equals("float")||argType.equals("Float"))
                        argValue = Float.parseFloat(argValue.toString());
                    else if (argType.equals("boolean")||argType.equals("Boolean"))
                        argValue = Boolean.parseBoolean(argValue.toString());
                }
            }
            methodArgs[i] = argValue;
        }

        try {
            Method method;
            Object result;
            if ( methodArgs.length == 0 ) {
                method = this.getClass().getMethod(methodName);
                result = method.invoke(this);
            } else {
                method = this.getClass().getMethod(methodName, methodArgs[0].getClass());
                result = method.invoke(this, methodArgs[0]);
            }
            return new JSONObject()
                    .put(Constants.METHOD_NAME, methodName)
                    .put(Constants.METHOD_RETURNS, result);
        } catch (Exception e) {
            e.printStackTrace();
        }
        return new JSONObject();
    }

    public void bind(
        UIAction<ValDelegate<Object>> observer
    ) {
        findProperties().forEach( p -> p.onShow(observer) );
    }

    private  <T> Var<T> getPropById( String id ) {
        return findProperties()
                .stream()
                .filter( p -> p instanceof Var<?> )
                .map( p -> (Var<T>) p )
                .filter( p -> p.id().equals(id) )
                .findFirst()
                .orElseThrow(() ->
                    new RuntimeException(
                        "Could not find property with " +
                        "id '" + id +"' in " + this.getClass().getName() + " " +
                        "with id '" + _vmid + "'"
                ));
    }

    public void applyToPropertyById( String id, String newValue ) {
        Var<Object> prop = getPropById(id);

        if ( newValue == null ) {
            if ( prop.allowsNull() )
                prop.act(null);
            else
                throw new RuntimeException("Property '" + id + "' does not allow null values");

            return;
        }

        Class<?> type = prop.type();
        // Now let's convert the value to the correct type
        if ( type == String.class ) {
            prop.act(newValue);
        }
        else if ( type == Integer.class ) {
            prop.act(Integer.parseInt(newValue));
        }
        else if ( type == Double.class ) {
            prop.act(Double.parseDouble(newValue));
        }
        else if ( type == Boolean.class ) {
            prop.act(Boolean.parseBoolean(newValue));
        }
        else if ( Enum.class.isAssignableFrom(type) ) {
            prop.act(Enum.valueOf((Class<Enum>) type, newValue));
        }
        // Now on to array, first primitives and then normal object arrays:
        else {
            String[] strings = newValue.substring(1, newValue.length() - 1).split(",");
            if ( type == byte[].class ) {
                // We expect this to be an array like [1,2,3,4,5]
                String[] parts = strings;
                byte[] bytes = new byte[parts.length];
                for ( int i = 0; i < parts.length; i++ )
                    bytes[i] = Byte.parseByte(parts[i]);
                prop.act(bytes);
            }
            else if ( type == short[].class ) {
                // We expect this to be an array like [1,2,3,4,5]
                String[] parts = strings;
                short[] shorts = new short[parts.length];
                for ( int i = 0; i < parts.length; i++ )
                    shorts[i] = Short.parseShort(parts[i]);
                prop.act(shorts);
            }
            else if ( type == int[].class ) {
                // We expect this to be an array like [1,2,3,4,5]
                String[] parts = strings;
                int[] ints = new int[parts.length];
                for ( int i = 0; i < parts.length; i++ )
                    ints[i] = Integer.parseInt(parts[i]);
                prop.act(ints);
            }
            else if ( type == long[].class ) {
                // We expect this to be an array like [1,2,3,4,5]
                String[] parts = strings;
                long[] longs = new long[parts.length];
                for ( int i = 0; i < parts.length; i++ )
                    longs[i] = Long.parseLong(parts[i]);
                prop.act(longs);
            }
            else if ( type == float[].class ) {
                // We expect this to be an array like [1,2,3,4,5]
                String[] parts = strings;
                float[] floats = new float[parts.length];
                for ( int i = 0; i < parts.length; i++ )
                    floats[i] = Float.parseFloat(parts[i]);
                prop.act(floats);
            }
            else if ( type == double[].class ) {
                // We expect this to be an array like [1,2,3,4,5]
                String[] parts = strings;
                double[] doubles = new double[parts.length];
                for ( int i = 0; i < parts.length; i++ )
                    doubles[i] = Double.parseDouble(parts[i]);
                prop.act(doubles);
            }
            else if ( type == boolean[].class ) {
                // We expect this to be an array like [true,false,true]
                String[] parts = strings;
                boolean[] booleans = new boolean[parts.length];
                for ( int i = 0; i < parts.length; i++ )
                    booleans[i] = Boolean.parseBoolean(parts[i]);
                prop.act(booleans);
            }
            else if ( type == char[].class ) {
                // We expect this to be an array like [a,b,c]
                String[] parts = strings;
                char[] chars = new char[parts.length];
                for ( int i = 0; i < parts.length; i++ )
                    chars[i] = parts[i].charAt(0);
                prop.act(chars);
            }
            else if ( type == String[].class ) {
                // We expect this to be an array like ["a","b","c"]
                String[] parts = strings;
                for ( int i = 0; i < parts.length; i++ )
                    parts[i] = parts[i].substring(1, parts[i].length()-1);
                prop.act(parts);
            }
            else {
                throw new RuntimeException("Property '" + id + "' has an unsupported type '" + type.getName() + "'");
            }
        }
    }

}
