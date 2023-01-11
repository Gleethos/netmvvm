/*
    Some constants needed to communicate with the server:
    ~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~
 */

const EVENT_TYPE = "EventType";
const EVENT_PAYLOAD = "EventPayload";

// Event Type Values:
const SET_PROP      = "act";
const RETURN_PROP   = "show";
const GET_VM        = "getVM";
const RETURN_GET_VM = "viewModel";
const CALL          = "call";
const CALL_RETURN   = "callReturn";

// View model properties:
const VM_ID = "vmId";
const PROPS = "props";
const METHOD_NAME = "name";
const METHOD_ARG_NAME = "name";
const METHOD_ARG_TYPE = "type";
const METHOD_ARGS = "args";
const METHOD_RETURNS = "returns";

// Property properties:
const PROP_NAME        = "propName";
const PROP_VALUE       = "value";
const PROP_TYPE        = "type";
const PROP_TYPE_NAME   = "name";
const PROP_TYPE_STATES = "states";

/*
    First we define the API for interacting with the MVVM backend:
    ~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~
 */

function Var(get, set, observe) {
    Var.prototype.get = get;
    Var.prototype.set = set;
    Var.prototype.onShow = observe;
}

function Val(get, observe) {
    Val.prototype.get = get;
    Val.prototype.onShow = observe;
}

/**
 *  This is used as a representation of a view model.
 *  it exposes the current state of the view model as
 *  well as a way to bind to its properties in both ways.
 *
 * @param  vm the state of the view model (a json object)
 * @param  vmSet a function for setting a property of the view model
 * @return vmGet a function for registering an observer for a property of the view model in the backend
 * @constructor
 */
function VM(
    vm,    // The current view model
    vmSet, // Send a property change to the server, expects 2 arguments: propName, value
    vmGet, // For binding to properties, expects 2 parameters: the property name and the action to call when the property changes
    vmCall // For calling methods, expects 3 parameters: the method name, the arguments and the action to call when the method returns
){
    this.state = vm;
    VM.prototype.set = vmSet;
    VM.prototype.get = vmGet;
    // Now we mirror the methods of the Java view model in JS!
    const methods = vm.methods;
    console.log("Methods: " + JSON.stringify(methods));
    for (let i = 0; i < methods.length; i++) {
        const method = methods[i];
        console.log("Method: " + JSON.stringify(method));
        // Currently we only support void methods:
        if (method[METHOD_RETURNS] === "void") {
            this[method[METHOD_NAME]] = function(...args) {
                console.log("Calling method: " + method);
                vmCall(
                    method[METHOD_NAME],
                    args,
                    () => console.log("Method returned")
                );
            }
        } else if ( method[METHOD_RETURNS] === "Var" || method[METHOD_RETURNS] === "Val" ) {
            this[method[METHOD_NAME]] = function(...args) {
                console.log("Calling property method: " + method[METHOD_NAME]);
                const propGet = (consumer) => {
                                    console.log("Registering consumer for property method: " + method[METHOD_NAME]);
                                    vmCall(
                                        method[METHOD_NAME],
                                        args,
                                        (property) => {
                                            console.log("GETTING!!: "+JSON.stringify(property));
                                            consumer(property[PROP_VALUE]);
                                        }
                                    );
                                };

                const propObserve = (consumer) => {
                                        console.log("Registering consumer for property method: " + method[METHOD_NAME]);
                                        vmCall(
                                            method[METHOD_NAME],
                                            args,
                                            (property) => {
                                                console.log("prop for observing: "+JSON.stringify(property));
                                                vmGet(property[PROP_NAME], consumer);
                                            }
                                        );
                                    };

                const propSet = (newValue) => {
                    console.log("Calling property set method: " + method[METHOD_NAME]);
                    vmCall(
                        method[METHOD_NAME],
                        args,
                        (property) => {
                            console.log("prop for setting: "+JSON.stringify(property));
                            vmSet(
                                property[PROP_NAME],
                                newValue
                            );
                        }
                    );
                };

                if (method[METHOD_RETURNS] === "Var") {
                    return new Var(propGet, propSet, propObserve);
                } else {
                    return new Val(propGet, propObserve);
                }
            }
        }
    }
}

/**
 *  This is a representation of a websocket session.
 *  It allows you to fetch new view models from the server.
 *
 * @param getViewModel a function for fetching a view model from the server
 * @constructor
 */
function Session(
    getViewModel // For loading a view model, expects 2 parameters: the view model id and the action to call when the view model is loaded
) {
    Session.prototype.get = getViewModel;
}

