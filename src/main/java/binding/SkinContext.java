package binding;

import app.AbstractViewModel;

import java.util.HashMap;
import java.util.Map;

/**
 *  A singleton hosting all the view model instances.
 */
public class SkinContext {

    private static final SkinContext INSTANCE = new SkinContext();

    public static SkinContext instance() { return INSTANCE; }


    private final Map<Class, Map<Integer, Object>> _viewModels = new HashMap<>();

    public <T extends AbstractViewModel> T get( VMID<T> id ) {
        return (T)_viewModels.get(id.type()).get(id.id());
    }

    public <T extends AbstractViewModel> T get( String id ) {
        // The string has the following format "ViewModelClassName-Instance_ID"
        // For example: "UserRegistrationViewModel-0"
        var parts = id.split("-");
        var type = parts[0];
        var instanceId = Integer.parseInt(parts[1]);
        // Now we try to find the class :
        try {
            var clazz = Class.forName(type);
            return (T)_viewModels.get(clazz).get(instanceId);
        } catch (ClassNotFoundException e) {
            e.printStackTrace();
        }
        throw new IllegalArgumentException("id");
    }

    public <T extends AbstractViewModel> void put( VMID<T> id, T viewModel ) {
        _viewModels.computeIfAbsent(id.type(), k -> new HashMap<>()).put(id.id(), viewModel);
    }

    public <T extends AbstractViewModel> VMID<T> put( T viewModel ) {
        var id = new VMID<T>((Class<T>) viewModel.getClass(), _viewModels.size());
        put(id, viewModel);
        return id;
    }

}
