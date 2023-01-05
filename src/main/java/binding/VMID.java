package binding;

import app.AbstractViewModel;

public record VMID<T extends AbstractViewModel>(Class<T> type, int id) {
    public VMID {
        if (type == null) {
            throw new IllegalArgumentException("type");
        }
    }
}
