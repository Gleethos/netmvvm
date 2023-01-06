package app;

import binding.SkinContext;
import binding.VMID;
import swingtree.api.mvvm.Val;

import java.util.List;
import java.util.function.Consumer;

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
            json.put(property.id(), escapeForJson(property.get()));

        return json;
    }

    private String escapeForJson( Object value ) {
        String asString = String.valueOf(value);
        asString = asString.replace("\"", "\\\"");
        asString = asString.replace("\r", "\\r");
        asString = asString.replace("\n", "\\n");
        return asString;
    }

    public void bind( Consumer<ValDelegate<Object>> observer ) {
        findProperties().forEach( p -> p.onShow(observer::accept) );
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