/*
    The last part of the API for doing MVVM binding is the entry point
    to a web socket server connection.
    ~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~
*/

/**
 *  This is the entrypoint for the MVVM binding.
 *  Here you can connect to a websocket and get a view model
 *  through the second parameter, a function that will receive the above defined
 *  Session object as well as VM object.
 *
 * @param serverAddress the address of the web-socket server to connect to
 * @param iniViewModelId the id of the view model to load
 * @param frontend the function to call when the view model is loaded
 */
function start(serverAddress, iniViewModelId, frontend) {
    let ws = null;
    const propertyObservers = {};
    const viewModelObservers = {};
    const methodObservers = {};

    function startWebsocket(doInit) {
        ws = new WebSocket(serverAddress)
        ws.onopen = () => { if (doInit) sendVMRequest(iniViewModelId); };
        ws.onclose = function(){
            // connection closed, discard old websocket and create a new one in 5s
            ws = null
            setTimeout(()=>startWebsocket(false), 5000)
        }
        ws.onmessage = (event) => {
            // We parse the data as json:
            console.log("Received data from server!");
            const data = JSON.parse(event.data);
            console.log('From server' + JSON.stringify(data));
            processResponse(data);
        };
    }
    startWebsocket(true);

    function send(message) {
        if ( ws === null ) {
            console.log("Websocket is null, cannot send message '" + message + "'");
            return;
        }
        // The web socket might be closed, if so we reopen it
        // and send the message when it is open again:
        if (ws.readyState === WebSocket.CLOSED) {
            ws.onopen = () => { send(message); };
            ws.open();
            return
        }
        ws.send(JSON.stringify(message));
    }

    function sendVMRequest(vmId) {
        send({[EVENT_TYPE]: GET_VM, [VM_ID]: vmId});
    }

    function processResponse(data) {
        // Now let's check the EventType: either a view model or a property change...
        if (data[EVENT_TYPE] === RETURN_GET_VM) {
            // We have a view model, so we can set it as the current view model:
            const viewModel = data[EVENT_PAYLOAD];
            const vmId = viewModel[VM_ID];

            if ( viewModelObservers[vmId] ) {
                viewModelObservers[vmId](viewModel);
                return;
            }

            console.log("Received view model: " + JSON.stringify(viewModel));
            const vm = new VM(
                            viewModel,
                            (propName, value) => {
                                send({
                                        [EVENT_TYPE]: SET_PROP,
                                        [VM_ID]: vmId,
                                        [PROP_NAME]: propName,
                                        [PROP_VALUE]: value,
                                    });
                            },
                            (propName, action) => {
                                propertyObservers[vmId + ":" + propName] = action;
                            },
                            (methodName, args, action) => {
                                let key = vmId + ":" + methodName;
                                if ( !methodObservers[key] ) methodObservers[key] = [];
                                methodObservers[key].push(action);
                                console.log("saved method observer: '" + vmId + ":" + methodName+"'");
                                send({
                                        [EVENT_TYPE]: CALL,
                                        [EVENT_PAYLOAD]: {
                                            [METHOD_NAME]: methodName,
                                            [METHOD_ARGS]: args
                                        },
                                        [VM_ID]: vmId
                                    });
                            }
                        );

            const session = new Session((vmId, action) => { viewModelObservers[vmId] = action; });
            frontend(session,vm);
        } else if (data[EVENT_TYPE] === RETURN_PROP) {
            // We look up the binding for the property change:
            const action = propertyObservers[data[EVENT_PAYLOAD][VM_ID] + ":" + data[EVENT_PAYLOAD][PROP_NAME]];
            // If we have a binding, we call it with the new value:
            if (action)
                action(data[EVENT_PAYLOAD][PROP_VALUE]);
        } else if (data[EVENT_TYPE] === CALL_RETURN) {
            const actions = methodObservers[data[EVENT_PAYLOAD][VM_ID] + ":" + data[EVENT_PAYLOAD][METHOD_NAME]];
            if ( actions ) {
                // We get and remove the first action from the list:
                let action = actions.shift();
                console.log("found method observer: '" + data[EVENT_PAYLOAD][VM_ID] + ":" + data[EVENT_PAYLOAD][METHOD_NAME] + "'");
                if ( action )
                    action(data[EVENT_PAYLOAD][METHOD_RETURNS]);
                console.log("Applied: " + JSON.stringify(data[EVENT_PAYLOAD][METHOD_RETURNS]));
            }
        }
    }
}
