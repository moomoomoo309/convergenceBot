import login from "facebook-chat-api";

let api = null;

function getApi(email, password) {
    login({email: email, password: password}, function (error, _api) {
        if (error)
            return console.error(error);
        api = _api
    });
    while (api === null)
        }
