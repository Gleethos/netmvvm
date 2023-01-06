
const EVENT_TYPE = "EventType";
const EVENT_PAYLOAD = "EventPayload";

// Event Type Values:
const SET_PROP = "act";
const RETURN_PROP = "show";
const GET_VM = "getVM";
const RETURN_GET_VM = "viewModel";

// View model properties:
const VM_ID = "vmId";
const PROPS = "props";

// Property properties:
const PROP_NAME = "propName";
const PROP_VALUE = "value";
const PROP_TYPE = "type";
const PROP_TYPE_NAME = "name";
const PROP_TYPE_STATES = "states";

function start(serverAddress, frontend) {

    const ws = new WebSocket(serverAddress);
    const propertyObservers = {};
    const viewModelObservers = {};

    ws.onopen = () => { sendVMRequest("app.UserRegistrationViewModel-0"); };
    ws.onmessage = (event) => {
        // We parse the data as json:
        console.log("Received data from server!");
        const data = JSON.parse(event.data);
        console.log('From server' + JSON.stringify(data));
        processResponse(data);
    };

    function sendVMRequest(vmId) {
        ws.send("{'" + EVENT_TYPE + "':'" + GET_VM + "','" + VM_ID + "':'" + vmId + "'}");
    }

    function processResponse(data) {
        // Now let's check the EventType: either a view model or a property change...
        if (data[EVENT_TYPE] === RETURN_GET_VM) {
            // We have a view model, so we can set it as the current view model:
            const viewModel = data[EVENT_PAYLOAD];
            const vmId = viewModel[VM_ID];

            if (viewModelObservers[vmId]) {
                viewModelObservers[vmId](viewModel);
                return;
            }

            console.log("Received view model: " + JSON.stringify(viewModel));
            frontend(
                viewModel,
                (propName, value) => {
                    ws.send(
                        "{" +
                        "'" + EVENT_TYPE + "':'" + SET_PROP + "', " +
                        "'" + VM_ID + "':'" + vmId + "', " +
                        "'" + PROP_NAME + "':'" + propName + "', " +
                        "'" + PROP_VALUE + "':'" + value + "'" +
                        "}"
                    );
                },
                (propName, action) => {
                    propertyObservers[vmId + ":" + propName] = action;
                },
                (vmId, action) => {
                    viewModelObservers[vmId] = action;
                }
            );
        } else if (data[EVENT_TYPE] === RETURN_PROP) {
            // We look up the binding for the property change:
            const action = propertyObservers[data[EVENT_PAYLOAD][VM_ID] + ":" + data[EVENT_PAYLOAD][PROP_NAME]];
            // If we have a binding, we call it with the new value:
            if (action)
                action(data[EVENT_PAYLOAD][PROP_VALUE]);
        }
    }
}
